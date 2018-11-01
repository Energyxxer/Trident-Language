package com.energyxxer.trident.ui.styledcomponents;

import com.energyxxer.trident.global.Commons;
import com.energyxxer.trident.ui.theme.change.ThemeListenerManager;
import com.energyxxer.util.Disposable;
import com.energyxxer.xswing.menu.XMenuItem;

import javax.swing.ImageIcon;
import java.awt.Color;
import java.awt.Image;

/**
 * Menu item that reacts to window theme changes.
 */
public class StyledMenuItem extends XMenuItem implements Disposable {

    private ThemeListenerManager tlm = new ThemeListenerManager();
    private String icon = null;

    public StyledMenuItem(String text, String icon) {
        if(text != null) setText(text);
        this.icon = icon;
        tlm.addThemeChangeListener(t -> {
            this.setRolloverBackground(t.getColor(new Color(190, 190, 190), "General.menu.selected.background"));
            this.setForeground(t.getColor(Color.BLACK, "General.menu.foreground","General.foreground"));
            this.setFont(t.getFont("General.menu","General"));
            updateIcon();
        });
    }
    public StyledMenuItem(String text) {
        this(text, null);
    }
    public StyledMenuItem() {this(null,null);}

    private void updateIcon() {
        if(this.icon != null) this.setIcon(new ImageIcon(Commons.getIcon(icon).getScaledInstance(16,16, Image.SCALE_SMOOTH)));
    }

    public void setIconName(String icon) {
        this.icon = icon;
        updateIcon();
    }

    public String getIconName() {
        return icon;
    }

    @Override
    public void dispose() {
        tlm.dispose();
    }
}