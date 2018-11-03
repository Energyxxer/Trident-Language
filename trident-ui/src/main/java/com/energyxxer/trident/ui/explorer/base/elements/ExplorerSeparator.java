package com.energyxxer.trident.ui.explorer.base.elements;

import com.energyxxer.trident.ui.explorer.base.ExplorerMaster;
import com.energyxxer.trident.ui.modules.ModuleToken;

import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * Created by User on 4/8/2017.
 */
public class ExplorerSeparator extends ExplorerElement {

    public ExplorerSeparator(ExplorerMaster master) {
        super(master);
    }

    @Override
    public void render(Graphics g) {
        master.getFlatList().add(this);

        g.setColor((this.rollover || this.selected) ? master.getColorMap().get("item.rollover.background") : master.getColorMap().get("item.background"));
        g.fillRect(0, master.getOffsetY(), master.getWidth(), this.getHeight());

        g.setColor(master.getColorMap().get("item.foreground"));
        g.fillRect(master.getWidth() / 10, master.getOffsetY() + ((this.getHeight() / 2) - 1), 8 * (master.getWidth() / 10), 2);

        master.setOffsetY(master.getOffsetY() + getHeight());
    }

    @Override
    public int getHeight() {
        return master.getRowHeight();
    }

    @Override
    public ModuleToken getToken() {
        return null;
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}
