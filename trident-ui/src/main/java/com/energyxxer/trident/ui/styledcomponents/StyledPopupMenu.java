package com.energyxxer.trident.ui.styledcomponents;

import com.energyxxer.util.Disposable;
import com.energyxxer.trident.ui.theme.change.ThemeListenerManager;

import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by User on 12/14/2016.
 */
public class StyledPopupMenu extends JPopupMenu implements Disposable, PopupMenuListener {

    private String namespace = null;

    private ThemeListenerManager tlm = new ThemeListenerManager();

    private List<Disposable> disposableChildren = new ArrayList<>();

    public StyledPopupMenu() {
        this(null,null);
    }

    public StyledPopupMenu(String label) {
        this(label,null);
    }

    public StyledPopupMenu(String label, String namespace) {
        if(label != null) setLabel(label);
        if(namespace != null) this.setNamespace(namespace);

        tlm.addThemeChangeListener(t -> {
            if (this.namespace != null) {
                setBackground(t.getColor(new Color(215, 215, 215), this.namespace + ".menu.background", "General.menu.background"));
                int borderThickness = Math.max(t.getInteger(1,this.namespace + ".menu.border.thickness","General.menu.border.thickness"),0);
                setBorder(BorderFactory.createMatteBorder(borderThickness, borderThickness, borderThickness, borderThickness, t.getColor(new Color(200, 200, 200), this.namespace + ".menu.border.color", "General.menu.border.color")));
            } else {
                setBackground(t.getColor(new Color(215, 215, 215), "General.menu.background"));
                int borderThickness = Math.max(t.getInteger(1,"General.menu.border.thickness"),0);
                setBorder(BorderFactory.createMatteBorder(borderThickness, borderThickness, borderThickness, borderThickness ,t.getColor(new Color(200, 200, 200), "General.menu.border.color")));
            }
        });

        this.addPopupMenuListener(this);
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public void addSeparator() {
        this.add(new StyledSeparator(namespace));
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

    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {

    }

    @Override
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {

    }

    @Override
    public void popupMenuCanceled(PopupMenuEvent e) {
        dispose();
    }

    @Override
    public String toString() {
        return "StyledPopupMenu{" +
                "namespace='" + namespace + '\'' +
                '}';
    }
}
