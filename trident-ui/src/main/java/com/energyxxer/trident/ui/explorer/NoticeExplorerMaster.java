package com.energyxxer.trident.ui.explorer;

import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.trident.global.Commons;
import com.energyxxer.trident.ui.explorer.base.ExplorerFlag;
import com.energyxxer.trident.ui.explorer.base.ExplorerMaster;
import com.energyxxer.trident.ui.theme.change.ThemeListenerManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by User on 5/16/2017.
 */
public class NoticeExplorerMaster extends ExplorerMaster {

    private ThemeListenerManager tlm = new ThemeListenerManager();

    public NoticeExplorerMaster() {
        tlm.addThemeChangeListener(t -> {
            colors.put("background",t.getColor(Color.WHITE, "Explorer.background"));
            colors.put("item.background",t.getColor(new Color(0,0,0,0), "Explorer.item.background"));
            colors.put("item.foreground",t.getColor(Color.BLACK, "Explorer.item.foreground","General.foreground"));
            colors.put("item.selected.background",t.getColor(Color.BLUE, "Explorer.item.selected.background","Explorer.item.background"));
            colors.put("item.selected.foreground",t.getColor(Color.BLACK, "Explorer.item.selected.foreground","Explorer.item.hover.foreground","Explorer.item.foreground","General.foreground"));
            colors.put("item.rollover.background",t.getColor(new Color(0,0,0,0), "Explorer.item.hover.background","Explorer.item.background"));
            colors.put("item.rollover.foreground",t.getColor(Color.BLACK, "Explorer.item.hover.foreground","Explorer.item.foreground","General.foreground"));

            rowHeight = Math.max(t.getInteger(20,"Explorer.item.height"), 1);
            indentPerLevel = Math.max(t.getInteger(20,"Explorer.item.indent"), 0);
            initialIndent = Math.max(t.getInteger(0,"Explorer.item.initialIndent"), 0);

            selectionStyle = t.getString("Explorer.item.selectionStyle","default:FULL");
            selectionLineThickness = Math.max(t.getInteger(2,"Explorer.item.selectionLineThickness"), 0);

            this.setFont(t.getFont("Explorer.item","General"));

            assets.put("expand", Commons.getIcon("triangle_right").getScaledInstance(16, 16, Image.SCALE_SMOOTH));
            assets.put("collapse",Commons.getIcon("triangle_down").getScaledInstance(16, 16, Image.SCALE_SMOOTH));
            assets.put("info",Commons.getIcon("info").getScaledInstance(16, 16, Image.SCALE_SMOOTH));
            assets.put("warning",Commons.getIcon("warn").getScaledInstance(16, 16, Image.SCALE_SMOOTH));
            assets.put("error",Commons.getIcon("error").getScaledInstance(16, 16, Image.SCALE_SMOOTH));
        });

        explorerFlags.put(ExplorerFlag.DYNAMIC_ROW_HEIGHT, true);
    }

    public void addNotice(Notice n) {
        this.children.add(new NoticeItem(this, n));
    }

    public void addNoticeGroup(String label, List<Notice> notices) {
        this.children.add(new NoticeGroupElement(this, label, notices));
    }

    public void clear() {
        this.children.clear();

        this.repaint();
    }

    public void setNotices(HashMap<String, ArrayList<Notice>> map) {
        this.children.clear();

        if(map.containsKey(null)) {
            ArrayList<Notice> standalones = map.get(null);
            standalones.forEach(this::addNotice);
        }
        map.keySet().forEach(k -> {
            if(k != null) this.addNoticeGroup(k, map.get(k));
        });

        this.repaint();
    }
}
