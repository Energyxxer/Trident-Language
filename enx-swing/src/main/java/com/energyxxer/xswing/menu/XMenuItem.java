package com.energyxxer.xswing.menu;

import javax.swing.*;
import java.awt.*;

/**
 * Created by User on 12/9/2016.
 */
public class XMenuItem extends JMenuItem {
    protected Color background = new Color(0,0,0,0);
    protected Color foreground = UIManager.getColor("Menu.foreground");
    protected Color rolloverBackground = UIManager.getColor("Menu.selectionBackground");
    protected Color rolloverForeground = UIManager.getColor("Menu.selectionForeground");

    {
        super.setBackground(new Color(0,0,0,0));
        super.setOpaque(false);
        super.setContentAreaFilled(false);
        super.setBorderPainted(false);
    }

    public XMenuItem() {
    }

    public XMenuItem(Icon icon) {
        super(icon);
    }

    public XMenuItem(String text) {
        super(text);
    }

    public XMenuItem(Action a) {
        super(a);
    }

    public XMenuItem(String text, Icon icon) {
        super(text, icon);
    }

    public XMenuItem(String text, int mnemonic) {
        super(text, mnemonic);
    }

    public Color getRolloverBackground() {
        return rolloverBackground;
    }

    public void setRolloverBackground(Color rolloverBackground) {
        this.rolloverBackground = rolloverBackground;
    }

    public Color getRolloverForeground() {
        return rolloverForeground;
    }

    public void setRolloverForeground(Color rolloverForeground) {
        this.rolloverForeground = rolloverForeground;
    }

    @Override
    public Color getBackground() {
        return background;
    }

    @Override
    public void setBackground(Color background) {
        this.background = background;
    }

    @Override
    public Color getForeground() {
        return foreground;
    }

    @Override
    public void setForeground(Color foreground) {
        this.foreground = foreground;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Color currentBackground = getBackground();
        boolean armed = this.isArmed();

        if(armed) {
            super.setForeground(getRolloverForeground());
            currentBackground = getRolloverBackground();
            this.setArmed(false);
        } else {
            super.setForeground(getForeground());
        }

        Color defaultColor = g.getColor();

        g.setColor(currentBackground);
        g.fillRect(0,0,this.getWidth(),this.getHeight());
        g.setColor(defaultColor);
        super.paintComponent(g);
        this.setArmed(armed);
    }


}
