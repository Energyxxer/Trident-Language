package com.energyxxer.trident.ui.modules;

import com.energyxxer.trident.global.Commons;
import com.energyxxer.trident.ui.Tab;
import com.energyxxer.trident.ui.audio.AudioPlayer;
import com.energyxxer.trident.ui.display.DisplayModule;
import com.energyxxer.trident.ui.editor.TridentEditorModule;
import com.energyxxer.trident.ui.imageviewer.ImageViewer;
import com.energyxxer.trident.ui.styledcomponents.StyledPopupMenu;
import com.energyxxer.util.logger.Debug;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class FileModuleToken implements ModuleToken {
    private final File file;

    public FileModuleToken(File file) {
        this.file = file;
    }

    @Override
    public String getTitle() {
        return file.getName();
    }

    @Override
    public Image getIcon() {
        if(file.isDirectory()) {
            return Commons.getIcon("package");
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
                case ".tdnproj":
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
                ModuleToken subToken = new FileModuleToken(subDir);
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
                return new AudioPlayer(file);
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
        return null;
    }

    public File getFile() {
        return file;
    }

    public String getPath() {
        return file.getPath();
    }

    @Override
    public String getIdentifier() {
        return getPath();
    }

    @Override
    public boolean equals(ModuleToken other) {
        return other instanceof FileModuleToken && ((FileModuleToken) other).file.equals(this.file);
    }
}
