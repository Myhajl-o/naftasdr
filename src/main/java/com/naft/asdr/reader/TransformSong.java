package com.naft.asdr.reader;

import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

public class TransformSong {

  private static final Integer COUNT_FOR_COMPRESSING = 10;

  private static List<Integer> compressing(List<Integer> data, int count) {
    List<Integer> newData = new ArrayList<>();
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

      newData.add(Math.round(avg));

    }
    return newData;
  }

  public static List<Integer> transform(String line) {
    Scanner scanf = new Scanner(line);
    List<Integer> data = new ArrayList<>();
    while (scanf.hasNextInt()) {
      data.add(scanf.nextInt());
    }
    scanf.close();
    compressing(data, COUNT_FOR_COMPRESSING);
    return data;
  }
}
