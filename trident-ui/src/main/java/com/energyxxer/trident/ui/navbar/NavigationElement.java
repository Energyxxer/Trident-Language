package com.energyxxer.trident.ui.navbar;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public abstract class NavigationElement implements MouseListener, MouseMotionListener {
    protected final NavigatorMaster master;
    protected boolean rollover;
    protected boolean selected;
    protected Rectangle bounds = null;

    public NavigationElement(NavigatorMaster master) {
        this.master = master;
    }

    public abstract Rectangle render(Graphics g, Point p);

    protected void setBounds(Rectangle bounds) {
        this.bounds = bounds;
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public NavigatorMaster getMaster() {
        return master;
    }

    public boolean isRollover() {
        return rollover;
    }

    public void setRollover(boolean rollover) {
        this.rollover = rollover;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
