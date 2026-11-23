package com.naft.asdr.ui;

import javax.swing.*;

import com.naft.asdr.model.StorageData;

import java.awt.*;
import java.awt.event.*;

public class Interface extends JFrame implements ActionListener {

  private Container c;

  private StorageData data;
  private PixelGraphRadio generPixel = new PixelGraphRadio();

  private Integer db;

  /* Temp */
  private JLabel name;
  private JButton setDb;
  private JTextField tDb;
  private JButton exit;
  private JTextArea tout;

  public Interface(StorageData data) {

    this.data = data;
    this.db = 0;

    setTitle("naftasdr");
    setBounds(300, 90, 900, 600);
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setResizable(false);

    c = getContentPane();
    c.setLayout(null);

    name = new JLabel("naftasdr");
    name.setFont(new Font("Arial", Font.PLAIN, 24));
    name.setLocation(200, 20);
    name.setSize(200, 20);
    c.add(name);

    setDb = new JButton("Set Db");
    setDb.addActionListener(this);
    setDb.setFont(new Font("Arial", Font.PLAIN, 14));
    setDb.setLocation(600, 450);
    setDb.setSize(70, 40);
    c.add(setDb);

    tDb = new JTextField();
    tDb.setFont(new Font("Arial", Font.PLAIN, 14));
    tDb.setLocation(490, 450);
    tDb.setSize(100, 30);
    c.add(tDb);

    exit = new JButton("Exit");
    exit.addActionListener(this);
    exit.setFont(new Font("Arial", Font.PLAIN, 14));
    exit.setLocation(600, 500);
    exit.setSize(70, 40);
    c.add(exit);

    tout = new JTextArea();
    tout.setFont(new Font("Arial", Font.PLAIN, 14));
    tout.setSize(50, 20);
    tout.setLocation(50, 500);
    tout.setLineWrap(true);
    tout.setEditable(false);
    c.add(tout);

    setVisible(true);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (data.getDataReady()) {
      data.setDataReady(false);
      generPixel.create(data.copyData(), db);
    }

    // refresh = new Timer(50, e -> refreshGraph()); // ~20 кадрів/с
    // refresh.start();
    if (e.getSource() == setDb) {
      String temp = tDb.getText();
      tout.setText(temp);
      db = Integer.valueOf(temp);
    } else if (e.getSource() == exit) {
      System.exit(0);
    }

  }
}
