package com.naft.asdr.reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

import static com.naft.asdr.reader.TransformSong.transform;
import com.naft.asdr.model.StorageData;

public class DataReader extends Thread {

  private BufferedReader reader;
  private StorageData storage;
  private List<Integer> data;

  public DataReader(BufferedReader reader, StorageData storage) {
    this.reader = reader;
    this.storage = storage;
  }

  private void waiting() throws InterruptedException {
    while (storage.getDataReady()) {
      Thread.sleep(1);
    }
  }

  private String reading() throws IOException {
    return reader.readLine();
  }

  @Override
  public void run() {
    while (true) {
      try {
        data = transform(reading());

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
