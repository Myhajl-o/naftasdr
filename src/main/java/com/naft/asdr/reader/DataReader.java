package com.naft.asdr.reader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.naft.asdr.connection.GetConnection;
import com.naft.asdr.model.StorageData;

public class DataReader extends Thread {

  private final StorageData storage;
  private final Path path;

  public DataReader(StorageData storage) {
    this(storage, GetConnection.DEFAULT_PATH);
  }

  public DataReader(StorageData storage, Path path) {
    this.storage = storage;
    this.path = path;
  }

  private void waiting() throws InterruptedException {
    while (storage.getDataReady()) {
      Thread.sleep(1);
    }
  }

  @Override
  public void run() {
    try (WavReader reader = new WavReader(path)) {
      TransformSong transformer = new TransformSong(reader.isIq());
      while (!isInterrupted()) {
        List<Short> data = transformer.transform(WavReader.Samples.EMPTY);
        if (data == null) {
          if (reader.isFinished()) {
            break;
          }
          data = transformer.transform(reader.readSamples());
        }
        if (data == null) {
          Thread.sleep(1);
          continue;
        }
        waiting();
        storage.setData(data);
        storage.setDataReady(true);
      }
    } catch (IOException e) {
      System.err.println("Unable to read " + path + ": " + e.getMessage());
    } catch (InterruptedException e) {
      interrupt();
    }
  }
}
