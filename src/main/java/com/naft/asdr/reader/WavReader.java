package com.naft.asdr.reader;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/** Reads little-endian RIFF/RF64 audio. Two channels are interpreted as I and Q. */
public final class WavReader implements AutoCloseable {
  public record Samples(double[] real, double[] imaginary) {
    public static final Samples EMPTY = new Samples(new double[0], new double[0]);
  }

  private final InputStream input;
  private int channels;
  private int bits;
  private int format;
  private int frameBytes;
  private long sampleRate;
  private long remaining;
  private byte[] buffer;
  private int buffered;

  public WavReader(Path path) throws IOException {
    this(Files.newInputStream(path));
  }

  WavReader(InputStream input) throws IOException {
    this.input = input;
    try {
      readHeader();
      buffer = new byte[8192 + frameBytes];
    } catch (IOException | RuntimeException e) {
      try {
        input.close();
      } catch (IOException closeError) {
        e.addSuppressed(closeError);
      }
      throw e;
    }
  }

  public boolean isIq() {
    return channels == 2;
  }

  public long sampleRate() {
    return sampleRate;
  }

  public boolean isFinished() {
    return remaining == 0;
  }

  public Samples readSamples() throws IOException {
    if (isFinished()) {
      return Samples.EMPTY;
    }
    int count = input.read(buffer, buffered, (int) Math.min(8192, remaining));
    if (count < 0) {
      // A partially written recording can be continued by appending its remaining audio.
      return Samples.EMPTY;
    }
    remaining -= count;
    buffered += count;
    int frames = buffered / frameBytes;
    double[] real = new double[frames];
    double[] imaginary = new double[frames];
    ByteBuffer samples = ByteBuffer.wrap(buffer, 0, frames * frameBytes).order(ByteOrder.LITTLE_ENDIAN);
    for (int i = 0; i < frames; i++) {
      real[i] = decode(samples);
      imaginary[i] = isIq() ? decode(samples) : 0;
    }
    int used = frames * frameBytes;
    buffered -= used;
    System.arraycopy(buffer, used, buffer, 0, buffered);
    return new Samples(real, imaginary);
  }

  private double decode(ByteBuffer samples) throws IOException {
    double value;
    if (format == 3) {
      value = bits == 32 ? samples.getFloat() : samples.getDouble();
    } else {
      value = switch (bits) {
        case 8 -> (Byte.toUnsignedInt(samples.get()) - 128) / 128.0;
        case 16 -> samples.getShort() / 32768.0;
        case 24 -> (Byte.toUnsignedInt(samples.get())
            | Byte.toUnsignedInt(samples.get()) << 8 | samples.get() << 16) / 8388608.0;
        case 32 -> samples.getInt() / 2147483648.0;
        default -> throw new IOException("Unsupported PCM bit depth: " + bits);
      };
    }
    if (!Double.isFinite(value)) {
      throw new IOException("WAV contains a non-finite sample");
    }
    return value;
  }

  private void readHeader() throws IOException {
    ByteBuffer header = readExact(12);
    String container = id(header);
    boolean rf64 = container.equals("RF64");
    if (!rf64 && !container.equals("RIFF")) {
      throw new IOException("Expected a RIFF or RF64 WAV file");
    }
    header.getInt();
    if (!id(header).equals("WAVE")) {
      throw new IOException("Missing WAVE signature");
    }
    long rf64DataSize = -1;
    Map<String, ArrayDeque<Long>> sizes = new HashMap<>();
    boolean haveFormat = false;
    while (true) {
      ByteBuffer chunk = readExact(8);
      String name = id(chunk);
      long size = Integer.toUnsignedLong(chunk.getInt());
      if (size == 0xffffffffL) {
        if (!rf64 || rf64DataSize < 0) {
          throw new IOException("Missing RF64 ds64 sizes");
        }
        if (name.equals("data")) {
          size = rf64DataSize;
        } else {
          ArrayDeque<Long> entries = sizes.get(name);
          if (entries == null || entries.isEmpty()) {
            throw new IOException("Missing RF64 size for " + name);
          }
          size = entries.removeFirst();
        }
      }
      switch (name) {
        case "ds64" -> {
          if (!rf64 || size < 28) {
            throw new IOException("Invalid ds64 chunk");
          }
          ByteBuffer ds64 = readExact(28);
          positiveSize(ds64.getLong());
          rf64DataSize = positiveSize(ds64.getLong());
          positiveSize(ds64.getLong());
          long entries = Integer.toUnsignedLong(ds64.getInt());
          if (entries > (size - 28) / 12) {
            throw new IOException("Invalid ds64 size table");
          }
          for (long i = 0; i < entries; i++) {
            ByteBuffer entry = readExact(12);
            sizes.computeIfAbsent(id(entry), key -> new ArrayDeque<>())
                .addLast(positiveSize(entry.getLong()));
          }
          input.skipNBytes(size - 28 - entries * 12);
        }
        case "fmt " -> {
          if (size < 16) {
            throw new IOException("Incomplete WAV format");
          }
          int length = (int) Math.min(size, 40);
          parseFormat(readExact(length));
          input.skipNBytes(size - length);
          haveFormat = true;
        }
        case "data" -> {
          if (!haveFormat || (rf64 && rf64DataSize < 0) || size % frameBytes != 0) {
            throw new IOException("Missing WAV format/ds64 or incomplete audio frame");
          }
          remaining = size;
          return;
        }
        default -> input.skipNBytes(size);
      }
      input.skipNBytes(size & 1); // RIFF chunks are padded to an even byte boundary.
    }
  }

  private void parseFormat(ByteBuffer fmt) throws IOException {
    format = Short.toUnsignedInt(fmt.getShort());
    channels = Short.toUnsignedInt(fmt.getShort());
    sampleRate = Integer.toUnsignedLong(fmt.getInt());
    long byteRate = Integer.toUnsignedLong(fmt.getInt());
    frameBytes = Short.toUnsignedInt(fmt.getShort());
    bits = Short.toUnsignedInt(fmt.getShort());
    if (format == 0xfffe) {
      if (fmt.limit() < 40 || Short.toUnsignedInt(fmt.getShort()) < 22) {
        throw new IOException("Incomplete extensible WAV format");
      }
      int validBits = Short.toUnsignedInt(fmt.getShort());
      fmt.getInt(); // Channel mask; channel order remains I then Q.
      format = fmt.getInt();
      if (fmt.getShort() != 0 || fmt.getShort() != 0x10
          || fmt.getLong() != 0x719b3800aa000080L || validBits != bits) {
        throw new IOException("Unsupported extensible WAV format");
      }
    }
    boolean supported = format == 1 && (bits == 8 || bits == 16 || bits == 24 || bits == 32)
        || format == 3 && (bits == 32 || bits == 64);
    if (!supported || channels < 1 || channels > 2 || sampleRate == 0
        || frameBytes != channels * (bits / 8) || byteRate != sampleRate * frameBytes) {
      throw new IOException("Unsupported WAV: format=" + format + ", channels=" + channels + ", bits=" + bits);
    }
  }

  private ByteBuffer readExact(int count) throws IOException {
    byte[] bytes = input.readNBytes(count);
    if (bytes.length != count) {
      throw new EOFException("Incomplete WAV header");
    }
    return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
  }

  private static String id(ByteBuffer bytes) {
    byte[] value = new byte[4];
    bytes.get(value);
    return new String(value, StandardCharsets.US_ASCII);
  }

  private static long positiveSize(long size) throws IOException {
    if (size < 0) {
      throw new IOException("WAV size exceeds supported range");
    }
    return size;
  }

  @Override
  public void close() throws IOException {
    input.close();
  }
}
