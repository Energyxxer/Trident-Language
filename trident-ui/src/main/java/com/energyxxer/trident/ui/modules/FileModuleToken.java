package com.energyxxer.trident.ui.modules;

import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.files.FileType;
import com.energyxxer.trident.global.Commons;
import com.energyxxer.trident.global.FileManager;
import com.energyxxer.trident.global.temp.projects.Project;
import com.energyxxer.trident.global.temp.projects.ProjectManager;
import com.energyxxer.trident.ui.Tab;
import com.energyxxer.trident.ui.common.MenuItems;
import com.energyxxer.trident.ui.display.DisplayModule;
import com.energyxxer.trident.ui.editor.TridentEditorModule;
import com.energyxxer.trident.ui.imageviewer.ImageViewer;
import com.energyxxer.trident.ui.styledcomponents.StyledMenu;
import com.energyxxer.trident.ui.styledcomponents.StyledMenuItem;
import com.energyxxer.trident.ui.styledcomponents.StyledPopupMenu;
import com.energyxxer.util.logger.Debug;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

public class FileModuleToken implements ModuleToken {
    public static ModuleTokenFactory<FileModuleToken> factory = str -> {
        if(!str.startsWith("file://")) return null;
        String path = str.substring("file://".length());
        File file = new File(path);
        return file.exists() ? new FileModuleToken(file) : null;
    };

    private final File file;
    private boolean isProjectRoot;
    private String overrideIconName = null;

    public FileModuleToken(File file) {
        this.file = file;

        this.isProjectRoot = file.isDirectory() && Objects.requireNonNull(file.listFiles(f -> TridentCompiler.PROJECT_FILE_NAME.equals(f.getName()))).length > 0;
    }

    @Override
    public String getTitle() {
        return file.getName();
    }

    @Override
    public Image getIcon() {
        if(overrideIconName != null) return Commons.getIcon(overrideIconName);
        if(file.isDirectory()) {
            if(isProjectRoot) {
                return Commons.getIcon("project");
            }
            return Commons.getIcon("folder");
        } else {
            String extension = file.getName().substring(file.getName().lastIndexOf("."));
            if(extension.equals(".png")) {
                try {
                    return ImageIO.read(file);
                } catch(IOException x) {
                    Debug.log("Couldn't load image from file '" + file + "'");
                    return Commons.getIcon("warn");
                }
            }
            switch(extension) {
                case ".tdn":
                    return Commons.getIcon("trident_file");
                case ".mcfunction":
                    return Commons.getIcon("function");
                case ".mp3":
                case ".ogg":
                    return Commons.getIcon("audio");
                case ".json": {
                    if(file.getName().equals("sounds.json"))
                        return Commons.getIcon("sound_config");
                    else if(file.getParentFile().getName().equals("blockstates"))
                        return Commons.getIcon("blockstate");
                    else if(file.getParentFile().getName().equals("lang"))
                        return Commons.getIcon("lang");
                    else
                        return Commons.getIcon("model");
                }
                case ".mcmeta":
                case TridentCompiler.PROJECT_FILE_NAME:
                    return Commons.getIcon("meta");
                case ".nbt":
                    return Commons.getIcon("structure");
                default: return Commons.getIcon("file");
            }
        }
    }

    @Override
    public String getHint() {
        return file.getPath();
    }

    @Override
    public Collection<ModuleToken> getSubTokens() {
        ArrayList<ModuleToken> children = new ArrayList<>();
        int firstFileIndex = 0;
        File[] subFiles = file.listFiles();
        if(subFiles != null) {
            for (File subDir : subFiles) {
                FileModuleToken subToken = new FileModuleToken(subDir);
                if(this.isProjectRoot) {
                    subToken.overrideIconName = subDir.getName().equals("datapack") ? "data" : subDir.getName().equals("resources") ? "resources" : null;
                }
                if (subDir.isDirectory()) {
                    children.add(firstFileIndex, subToken);
                    firstFileIndex++;
                }
                else {
                    children.add(subToken);
                }
            }
        }
        return children;
    }

    @Override
    public boolean isExpandable() {
        return file.isDirectory();
    }

    @Override
    public DisplayModule createModule(Tab tab) {
        if(file.isFile()) {
            String name = file.getName();
            if(name.endsWith(".png")) {
                return new ImageViewer(file);
            } else if(name.endsWith(".ogg") || name.endsWith(".mp3")) {

            } else {
                return new TridentEditorModule(tab, file);
            }
        }
        return null;
    }

    @Override
    public void onInteract() {

    }

    @Override
    public StyledPopupMenu generateMenu() {
        StyledPopupMenu menu = new StyledPopupMenu();

        String path = getPath();

        String newPath;
        if(file.isDirectory()) newPath = path;
        else newPath = file.getParent();

        //List<ModuleToken> selectedTokens = master.getSelectedTokens();
        ArrayList<FileModuleToken> selectedFiles = new ArrayList<>();
        selectedFiles.add(this);
        /*for(ModuleToken token : selectedTokens) {
            if(token instanceof FileModuleToken) selectedFiles.add((FileModuleToken) token);
        }*/

        {
            StyledMenu newMenu = new StyledMenu("New");

            menu.add(newMenu);

            // --------------------------------------------------

            Project project = ProjectManager.getAssociatedProject(file);

            String projectDir = (project != null) ? project.getRootDirectory().getPath() + File.separator : null;

            int lastGroup = 0;

            for(FileType type : FileType.values()) {
                if(type.canCreate(projectDir, path + File.separator)) {
                    if(type.group != lastGroup) {
                        newMenu.addSeparator();
                        lastGroup = type.group;
                    }
                    newMenu.add(type.createMenuItem(newPath));
                }
            }
            /*
            if((projectDir != null && (path + File.separator).startsWith(projectDir + "src" + File.separator)) || (path + File.separator).startsWith(Resources.nativeLib.getDir().getPath() + File.separator)) {

                StyledMenuItem entityItem = new StyledMenuItem("Entity", "entity");
                entityItem.addActionListener(e -> FileType.ENTITY.create(newPath));

                newMenu.add(entityItem);

                // --------------------------------------------------

                StyledMenuItem itemItem = new StyledMenuItem("Item", "item");
                itemItem.addActionListener(e -> FileType.ITEM.create(newPath));

                newMenu.add(itemItem);

                // --------------------------------------------------

                StyledMenuItem classItem = new StyledMenuItem("Class", "class");
                classItem.addActionListener(e -> FileType.CLASS.create(newPath));

                newMenu.add(classItem);

                // --------------------------------------------------

                StyledMenuItem enumItem = new StyledMenuItem("Enum", "enum");
                enumItem.addActionListener(e -> FileType.ENUM.create(newPath));

                newMenu.add(enumItem);

                // --------------------------------------------------

                StyledMenuItem featureItem = new StyledMenuItem("Feature", "feature");
                featureItem.addActionListener(e -> FileType.FEATURE.create(newPath));

                newMenu.add(featureItem);

                // --------------------------------------------------

                newMenu.addSeparator();

                // --------------------------------------------------

                StyledMenuItem worldItem = new StyledMenuItem("World", "world");
                worldItem.addActionListener(e -> FileType.WORLD.create(newPath));

                newMenu.add(worldItem);

                hasElements = true;
            } else if(projectDir != null && (path + File.separator).startsWith(projectDir + "resources" + File.separator)) {

                // --------------------------------------------------

                StyledMenuItem modelItem = new StyledMenuItem("Model", "model");
                modelItem.addActionListener(e -> FileType.MODEL.create(newPath));

                newMenu.add(modelItem);

                // --------------------------------------------------

                StyledMenuItem langItem = new StyledMenuItem("Language File", "lang");
                langItem.addActionListener(e -> FileType.LANG.create(newPath));

                newMenu.add(langItem);

                // --------------------------------------------------

                StyledMenuItem mcmetaItem = new StyledMenuItem("META File", "meta");
                mcmetaItem.addActionListener(e -> FileType.META.create(newPath));

                newMenu.add(mcmetaItem);

                hasElements = true;
                // --------------------------------------------------
            }

            // --------------------------------------------------

            if(hasElements) newMenu.addSeparator();

            // --------------------------------------------------

            StyledMenuItem packageItem = new StyledMenuItem("Package", "folder");
            packageItem.addActionListener(e -> FileType.PACKAGE.create(newPath));

            newMenu.add(packageItem);*/

        }
        menu.addSeparator();


        menu.add(MenuItems.fileItem(MenuItems.FileMenuItem.COPY));
        menu.add(MenuItems.fileItem(MenuItems.FileMenuItem.PASTE));

        StyledMenuItem deleteItem = MenuItems.fileItem(MenuItems.FileMenuItem.DELETE);
        deleteItem.setEnabled(selectedFiles.size() >= 1);
        ArrayList<String> selectedPaths = new ArrayList<>();
        for(FileModuleToken file : selectedFiles) {
            selectedPaths.add(file.getPath());
        }
        deleteItem.addActionListener(e -> FileManager.delete(selectedPaths));
        menu.add(deleteItem);

        menu.addSeparator();
        StyledMenu refactorMenu = new StyledMenu("Refactor");
        menu.add(refactorMenu);

        StyledMenuItem renameItem = MenuItems.fileItem(MenuItems.FileMenuItem.RENAME);
        /*renameItem.addActionListener(e -> {
            if(ExplorerMaster.selectedLabels.size() != 1) return;

            String path = ExplorerMaster.selectedLabels.get(0).parent.path;
            String name = new File(path).getName();
            String rawName = StringUtil.stripExtension(name);
            final String extension = name.replaceAll(rawName, "");
            final String pathToParent = path.substring(0, path.lastIndexOf(name));

            String newName = StringPrompt.prompt("Rename", "Enter a new name for the file:", rawName,
                    new StringValidator() {
                        @Override
                        public boolean validate(String str) {
                            return str.trim().length() > 0 && FileUtil.validateFilename(str)
                                    && !new File(pathToParent + str + extension).exists();
                        }
                    });

            if (newName != null) {
                if (ProjectManager.renameFile(new File(path), newName)) {
                    com.energyxxer.trident.ui.projectExplorer.ProjectExplorerItem parentItem = ExplorerMaster.selectedLabels.get(0).parent;
                    parentItem.path = pathToParent + newName + extension;
                    if (parentItem.parent != null) {
                        parentItem.parent.collapse();
                        parentItem.parent.refresh();
                    } else {
                        TridentWindow.projectExplorer.refresh();
                    }

                    TabManager.renameTab(path, pathToParent + newName + extension);

                } else {
                    JOptionPane.showMessageDialog(null,
                            "<html>The action can't be completed because the folder or file is open in another program.<br>Close the folder and try again.</html>",
                            "An error occurred.", JOptionPane.ERROR_MESSAGE);
                }
            }
        });*/
        refactorMenu.add(renameItem);
        refactorMenu.add(MenuItems.fileItem(MenuItems.FileMenuItem.MOVE));

        menu.addSeparator();

        StyledMenuItem openInSystemItem = new StyledMenuItem("Show in System Explorer", "explorer");
        openInSystemItem.addActionListener(e -> Commons.showInExplorer(path));

        menu.add(openInSystemItem);

        return menu;
    }

    public File getFile() {
        return file;
    }

    public String getPath() {
        return file.getPath();
    }

    @Override
    public String getIdentifier() {
        return "file://" + getPath();
    }

    @Override
    public boolean equals(ModuleToken other) {
        return other instanceof FileModuleToken && ((FileModuleToken) other).file.equals(this.file);
    }
}
