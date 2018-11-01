package com.energyxxer.trident.ui.styledcomponents;

import com.energyxxer.trident.ui.theme.change.ThemeListenerManager;
import com.energyxxer.xswing.XList;

import javax.swing.BorderFactory;
import java.awt.Color;

/**
 * Created by User on 12/14/2016.
 */
public class StyledList<T> extends XList<T> {

    private String namespace = null;

    private ThemeListenerManager tlm = new ThemeListenerManager();

    public StyledList() {
        this(null, null);
    }

    public StyledList(T[] options) {
        this(options, null);
    }

    public StyledList(T[] options, String namespace) {
        if(options != null) setOptions(options);
        if(namespace != null) this.setNamespace(namespace);

        tlm.addThemeChangeListener(t -> {
            if(this.namespace != null) {
                setBackground               (t.getColor(Color.WHITE, this.namespace + ".list.background","General.list.background"));
                setForeground               (t.getColor(Color.BLACK, this.namespace + ".list.cell.foreground","General.list.cell.foreground","General.foreground"));

                setCellBackground           (t.getColor(new Color(215, 215, 215), this.namespace + ".list.cell.background","General.list.cell.background"));

                setSelectedCellBackground(  t.getColor(new Color(235, 235, 235), this.namespace + ".list.cell.selected.background","General.list.cell.selected.background"));
                setSelectedCellBorder(
                        BorderFactory.createMatteBorder(
                        0, 0, Math.max(t.getInteger(1,this.namespace + ".list.cell.selected.border.thickness","General.list.cell.selected.border.thickness"),0), 0,
                                            t.getColor(new Color(0, 0, 0, 0), this.namespace + ".list.cell.selected.border.color","General.list.cell.selected.border.color")));

                setRolloverCellBackground(  t.getColor(new Color(235, 235, 235), this.namespace + ".list.cell.hover.background","General.list.cell.hover.background"));
                setRolloverCellBorder(
                        BorderFactory.createMatteBorder(
                        0, 0, Math.max(t.getInteger(1,this.namespace + ".list.cell.rollover.border.thickness","General.list.cell.rollover.border.thickness"),0), 0,
                                            t.getColor(new Color(0, 0, 0, 0), this.namespace + ".list.cell.hover.border.color","General.list.cell.hover.border.color")));

                setFont(t.getFont(this.namespace+".list","General.list","General"));
            } else {
                setBackground(              t.getColor(Color.WHITE, "General.list.background"));
                setForeground(              t.getColor(Color.BLACK, "General.list.cell.foreground","General.foreground"));

                setCellBackground(          t.getColor(new Color(215, 215, 215), "General.list.cell.background"));

                setSelectedCellBackground(  t.getColor(new Color(235, 235, 235), "General.list.cell.selected.background"));
                setSelectedCellBorder(
                        BorderFactory.createMatteBorder(
                        0, 0, Math.max(t.getInteger(1,"General.list.cell.selected.border.thickness"),0), 0,
                                t.getColor(new Color(0, 0, 0, 0), "General.list.cell.selected.border")));

                setRolloverCellBackground(  t.getColor(new Color(235, 235, 235), "General.list.cell.hover.background"));
                setRolloverCellBorder(
                        BorderFactory.createMatteBorder(
                        0, 0, Math.max(t.getInteger(1,"General.list.cell.rollover.border.thickness"),0), 0,
                                t.getColor(new Color(0, 0, 0, 0), "General.list.cell.hover.border")));

                setFont(t.getFont("General.list","General"));
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
