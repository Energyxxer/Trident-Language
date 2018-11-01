package com.energyxxer.xswing.hints;

import javax.swing.*;
import java.awt.*;

public class TextHint extends Hint {
    private final JTextPane textPane;

    public TextHint(JFrame owner) {
        super(owner);
        textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setOpaque(false);
        textPane.setBackground(new Color(0,0,0,0));
        textPane.setForeground(new Color(187, 187, 187));
        this.setContent(textPane);
    }

    public TextHint(JFrame owner, String text) {
        this(owner);
        setText(text);
    }

    public void setText(String text) {
        textPane.setText(text);
        textPane.invalidate();
        textPane.validate();
        this.update();
    }

    public Color getForeground() {
        return textPane.getForeground();
    }

    public void setForeground(Color fg) {
        textPane.setForeground(fg);
    }

    public Font getFont() {
        return textPane.getFont();
    }

    public void setFont(Font font) {
        textPane.setFont(font);
    }
}
