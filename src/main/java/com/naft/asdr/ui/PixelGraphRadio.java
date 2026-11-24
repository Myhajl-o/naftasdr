package com.naft.asdr.ui;

import java.util.ArrayList;
import java.util.List;

import com.naft.asdr.model.Pixel;

public class PixelGraphRadio {

  private final List<List<Pixel>> pixels = new ArrayList<>();
  private final short width = 800;
  private final short height = 50;

  private final short max = 255;
  private final short min = 0;

  private final short maxR = 0;
  private final short minR = -255;

  public PixelGraphRadio() {
    for (short i = 0; i < height; i++) {
      List<Pixel> line = new ArrayList<>();
      for (short j = 0; j < width; j++) {
        line.add(new Pixel(j, i, min, min, min));
      }
      pixels.add(line);
    }

  }

  public List<List<Pixel>> getPixels() {
    return this.pixels;
  }

  public void create(List<Short> data, short db) {
    if (data.size() != width) {
      throw new IllegalArgumentException("Expected " + width + " samples");
    }

    List<Pixel> line = new ArrayList<>();
    for (short i = 0; i < width; i++) {
      short brightness = (short) Math.max(
          0, Math.min(255, data.get(i) + 255 + db));
      line.add(new Pixel(i, (short) (height - 1), brightness, brightness, brightness));
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
