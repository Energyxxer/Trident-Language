package com.energyxxer.trident.ui.editor.behavior.editmanager;

import com.energyxxer.trident.ui.editor.behavior.AdvancedEditor;

import java.util.Date;

/**
 * Created by User on 1/10/2017.
 */
public abstract class Edit {
    public final long time = new Date().getTime();

    public abstract boolean redo(AdvancedEditor editor);
    public abstract boolean undo(AdvancedEditor editor);
}
