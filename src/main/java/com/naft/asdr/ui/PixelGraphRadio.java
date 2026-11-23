package com.naft.asdr.ui;

import java.util.ArrayList;
import java.util.List;

import com.naft.asdr.model.Pixel;

public class PixelGraphRadio {

  private List<List<Pixel>> pixels = new ArrayList<>();
  private final Integer width = 800;
  private final Integer height = 50;

  private final Integer x = 100;
  private final Integer y = 200;

  private final Integer max = 255;
  private final Integer min = 0;

  private final Integer maxR = 0;
  private final Integer minR = -255;

  public PixelGraphRadio() {
    // pixels.forEach(pixel -> pixel.forEaсh(p -> p.add(new Pixel(x + j, y - i, min,
    // min, min))));

    for (int i = 0; i < height; i++) {
      List<Pixel> line = new ArrayList<>();
      for (int j = 0; j < width; j++) {
        line.add(new Pixel(x + j, y - i, min, min, min));
      }
      pixels.add(line);
    }

  }

  public List<List<Pixel>> getPixels() {
    return this.pixels;
  }

  private int convert(int num) {
    return num - max;
  }

  private int max(int a, int b) {
    return (a > b) ? a : b;
  }

  public void create(List<Integer> data, Integer db) {
    List<Integer> newData = new ArrayList<>();

    Integer coef = max / db;

    for (int i = 0; i < width; i++) {
      newData.add(max(convert(data.get(i)) + coef, minR));
    }

    for (int i = 0; i < height; i++) {
      if (pixels.get(i).get(0).y == height - 1) {
        pixels.remove(i);
      }
    }

    for (List<Pixel> pixel : pixels) {
      for (Pixel p : pixel) {
        p.y -= 1;
      }
    }
    // pixels.forEach(pixel -> pixel.forEaсh(p -> p.y-=1));

    List<Pixel> line = new ArrayList<>();
    for (int i = 0; i < width; i++) {
      line.add(new Pixel(i + x, y, newData.get(i), newData.get(i), newData.get(i)));
    }
    pixels.add(line);
  }
}
