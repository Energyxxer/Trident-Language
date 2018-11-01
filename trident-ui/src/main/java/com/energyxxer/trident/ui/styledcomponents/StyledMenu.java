package com.energyxxer.trident.ui.styledcomponents;

import com.energyxxer.trident.global.Commons;
import com.energyxxer.trident.ui.theme.change.ThemeListenerManager;
import com.energyxxer.util.Disposable;
import com.energyxxer.xswing.menu.XMenu;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

/**
 * Separator that reacts to window theme changes.
 */
public class StyledMenu extends XMenu implements Disposable {

    private ThemeListenerManager tlm = new ThemeListenerManager();

    private List<Disposable> disposableChildren = new ArrayList<>();

    static {
        UIManager.put("Menu.submenuPopupOffsetX",0);
        UIManager.put("Menu.submenuPopupOffsetY",0);
    }

    public StyledMenu(String text, String icon) {
        super(text);

        tlm.addThemeChangeListener(t -> {
            //this.setBackground(t.getColor(new Color(215, 215, 215), "General.menu.background"));
            this.setForeground(t.getColor(Color.BLACK, "General.menu.foreground","General.foreground"));
            this.setRolloverBackground(t.getColor(new Color(190, 190, 190), "General.menu.selected.background"));
            this.setFont(t.getFont("General.menu","General"));
            if(icon != null) this.setIcon(new ImageIcon(Commons.getIcon(icon).getScaledInstance(16,16, Image.SCALE_SMOOTH)));

            getPopupMenu().setBackground(t.getColor(new Color(215, 215, 215), "General.menu.background"));
            int borderThickness = Math.max(t.getInteger(1,"General.menu.border.thickness"),0);
            getPopupMenu().setBorder(BorderFactory.createMatteBorder(borderThickness, borderThickness, borderThickness, borderThickness ,t.getColor(new Color(200, 200, 200), "General.menu.border.color")));
        });
    }
    public StyledMenu(String text) {
        this(text, null);
    }
    public void addSeparator() {
        this.add(new StyledSeparator());
    }

    @Override
    public Component add(Component comp) {
        filterDisposable(comp);
        return super.add(comp);
    }

    @Override
    public JMenuItem add(JMenuItem menuItem) {
        filterDisposable(menuItem);
        return super.add(menuItem);
    }

    private void filterDisposable(Object obj) {
        if(obj instanceof Disposable) {
            disposableChildren.add((Disposable) obj);
        }
    }

    @Override
    public void dispose() {
        tlm.dispose();
        disposableChildren.forEach(Disposable::dispose);
        disposableChildren.clear();
    }
}