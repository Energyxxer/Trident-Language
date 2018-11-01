package com.energyxxer.trident.ui.styledcomponents;

import com.energyxxer.util.Disposable;
import com.energyxxer.trident.ui.theme.change.ThemeListenerManager;

import javax.swing.JSeparator;
import java.awt.Color;

/**
 * Separator that reacts to window theme changes.
 */
public class StyledSeparator extends JSeparator implements Disposable {

    private String namespace = null;

    private ThemeListenerManager tlm = new ThemeListenerManager();

    public StyledSeparator() {
        this(null);
    }

    public StyledSeparator(String namespace) {
        if(namespace != null) setNamespace(namespace);
        this.setOpaque(false);
        this.setBackground(new Color(0,0,0,0));
        tlm.addThemeChangeListener(t -> {
            if(this.namespace != null) {
                this.setForeground(t.getColor(new Color(150, 150, 150), this.namespace + ".menu.separator","General.menu.separator"));
            } else {
                this.setForeground(t.getColor(new Color(150, 150, 150), "General.menu.separator"));
            }
        });
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getNamespace() {
        return this.namespace;
    }

    @Override
    public void dispose() {
        tlm.dispose();
    }
}