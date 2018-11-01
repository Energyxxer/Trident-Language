package com.energyxxer.trident.main.window.sections;

import com.energyxxer.trident.global.Commons;
import com.energyxxer.trident.ui.styledcomponents.StyledLabel;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * Created by User on 1/3/2017.
 */
public class ExtendedStatusBar extends JPanel {

    StyledLabel caretInfo;
    StyledLabel selectionInfo;

    {
        this.setLayout(new FlowLayout(FlowLayout.RIGHT, 20,5));
        this.setPreferredSize(new Dimension(500, 25));

        this.setOpaque(false);
        this.setBackground(new Color(0,0,0,0));

        caretInfo = new StyledLabel(Commons.DEFAULT_CARET_DISPLAY_TEXT);
        selectionInfo = new StyledLabel(" ");

        this.add(selectionInfo);
        this.add(caretInfo);
    }

    public void setCaretInfo(String text) {
        caretInfo.setText(text);
    }
    public void setSelectionInfo(String text) {
        selectionInfo.setText(text);
    }
}
