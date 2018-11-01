package com.energyxxer.trident.ui.decorationpane;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;

public abstract class DecorationObject {
    public int x, y, w, h;

    public DecorationObject(Point pos, Dimension size) {
        this(pos.x, pos.y, size.width, size.height);
    }

    public DecorationObject(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public boolean contains(Point p) {
        return p.x >= this.x && p.x < this.x + this.w && p.y >= this.y && p.y < this.y + this.h;
    }

    public void paint(Graphics g) {}

    public Cursor getCursor() {
        return Cursor.getDefaultCursor();
    }

    public String getToolTipText() {return null;}

    public void mouseClicked(MouseEvent e) {

    }

    public void mousePressed(MouseEvent e) {

    }

    public void mouseReleased(MouseEvent e) {

    }

    public void mouseEntered(MouseEvent e) {

    }

    public void mouseExited(MouseEvent e) {

    }
}
