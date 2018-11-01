package com.energyxxer.trident.ui.styledcomponents;

import com.energyxxer.trident.ui.theme.change.ThemeListenerManager;
import com.energyxxer.xswing.XButton;
import com.energyxxer.xswing.XFileField;
import com.energyxxer.xswing.XTextField;

import java.awt.Color;
import java.io.File;

/**
 * Provides a file field that reacts to theme changes and
 * adjusts their own color. It also provides a namespace for
 * more specific paths on the theme file. If the namespace is
 * not specified, it defaults to the general style.
 */
public class StyledFileField extends XFileField {

    private String namespace = null;

    private ThemeListenerManager tlm = new ThemeListenerManager();

    public StyledFileField() {
        this((byte) -1, null, null);
    }

    public StyledFileField(String namespace) {
        this((byte) -1, null, namespace);
    }

    public StyledFileField(byte operation) {
        this(operation, null, null);
    }

    public StyledFileField(byte operation, String namespace) {
        this(operation, null, namespace);
    }

    public StyledFileField(File file) {
        this((byte) -1, file, null);
    }

    public StyledFileField(File file, String namespace) {
        this((byte) -1, file, namespace);
    }

    public StyledFileField(byte operation, File file) {
        this(operation, file, null);
    }

    public StyledFileField(byte operation, File file, String namespace) {
        if(operation >= 0) setOperation(operation);
        if(file != null) setFile(file);
        if(namespace != null) this.setNamespace(namespace);

        tlm.addThemeChangeListener(t -> {

            XTextField field = this.getField();
            XButton button = this.getButton();
            
            if(this.namespace != null) {
                field.setBackground         (t.getColor(new Color(220, 220, 220), this.namespace + ".textfield.background","General.textfield.background"));
                field.setForeground         (t.getColor(Color.BLACK, this.namespace + ".textfield.foreground","General.textfield.foreground","General.foreground"));
                field.setSelectionColor     (t.getColor(Color.BLACK, this.namespace + ".textfield.selection.background","General.textfield.selection.background"));
                field.setSelectedTextColor  (t.getColor(field.getForeground(), this.namespace + ".textfield.selection.foreground","General.textfield.selection.foreground"));
                field.setBorder             (t.getColor(new Color(200, 200, 200), this.namespace + ".textfield.border.color","General.textfield.border.color"),Math.max(t.getInteger(1,this.namespace + ".textfield.border.thickness","General.textfield.border.thickness"),0));
                field.setFont(t.getFont(this.namespace + ".textfield","General.textfield","General"));

                button.setBackground        (t.getColor(new Color(215, 215, 215), this.namespace + ".button.background","General.button.background"));
                button.setForeground        (t.getColor(Color.BLACK, this.namespace + ".button.foreground","General.button.foreground","General.foreground"));
                button.setBorder            (t.getColor(new Color(200, 200, 200), this.namespace + ".button.border.color","General.button.border.color"), Math.max(t.getInteger(1,this.namespace + ".button.border.thickness","General.button.border.thickness"),0));
                button.setRolloverColor     (t.getColor(new Color(200, 202, 205), this.namespace + ".button.hover.background","General.button.hover.background"));
                button.setPressedColor      (t.getColor(Color.WHITE, this.namespace + ".button.pressed.background","General.button.pressed.background"));
                button.setFont(t.getFont(this.namespace + ".button","General.button","General"));
            } else {
                field.setBackground         (t.getColor(new Color(220, 220, 220), "General.textfield.background"));
                field.setForeground         (t.getColor(Color.BLACK, "General.textfield.foreground","General.foreground"));
                field.setSelectionColor     (t.getColor(new Color(50, 100, 175), "General.textfield.selection.background"));
                field.setSelectedTextColor  (t.getColor(field.getForeground(), "General.textfield.selection.foreground"));
                field.setBorder             (t.getColor(new Color(200, 200, 200), "General.textfield.border.color"),Math.max(t.getInteger(1,"General.textfield.border.thickness"),0));
                field.setFont(t.getFont("General.textfield","General"));

                button.setBackground        (t.getColor(new Color(215, 215, 215), "General.button.background"));
                button.setForeground        (t.getColor(Color.BLACK, "General.button.foreground","General.foreground"));
                button.setBorder            (t.getColor(new Color(200, 200, 200), "General.button.border.color"), Math.max(t.getInteger(1,"General.button.border.thickness"),0));
                button.setRolloverColor     (t.getColor(new Color(200, 202, 205), "General.button.hover.background"));
                button.setPressedColor      (t.getColor(Color.WHITE, "General.button.pressed.background"));
                button.setFont(t.getFont("General.button","General"));
            }
        });
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getNamespace() {
        return this.namespace;
    }


}
