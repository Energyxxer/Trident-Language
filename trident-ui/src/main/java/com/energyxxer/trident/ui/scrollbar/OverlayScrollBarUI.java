package com.energyxxer.trident.ui.scrollbar;

import com.energyxxer.trident.ui.theme.change.ThemeListenerManager;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Created by User on 12/13/2016.
 */
public class OverlayScrollBarUI extends BasicScrollBarUI {

    private int thumbSize = 10;
    private Color thumbColor;
    private Color thumbRolloverColor;

    private JScrollPane sp;

    private ThemeListenerManager tlm = new ThemeListenerManager();

    public OverlayScrollBarUI(JScrollPane sp) {
        super();
        this.sp = sp;

        tlm.addThemeChangeListener(t -> {
            thumbColor = t.getColor(new Color(0,0,0,50), "General.scrollbar.color");
            thumbRolloverColor = t.getColor(new Color(0,0,0,100), "General.scrollbar.hover.color");
            thumbSize = t.getInteger(10, "General.scrollbar.thickness");
            sp.repaint();
        });
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        // Not doing this
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        Color color = isThumbRollover() ? thumbRolloverColor : thumbColor;
        int orientation = scrollbar.getOrientation();
        int x = thumbBounds.x;
        int y = thumbBounds.y;

        int width = orientation == JScrollBar.VERTICAL ? thumbSize : thumbBounds.width;
        //width = Math.max(width, thumbSize);

        int height = orientation == JScrollBar.VERTICAL ? thumbBounds.height : thumbSize;
        //height = Math.max(height, thumbSize);

        Graphics2D graphics2D = (Graphics2D) g.create();
        graphics2D.setColor(color);
        graphics2D.fillRect(x, y, width, height);
        graphics2D.dispose();
    }

    @Override
    protected void setThumbBounds(int x, int y, int width, int height) {
        super.setThumbBounds(x,y,width,height);
        sp.repaint();
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return createZeroButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return createZeroButton();
    }

    private JButton createZeroButton() {
        JButton jbutton = new JButton();
        jbutton.setOpaque(false);
        jbutton.setFocusable(false);
        jbutton.setFocusPainted(false);
        jbutton.setBorderPainted(false);
        jbutton.setBorder(BorderFactory.createEmptyBorder());
        return jbutton;
    }
}
