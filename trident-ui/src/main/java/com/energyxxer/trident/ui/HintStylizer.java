package com.energyxxer.trident.ui;

import com.energyxxer.trident.main.window.TridentWindow;
import com.energyxxer.trident.ui.theme.Theme;
import com.energyxxer.xswing.hints.Hint;
import com.energyxxer.xswing.hints.TextHint;

import java.awt.Color;

public class HintStylizer {

    public static void style(Hint hint) {
        style(hint, null);
    }

    public static void style(Hint hint, String type) {
        Theme t = TridentWindow.getTheme();

        hint.setBackgroundColor(t.getColor(Color.BLACK, "Hint."+type+".background","Hint.background"));
        hint.setBorderColor(t.getColor(Color.WHITE, "Hint."+type+".border","Hint.border"));
        if(hint instanceof TextHint) {
            TextHint thint = (TextHint) hint;
            thint.setForeground(t.getColor(Color.WHITE, "Hint."+type+".foreground","Hint.foreground","General.foreground"));
            thint.setFont(t.getFont("Hint."+type,"Hint","General"));
        }
    }
}
