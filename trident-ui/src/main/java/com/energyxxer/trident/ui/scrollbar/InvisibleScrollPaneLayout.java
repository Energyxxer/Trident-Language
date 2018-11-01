package com.energyxxer.trident.ui.scrollbar;

import javax.swing.JScrollPane;
import javax.swing.ScrollPaneLayout;
import java.awt.Container;
import java.awt.Rectangle;

public class InvisibleScrollPaneLayout extends ScrollPaneLayout {

    public InvisibleScrollPaneLayout(JScrollPane sp) {

        sp.getVerticalScrollBar().setUI(new InvisibleScrollBarUI());
        sp.getHorizontalScrollBar().setUI(new InvisibleScrollBarUI());
        sp.getVerticalScrollBar().setUnitIncrement(20);
        sp.getHorizontalScrollBar().setUnitIncrement(20);
        sp.getVerticalScrollBar().setOpaque(false);
        sp.getHorizontalScrollBar().setOpaque(false);

        sp.setComponentZOrder(sp.getVerticalScrollBar(), 0);
        sp.setComponentZOrder(sp.getHorizontalScrollBar(), 1);
        sp.setComponentZOrder(sp.getViewport(), 2);
    }

    @Override
    public void layoutContainer(Container parent) {
        super.layoutContainer(parent);

        if(viewport != null) viewport.setBounds(parent.getBounds());

        if (vsb != null) {
            // vertical scroll bar
            Rectangle vsbR = new Rectangle();
            vsb.setBounds(vsbR);
        }

        if (hsb != null) {
            // horizontal scroll bar
            Rectangle hsbR = new Rectangle();
            hsb.setBounds(hsbR);
        }
    }
}
