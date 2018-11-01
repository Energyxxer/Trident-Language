package com.energyxxer.trident.ui.styledcomponents;

import com.energyxxer.trident.global.Commons;
import com.energyxxer.trident.ui.theme.change.ThemeListenerManager;
import com.energyxxer.xswing.XIcon;

import java.awt.image.BufferedImage;

/**
 * Created by User on 2/11/2017.
 */
public class StyledIcon extends XIcon {

    private ThemeListenerManager tlm = new ThemeListenerManager();

    public StyledIcon(String icon) {
        this(icon, -1, -1, -1);
    }

    public StyledIcon(String icon, int width, int height, int hints) {
        if(width + height < 0) {
            tlm.addThemeChangeListener(t -> {
                this.setImage((BufferedImage) Commons.getIcon(icon).getScaledInstance(width, height, hints));
            });
        } else {
            tlm.addThemeChangeListener(t -> {
                this.setImage(Commons.getIcon(icon));
            });
        }
    }
}
