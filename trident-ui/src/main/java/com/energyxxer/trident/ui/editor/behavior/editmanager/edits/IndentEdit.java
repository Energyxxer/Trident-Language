package com.energyxxer.trident.ui.editor.behavior.editmanager.edits;

import com.energyxxer.trident.ui.editor.behavior.AdvancedEditor;
import com.energyxxer.trident.ui.editor.behavior.caret.CaretProfile;
import com.energyxxer.trident.ui.editor.behavior.caret.Dot;
import com.energyxxer.trident.ui.editor.behavior.caret.EditorCaret;
import com.energyxxer.trident.ui.editor.behavior.editmanager.Edit;
import com.energyxxer.util.StringUtil;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.util.ArrayList;

/**
 * Created by User on 1/27/2017.
 */
public class IndentEdit extends Edit {
    private CaretProfile previousProfile;
    /**
     * ArrayList containing information about how to undo the edit.
     * Must contain even entries.
     * Every even index (0, 2, 4...) contains the position in the document where spaces were added/removed.
     * Every odd index (1, 3, 5...) contains the amount of spaces added/removed at the position given by the index before it.
     * */
    private ArrayList<Integer> modifications = new ArrayList<>();
    private final boolean reverse;

    public IndentEdit(AdvancedEditor editor) {
        this(editor,false);
    }

    public IndentEdit(AdvancedEditor editor, boolean reverse) {
        previousProfile = editor.getCaret().getProfile();
        this.reverse = reverse;
    }

    @Override
    public boolean redo(AdvancedEditor editor) {
        Document doc = editor.getDocument();
        EditorCaret caret = editor.getCaret();

        boolean actionPerformed = false;

        try {
            String text = doc.getText(0, doc.getLength()); //Result
            this.modifications.clear();

            int characterDrift = 0;

            CaretProfile nextProfile = new CaretProfile(previousProfile);

            for (int i = 0; i < previousProfile.size()-1; i += 2) {
                //Get bounds of the line to move
                int selectionStart = previousProfile.get(i) + characterDrift;
                int selectionEnd = previousProfile.get(i + 1) + characterDrift;

                int start = new Dot(Math.min(selectionStart,selectionEnd),editor).getRowStart();
                int end = new Dot(Math.max(selectionStart,selectionEnd),editor).getRowEnd();

                int spacesChanged = 0;

                for(int l = start; l <= end + characterDrift; l = new Dot(l, editor).getPositionBelow()) {
                    int spaces = StringUtil.getSequenceCount(text," ", l - characterDrift);
                    if(!reverse) {
                        int spacesToAdd = 4 - (spaces % 4);
                        spacesToAdd = (spacesToAdd > 0) ? spacesToAdd : 4;
                        doc.insertString(l,StringUtil.repeat(" ", spacesToAdd),null);
                        modifications.add(l);
                        modifications.add(spacesToAdd);
                        nextProfile.pushFrom(l,spacesToAdd);
                        actionPerformed = true;
                        if(l == end + characterDrift) break;
                        characterDrift += spacesToAdd;
                        spacesChanged += spacesToAdd;
                    } else {
                        if(spaces == 0) {
                            if(l == end + characterDrift) break; continue;
                        }
                        int spacesToRemove = (spaces % 4 == 0) ? 4 : spaces % 4;
                        if(spacesToRemove != 0) {
                            nextProfile.pushFrom(l,Math.min(0,spaces - (Math.min(selectionStart, selectionEnd) - start) - spacesToRemove));
                        }
                        actionPerformed = true;
                        doc.remove(l,spacesToRemove);
                        modifications.add(l);
                        modifications.add(spacesToRemove);
                        if(l == end + characterDrift) break;
                        characterDrift -= spacesToRemove;
                        spacesChanged -= spacesToRemove;
                    }
                }

                final int fsc = spacesChanged;

                editor.registerCharacterDrift(o -> (o >= start) ? o + fsc : o);
            }

            caret.setProfile(nextProfile);
        } catch(BadLocationException x) {
            x.printStackTrace();
        }
        return actionPerformed;
    }

    @Override
    public boolean undo(AdvancedEditor editor) {
        Document doc = editor.getDocument();
        EditorCaret caret = editor.getCaret();

        try {
            for(int i = modifications.size()-2; i >= 0; i -= 2) {
                int index = modifications.get(i);
                int spaces = modifications.get(i+1);

                if(!reverse) doc.remove(index, spaces);
                else doc.insertString(index, StringUtil.repeat(" ", spaces), null);

                editor.registerCharacterDrift(o -> (o >= index) ? o - ((!reverse) ? spaces : -spaces) : o);
            }
        } catch(BadLocationException x) {
            x.printStackTrace();
        }
        caret.setProfile(previousProfile);
        return true;
    }
}
