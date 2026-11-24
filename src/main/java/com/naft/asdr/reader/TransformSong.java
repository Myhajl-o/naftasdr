package com.naft.asdr.reader;

import java.util.ArrayDeque;
import java.util.List;

/** Accumulates 8000 decoded frames before producing a complete spectrum row. */
public class TransformSong {
  private static final int BLOCK_SIZE = 8000;
  private final double[] real = new double[BLOCK_SIZE];
  private final double[] imaginary = new double[BLOCK_SIZE];
  private final ArrayDeque<List<Short>> ready = new ArrayDeque<>();
  private final boolean iq;
  private int buffered;

  public TransformSong(boolean iq) {
    this.iq = iq;
  }

  public List<Short> transform(WavReader.Samples samples) {
    if (samples.real().length != samples.imaginary().length) {
      throw new IllegalArgumentException("I and Q must have the same number of samples");
    }
    int offset = 0;
    while (offset < samples.real().length) {
      int count = Math.min(BLOCK_SIZE - buffered, samples.real().length - offset);
      System.arraycopy(samples.real(), offset, real, buffered, count);
      System.arraycopy(samples.imaginary(), offset, imaginary, buffered, count);
      buffered += count;
      offset += count;
      if (buffered == BLOCK_SIZE) {
        ready.addLast(Spectrum.levels(real, imaginary, iq));
        buffered = 0;
      }
    }
    // Empty input drains complete queued rows, but never publishes an incomplete block.
    return ready.pollFirst();
  }
}
