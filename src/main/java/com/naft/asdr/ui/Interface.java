package com.naft.asdr.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Interface extends JFrame implements ActionListener {

  private Container c;
  private JLabel name;
  private JButton setDb;
  private JTextField tDb;
  private String Db;
  private JButton exit;
  private JTextArea tout;

  public Interface() {
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
    setDb.setFont(new Font("Arial", Font.PLAIN, 14));
    setDb.setLocation(300, 400);
    setDb.setSize(100, 50);
    c.add(setDb);

    tDb = new JTextField();
    tDb.setFont(new Font("Arial", Font.PLAIN, 14));
    tDb.setSize(90, 400);
    tDb.setLocation(200, 50);
    c.add(tDb);

    exit = new JButton("Exit");
    exit.setFont(new Font("Arial", Font.PLAIN, 14));
    exit.setLocation(300, 500);
    exit.setSize(100, 50);

    tout = new JTextArea();
    tout.setFont(new Font("Arial", Font.PLAIN, 14));
    tout.setSize(300, 100);
    tout.setLocation(50, 100);
    tout.setLineWrap(true);
    tout.setEditable(false);
    c.add(tout);

  }

  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == setDb) {
      Db = tDb.getText();
      tout.setText(Db);
    } else if (e.getSource() == exit) {
      System.exit(0);
    }

  }
}
