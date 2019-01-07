package com.energyxxer.trident.ui.explorer.base;

import com.energyxxer.trident.global.TabManager;
import com.energyxxer.trident.ui.explorer.base.elements.ExplorerElement;
import com.energyxxer.trident.ui.modules.ModuleToken;
import com.energyxxer.trident.util.ImageUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public class StandardExplorerItem extends ExplorerElement {
    private ExplorerElement parent = null;

    private ModuleToken token = null;
    private int indentation = 0;

    private boolean expanded = false;

    private Image icon = null;

    private int x = 0;

    public StandardExplorerItem(ModuleToken token, StandardExplorerItem parent, ArrayList<String> toOpen) {
        this(parent, parent.getMaster(), token, toOpen);
    }

    public StandardExplorerItem(ModuleToken token, ExplorerMaster master, ArrayList<String> toOpen) {
        this(null, master, token, toOpen);
    }

    private StandardExplorerItem(StandardExplorerItem parent, ExplorerMaster master, ModuleToken token, ArrayList<String> toOpen) {
        super(master);
        this.parent = parent;
        this.token = token;

        this.icon = token.getIcon();
        if(this.icon != null) this.icon = ImageUtil.fitToSize(this.icon, 16, 16);

        if(parent != null) {
            this.indentation = parent.indentation + 1;
            this.x = indentation * master.getIndentPerLevel() + master.getInitialIndent();
        }

        if(toOpen.contains(this.token.getIdentifier())) {
            expand(toOpen);
        }
    }

    private void expand(ArrayList<String> toOpen) {
        for(ModuleToken subToken : token.getSubTokens()) {
            this.children.add(new StandardExplorerItem(subToken, this, toOpen));
        }
        expanded = true;
        master.getExpandedElements().add(this.token);
        master.repaint();
    }

    private void collapse() {
        this.propagateCollapse();
        this.children.clear();
        expanded = false;
        master.repaint();
    }

    private void propagateCollapse() {
        master.getExpandedElements().remove(this.token);
        for(ExplorerElement element : children) {
            if(element instanceof StandardExplorerItem) ((StandardExplorerItem) element).propagateCollapse();
        }
    }

    public void render(Graphics g) {
        g.setFont(master.getFont());
        int y = master.getOffsetY();
        master.getFlatList().add(this);

        int x = (indentation * master.getIndentPerLevel()) + master.getInitialIndent();

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

        //Expand/Collapse button
        if(token.isExpandable()){
            int margin = ((master.getRowHeight() - 16) / 2);
            if(expanded) {
                g.drawImage(master.getAssetMap().get("collapse"),x,y + margin,16, 16,new Color(0,0,0,0),null);
            } else {
                g.drawImage(master.getAssetMap().get("expand"),x,y + margin,16, 16,new Color(0,0,0,0),null);
            }
        }
        x += 23;

        //File Icon
        if(icon != null) {
            int margin = ((master.getRowHeight() - 16) / 2);
            g.drawImage(this.icon,x + 8 - icon.getWidth(null)/2,y + margin + 8 - icon.getHeight(null)/2, null);
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

        Graphics2D g2d = (Graphics2D) g;
        Composite oldComposite = g2d.getComposite();
        /*if(translucent) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        }*/

        g.drawString(token.getTitle(), x, master.getOffsetY() + metrics.getAscent() + ((master.getRowHeight() - metrics.getHeight())/2));
        x += metrics.stringWidth(token.getTitle());

        g2d.setComposite(oldComposite);

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

    private void open() {
        this.token.onInteract();
        if(token.isExpandable()) {
            if(expanded) collapse();
            else expand(new ArrayList<>());
        } else {
            TabManager.openTab(token);
        }
    }

    @Override
    public int getHeight() {
        return master.getRowHeight();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if(e.getButton() == MouseEvent.BUTTON1 && !e.isControlDown() && e.getClickCount() % 2 == 0 && (!token.isExpandable() || e.getX() < x || e.getX() > x + master.getRowHeight())) {
            this.open();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if(e.getButton() == MouseEvent.BUTTON1) {
            //x = indentation * master.getIndentPerLevel() + master.getInitialIndent();
            if(token.isExpandable() && e.getX() >= x && e.getX() <= x + master.getRowHeight()) {
                if(expanded) collapse();
                else expand(new ArrayList<>());
            } else {
                master.setSelected(this, e);
            }
        }
        confirmActivationMenu(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        confirmActivationMenu(e);
    }

    private void confirmActivationMenu(MouseEvent e) {
        if(e.isPopupTrigger()) {
            if(!this.selected) master.setSelected(this, new MouseEvent(e.getComponent(), e.getID(), e.getWhen(), 0, e.getX(), e.getY(), e.getClickCount(), e.isPopupTrigger(), MouseEvent.BUTTON1));
            JPopupMenu menu = token.generateMenu();
            if(menu != null) menu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public ModuleToken getToken() {
        return token;
    }
}
