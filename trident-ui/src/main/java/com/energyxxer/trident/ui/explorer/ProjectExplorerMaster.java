package com.energyxxer.trident.ui.explorer;

import com.energyxxer.commodore.util.MinecraftUtils;
import com.energyxxer.trident.global.Commons;
import com.energyxxer.trident.global.Preferences;
import com.energyxxer.trident.global.temp.projects.ProjectManager;
import com.energyxxer.trident.ui.explorer.base.ExplorerFlag;
import com.energyxxer.trident.ui.explorer.base.ExplorerMaster;
import com.energyxxer.trident.ui.explorer.base.elements.ExplorerSeparator;
import com.energyxxer.trident.ui.theme.change.ThemeListenerManager;
import com.energyxxer.util.logger.Debug;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;

/**
 * Created by User on 5/16/2017.
 */
public class ProjectExplorerMaster extends ExplorerMaster {
    private File root;

    private ThemeListenerManager tlm = new ThemeListenerManager();

    public static final ExplorerFlag
            FLATTEN_EMPTY_PACKAGES = new ExplorerFlag("Flatten Empty Packages"),
            SHOW_PROJECT_FILES = new ExplorerFlag("Show Project Files");

    public ProjectExplorerMaster() {
        updateRoot();
        tlm.addThemeChangeListener(t -> {
            colors.put("background",t.getColor(Color.WHITE, "Explorer.background"));
            colors.put("item.background",t.getColor(new Color(0,0,0,0), "Explorer.item.background"));
            colors.put("item.foreground",t.getColor(Color.BLACK, "Explorer.item.foreground","General.foreground"));
            colors.put("item.selected.background",t.getColor(Color.BLUE, "Explorer.item.selected.background","Explorer.item.background"));
            colors.put("item.selected.foreground",t.getColor(Color.BLACK, "Explorer.item.selected.foreground","Explorer.item.hover.foreground","Explorer.item.foreground","General.foreground"));
            colors.put("item.rollover.background",t.getColor(new Color(0,0,0,0), "Explorer.item.hover.background","Explorer.item.background"));
            colors.put("item.rollover.foreground",t.getColor(Color.BLACK, "Explorer.item.hover.foreground","Explorer.item.foreground","General.foreground"));

            rowHeight = Math.max(t.getInteger(20,"Explorer.item.height"), 1);
            indentPerLevel = Math.max(t.getInteger(20,"Explorer.item.indent"), 0);
            initialIndent = Math.max(t.getInteger(0,"Explorer.item.initialIndent"), 0);

            selectionStyle = t.getString("Explorer.item.selectionStyle","default:FULL");
            selectionLineThickness = Math.max(t.getInteger(2,"Explorer.item.selectionLineThickness"), 0);

            this.setFont(t.getFont("Explorer.item","General"));

            assets.put("expand", Commons.getIcon("triangle_right").getScaledInstance(16, 16, Image.SCALE_SMOOTH));
            assets.put("collapse",Commons.getIcon("triangle_down").getScaledInstance(16, 16, Image.SCALE_SMOOTH));
        });

        explorerFlags.put(FLATTEN_EMPTY_PACKAGES, Preferences.get("explorer.flatten_empty_packages","true").equals("true"));
        explorerFlags.put(SHOW_PROJECT_FILES, Preferences.get("explorer.show_project_files","false").equals("true"));
        explorerFlags.put(ExplorerFlag.DEBUG_WIDTH, Preferences.get("explorer.debug_width","false").equals("true"));

        refresh();
    }

    private void updateRoot() {
        this.root = new File(Preferences.get("workspace_dir", Preferences.DEFAULT_WORKSPACE_PATH));
    }

    @Override
    public void refresh() {
        ProjectManager.setWorkspaceDir(Preferences.get("workspace_dir", Preferences.DEFAULT_WORKSPACE_PATH));
        ProjectManager.loadWorkspace();

        updateRoot();

        clearSelected();
        refresh(new ArrayList<>(this.getExpandedElements()));
    }

    private void refresh(ArrayList<String> toOpen) {
        children.clear();
        flatList.clear();
        this.getExpandedElements().clear();

        File[] subfiles = root.listFiles();
        if(subfiles == null) return;

        ArrayList<File> subfiles1 = new ArrayList<>();

        for(File f : subfiles) {
            if(f.isDirectory()) {
                this.children.add(new ProjectExplorerItem(this, f, toOpen));
            } else {
                subfiles1.add(f);
            }
        }
        for(File f : subfiles1) {
            this.children.add(new ProjectExplorerItem(this, f, toOpen));
        }

        this.children.add(new ExplorerSeparator(this));

        File[] resourceFiles = new File(System.getProperty("user.home") + File.separator + "Trident").listFiles();
        if(resourceFiles != null) {
            for(File f : resourceFiles) {
                this.children.add(new ProjectExplorerItem(this, f, toOpen));
            }
        }

        this.children.add(new ExplorerSeparator(this));

        File[] minecraftFiles = new File(MinecraftUtils.getMinecraftDir() + File.separator + "saves" + File.separator + "Snapshot Tests" + File.separator + "datapacks").listFiles();
        if(minecraftFiles != null) {
            for(File f : minecraftFiles) {
                this.children.add(new ProjectExplorerItem(this, f, toOpen));
            }
        }

        repaint();
    }

    @Override
    protected void selectionUpdated() {
        super.selectionUpdated();
        Commons.updateActiveProject();
    }

    public void saveExplorerTree() {
        StringBuilder sb = new StringBuilder();
        Collection<String> expandedElements = this.getExpandedElements();
        for(String elem : expandedElements) {
            sb.append(elem);
            sb.append(File.pathSeparator);
        }
        Debug.log("Saving: " + sb);
        Preferences.put("open_tree",sb.toString());
    }

    public void openExplorerTree() {
        String openTree = Preferences.get("open_tree",null);
        Debug.log("Opening: " + openTree);
        refresh(new ArrayList<>(Arrays.asList(openTree.split(Matcher.quoteReplacement(File.pathSeparator)))));
    }
}
