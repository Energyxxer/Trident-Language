package com.energyxxer.trident.ui.tablist;

import com.energyxxer.trident.ui.theme.change.ThemeChangeListener;

import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public abstract class TabListElement implements MouseListener, MouseMotionListener, ThemeChangeListener {
    protected final TabListMaster master;
    protected boolean selected;
    protected boolean rollover;

    protected int lastRecordedOffset = 0;

    public TabListElement(TabListMaster master) {
        this.master = master;
    }

    public abstract void render(Graphics g);
    public abstract int getWidth();
    public int getLastRecordedOffset() {
        return lastRecordedOffset;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isRollover() {
        return rollover;
    }

    public void setRollover(boolean rollover) {
        this.rollover = rollover;
    }

    public abstract boolean select(MouseEvent e);

    public abstract String getToolTipText();
}
