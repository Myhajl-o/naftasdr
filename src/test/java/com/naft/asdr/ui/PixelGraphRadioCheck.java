package com.naft.asdr.ui;

import com.naft.asdr.model.Pixel;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/** Standalone regression checks; run main with the application classes on the classpath. */
public class PixelGraphRadioCheck {

  public static void main(String[] args) {
    List<Short> samples = new ArrayList<>();
    for (int i = 0; i < 800; i++) {
      samples.add((short) ((i % 400) * Short.MAX_VALUE / 399));
    }
    samples.set(1, (short) 16384);
    samples.set(2, (short) -16384);
    samples.set(3, Short.MAX_VALUE);
    samples.set(4, Short.MIN_VALUE);

    PixelGraphRadio graph = new PixelGraphRadio();
    List<Pixel> previous = null;
    int previousZeros = -1;
    for (short db : new short[] {0, -64, -128, -255}) {
      graph.create(samples, db);
      List<Pixel> line = graph.getPixels().getLast();
      int zeros = 0;
      for (int i = 0; i < line.size(); i++) {
        Pixel pixel = line.get(i);
        new Color(pixel.r, pixel.g, pixel.b);
        require(pixel.r == pixel.g && pixel.g == pixel.b, "Color is not grayscale");
        if (previous != null) {
          require(pixel.r <= previous.get(i).r, "Lower db increased brightness");
        }
        if (pixel.r == 0) {
          zeros++;
        }
      }
      require(zeros > previousZeros, "Lower db did not suppress more values");
      require(line.get(0).r == 0, "Silence is visible");
      require(line.get(1).r == line.get(2).r, "Opposite sample signs differ in brightness");
      require(line.get(3).r == 255 + db && line.get(4).r == 255 + db,
          "Full-scale sample overflow or incorrect offset");
      require(line.get(1).r == Math.max(0, 127 + db), "Incorrect noise offset");
      previous = line;
      previousZeros = zeros;
    }
    require(previousZeros == 800, "db=-255 must suppress every value");

    graph.create(samples, Short.MIN_VALUE);
    require(graph.getPixels().getLast().get(3).r == 0, "Lower db bound was not clamped");
    graph.create(samples, Short.MAX_VALUE);
    require(graph.getPixels().getLast().get(3).r == 255, "Upper db bound was not clamped");

    for (int frame = 0; frame < 200; frame++) {
      graph.create(samples, (short) -64);
      require(graph.getPixels().size() == 50, "History grew beyond 50 rows");
      for (int row = 0; row < 50; row++) {
        List<Pixel> line = graph.getPixels().get(row);
        require(line.size() == 800, "Incorrect row width");
        for (int column = 0; column < 800; column++) {
          Pixel pixel = line.get(column);
          require(pixel.x == 100 + column && pixel.y == 151 + row, "Incorrect scrolling coordinates");
        }
      }
    }
    System.out.println("Pixel graph checks passed.");
  }

  private static void require(boolean condition, String message) {
    if (!condition) {
      throw new AssertionError(message);
    }
  }
}
