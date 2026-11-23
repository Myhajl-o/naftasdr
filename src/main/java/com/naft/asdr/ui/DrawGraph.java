package com.naft.asdr.ui;

import com.naft.asdr.model.Pixel;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.awt.image.*;
import javax.swing.JPanel;

public class DrawGraph extends JPanel {

  List<List<Pixel>> pixels = List.of();
  private final BufferedImage canvas;

  public DrawGraph(List<List<Pixel>> pixels) {
    this.pixels = pixels;
    this.canvas = new BufferedImage();
  }

  private void render() {
    Graphics2D g = canvas.createGraphics();

    for (List<Pixel> pix : pixels) {
      for (Pixel p : pix) {
        g.setColor(new Color(p.r, p.g, p.b));
        g.fillRect(p.x, p.y, 1, 1);
      }
    }

    g.dispose();
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    repaint();
    g.drawImage(canvas, 0, 0, getWidth(), getHeight(), null);
  }

}
