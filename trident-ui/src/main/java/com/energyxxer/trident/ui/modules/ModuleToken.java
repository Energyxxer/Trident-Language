package com.energyxxer.trident.ui.modules;

import com.energyxxer.trident.ui.Tab;
import com.energyxxer.trident.ui.display.DisplayModule;
import com.energyxxer.trident.ui.styledcomponents.StyledPopupMenu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface ModuleToken {
    String getTitle();
    java.awt.Image getIcon();
    String getHint();
    Collection<ModuleToken> getSubTokens();
    boolean isExpandable();
    DisplayModule createModule(Tab tab);
    void onInteract();
    StyledPopupMenu generateMenu();
    default String getSearchTerms() { return null; }

    String getIdentifier();

    boolean equals(ModuleToken other);

    class Static {
        public static List<ModuleTokenFactory> tokenFactories = new ArrayList<>();

        static {
            tokenFactories.add(FileModuleToken.factory);
        }

        public static ModuleToken createFromIdentifier(String identifier) {
            for(ModuleTokenFactory factory : tokenFactories) {
                ModuleToken created = factory.createFromIdentifier(identifier);
                if(created != null) return created;
            }
            return null;
        }
    }
}
