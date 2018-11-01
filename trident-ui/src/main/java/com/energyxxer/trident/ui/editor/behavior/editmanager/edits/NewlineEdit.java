package com.energyxxer.trident.ui.editor.behavior.editmanager.edits;

import com.energyxxer.trident.ui.editor.behavior.AdvancedEditor;
import com.energyxxer.trident.ui.editor.behavior.caret.CaretProfile;
import com.energyxxer.trident.ui.editor.behavior.caret.EditorCaret;
import com.energyxxer.trident.ui.editor.behavior.editmanager.Edit;
import com.energyxxer.util.StringUtil;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.util.ArrayList;

/**
 * Created by User on 3/20/2017.
 */
public class NewlineEdit extends Edit {
    private CaretProfile previousProfile;
    private CaretProfile nextProfile;
    private ArrayList<Integer> modificationIndices = new ArrayList<>();
    private ArrayList<String> previousValues = new ArrayList<>();
    private ArrayList<String> nextValues = new ArrayList<>();

    private final boolean pushCaret;

    public NewlineEdit(AdvancedEditor editor) {
        this(editor, true);
    }

    public NewlineEdit(AdvancedEditor editor, boolean pushCaret) {
        this.previousProfile = editor.getCaret().getProfile();
        this.pushCaret = pushCaret;
    }

    @Override
    public boolean redo(AdvancedEditor editor) {
        Document doc = editor.getDocument();
        EditorCaret caret = editor.getCaret();

        modificationIndices.clear();
        previousValues.clear();
        nextValues.clear();

        boolean actionPerformed = false;

        nextProfile = (pushCaret) ? new CaretProfile() : new CaretProfile(previousProfile);

        try {
            String text = doc.getText(0, doc.getLength()); //Result

            int characterDrift = 0;

            for(int i = 0; i < previousProfile.size() - 1; i += 2) {
                int start = Math.min(previousProfile.get(i), previousProfile.get(i+1)) + characterDrift;
                int end = Math.max(previousProfile.get(i), previousProfile.get(i+1)) + characterDrift;

                String str = "\n";

                int lineStart = Math.max(0, text.lastIndexOf('\n',start - characterDrift - 1)+1);

                int spaces = 0;
                for(int j = lineStart; j < start; j++) {
                    if(text.charAt(j) == ' ') spaces++;
                    else break;
                }
                int tabs = spaces / 4;
                if(text.substring(lineStart, start - characterDrift).trim().endsWith("{")) tabs++;

                str += StringUtil.repeat("    ", tabs);

                modificationIndices.add(start);
                previousValues.add(text.substring(start - characterDrift, end - characterDrift));
                nextValues.add(str);

                doc.remove(start, end-start);
                doc.insertString(start, str, null);
                actionPerformed = true;

                if(pushCaret) nextProfile.add(start + str.length(), start + str.length());
                else nextProfile.pushFrom(start+1, str.length());
                characterDrift += (end - start) + (tabs * 4) + 1;

                int ftabs = tabs;

                editor.registerCharacterDrift(o -> (o >= start) ? ((o <= end) ? start + (ftabs * 4) + 1 : o + (ftabs * 4) + 1 - (end - start)) : o);
            }
        } catch(BadLocationException x) {
            x.printStackTrace();
        }

        caret.setProfile(nextProfile);
        return actionPerformed;
    }

    @Override
    public boolean undo(AdvancedEditor editor) {
        Document doc = editor.getDocument();
        EditorCaret caret = editor.getCaret();

        try {
            for(int i = modificationIndices.size()-1; i >= 0; i--) {
                int start = modificationIndices.get(i);
                doc.remove(start, nextValues.get(i).length());
                doc.insertString(start, previousValues.get(i), null);

                final int fnlen = nextValues.get(i).length();
                final int fplen = previousValues.get(i).length();

                editor.registerCharacterDrift(o -> (o >= start) ? ((o <= start + fnlen) ? start + fplen : o - fnlen + fplen) : o);
            }
        } catch(BadLocationException x) {
            x.printStackTrace();
        }

        caret.setProfile(previousProfile);
        return true;
    }
}
