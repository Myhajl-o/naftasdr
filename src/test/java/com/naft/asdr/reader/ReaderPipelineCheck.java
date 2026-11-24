package com.naft.asdr.reader;

import com.naft.asdr.connection.GetConnection;
import com.naft.asdr.model.StorageData;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Standalone regression checks; run main with the reader classes on the classpath. */
public class ReaderPipelineCheck {

  public static void main(String[] args) throws Exception {
    checkBuffering();
    checkSplitBytes();
    checkReader();
    System.out.println("Reader pipeline checks passed.");
  }

  private static ShortBuffer samples(int start, int count) {
    short[] values = new short[count];
    for (int i = 0; i < count; i++) {
      values[i] = (short) (start + i);
    }
    return ShortBuffer.wrap(values);
  }

  private static byte[] bytes(int start, int count) {
    ByteBuffer result = ByteBuffer.allocate(count * 2).order(ByteOrder.LITTLE_ENDIAN);
    result.asShortBuffer().put(samples(start, count));
    return result.array();
  }

  private static void checkBlock(List<Short> block, int start) {
    require(block != null && block.size() == 800, "Expected 800 compressed values");
    for (int i = 0; i < block.size(); i++) {
      require(block.get(i) == start + i * 10 + 5, "Incorrect order or average at " + i);
    }
  }

  private static void checkBuffering() {
    TransformSong transformer = new TransformSong();
    require(transformer.transform(samples(0, 0)) == null, "Empty input produced data");
    require(transformer.transform(samples(0, 3000)) == null, "Partial block was published");
    require(transformer.transform(samples(3000, 4000)) == null, "Partial block was published");
    checkBlock(transformer.transform(samples(7000, 2000)), 0);
    require(transformer.transform(samples(0, 0)) == null, "Remainder became a full block");
    checkBlock(transformer.transform(samples(9000, 15000)), 8000);
    checkBlock(transformer.transform(samples(0, 0)), 16000);
    require(transformer.transform(samples(0, 0)) == null, "Block was repeated");

    TransformSong independent = new TransformSong();
    require(transformer.transform(samples(0, 7999)) == null, "7999 is not a complete block");
    require(independent.transform(samples(0, 1)) == null, "Buffers are shared between readers");
    checkBlock(transformer.transform(samples(7999, 1)), 0);
    checkBlock(new TransformSong().transform(samples(-8000, 8000)), -8000);
  }

  private static void checkSplitBytes() throws Exception {
    Path file = Files.createTempFile("naftasdr-bytes-", ".pcm");
    byte[] expected = bytes(-1000, 16001);
    try {
      Files.write(file, Arrays.copyOf(expected, 1));
      try (GetConnection connection = new GetConnection(file)) {
        require(connection.getConnectInput().length == 0, "Incomplete sample escaped");
        require(connection.getConnectInput().length == 0, "EOF repeated data");
        Files.write(file, Arrays.copyOfRange(expected, 1, expected.length), StandardOpenOption.APPEND);
        int offset = 0;
        while (offset < expected.length) {
          byte[] chunk = connection.getConnectInput();
          require(chunk.length > 0 && chunk.length % 2 == 0, "Invalid sample boundary");
          require(Arrays.equals(chunk, Arrays.copyOfRange(expected, offset, offset + chunk.length)),
              "Bytes were lost, reordered or repeated");
          offset += chunk.length;
        }
        require(connection.getConnectInput().length == 0, "EOF repeated data");
      }
    } finally {
      Files.deleteIfExists(file);
    }
  }

  private static void checkReader() throws Exception {
    Path file = Files.createTempFile("naftasdr-reader-", ".pcm");
    StorageData storage = new StorageData();
    storage.setData(List.of((short) -1));
    storage.setDataReady(true);
    Files.write(file, bytes(0, 17000));
    DataReader reader = new DataReader(storage, file);
    reader.setDaemon(true);
    try {
      reader.start();
      await(() -> reader.getState() == Thread.State.TIMED_WAITING);
      require(storage.copyData().equals(List.of((short) -1)), "Occupied storage was overwritten");
      storage.setDataReady(false);
      await(storage::getDataReady);
      checkBlock(storage.copyData(), 0);
      storage.setDataReady(false);
      await(storage::getDataReady);
      checkBlock(storage.copyData(), 8000);
      storage.setDataReady(false);
      await(() -> reader.getState() == Thread.State.TIMED_WAITING);
      require(!storage.getDataReady(), "Partial block or duplicate was published");
      Files.write(file, bytes(17000, 7000), StandardOpenOption.APPEND);
      await(storage::getDataReady);
      checkBlock(storage.copyData(), 16000);
    } finally {
      reader.interrupt();
      reader.join(2000);
      Files.deleteIfExists(file);
    }
    require(!reader.isAlive(), "Reader did not stop after interruption");
  }

  private static void await(BooleanSupplier condition) throws InterruptedException {
    long deadline = System.nanoTime() + 2_000_000_000L;
    while (!condition.getAsBoolean()) {
      require(System.nanoTime() < deadline, "Timed out waiting for reader");
      Thread.sleep(1);
    }
  }

  private static void require(boolean condition, String message) {
    if (!condition) {
      throw new AssertionError(message);
    }
  }
}
