package com.naft.asdr.model;

import java.util.List;

public class StorageData {

  private boolean dataReady;
  private List<Short> data;

  public StorageData() {
    this.dataReady = false;
    this.data = null;
  }

  public void setDataReady(boolean dataReady) {
    this.dataReady = dataReady;
  }

  public boolean getDataReady() {
    return this.dataReady;
  }

  public void setData(List<Short> data) {
    this.data = data;
  }

  public List<Short> copyData() {
    return List.copyOf(this.data);
  }

}
