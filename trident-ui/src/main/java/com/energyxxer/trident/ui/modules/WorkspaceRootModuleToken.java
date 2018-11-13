package com.energyxxer.trident.ui.modules;

import com.energyxxer.trident.global.Preferences;
import com.energyxxer.trident.ui.Tab;
import com.energyxxer.trident.ui.display.DisplayModule;
import com.energyxxer.trident.ui.styledcomponents.StyledPopupMenu;
import com.energyxxer.util.FileUtil;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

public class WorkspaceRootModuleToken implements ModuleToken {

    private File root;

    @Override
    public String getTitle() {
        return "Workspace";
    }

    @Override
    public Image getIcon() {
        return null;
    }

    @Override
    public String getHint() {
        return null;
    }

    @Override
    public Collection<ModuleToken> getSubTokens() {
        this.root = new File(Preferences.get("workspace_dir", Preferences.DEFAULT_WORKSPACE_PATH));

        ArrayList<ModuleToken> subTokens = new ArrayList<>();
        if(root.exists()) {
            for(File file : FileUtil.listFilesOrdered(root)) {
                subTokens.add(new FileModuleToken(file));
            }
        }

        return subTokens;
    }

    @Override
    public boolean isExpandable() {
        return true;
    }

    @Override
    public DisplayModule createModule(Tab tab) {
        return null;
    }

    @Override
    public void onInteract() {

    }

    @Override
    public StyledPopupMenu generateMenu() {
        return null;
    }

    @Override
    public String getIdentifier() {
        return null;
    }

    @Override
    public boolean equals(ModuleToken other) {
        return false;
    }
}
