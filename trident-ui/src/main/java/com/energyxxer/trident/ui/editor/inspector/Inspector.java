package com.energyxxer.trident.ui.editor.inspector;

import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenStream;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.trident.compiler.lexer.TridentTokens;
import com.energyxxer.trident.main.window.TridentWindow;
import com.energyxxer.trident.ui.HintStylizer;
import com.energyxxer.trident.ui.editor.TridentEditorComponent;
import com.energyxxer.trident.ui.editor.behavior.editmanager.CharacterDriftHandler;
import com.energyxxer.util.StringBounds;
import com.energyxxer.util.logger.Debug;
import com.energyxxer.xswing.hints.TextHint;

import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;

/**
 * Created by User on 1/1/2017.
 */
public class Inspector implements Highlighter.HighlightPainter, MouseMotionListener {

    private volatile ArrayList<InspectionItem> items = new ArrayList<>();

    private TridentEditorComponent editor;

    private TextHint hint = TridentWindow.hintManager.createTextHint("a");

    private InspectionItem rolloverItem = null;

    public Inspector(TridentEditorComponent editor) {
        this.editor = editor;
        editor.addMouseMotionListener(this);

        try
        {
            editor.getHighlighter().addHighlight(0, 0, this);
        }
        catch(BadLocationException ble) {}

        hint.setInteractive(true);
    }

    public void inspect(TokenStream ts) {
        items.clear();

        for(InspectionStructureMatch inspect : InspectionStructures.getAll()) {
            ArrayList<TokenPattern<?>> matches = ts.search(inspect);
            for(TokenPattern<?> match : matches) {
                items.add(new InspectionItem(inspect.type, inspect.name, match.getStringBounds()));
            }
        }

        for(Token token : ts.tokens) {
            if(token.type == TridentTokens.COMMENT && token.subSections.containsValue("deprecated_syntax")) {
                items.add(new InspectionItem(InspectionType.WARNING, "Changed syntax, use @ instead of #:", token.getStringBounds()));
            }
        }
        editor.repaint();
    }

    @Override
    public void paint(Graphics g, int p0, int p1, Shape graphicBounds, JTextComponent c) {
        try {
            for (InspectionItem item : items) {

                g.setColor(TridentWindow.getTheme().getColor("Inspector." + item.type.key));

                try {

                    StringBounds bounds = item.bounds;

                    for (int l = bounds.start.line; l <= bounds.end.line; l++) {
                        Rectangle rectangle;
                        if (l == bounds.start.line) {
                            rectangle = editor.modelToView(bounds.start.index);
                            if (bounds.start.line == bounds.end.line) {
                                //One line only
                                rectangle.width = editor.modelToView(bounds.end.index).x - rectangle.x;
                            } else {
                                rectangle.width = c.getWidth() - rectangle.x;
                            }
                        } else if (l == bounds.end.line) {
                            rectangle = editor.modelToView(bounds.end.index);
                            rectangle.width = rectangle.x - c.modelToView(0).x;
                            rectangle.x = c.modelToView(0).x; //0
                        } else {
                            rectangle = editor.modelToView(bounds.start.index);
                            rectangle.x = c.modelToView(0).x; //0
                            rectangle.y += rectangle.height * (l - bounds.start.line);
                            rectangle.width = c.getWidth();
                        }

                        if (item.type.line) {
                            for (int x = rectangle.x; x < rectangle.x + rectangle.width; x += 4) {
                                g.drawLine(x, rectangle.y + rectangle.height, x + 2, rectangle.y + rectangle.height - 2);
                                g.drawLine(x + 2, rectangle.y + rectangle.height - 2, x + 4, rectangle.y + rectangle.height);
                            }
                        } else {
                            g.fillRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
                        }
                    }
                } catch (BadLocationException e) {
                    //e.printStackTrace();
                }
            }
        } catch(ConcurrentModificationException e) {}
    }

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {
        int index = editor.viewToModel(e.getPoint());
        for(InspectionItem item : items) {
            if(index >= item.bounds.start.index && index < item.bounds.end.index) {
                if(rolloverItem != item) {
                    rolloverItem = item;
                    if(!hint.isShowing()) {
                        hint.setText(item.message);
                        HintStylizer.style(hint, item.type.key);
                        hint.show(e.getLocationOnScreen(), () -> rolloverItem != null && editor.isShowing());
                    }
                } else if(!hint.isShowing()) {
                    hint.updateLocation(e.getLocationOnScreen());
                }
                return;
            }
        }
        rolloverItem = null;
    }

    public void insertNotices(ArrayList<Notice> notices) {
        for(Notice n : notices) {
            insertNotice(n);
        }
    }

    public void insertNotice(Notice n) {
        InspectionType type = InspectionType.SUGGESTION;
        switch(n.getType()) {
            case ERROR: {
                type = InspectionType.ERROR;
                break;
            }
            case WARNING: {
                type = InspectionType.WARNING;
                break;
            }
        }
        InspectionItem item = new InspectionItem(type, n.getMessage(), new StringBounds(editor.getLocationForOffset(n.getLocationIndex()), editor.getLocationForOffset(n.getLocationIndex() + n.getLocationLength())));
        Debug.log("Created item: " + item);
        items.add(item);
    }

    public void registerCharacterDrift(CharacterDriftHandler h) {
        for(InspectionItem item : items) {
            item.bounds.start = editor.getLocationForOffset(h.shift(item.bounds.start.index));
            item.bounds.end = editor.getLocationForOffset(h.shift(item.bounds.end.index));
        }
    }
}
