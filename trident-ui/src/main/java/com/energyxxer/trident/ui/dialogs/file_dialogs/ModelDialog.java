package com.energyxxer.trident.ui.dialogs.file_dialogs;

import com.energyxxer.trident.files.FileType;
import com.energyxxer.trident.main.window.TridentWindow;

import javax.swing.JDialog;

/**
 * Created by User on 2/10/2017.
 */
public class ModelDialog {

    private static JDialog dialog = new JDialog(TridentWindow.jframe);

    public static void create(FileType type, String destination) {
        dialog.setVisible(true);
    }
}
