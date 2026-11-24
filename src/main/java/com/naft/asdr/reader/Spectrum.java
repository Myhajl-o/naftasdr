package com.naft.asdr.reader;

import java.util.ArrayList;
import java.util.List;

final class Spectrum {
  private static final int FFT_SIZE = 8192;
  private static final int WIDTH = 800;
  private static final double FLOOR_DBFS = -100;

  private Spectrum() {}

  static List<Short> levels(double[] inputReal, double[] inputImaginary, boolean iq) {
    double[] real = new double[FFT_SIZE];
    double[] imaginary = new double[FFT_SIZE];
    double windowSum = 0;
    for (int i = 0; i < inputReal.length; i++) {
      double window = 0.5 - 0.5 * Math.cos(2 * Math.PI * i / (inputReal.length - 1));
      real[i] = inputReal[i] * window;
      imaginary[i] = inputImaginary[i] * window;
      windowSum += window;
    }
    fft(real, imaginary);
    List<Short> result = new ArrayList<>(WIDTH);
    int bins = iq ? FFT_SIZE : FFT_SIZE / 2 + 1;
    for (int column = 0; column < WIDTH; column++) {
      double peak = 0;
      for (int bin = column * bins / WIDTH; bin < (column + 1) * bins / WIDTH; bin++) {
        // IQ: -sampleRate/2 ... +sampleRate/2; mono: 0 ... sampleRate/2.
        int index = iq ? (bin + FFT_SIZE / 2) % FFT_SIZE : bin;
        double amplitude = Math.hypot(real[index], imaginary[index]) / windowSum;
        if (!iq && index > 0 && index < FFT_SIZE / 2) {
          amplitude *= 2;
        }
        peak = Math.max(peak, amplitude); // Preserve narrow tones when combining bins.
      }
      // Fixed calibration: -100 dBFS -> -255, full scale -> 0. No per-frame auto gain.
      double dbfs = peak == 0 ? FLOOR_DBFS : 20 * Math.log10(peak);
      dbfs = Math.max(FLOOR_DBFS, Math.min(0, dbfs));
      result.add((short) Math.round(dbfs * 255 / -FLOOR_DBFS));
    }
    return result;
  }

  private static void fft(double[] real, double[] imaginary) {
    for (int i = 1, j = 0; i < FFT_SIZE; i++) {
      int bit = FFT_SIZE >> 1;
      while ((j & bit) != 0) {
        j ^= bit;
        bit >>= 1;
      }
      j ^= bit;
      if (i < j) {
        double temp = real[i];
        real[i] = real[j];
        real[j] = temp;
        temp = imaginary[i];
        imaginary[i] = imaginary[j];
        imaginary[j] = temp;
      }
    }
    for (int length = 2; length <= FFT_SIZE; length <<= 1) {
      double angle = -2 * Math.PI / length;
      double stepReal = Math.cos(angle);
      double stepImaginary = Math.sin(angle);
      for (int start = 0; start < FFT_SIZE; start += length) {
        double wr = 1;
        double wi = 0;
        for (int i = start; i < start + length / 2; i++) {
          int other = i + length / 2;
          double vr = real[other] * wr - imaginary[other] * wi;
          double vi = real[other] * wi + imaginary[other] * wr;
          real[other] = real[i] - vr;
          imaginary[other] = imaginary[i] - vi;
          real[i] += vr;
          imaginary[i] += vi;
          double nextWr = wr * stepReal - wi * stepImaginary;
          wi = wr * stepImaginary + wi * stepReal;
          wr = nextWr;
        }
      }
    }
  }
}
