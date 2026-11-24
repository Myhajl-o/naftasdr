package com.naft.asdr.reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.file.Path;
import java.util.List;

import com.naft.asdr.connection.GetConnection;
import com.naft.asdr.model.StorageData;

public class DataReader extends Thread {

  private final StorageData storage;
  private final Path path;
  private final TransformSong transformer = new TransformSong();

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

  private ShortBuffer reading(byte[] bytes) {
    return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
  }

  @Override
  public void run() {
    try (GetConnection connection = new GetConnection(path)) {
      ShortBuffer empty = ShortBuffer.allocate(0);
      while (!isInterrupted()) {
        List<Short> data = transformer.transform(empty);
        if (data == null) {
          data = transformer.transform(reading(connection.getConnectInput()));
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
