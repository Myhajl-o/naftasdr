package com.naft.asdr.connection;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class GetConnection implements AutoCloseable {

  public static final Path DEFAULT_PATH = Path.of("tempStorage/data.wav");
  private final InputStream input;
  private int pendingByte = -1;

  public GetConnection(Path path) throws IOException {
    input = Files.newInputStream(path);
  }

  public byte[] getConnectInput() throws IOException {
    byte[] bytes = new byte[16001];
    int offset = pendingByte < 0 ? 0 : 1;
    if (offset == 1) {
      bytes[0] = (byte) pendingByte;
    }
    int count = input.read(bytes, offset, 16000);
    if (count == -1) {
      // Retain the final byte until more data is appended to the file.
      return new byte[0];
    }
    int length = offset + count;
    pendingByte = -1;
    if (length % 2 != 0) {
      pendingByte = Byte.toUnsignedInt(bytes[--length]);
    }
    return Arrays.copyOf(bytes, length);
  }

  @Override
  public void close() throws IOException {
    input.close();
  }
}
