package com.naft.asdr.reader;

import com.naft.asdr.model.StorageData;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Standalone checks. An optional WAV path also checks the project's three-tone IQ recording. */
public class ReaderPipelineCheck {
  public static void main(String[] args) throws Exception {
    checkFormats();
    checkBuffering();
    checkReader();
    if (args.length > 0) {
      checkRecording(Path.of(args[0]));
    }
    System.out.println("Reader pipeline checks passed.");
  }

  private static void checkFormats() throws Exception {
    for (boolean rf64 : new boolean[] {false, true}) {
      for (int bits : new int[] {8, 16, 24, 32}) {
        ByteBuffer audio = ByteBuffer.allocate(6 * bits / 8).order(ByteOrder.LITTLE_ENDIAN);
        for (double value : new double[] {-1, 0.5, 0, -0.5, 0.5, 0}) {
          long integer = bits == 8 ? (long) (value * 128 + 128) : (long) (value * (1L << (bits - 1)));
          for (int b = 0; b < bits / 8; b++) {
            audio.put((byte) (integer >> (8 * b)));
          }
        }
        // Force reads to end inside samples/frames, including 24-bit I/Q frames.
        byte[] wave = wav(audio.array(), audio.capacity(), 1, bits, 2, rf64);
        try (WavReader reader = new WavReader(new ByteArrayInputStream(wave) {
          @Override
          public synchronized int read(byte[] b, int off, int len) {
            return super.read(b, off, Math.min(len, 3));
          }
        })) {
          double[] expected = {-1, 0.5, 0, -0.5, 0.5, 0};
          int index = 0;
          while (!reader.isFinished()) {
            WavReader.Samples samples = reader.readSamples();
            for (int i = 0; i < samples.real().length; i++) {
              require(samples.real()[i] == expected[index++], "Incorrect PCM I value");
              require(samples.imaginary()[i] == expected[index++], "Incorrect PCM Q value");
            }
          }
          require(index == 6, "Header, padding or trailer was decoded as audio");
          require(reader.sampleRate() == 2048000 && reader.isIq(), "Incorrect WAV format");
        }
      }
    }
    for (int bits : new int[] {32, 64}) {
      ByteBuffer audio = ByteBuffer.allocate(2 * bits / 8).order(ByteOrder.LITTLE_ENDIAN);
      if (bits == 32) {
        audio.putFloat(-0.5f).putFloat(0.25f);
      } else {
        audio.putDouble(-0.5).putDouble(0.25);
      }
      try (WavReader reader = new WavReader(new ByteArrayInputStream(wav(audio.array(), audio.capacity(), 3, bits, 1, false)))) {
        WavReader.Samples samples = reader.readSamples();
        require(!reader.isIq() && samples.real()[0] == -0.5 && samples.real()[1] == 0.25,
            "Incorrect floating-point samples");
        require(samples.imaginary()[0] == 0, "Mono has an imaginary component");
      }
    }
    expectInvalid(new byte[12]);
    expectInvalid(Arrays.copyOf(wav(new byte[0], 0, 1, 8, 1, false), 20));
    expectInvalid(wav(new byte[0], 0, 7, 8, 1, false));
    expectInvalid(wav(new byte[1], 1, 1, 16, 1, false));
  }

  private static void checkBuffering() {
    TransformSong transformer = new TransformSong(true);
    WavReader.Samples tone = tone(24000, 0.125, 0.5);
    require(transformer.transform(slice(tone, 0, 3000)) == null, "Partial block escaped");
    require(transformer.transform(slice(tone, 3000, 7000)) == null, "Partial block escaped");
    List<Short> first = transformer.transform(slice(tone, 7000, 9000));
    checkPeak(first, 500);
    require(transformer.transform(WavReader.Samples.EMPTY) == null, "Remainder became a full block");
    checkPeak(transformer.transform(slice(tone, 9000, 24000)), 500);
    checkPeak(transformer.transform(WavReader.Samples.EMPTY), 500);
    require(transformer.transform(WavReader.Samples.EMPTY) == null, "Block repeated");
    TransformSong negative = new TransformSong(true);
    checkPeak(negative.transform(tone(8000, -0.125, 0.5)), 300);
    List<Short> quiet = new TransformSong(true).transform(tone(8000, 0.125, 0.05));
    require(first.get(500) - quiet.get(500) >= 49, "Automatic gain hides signal strength changes");
    List<Short> silence = new TransformSong(true).transform(tone(8000, 0, 0));
    require(silence.stream().allMatch(v -> v == -255), "Silence must be black");
    WavReader.Samples mono = tone(8000, 0.125, 0.5);
    checkPeak(new TransformSong(false).transform(new WavReader.Samples(mono.real(), new double[8000])), 200);
  }

  private static void checkReader() throws Exception {
    Path file = Files.createTempFile("naftasdr-reader-", ".wav");
    byte[] audio = pcm8(tone(16000, 0.125, 0.5));
    byte[] whole = wav(audio, audio.length, 1, 8, 2, true);
    // Find the audio offset, including the odd-sized JUNK chunk used by the fixture.
    int offset = whole.length - audio.length - 10;
    StorageData storage = new StorageData();
    storage.setData(List.of((short) -1));
    storage.setDataReady(true);
    DataReader reader = new DataReader(storage, file);
    reader.setDaemon(true);
    try {
      Files.write(file, Arrays.copyOf(whole, offset + 16001));
      reader.start();
      await(() -> reader.getState() == Thread.State.TIMED_WAITING);
      require(storage.copyData().equals(List.of((short) -1)), "Occupied storage was overwritten");
      storage.setDataReady(false);
      await(storage::getDataReady);
      checkPeak(storage.copyData(), 500);
      storage.setDataReady(false);
      await(() -> reader.getState() == Thread.State.TIMED_WAITING);
      require(!storage.getDataReady(), "Partial frame became a complete spectrum");
      Files.write(file, Arrays.copyOfRange(whole, offset + 16001, whole.length), StandardOpenOption.APPEND);
      await(storage::getDataReady);
      checkPeak(storage.copyData(), 500);
      reader.join(2000);
      require(!reader.isAlive(), "Reader did not finish at the end of the data chunk");
    } finally {
      reader.interrupt();
      reader.join(2000);
      Files.deleteIfExists(file);
    }
  }

  private static void checkRecording(Path path) throws Exception {
    try (WavReader reader = new WavReader(path)) {
      TransformSong transformer = new TransformSong(reader.isIq());
      int rows = 0;
      long blackBefore = 0;
      long blackAfter = 0;
      int[] columns = {243, 439, 575};
      while (!reader.isFinished() && rows < 100) {
        List<Short> row = transformer.transform(reader.readSamples());
        if (row == null) continue;
        require(row.size() == 800, "Wrong spectrum width");
        for (int column : columns) {
          int localPeak = column - 1;
          for (int i = column; i <= column + 1; i++) {
            if (row.get(i) > row.get(localPeak)) localPeak = i;
          }
          require(row.get(localPeak) > -70, "Expected tone missing near column " + column);
        }
        for (short value : row) {
          require(value >= -255 && value <= 0, "Level is outside UI range");
          if (value == -255) blackBefore++;
          if (value <= -155) blackAfter++;
        }
        if (rows == 0) {
          System.out.println("Recording: sampleRate=" + reader.sampleRate() + ", IQ=" + reader.isIq());
          System.out.println("Tone levels at columns 243, 439, 575: "
              + row.get(243) + ", " + row.get(439) + ", " + row.get(575));
        }
        rows++;
      }
      require(rows == 100 && blackAfter > blackBefore, "db=-100 does not suppress background");
      System.out.println("100 recording rows: black pixels at db=0: " + blackBefore
          + ", at db=-100: " + blackAfter + " / 80000");
    }
  }

  private static WavReader.Samples tone(int count, double frequency, double amplitude) {
    double[] real = new double[count];
    double[] imaginary = new double[count];
    for (int i = 0; i < count; i++) {
      real[i] = amplitude * Math.cos(2 * Math.PI * frequency * i);
      imaginary[i] = amplitude * Math.sin(2 * Math.PI * frequency * i);
    }
    return new WavReader.Samples(real, imaginary);
  }

  private static WavReader.Samples slice(WavReader.Samples samples, int start, int end) {
    return new WavReader.Samples(Arrays.copyOfRange(samples.real(), start, end),
        Arrays.copyOfRange(samples.imaginary(), start, end));
  }

  private static byte[] pcm8(WavReader.Samples samples) {
    byte[] bytes = new byte[samples.real().length * 2];
    for (int i = 0; i < samples.real().length; i++) {
      bytes[i * 2] = (byte) Math.round(samples.real()[i] * 128 + 128);
      bytes[i * 2 + 1] = (byte) Math.round(samples.imaginary()[i] * 128 + 128);
    }
    return bytes;
  }

  private static byte[] wav(byte[] audio, int declaredSize, int format, int bits, int channels, boolean rf64) throws IOException {
    ByteArrayOutputStream chunks = new ByteArrayOutputStream();
    if (rf64) {
      chunk(chunks, "ds64", ByteBuffer.allocate(28).order(ByteOrder.LITTLE_ENDIAN)
          .putLong(0).putLong(declaredSize).putLong(declaredSize / (channels * bits / 8)).putInt(0).array());
    }
    chunk(chunks, "JUNK", new byte[] {1, 2, 3});
    int align = channels * bits / 8;
    chunk(chunks, "fmt ", ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
        .putShort((short) format).putShort((short) channels).putInt(2048000)
        .putInt(2048000 * align).putShort((short) align).putShort((short) bits).array());
    chunks.write("data".getBytes(StandardCharsets.US_ASCII));
    chunks.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(rf64 ? -1 : declaredSize).array());
    chunks.write(audio);
    if (audio.length % 2 != 0) chunks.write(0);
    chunk(chunks, "LIST", new byte[] {9, 9});
    byte[] body = chunks.toByteArray();
    if (rf64) ByteBuffer.wrap(body).order(ByteOrder.LITTLE_ENDIAN).putLong(8, body.length + 4L);
    ByteBuffer file = ByteBuffer.allocate(12 + body.length).order(ByteOrder.LITTLE_ENDIAN);
    file.put((rf64 ? "RF64" : "RIFF").getBytes(StandardCharsets.US_ASCII));
    file.putInt(rf64 ? -1 : body.length + 4).put("WAVE".getBytes(StandardCharsets.US_ASCII)).put(body);
    return file.array();
  }

  private static void chunk(ByteArrayOutputStream out, String name, byte[] data) throws IOException {
    out.write(name.getBytes(StandardCharsets.US_ASCII));
    out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(data.length).array());
    out.write(data);
    if (data.length % 2 != 0) out.write(0);
  }

  private static void expectInvalid(byte[] bytes) throws Exception {
    try (WavReader ignored = new WavReader(new ByteArrayInputStream(bytes))) {
      throw new AssertionError("Invalid WAV was accepted");
    } catch (IOException expected) {
      // Invalid input must be reported, not silently interpreted as PCM.
    }
  }

  private static void checkPeak(List<Short> row, int expected) {
    require(row != null && row.size() == 800, "Expected 800 spectral levels");
    int peak = 0;
    for (int i = 0; i < row.size(); i++) {
      require(row.get(i) >= -255 && row.get(i) <= 0, "Invalid spectral level");
      if (row.get(i) > row.get(peak)) peak = i;
    }
    require(Math.abs(peak - expected) <= 1 && row.get(peak) > -30, "Wrong frequency/amplitude: " + peak);
  }

  private static void await(BooleanSupplier condition) throws InterruptedException {
    long deadline = System.nanoTime() + 2_000_000_000L;
    while (!condition.getAsBoolean()) {
      require(System.nanoTime() < deadline, "Timed out waiting for reader");
      Thread.sleep(1);
    }
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }
}
