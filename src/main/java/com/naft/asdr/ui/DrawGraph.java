package com.naft.asdr.ui;

import com.naft.asdr.model.Pixel;

import java.awt.*;
import java.util.List;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

public class DrawGraph extends JPanel {

  private List<List<Pixel>> pixels;
  private final BufferedImage canvas;

  public DrawGraph(List<List<Pixel>> pixels) {
    this.pixels = pixels;
    int width = 1;
    int height = 1;
    for (List<Pixel> line : pixels) {
      for (Pixel pixel : line) {
        width = Math.max(width, pixel.x + 1);
        height = Math.max(height, pixel.y + 1);
      }
    }
    this.canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    setPreferredSize(new Dimension(width, height));
    render();
  }

  public void updatePixels(List<List<Pixel>> pixels) {
    this.pixels = pixels;
    render();
    repaint();
  }

  private void render() {
    Graphics2D g = canvas.createGraphics();

    try {
      g.setColor(getBackground());
      g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
      for (List<Pixel> pix : pixels) {
        for (Pixel p : pix) {
          g.setColor(new Color(p.r, p.g, p.b));
          g.fillRect(p.x, p.y, 1, 1);
        }
      }
    } finally {
      g.dispose();
    }
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    g.drawImage(canvas, 0, 0, getWidth(), getHeight(), this);
  }

}
