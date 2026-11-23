package com.naft.asdr.reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import static com.naft.asdr.reader.TransformSong.transform;
import static com.naft.asdr.connection.GetConnection.getConnectInput;

import com.naft.asdr.model.StorageData;

public class DataReader extends Thread {

  private StorageData storage;
  private List<Short> data;
  private ShortBuffer shortBuffer;

  public DataReader(StorageData storage) {
    this.storage = storage;
  }

  private void waiting() throws InterruptedException {
    while (storage.getDataReady()) {
      Thread.sleep(1);
    }
  }

  private ShortBuffer reading(byte[] bytes) throws IOException {
    return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
  }

  @Override
  public void run() {
    while (true) {
      try {
        shortBuffer = reading(getConnectInput());

        data = transform(shortBuffer);

        if (!storage.getDataReady()) {
          storage.setData(data);
          storage.setDataReady(true);
        } else {
          waiting();
        }

      } catch (IOException e) {

      } catch (InterruptedException e) {

      }

    }

  }

}
