package com.naft.asdr;

import com.naft.asdr.model.StorageData;
import com.naft.asdr.reader.DataReader;
import static com.naft.asdr.connection.GetConnection.getConnectInput;

public class Main {

  private static StorageData storage = new StorageData();

  public static void main(String... arg) {
    Thread reader = new DataReader(getConnectInput(), storage);

    reader.start();

  }
}
