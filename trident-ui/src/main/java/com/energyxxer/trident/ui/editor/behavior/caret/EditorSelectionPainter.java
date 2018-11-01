package com.energyxxer.trident.ui.editor.behavior.caret;

import com.energyxxer.util.StringBounds;

import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;

/**
 * Created by User on 1/9/2017.
 */
public class EditorSelectionPainter implements Highlighter.HighlightPainter {

    private EditorCaret caret;

    public EditorSelectionPainter(EditorCaret caret) {
        this.caret = caret;
    }

    @Override
    public void paint(Graphics g, int p0, int p1, Shape graphicBounds, JTextComponent c) {
        g.setColor(c.getSelectionColor());

        ArrayList<Dot> dots = caret.getDots();

        for(Dot dot : dots) {
            try {
                StringBounds bounds = dot.getBounds();

                for (int l = bounds.start.line; l <= bounds.end.line; l++) {
                    Rectangle rectangle;
                    if (l == bounds.start.line) {
                        rectangle = c.modelToView(bounds.start.index);
                        if (bounds.start.line == bounds.end.line) {
                            rectangle.width = c.modelToView(bounds.end.index).x - rectangle.x;
                        } else {
                            rectangle.width = c.getWidth() - rectangle.x;
                        }
                    } else if (l == bounds.end.line) {
                        rectangle = c.modelToView(bounds.end.index);
                        rectangle.width = rectangle.x - c.modelToView(0).x;
                        rectangle.x = c.modelToView(0).x; //0
                    } else {
                        rectangle = c.modelToView(bounds.start.index);
                        rectangle.x = c.modelToView(0).x; //0
                        rectangle.y += rectangle.height * (l - bounds.start.line);
                        rectangle.width = c.getWidth();
                    }

                    if(rectangle.width < 0) {
                        rectangle.x += rectangle.width;
                        rectangle.width *= -1;
                    }
                    rectangle.width = Math.abs(rectangle.width);
                    g.fillRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
                }
            } catch (BadLocationException e) {
                //Can't render
            }
        }
    }
}
