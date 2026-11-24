package com.naft.asdr.reader;

import java.nio.ShortBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class TransformSong {

  private static final int BLOCK_SIZE = 8000;
  private static final int COUNT_FOR_COMPRESSING = 10;
  private final Deque<Short> tempData = new ArrayDeque<>();

  // Empty input lets the caller collect a complete block already in the buffer.
  public List<Short> transform(ShortBuffer shortBuffer) {
    while (shortBuffer.hasRemaining()) {
      tempData.addLast(shortBuffer.get());
    }
    if (tempData.size() < BLOCK_SIZE) {
      return null;
    }
    return compressing();
  }

  private List<Short> compressing() {
    List<Short> result = new ArrayList<>(BLOCK_SIZE / COUNT_FOR_COMPRESSING);
    for (int i = 0; i < BLOCK_SIZE; i += COUNT_FOR_COMPRESSING) {
      int sum = 0;
      for (int j = 0; j < COUNT_FOR_COMPRESSING; j++) {
        sum += tempData.removeFirst();
      }
      result.add((short) Math.round((float) sum / COUNT_FOR_COMPRESSING));
    }
    return result;
  }
}
