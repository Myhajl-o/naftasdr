package com.naft.asdr;

import com.naft.asdr.model.StorageData;
import com.naft.asdr.reader.DataReader;
import com.naft.asdr.ui.Interface;

public class Main {

  private static StorageData storage = new StorageData();

  public static void main(String... arg) {
    Thread reader = new DataReader(storage);
    Interface ui = new Interface(storage);
    reader.start();

  }
}
