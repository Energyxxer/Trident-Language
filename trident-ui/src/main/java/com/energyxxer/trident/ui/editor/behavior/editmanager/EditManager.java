package com.energyxxer.trident.ui.editor.behavior.editmanager;

import com.energyxxer.trident.ui.editor.behavior.AdvancedEditor;
import com.energyxxer.trident.ui.editor.behavior.caret.CaretProfile;

import java.util.ArrayList;

/**
 * Created by User on 1/5/2017.
 */
public class EditManager {
    private ArrayList<Edit> edits = new ArrayList<>();
    private int currentEdit = 0;
    private CaretProfile lastProfile = null;

    /**
     * Time interval in milliseconds within text editing runOperation will be done and undone together in the edit manager.
     * */
    private static final int EDIT_GROUP_DELAY = 300;

    private AdvancedEditor editor;

    public EditManager(AdvancedEditor editor) {
        this.editor = editor;
    }

    public void undo() {
        if(currentEdit-1 >= 0) {
            if(editor.getCaret().getProfile().equals(lastProfile)) {
                edits.get(--currentEdit).undo(editor);
                lastProfile = editor.getCaret().getProfile();
                if(currentEdit > 0 && Math.abs(edits.get(currentEdit).time - edits.get(currentEdit-1).time) <= EDIT_GROUP_DELAY) undo();
            } else {
                editor.getCaret().setProfile(lastProfile);
            }
        }
    }

    public void redo() {
        if(currentEdit < edits.size()) {
            if(editor.getCaret().getProfile().equals(lastProfile)) {
                edits.get(currentEdit++).redo(editor);
                lastProfile = editor.getCaret().getProfile();
                if(currentEdit < edits.size() && Math.abs(edits.get(currentEdit-1).time - edits.get(currentEdit).time) <= EDIT_GROUP_DELAY) {
                    redo();
                }
            } else {
                editor.getCaret().setProfile(lastProfile);
            }
        }
    }

    public void insertEdit(Edit edit) {
        if(edit.redo(editor)) {
            while(edits.size() > currentEdit) {
                edits.remove(currentEdit);
            }
            edits.add(edit);
            currentEdit++;
            lastProfile = editor.getCaret().getProfile();
        }
    }
}
