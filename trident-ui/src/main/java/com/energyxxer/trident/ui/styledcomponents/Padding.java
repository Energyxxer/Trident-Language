package com.energyxxer.trident.ui.styledcomponents;

import com.energyxxer.trident.ui.theme.change.ThemeListenerManager;

import java.awt.Dimension;

/**
 * Created by User on 5/12/2017.
 */
public class Padding extends com.energyxxer.xswing.Padding {

    private ThemeListenerManager tlm = new ThemeListenerManager();

    public Padding(int size, String... keys) {
        tlm.addThemeChangeListener(t -> {
            int realSize = t.getInteger(size, keys);
            Dimension dim = new Dimension(realSize, realSize);
            this.setPreferredSize(dim);
            this.setMaximumSize(dim);
        });
    }
}
