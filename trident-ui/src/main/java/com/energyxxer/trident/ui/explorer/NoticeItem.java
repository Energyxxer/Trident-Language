package com.energyxxer.trident.ui.explorer;

import com.energyxxer.trident.global.TabManager;
import com.energyxxer.trident.ui.explorer.base.ExplorerFlag;
import com.energyxxer.trident.ui.explorer.base.ExplorerMaster;
import com.energyxxer.trident.ui.explorer.base.elements.ExplorerElement;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.trident.ui.modules.FileModuleToken;
import com.energyxxer.trident.ui.modules.ModuleToken;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.io.File;

/**
 * Created by User on 5/16/2017.
 */
public class NoticeItem extends ExplorerElement {
    private Notice notice;

    private int x;

    public NoticeItem(ExplorerMaster master, Notice notice) {
        super(master);
        this.notice = notice;

        this.x = master.getInitialIndent();
    }

    public NoticeItem(NoticeGroupElement parent, Notice notice) {
        super(parent.getMaster());
        this.notice = notice;

        this.x = (parent.indentation + 1) * master.getIndentPerLevel() + master.getInitialIndent();
    }

    @Override
    public void render(Graphics g) {
        g.setFont(master.getFont());
        int y = master.getOffsetY();
        master.getFlatList().add(this);

        int x = this.x + 23;

        g.setColor((this.rollover || this.selected) ? master.getColorMap().get("item.rollover.background") : master.getColorMap().get("item.background"));
        g.fillRect(0, master.getOffsetY(), master.getWidth(), master.getRowHeight());
        if(this.selected) {
            g.setColor(master.getColorMap().get("item.selected.background"));

            switch(master.getSelectionStyle()) {
                case "FULL": {
                    g.fillRect(0, master.getOffsetY(), master.getWidth(), master.getRowHeight());
                    break;
                }
                case "LINE_LEFT": {
                    g.fillRect(0, master.getOffsetY(), master.getSelectionLineThickness(), master.getRowHeight());
                    break;
                }
                case "LINE_RIGHT": {
                    g.fillRect(master.getWidth() - master.getSelectionLineThickness(), master.getOffsetY(), master.getSelectionLineThickness(), master.getRowHeight());
                    break;
                }
                case "LINE_TOP": {
                    g.fillRect(0, master.getOffsetY(), master.getWidth(), master.getSelectionLineThickness());
                    break;
                }
                case "LINE_BOTTOM": {
                    g.fillRect(0, master.getOffsetY() + master.getRowHeight() - master.getSelectionLineThickness(), master.getWidth(), master.getSelectionLineThickness());
                    break;
                }
            }
        }

        //File Icon
        {
            int margin = ((master.getRowHeight() - 16) / 2);
            g.drawImage(master.getAssetMap().get(notice.getType().name().toLowerCase()),x,y + margin,16, 16,new Color(0,0,0,0),null);
        }
        x += 25;

        //File Name

        if(this.selected) {
            g.setColor(master.getColorMap().get("item.selected.foreground"));
        } else if(this.rollover) {
            g.setColor(master.getColorMap().get("item.rollover.foreground"));
        } else {
            g.setColor(master.getColorMap().get("item.foreground"));
        }
        FontMetrics metrics = g.getFontMetrics(g.getFont());

        g.drawString(notice.getMessage(), x, master.getOffsetY() + metrics.getAscent() + ((master.getRowHeight() - metrics.getHeight())/2));
        x += metrics.stringWidth(notice.getMessage());

        if(master.getFlag(ExplorerFlag.DEBUG_WIDTH)) {
            g.setColor(Color.YELLOW);
            g.fillRect(master.getContentWidth()-2, master.getOffsetY(), 2, master.getRowHeight());
            g.setColor(Color.GREEN);
            g.fillRect(x-2, master.getOffsetY(), 2, master.getRowHeight());
        }

        master.setOffsetY(master.getOffsetY() + master.getRowHeight());
        master.setContentWidth(Math.max(master.getContentWidth(), x));
        for(ExplorerElement i : children) {
            i.render(g);
        }
    }

    @Override
    public ModuleToken getToken() {
        return null;
    }

    @Override
    public int getHeight() {
        return master.getRowHeight();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if(e.getButton() == MouseEvent.BUTTON1 && !e.isControlDown() && e.getClickCount() % 2 == 0 && notice.getFilePath() != null) {
            TabManager.openTab(new FileModuleToken(new File(notice.getFilePath())), notice.getLocationIndex(), notice.getLocationLength());
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if(e.getButton() == MouseEvent.BUTTON1) {
            master.setSelected(this, e);
        } else if(e.getButton() == MouseEvent.BUTTON3) {
            /*if(!this.selected) master.setSelected(this, new MouseEvent(e.getComponent(), e.getID(), e.getWhen(), 0, e.getX(), e.getY(), e.getClickCount(), e.isPopupTrigger(), MouseEvent.BUTTON1));
            StyledPopupMenu menu = this.generatePopup();
            menu.show(e.getComponent(), e.getX(), e.getY());*/
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}
