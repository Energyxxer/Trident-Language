package com.energyxxer.xswing.menu;

import javax.swing.*;
import java.awt.*;

/**
 * Created by User on 12/9/2016.
 */
public class XMenu extends JMenu {
    protected Color background = new Color(0,0,0,0);
    protected Color foreground = UIManager.getColor("Menu.foreground");
    protected Color rolloverBackground = UIManager.getColor("Menu.selectionBackground");
    protected Color rolloverForeground = UIManager.getColor("Menu.selectionForeground");

    {
        super.setBackground(new Color(0,0,0,0));
        super.setOpaque(false);
        super.setContentAreaFilled(false);
        super.setBorderPainted(false);
        //super.getPopupMenu().setBorderPainted(false);
        this.getPopupMenu().setBorder(BorderFactory.createEmptyBorder());
    }

    public XMenu() {
    }

    public XMenu(String s) {
        super(s);
    }

    public XMenu(Action a) {
        super(a);
    }

    public XMenu(String s, boolean b) {
        super(s, b);
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
        ButtonModel model = this.getModel();

        boolean rollover = model.isRollover();
        boolean armed = model.isArmed();
        boolean pressed = model.isPressed();
        boolean selected = model.isSelected();

        if(rollover || armed || pressed || selected) {
            super.setForeground(getRolloverForeground());
            currentBackground = getRolloverBackground();
            model.setRollover(false);
            model.setArmed(false);
            model.setPressed(false);
            model.setSelected(false);
        } else {
            super.setForeground(getForeground());
        }

        Color defaultColor = g.getColor();

        g.setColor(currentBackground);
        g.fillRect(0,0,this.getWidth(),this.getHeight());
        g.setColor(defaultColor);
        super.paintComponent(g);

        model.setRollover(rollover);
        model.setArmed(armed);
        model.setPressed(pressed);
        model.setSelected(selected);
    }


}