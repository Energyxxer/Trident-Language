package com.energyxxer.trident.ui.editor;

import com.energyxxer.trident.main.window.TridentWindow;
import com.energyxxer.trident.ui.editor.behavior.AdvancedEditor;
import com.energyxxer.trident.ui.editor.behavior.editmanager.CharacterDriftHandler;
import com.energyxxer.trident.ui.editor.inspector.Inspector;
import com.energyxxer.trident.global.temp.Lang;
import com.energyxxer.enxlex.lexical_analysis.Lexer;
import com.energyxxer.enxlex.lexical_analysis.profiles.ScannerProfile;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenSection;
import com.energyxxer.enxlex.lexical_analysis.token.TokenStream;
import com.energyxxer.enxlex.pattern_matching.TokenMatchResponse;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

/**
 * Created by User on 1/1/2017.
 */
public class TridentEditorComponent extends AdvancedEditor implements KeyListener, CaretListener, ActionListener {

    private TridentEditorModule parent;

    private StyledDocument sd;

    private Inspector inspector = null;

    private long lastEdit;

    TridentEditorComponent(TridentEditorModule parent) {
        super(new DefaultStyledDocument());
        this.parent = parent;

        sd = this.getStyledDocument();

        //if(Lang.getLangForFile(parent.associatedTab.path) != null) this.inspector = new Inspector(this);

        this.addCaretListener(this);

        Timer timer = new Timer(20, this);
        timer.start();

        //this.setOpaque(false);
    }

    @Override
    public void caretUpdate(CaretEvent e) {
        displayCaretInfo();
    }

    private void highlightSyntax() {
        if(parent.syntax == null) return;

        sd.putProperty(DefaultEditorKit.EndOfLineStringProperty, "\n");

        Style defaultStyle = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);

        String text = getText();

        Lang lang = Lang.getLangForFile(parent.file.getPath());
        ScannerProfile fileProfile = lang != null ? lang.createProfile() : null;
        Lexer sc = new Lexer(parent.file, text, new TokenStream(true), fileProfile);
        ArrayList<Notice> newNotices = new ArrayList<>(sc.getNotices());

        boolean doParsing = lang != null && lang.getParserProduction() != null;

        ArrayList<Token> tokens = new ArrayList<>(sc.getStream().tokens);
        tokens.remove(0);

        TokenMatchResponse match = null;

        if(doParsing) {
            ArrayList<Token> f = new ArrayList<>(tokens);
            f.removeIf(t -> !t.isSignificant());

            match = lang.getParserProduction().match(f);

            match.pattern.validate();
        }

        for(Token token : tokens) {
            Style style = TridentEditorComponent.this.getStyle(token.type.toString().toLowerCase());
            if(style != null)
                sd.setCharacterAttributes(token.loc.index, token.value.length(), style, false);
            else
                sd.setCharacterAttributes(token.loc.index, token.value.length(), defaultStyle, true);

            for(Map.Entry<String, Object> entry : token.attributes.entrySet()) {
                if(!entry.getValue().equals(true)) continue;
                Style attrStyle = TridentEditorComponent.this.getStyle("~" + entry.getKey().toLowerCase());
                if(attrStyle == null) continue;
                sd.setCharacterAttributes(token.loc.index, token.value.length(), attrStyle, false);
            }
            for(Map.Entry<TokenSection, String> entry : token.subSections.entrySet()) {
                TokenSection section = entry.getKey();
                Style attrStyle = TridentEditorComponent.this.getStyle("~" + entry.getValue().toLowerCase());
                if(attrStyle == null) continue;
                sd.setCharacterAttributes(token.loc.index + section.start, section.length, attrStyle, false);
            }
            for(String tag : token.tags) {
                Style attrStyle = TridentEditorComponent.this.getStyle("$" + tag.toLowerCase());
                if(attrStyle == null) continue;
                sd.setCharacterAttributes(token.loc.index, token.value.length(), attrStyle, false);
            }

            if(doParsing) {
                ps: for(Map.Entry<String, String[]> entry : parent.parserStyles.entrySet()) {
                    String[] tagList = entry.getValue();
                    int startIndex = token.tags.indexOf(tagList[0]);
                    if(startIndex < 0) continue;
                    for(int i = 0; i < tagList.length; i++) {
                        if(startIndex+i >= token.tags.size() || !tagList[i].equalsIgnoreCase(token.tags.get(startIndex+i))) continue ps;
                    }
                    Style attrStyle = TridentEditorComponent.this.getStyle(entry.getKey());
                    if(attrStyle == null) continue;
                    sd.setCharacterAttributes(token.loc.index, token.value.length(), attrStyle, false);
                }
            }
        }

        if(match != null && !match.matched) {
            TridentWindow.setStatus(match.getErrorMessage());
            newNotices.add(new Notice(NoticeType.ERROR, match.getErrorMessage(), match.faultyToken));
            sd.setCharacterAttributes(match.faultyToken.loc.index, match.faultyToken.value.length(), TridentEditorComponent.this.getStyle("error"), true);
        }

        if(this.inspector != null) {
            this.inspector.inspect(sc.getStream());
            this.inspector.insertNotices(newNotices);
        }
    }

    void highlight() {
        lastEdit = new Date().getTime();
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        if (lastEdit > -1 && (new Date().getTime()) - lastEdit > 500 && parent.associatedTab.isActive()) {
            lastEdit = -1;
            new Thread(this::highlightSyntax,"Text Highlighter").start();
        }
    }

    @Override
    public void registerCharacterDrift(CharacterDriftHandler h) {
        super.registerCharacterDrift(h);

        if(this.inspector != null) this.inspector.registerCharacterDrift(h);
    }

    @Override
    public void repaint() {
        if(this.getParent() instanceof JViewport && this.getParent().getParent() instanceof JScrollPane) {
            this.getParent().getParent().repaint();
        } else super.repaint();
    }

    void displayCaretInfo() {
        TridentWindow.statusBar.setCaretInfo(getCaretInfo());
        TridentWindow.statusBar.setSelectionInfo(getSelectionInfo());
    }

    @Override
    public String getText() {
        try {
            return getDocument().getText(0, getDocument().getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
            return null;
        }
    }
}
