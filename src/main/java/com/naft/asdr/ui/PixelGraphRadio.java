package com.naft.asdr.ui;

import java.util.ArrayList;
import java.util.List;

import com.naft.asdr.model.Pixel;

public class PixelGraphRadio {

  private final List<List<Pixel>> pixels = new ArrayList<>();
  private final short width = 800;
  private final short height = 50;

  private final short x = 100;
  private final short y = 200;

  private final short max = 255;
  private final short min = 0;

  private final short maxR = 0;
  private final short minR = -255;

  public PixelGraphRadio() {
    for (short i = 0; i < height; i++) {
      List<Pixel> line = new ArrayList<>();
      for (short j = 0; j < width; j++) {
        line.add(new Pixel((short) (x + j), (short) (y - height + 1 + i), min, min, min));
      }
      pixels.add(line);
    }

  }

  public List<List<Pixel>> getPixels() {
    return this.pixels;
  }

  private short convert(short num) {
    // Widen before taking the absolute value: abs(-32768) does not fit in short.
    return (short) Math.min(max, Math.abs((int) num) * max / Short.MAX_VALUE);
  }

  public void create(List<Short> data, short db) {
    if (data.size() != width) {
      throw new IllegalArgumentException("Expected " + width + " samples");
    }
    short noiseOffset = (short) Math.max(minR, Math.min(maxR, db));

    List<Pixel> line = new ArrayList<>();
    for (short i = 0; i < width; i++) {
      short brightness = (short) Math.max(min, convert(data.get(i)) + noiseOffset);
      line.add(new Pixel((short) (x + i), y, brightness, brightness, brightness));
    }

    pixels.remove(0);
    for (List<Pixel> pixel : pixels) {
      for (Pixel p : pixel) {
        p.y = (short) (p.y - 1);
      }
    }
    pixels.add(line);
  }
}
