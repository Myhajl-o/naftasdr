package com.naft.asdr.connection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class GetConnection {

  private static String path = "tempStorage/data.txt";

  public static BufferedReader getConnectInput() {
    File file = new File(path);

    try (BufferedReader buffer = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {

      return buffer;

    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }

  }
}
