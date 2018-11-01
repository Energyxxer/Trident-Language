package com.energyxxer.trident.ui.editor.behavior.caret;

import com.energyxxer.trident.ui.editor.behavior.AdvancedEditor;
import com.energyxxer.util.StringBounds;

import javax.swing.text.BadLocationException;
import javax.swing.text.Utilities;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;

/**
 * Created by User on 1/7/2017.
 */
public class Dot {
    private AdvancedEditor component;
    int index = 0;
    int mark = 0;
    int x = 0;

    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final int UP = 2;
    public static final int DOWN = 3;

    public Dot(int index, AdvancedEditor component) {
        this(index, index, component);
    }

    public Dot(int index, int mark, AdvancedEditor component) {
        this.component = component;
        this.index = index;
        this.mark = mark;
        updateX();
    }

    private void updateX() {
        try {
            Rectangle view = component.modelToView(index);
            if(view != null) this.x = view.x;
        } catch (BadLocationException ble) {
            //
        }
    }

    public boolean handleEvent(KeyEvent e) {
        int key = e.getKeyCode();

        boolean actionPerformed = false;
        int nextPos = 0;
        boolean doUpdateX = false;

        if(key == KeyEvent.VK_LEFT) {
            if(index != mark && !e.isShiftDown()) {
                nextPos = Math.min(index, mark);
                if(e.isControlDown()) nextPos = getPositionBeforeWord();
            } else nextPos = (e.isControlDown()) ? getPositionBeforeWord() : getPositionBefore();
            doUpdateX = true;
            actionPerformed = true;
        } else if(key == KeyEvent.VK_RIGHT) {
            if(index != mark && !e.isShiftDown()) {
                nextPos = Math.max(index, mark);
                if(e.isControlDown()) nextPos = getPositionAfterWord();
            } else nextPos = (e.isControlDown()) ? getPositionAfterWord() : getPositionAfter();
            doUpdateX = true;
            actionPerformed = true;
        } else if(key == KeyEvent.VK_UP) {
            if(e.isControlDown()) {
                e.consume();
                return false;
            }
            nextPos = getPositionAbove();
            if(nextPos < 0) {
                nextPos = 0;
                doUpdateX = true;
            }
            actionPerformed = true;
        } else if(key == KeyEvent.VK_DOWN) {
            if(e.isControlDown()) {
                e.consume();
                return false;
            }
            nextPos = getPositionBelow();
            if(nextPos < 0) {
                nextPos = component.getDocument().getLength();
                doUpdateX = true;
            }
            actionPerformed = true;
        } else if(key == KeyEvent.VK_HOME) {
            if(e.isControlDown()) nextPos = 0;
            else nextPos = getRowStart();
            doUpdateX = true;
            actionPerformed = true;
        } else if(key == KeyEvent.VK_END) {
            if(e.isControlDown()) nextPos = component.getDocument().getLength();
            else nextPos = getRowEnd();
            doUpdateX = true;
            actionPerformed = true;
        }

        if(actionPerformed) {
            e.consume();
            index = nextPos;
            if(!e.isShiftDown()) mark = nextPos;
            if(doUpdateX) updateX();
        }
        return actionPerformed;
    }

    public void deselect() {
        mark = index;
    }

    public int getPositionBefore() {
        return Math.max(0, Math.min(component.getDocument().getLength(), index-1));
    }

    public int getPositionAfter() {
        return Math.max(0, Math.min(component.getDocument().getLength(), index+1));
    }

    public int getPositionAbove() {
        try {
            return Utilities.getPositionAbove(component, index, x);
        } catch(BadLocationException ble) {}
        return index;
    }

    public int getPositionBelow() {
        try {
            return Utilities.getPositionBelow(component, index, x);
        } catch(BadLocationException ble) {}
        return index;
    }

    public int getPositionBeforeWord() {
        try {
            return Math.max(component.getPreviousWord(Math.min(index,mark)), Math.max(0,getRowStart()-1));
        } catch(BadLocationException ble) {}
        return 0;
    }

    public int getPositionAfterWord() {
        try {
            int pos = component.getNextWord(Math.max(index,mark));
            int rowEnd = getRowEnd();
            return (Math.max(index, mark) == rowEnd) ? pos : Math.min(pos, rowEnd);
        } catch(BadLocationException ble) {}
        return component.getDocument().getLength();
    }

    public int getRowStart() {
        try {
            return Utilities.getRowStart(component, index);
        } catch(BadLocationException ble) {}
        return index;
    }

    public int getRowEnd() {
        try {
            return Utilities.getRowEnd(component, index);
        } catch(BadLocationException ble) {}
        return index;
    }

    public StringBounds getBounds() {
        return new StringBounds(component.getLocationForOffset(index),component.getLocationForOffset(mark));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Dot dot = (Dot) o;

        return index == dot.index;
    }

    @Override
    public String toString() {
        return "Dot{" +
                "index=" + index +
                ", mark=" + mark +
                ", x=" + x +
                '}';
    }
}
