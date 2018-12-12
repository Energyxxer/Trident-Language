package com.energyxxer.trident.files;

import com.energyxxer.trident.ui.dialogs.file_dialogs.*;
import com.energyxxer.trident.ui.styledcomponents.StyledMenuItem;

import java.io.File;

/**
 * Created by User on 2/9/2017.
 */
public enum FileType {
    PROJECT(0, "Project", "project", ProjectDialog::create, (pr, pth) -> true),
    TDN(1, "Trident Function", "trident_file", UnitDialog::create, (pr, pth) -> pr != null && pth.startsWith(pr + "datapack" + File.separator)),
    MODEL(1, "Model", "model", ModelDialog::create, (pr, pth) -> pr != null && pth.startsWith(pr + "resources" + File.separator)),
    LANG(1, "Language File", "lang", ResourceDialog::create, (pr, pth) -> pr != null && pth.startsWith(pr + "resources" + File.separator)),
    FUNCTION(1, "Function", "function", MCMETADialog::create, (pr, pth) -> pr != null && pth.startsWith(pr + "datapack" + File.separator)),
    META(2, "Meta File", "meta", MCMETADialog::create, (pr, pth) -> pr != null && pth.startsWith(pr + "resources" + File.separator)),
    JSON(2, "JSON File", "json", MCMETADialog::create, (pr, pth) -> true),
    PACKAGE(3, "Folder", "folder", PackageDialog::create, (pr, pth) -> true);

    public final int group;
    public final String name;
    public final String icon;
    public final FileTypeDialog dialog;
    public final DirectoryValidator validator;

    FileType(int group, String name, String icon, FileTypeDialog dialog, DirectoryValidator validator) {
        this.group = group;
        this.name = name;
        this.icon = icon;
        this.dialog = dialog;
        this.validator = validator;
    }

    public void create(String destination) {
        this.dialog.create(this, destination);
    }

    public StyledMenuItem createMenuItem(String newPath) {
        StyledMenuItem item = new StyledMenuItem(name, icon);
        item.addActionListener(e -> create(newPath));
        return item;
    }

    @Override
    public String toString() {
        return name;
    }

    public boolean canCreate(String projectDir, String path) {
        return validator.canCreate(projectDir, path);
    }
}
