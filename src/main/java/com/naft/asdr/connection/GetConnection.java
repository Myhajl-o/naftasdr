package com.naft.asdr.connection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class GetConnection {

  private static String path = "tempStorage/data.txt";

  public static byte[] getConnectInput() {
    try {
      return Files.readAllBytes(Path.of(path));
    } catch (IOException e) {
      return null;
    }
  }
}
