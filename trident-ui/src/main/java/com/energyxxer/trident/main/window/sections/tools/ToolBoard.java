package com.energyxxer.trident.main.window.sections.tools;

import com.energyxxer.trident.ui.navbar.NavigationItem;

import javax.swing.JPanel;

public abstract class ToolBoard extends JPanel {
    protected ToolBoardMaster parent;
    protected NavigationItem navbarItem;

    public ToolBoard(ToolBoardMaster parent) {
        this.parent = parent;
        this.navbarItem = new NavigationItem(parent.getNavbar(), this.getIconName());
        this.navbarItem.setHintText(this.getName());
        this.navbarItem.addSelectAction(this::open);

        parent.getNavbar().addElement(this.navbarItem);
    }

    public void open() {
        parent.open(this);
    }

    public abstract String getName();

    public abstract String getIconName();
}
