package com.energyxxer.trident.ui.editor.behavior.editmanager.edits;

import com.energyxxer.trident.ui.editor.behavior.AdvancedEditor;
import com.energyxxer.trident.ui.editor.behavior.editmanager.Edit;

import java.util.ArrayList;

/**
 * Created by User on 1/6/2017.
 */
public class CompoundEdit extends Edit {
    private ArrayList<Edit> edits = new ArrayList<>();

    public CompoundEdit() {
    }

    public CompoundEdit(ArrayList<Edit> edits) {
        this.edits = edits;
    }

    public void appendEdit(Edit edit) {
        edits.add(edit);
    }

    @Override
    public boolean redo(AdvancedEditor editor) {
        boolean actionPerformed = false;
        for(Edit e : edits) {
            if(e.redo(editor)) actionPerformed = true;
        }
        return actionPerformed;
    }

    @Override
    public boolean undo(AdvancedEditor editor) {
        boolean actionPerformed = false;
        for(int i = edits.size()-1; i >= 0; i--) {
            Edit e = edits.get(i);
            if(e.undo(editor)) actionPerformed = true;
        }
        return actionPerformed;
    }
}
