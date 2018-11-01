package com.energyxxer.trident.ui.scrollbar;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Graphics;
import java.awt.Rectangle;

public class InvisibleScrollBarUI extends BasicScrollBarUI {

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        // Not doing this
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
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
