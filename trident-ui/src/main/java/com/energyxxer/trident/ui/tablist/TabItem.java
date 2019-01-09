package com.energyxxer.trident.ui.tablist;

import com.energyxxer.trident.global.TabManager;
import com.energyxxer.trident.ui.Tab;
import com.energyxxer.trident.ui.styledcomponents.StyledMenuItem;
import com.energyxxer.trident.ui.styledcomponents.StyledPopupMenu;
import com.energyxxer.trident.ui.theme.Theme;
import com.energyxxer.trident.util.ImageUtil;
import com.energyxxer.util.StringUtil;

import java.awt.*;
import java.awt.event.MouseEvent;

public class TabItem extends TabListElement {
    private final Tab associatedTab;

    private Image icon = null;
    private String name = null;

    private int x = 0;
    private int width = 0;

    private boolean closeRollover = false;

    public TabItem(TabListMaster master, Tab associatedTab) {
        super(master);
        this.associatedTab = associatedTab;

        this.updateName();
        this.updateIcon();

        associatedTab.linkTabItem(this);
    }

    void updateIcon() {
        this.icon = associatedTab.token.getIcon();
        if(this.icon != null) {
            this.icon = ImageUtil.fitToSize(this.icon, 16, 16);
        }
        /*if(associatedTab.path.endsWith(".png")) {
            try {
                BufferedImage img = ImageIO.read(new File(associatedTab.path));
                Dimension size = new Dimension(img.getWidth(), img.getHeight());
                if(img.getWidth() < img.getHeight()) {
                    size.height = 16;
                    size.width = Math.max(1,(int) Math.round(16 * (double) img.getWidth() / img.getHeight()));
                } else {
                    size.width = 16;
                    size.height = Math.max(1,(int) Math.round(16 * (double) img.getHeight() / img.getWidth()));
                }
                this.icon = img.getScaledInstance(size.width,size.height, Image.SCALE_SMOOTH);
            } catch(IOException x) {
                this.icon = Commons.getIcon("file").getScaledInstance(16,16, Image.SCALE_SMOOTH);
            }
            return;
        }

        String icon = ProjectManager.getIconFor(new File(associatedTab.path));
        if(icon != null) {
            this.icon = Commons.getIcon(icon).getScaledInstance(16,16, Image.SCALE_SMOOTH);
        } else if(name.endsWith(".trident")) {
            this.icon = Commons.getIcon("entity").getScaledInstance(16, 16,java.awt.Image.SCALE_SMOOTH);
        } else {
            this.icon = Commons.getIcon("file").getScaledInstance(16, 16, java.awt.Image.SCALE_SMOOTH);
        }*/
    }

    @Override
    public void render(Graphics g) {
        g.setFont(master.getFont());
        FontMetrics fm = g.getFontMetrics();

        this.x = master.getOffsetX();
        this.lastRecordedOffset = x;
        int w = 8 + 16 + 2 + fm.stringWidth(this.name) + 10 + 6 + 15;
        this.width = w;
        int h = master.getHeight();

        int offsetX = x;
        if(master.draggedElement == this) offsetX = (int) (master.dragPoint.x - (w * master.dragPivot));

        g.setColor((this.rollover || this.selected) ? master.getColors().get("tab.rollover.background") : master.getColors().get("tab.background"));
        g.fillRect(offsetX, 0, w, h);

        if(this.selected) {
            g.setColor(master.getColors().get("tab.selected.background"));

            switch(master.getSelectionStyle()) {
                case "FULL": {
                    g.fillRect(offsetX, 0, w, h);
                    break;
                }
                case "LINE_LEFT": {
                    g.fillRect(offsetX, 0, master.getSelectionLineThickness(), h);
                    break;
                }
                case "LINE_RIGHT": {
                    g.fillRect(offsetX + h - master.getSelectionLineThickness(), 0, master.getSelectionLineThickness(), h);
                    break;
                }
                case "LINE_TOP": {
                    g.fillRect(offsetX, 0, w, master.getSelectionLineThickness());
                    break;
                }
                case "LINE_BOTTOM": {
                    g.fillRect(offsetX, h - master.getSelectionLineThickness(), w, master.getSelectionLineThickness());
                    break;
                }
            }
        }

        if(icon != null) g.drawImage(icon, offsetX + 16 - icon.getWidth(null)/2, (h-16)/2 + 8 - icon.getHeight(null)/2, null);
        offsetX += 26;

        if(this.selected) {
            g.setColor(master.getColors().get("tab.selected.foreground"));
        } else if(this.rollover) {
            g.setColor(master.getColors().get("tab.rollover.foreground"));
        } else {
            g.setColor(master.getColors().get("tab.foreground"));
        }

        g.drawString(this.name, offsetX + 3, (h+fm.getAscent()-fm.getDescent())/2);

        offsetX += fm.stringWidth(this.name) + 13;

        if(this.closeRollover) {
            g.setColor(master.getColors().get("tab.close.rollover.color"));
        } else {
            g.setColor(master.getColors().get("tab.close.color"));
        }
        if(associatedTab.isSaved()) {
            g.drawLine(offsetX, (h - 6) / 2, offsetX + 6, (h + 6) / 2);
            g.drawLine(offsetX, (h + 6) / 2, offsetX + 6, (h - 6) / 2);
        } else {
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.fillOval(offsetX, (h - 6) / 2, 6, 6);
        }
        g.dispose();
    }

    private boolean isOverCloseButton(MouseEvent e) {
        int padding = 5;
        return (e.getX() >= this.x + this.width - 12 - 6 - padding && e.getX() <= this.x + this.width - 12 + padding);
    }

    @Override
    public boolean select(MouseEvent e) {
        return !isOverCloseButton(e);
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public String getToolTipText() {
        return associatedTab.token.getHint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if(isOverCloseButton(e) || e.getButton() == MouseEvent.BUTTON2) {
            TabManager.closeTab(this.associatedTab);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if(!isOverCloseButton(e)) {
            if(e.getButton() == MouseEvent.BUTTON1) {
                TabManager.setSelectedTab(this.associatedTab);
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if(e.isPopupTrigger()) {
            StyledPopupMenu menu = this.generatePopup();
            menu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {
        closeRollover = false;
    }

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {
        closeRollover = isOverCloseButton(e);
    }

    public Image getIcon() {
        return icon;
    }

    public void updateName() {
        this.name = StringUtil.ellipsis(associatedTab.getName(),32);
    }

    private StyledPopupMenu generatePopup() {
        StyledPopupMenu menu = new StyledPopupMenu();

        {
            StyledMenuItem item = new StyledMenuItem("Close");
            item.addActionListener(e -> TabManager.closeTab(associatedTab));
            menu.add(item);
        }
        {
            StyledMenuItem item = new StyledMenuItem("Close Others");
            item.addActionListener(e -> {
                for(int i = 0; i < TabManager.openTabs.size();) {
                    Tab tab = TabManager.openTabs.get(i);
                    if(tab != associatedTab) {
                        TabManager.closeTab(tab);
                    } else {
                        i++;
                    }
                }
            });
            menu.add(item);
        }
        {
            StyledMenuItem item = new StyledMenuItem("Close Tabs To The Left");
            item.addActionListener(e -> {
                for(int i = 0; i < master.children.size();) {
                    TabListElement tabListElement = master.children.get(i);
                    if(tabListElement instanceof TabItem) {
                        Tab tab = ((TabItem) tabListElement).associatedTab;
                        if(tab != associatedTab) {
                            TabManager.closeTab(tab);
                        } else {
                            break;
                        }
                    }
                }
            });
            menu.add(item);
        }
        {
            StyledMenuItem item = new StyledMenuItem("Close Tabs To The Right");
            item.addActionListener(e -> {
                boolean doClose = false;
                for(int i = 0; i < master.children.size();) {
                    TabListElement tabListElement = master.children.get(i);
                    if(tabListElement instanceof TabItem) {
                        Tab tab = ((TabItem) tabListElement).associatedTab;
                        if(tab != associatedTab) {
                            if(doClose) TabManager.closeTab(tab);
                            else i++;
                        } else {
                            doClose = true;
                            i++;
                        }
                    }
                }
            });
            menu.add(item);
        }
        menu.addSeparator();
        {
            StyledMenuItem item = new StyledMenuItem("Close All");
            item.addActionListener(e -> {
                while(!TabManager.openTabs.isEmpty()) {
                    TabManager.closeTab(TabManager.openTabs.get(0));
                }
            });
            menu.add(item);
        }
        return menu;
    }

    public Tab getAssociatedTab() {
        return associatedTab;
    }

    @Override
    public void themeChanged(Theme t) {
        this.updateIcon();
    }
}
