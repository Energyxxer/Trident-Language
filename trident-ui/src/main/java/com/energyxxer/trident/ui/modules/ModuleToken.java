package com.energyxxer.trident.ui.modules;

import com.energyxxer.trident.ui.Tab;
import com.energyxxer.trident.ui.display.DisplayModule;
import com.energyxxer.trident.ui.styledcomponents.StyledPopupMenu;

import java.awt.*;
import java.util.Collection;

public interface ModuleToken {
    String getTitle();
    Image getIcon();
    String getHint();
    Collection<ModuleToken> getSubTokens();
    boolean isExpandable();
    DisplayModule createModule(Tab tab);
    void onInteract();
    StyledPopupMenu generateMenu();
    default String getSearchTerms() { return null; }

    String getIdentifier();

    boolean equals(ModuleToken other);
}
