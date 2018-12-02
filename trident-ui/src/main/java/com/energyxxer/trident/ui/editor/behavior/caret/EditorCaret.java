package com.energyxxer.trident.ui.editor.behavior.caret;

import com.energyxxer.trident.ui.editor.behavior.AdvancedEditor;
import com.energyxxer.trident.util.Range;
import com.energyxxer.util.StringLocation;

import javax.swing.plaf.TextUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

/**
 * Created by User on 1/3/2017.
 */
public class EditorCaret extends DefaultCaret {

    private ArrayList<Dot> dots = new ArrayList<>();
    private AdvancedEditor editor;

    private static Highlighter.HighlightPainter nullHighlighter = (g,p0,p1,bounds,c) -> {};

    public EditorCaret(AdvancedEditor c) {
        this.editor = c;
        this.setBlinkRate(500);
        this.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        c.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleEvent(e);
            }
        });

        addDot(new Dot(0, 0, editor));

        try {
            c.getHighlighter().addHighlight(0,0,new EditorSelectionPainter(this));
        } catch(BadLocationException x) {
            //
        }
    }

    private void handleEvent(KeyEvent e) {
        if(e.isConsumed()) return;
        boolean actionPerformed = false;
        for(Dot dot : dots) {
            if(dot.handleEvent(e)) actionPerformed = true;
        }
        if(actionPerformed) {
            removeDuplicates();
            update();
        }
    }

    private void update() {
        editor.repaint();
        this.setVisible(true);
        readjustRect();
        this.fireStateChanged();
    }

    public void setPosition(int pos) {
        dots.clear();
        addDot(new Dot(pos, editor));
        update();
    }

    public void addDot(int... pos) {
        for(int dot : pos) {
            Dot newDot = new Dot(dot, editor);
            if(!dots.contains(newDot)) dots.add(newDot);
        }
    }

    public void addDot(Dot... newDots) {
        for(Dot dot : newDots) {
            if(!dots.contains(dot)) dots.add(dot);
        }
    }

    public void removeDuplicates() {
        boolean stateChanged = false;
        for(int i = 0; i < dots.size(); i++) {
            Dot d = dots.get(i);
            if(dots.indexOf(d) != i) {
                dots.remove(i);
                i--;
                stateChanged = true;
            }
        }
        if(stateChanged) this.fireStateChanged();
    }

    @Override
    public void paint(Graphics g) {
        try {
            TextUI mapper = getComponent().getUI();

            ArrayList<Dot> allDots = new ArrayList<>(dots);

            for (Dot dot : allDots) {
                Rectangle r = mapper.modelToView(getComponent(), dot.index, getDotBias());

                if(isVisible()) {
                    g.setColor(getComponent().getCaretColor());
                    int paintWidth = 2;
                    r.x -= paintWidth >> 1;
                    g.fillRect(r.x, r.y, paintWidth, r.height);
                }
                else {
                    getComponent().repaint(r);
                }
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void readjustRect() {
        try {
            ArrayList<Dot> allDots = new ArrayList<>(dots);

            Rectangle unionRect = null;

            for (Dot dot : allDots) {
                Rectangle r = editor.modelToView(dot.index);
                if(unionRect == null) unionRect = r; else unionRect = unionRect.union(r);
            }

            if(unionRect != null) {
                x = unionRect.x - 1;
                y = unionRect.y;
                width = unionRect.width + 3;
                height = unionRect.height;
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected synchronized void damage(Rectangle rect) {
        readjustRect();
        repaint();
    }

    public String getSelectionInfo() {
        StringBuilder s = new StringBuilder(" ");

        CaretProfile profile = this.getProfile();
        int selectedCharCount = profile.getSelectedCharCount();
        if(selectedCharCount > 0) {
            s.append(selectedCharCount).append(" chars");

            int selectedLineCount = profile.getSelectedLineCount(editor);
            if(selectedLineCount > 1) {
                s.append(", ").append(selectedLineCount).append(" lines");
            }
        }
        return s.toString();
    }

    public String getCaretInfo() {
        if(dots.size() > 1) {
            return dots.size() + " carets";
        } else {
            StringLocation loc = editor.getLocationForOffset(dots.get(0).index);
            return loc.line + ":" + loc.column;
        }
    }

    public void moveBy(int offset) {
        pushFrom(0, offset);
    }

    public void pushFrom(int pos, int offset) {
        int docLength = editor.getDocument().getLength();

        for(Dot dot : dots) {
            if(dot.index >= pos) {
                dot.index = Math.min(docLength, Math.max(0, dot.index + offset));
            }
            if(dot.mark >= pos) {
                dot.mark = Math.min(docLength, Math.max(0, dot.mark + offset));
            }
        }

        update();
    }

    public void deselect() {
        for(Dot dot : dots) {
            dot.deselect();
        }
    }

    @Override
    public int getDot() {
        return dots.get(dots.size()-1).index;
    }

    public ArrayList<Dot> getDots() {
        return new ArrayList<>(this.dots);
    }

    public CaretProfile getProfile() {
        CaretProfile profile = new CaretProfile();
        profile.addAllDots(dots);
        profile.sort();
        return profile;
    }

    @Override
    protected Highlighter.HighlightPainter getSelectionPainter() {
        return nullHighlighter;
    }

    public void setProfile(CaretProfile profile) {
        this.dots.clear();
        Range r = new Range(0,editor.getDocument().getLength());
        for(int i = 0; i < profile.size(); i += 2) {
            addDot(new Dot(r.clamp(profile.get(i)),r.clamp(profile.get(i+1)), editor));
        }
        removeDuplicates();
        update();
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    //Handle mouse selection

    private Dot bufferedDot = null;

    @Override
    public void mousePressed(MouseEvent e) {
        if(!e.isAltDown() || !e.isShiftDown()) {
            dots.clear();
        }
        int index = editor.viewToModel(e.getPoint());

        bufferedDot = new Dot(index, index, editor);
        if (e.getClickCount() == 2 && !e.isConsumed() && e.getButton() == MouseEvent.BUTTON1) {
            bufferedDot.mark = bufferedDot.getPositionBeforeWord();
            bufferedDot.index = bufferedDot.getPositionAfterWord();
        } else if(e.getClickCount() >= 3 && !e.isConsumed() && e.getButton() == MouseEvent.BUTTON1) {
            bufferedDot.mark = bufferedDot.getRowStart();
            bufferedDot.index = bufferedDot.getRowEnd();
        }
        addDot(bufferedDot);
        e.consume();
        update();
        //super.mousePressed(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        editor.repaint();
        this.setVisible(true);
        readjustRect();
        e.consume();
    }

    private void adjustFocus() {
        if ((editor != null) && editor.isEnabled() &&
                editor.isRequestFocusEnabled()) {
            editor.requestFocus();
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        super.mouseEntered(e);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        super.mouseExited(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if(bufferedDot != null) bufferedDot.index = editor.viewToModel(e.getPoint());
        update();
        //super.mouseDragged(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        super.mouseMoved(e);
    }
}
