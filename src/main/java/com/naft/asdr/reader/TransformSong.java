package com.naft.asdr.reader;

import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

public class TransformSong {

  private static final Integer COUNT_FOR_COMPRESSING = 10;

  private static List<Short> compressing(List<Short> data, int count) {
    List<Short> newData = new ArrayList<>();
    for (int i = 0; i < data.size(); i += count) {
      if (data.get(i + count) == null) {
        return newData;
      }
      int sum = 0;
      float avg;
      for (int j = i; j < i + count; j++) {
        sum += data.get(j);
      }
      avg = (float) sum / (float) count;

      newData.add((short) Math.round(avg));

    }
    return newData;
  }

  public static List<Short> transform(ShortBuffer shortBuffer) {
    List<Short> data = new ArrayList<>();
    for (int i = 0; i < 8000 && shortBuffer.hasRemaining(); i++) {
      data.add(shortBuffer.get());
    }
    compressing(data, COUNT_FOR_COMPRESSING);
    return data;
  }
}
