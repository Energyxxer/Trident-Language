package com.energyxxer.trident.ui.tablist;

import com.energyxxer.trident.main.window.TridentWindow;
import com.energyxxer.trident.ui.HintStylizer;
import com.energyxxer.trident.ui.Tab;
import com.energyxxer.trident.ui.theme.change.ThemeListenerManager;
import com.energyxxer.xswing.hints.TextHint;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.HashMap;

public class TabListMaster extends JComponent implements MouseListener, MouseMotionListener {
    ArrayList<TabListElement> children = new ArrayList<>();
    private int x = 0;

    private TabListElement rolloverElement = null;
    private TabListElement selectedElement = null;

    private HashMap<String, Color> colors = new HashMap<>();
    private String selectionStyle = "FULL";
    private int selectionLineThickness = 2;

    Point dragPoint = null;
    float dragPivot = -1;
    TabListElement draggedElement = null;
    int height = 5;

    private TextHint hint = TridentWindow.hintManager.createTextHint("a");

    private ThemeListenerManager tlm = new ThemeListenerManager();

    public TabListMaster() {
        tlm.addThemeChangeListener(t -> {
            colors.put("background",t.getColor(Color.WHITE, "TabList.background"));
            colors.put("tab.background",t.getColor(new Color(0,0,0,0), "TabList.tab.background"));
            colors.put("tab.foreground",t.getColor(Color.BLACK, "TabList.tab.foreground","General.foreground"));
            colors.put("tab.close.color",t.getColor(Color.DARK_GRAY, "TabList.tab.close.color"));
            colors.put("tab.close.rollover.color",t.getColor(Color.LIGHT_GRAY, "TabList.tab.close.hover.color"));
            colors.put("tab.selected.background",t.getColor(Color.BLUE, "TabList.tab.selected.background","TabList.tab.background"));
            colors.put("tab.selected.foreground",t.getColor(Color.BLACK, "TabList.tab.selected.foreground","TabList.tab.hover.foreground","TabList.tab.foreground","General.foreground"));
            colors.put("tab.rollover.background",t.getColor(new Color(0,0,0,0), "TabList.tab.hover.background","TabList.tab.background"));
            colors.put("tab.rollover.foreground",t.getColor(Color.BLACK, "TabList.tab.hover.foreground","TabList.tab.foreground","General.foreground"));

            selectionStyle = t.getString("TabList.tab.selectionStyle","default:FULL");
            selectionLineThickness = Math.max(t.getInteger(2,"TabList.tab.selectionLineThickness"), 0);
            height = Math.max(t.getInteger(5,"TabList.height"),5);

            this.setFont(t.getFont("TabList.tab","General"));

            children.forEach(e -> e.themeChanged(t));
        });

        hint.setOutDelay(1);

        this.addMouseListener(this);
        this.addMouseMotionListener(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(colors.get("background"));
        g.fillRect(0,0,this.getWidth(), this.getHeight());

        this.x = 0;

        int draggedX = -1;

        ArrayList<TabListElement> toRender = new ArrayList<>(children);

        for(TabListElement element : toRender) {
            if(element != draggedElement) {
                element.render(g.create());
            } else draggedX = x;
            this.x += element.getWidth();
        }

        Dimension newSize = new Dimension(this.x, height);

        if(!newSize.equals(this.getPreferredSize())) {
            this.setPreferredSize(newSize);
            this.getParent().revalidate();
        }

        if(draggedElement != null) {
            this.x = draggedX;
            draggedElement.render(g.create());
        }
    }

    public void addTab(Tab tab) {
        this.children.add(new TabItem(this, tab));
    }

    int getOffsetX() {
        return x;
    }

    void setOffsetX(int x) {
        this.x = x;
    }

    String getSelectionStyle() {
        return selectionStyle;
    }

    int getSelectionLineThickness() {
        return selectionLineThickness;
    }

    HashMap<String, Color> getColors() {
        return colors;
    }

    private TabListElement getElementAtMousePos(MouseEvent e) {
        int x = 0;
        for(TabListElement element : children) {
            x += element.getWidth();
            if(e.getX() < x) return element;
        }
        return null;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        TabListElement element = getElementAtMousePos(e);
        if(element != null) element.mouseClicked(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        TabListElement element = getElementAtMousePos(e);

        if(e.getButton() == MouseEvent.BUTTON1) {
            dragPoint = e.getPoint();
            draggedElement = element;
            dragPivot = -1;
        }
        if(element == null) return;

        int x = 0;
        for(TabListElement elem : children) {
            int w = elem.getWidth();
            if(e.getX() < x + w) {
                dragPivot = (float) (e.getX()-x)/w;
                break;
            }
            x += w;
        }

        if(e.getButton() == MouseEvent.BUTTON1 && element.select(e)) selectTab(element);

        element.mousePressed(e);
        repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragPoint = null;
        draggedElement = null;
        dragPivot = -1;
        TabListElement element = getElementAtMousePos(e);
        if(element != null) element.mouseReleased(e);
        repaint();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if(rolloverElement != null) {
            rolloverElement.setRollover(false);
            rolloverElement.mouseExited(e);
            rolloverElement = null;
        }
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if(draggedElement != null) {
            draggedElement.mouseDragged(e);
            dragPoint = e.getPoint();
            int x = 0;
            for (int i = 0; i < children.size(); i++) {
                TabListElement element = children.get(i);
                int w = element.getWidth();
                int center = (int) (e.getX() + (0.5 - dragPivot * w));
                if (center >= x && center < x + w) {
                    children.remove(draggedElement);
                    if (center <= x + w / 2) {
                        children.add(i, draggedElement);
                    } else {
                        children.add(Math.min(i + 1, children.size()), draggedElement);
                    }
                    break;
                }
                x += w;
            }
        }
        repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        TabListElement element = getElementAtMousePos(e);
        if(rolloverElement != null) {
            rolloverElement.setRollover(false);
            if(rolloverElement != element) {
                rolloverElement.mouseExited(e);
            }
        }
        if(element != null) {
            element.setRollover(true);
            if(rolloverElement != element) {
                element.mouseEntered(e);
                String text = element.getToolTipText();
                if(text != null && (rolloverElement != null || !hint.isShowing())) {
                    hint.setText(text);
                    HintStylizer.style(hint);
                    hint.show(new Point(this.getLocationOnScreen().x+element.getLastRecordedOffset()+element.getWidth()/2,this.getLocationOnScreen().y+this.getHeight()/2), () -> rolloverElement == element);
                }
            }
            element.mouseMoved(e);
        }
        repaint();
        rolloverElement = element;
    }

    public void removeTab(Tab tab) {
        children.remove(tab.getLinkedTabItem());
        if(selectedElement == tab.getLinkedTabItem()) selectedElement = null;
        if(rolloverElement == tab.getLinkedTabItem()) rolloverElement = null;
        repaint();
    }

    public void selectTab(Tab tab) {
        if(tab != null) selectTab(tab.getLinkedTabItem());
        else selectTab((TabListElement) null);
    }

    private void selectTab(TabListElement element) {
        if(selectedElement != null) {
            selectedElement.setSelected(false);
        }
        selectedElement = element;
        if(element != null) {
            element.setSelected(true);
        }
        repaint();
    }

    public Tab getFallbackTab(Tab tab) {
        ArrayList<Tab> allTabs = new ArrayList<>();
        for(TabListElement element : children) {
            if(element instanceof TabItem) allTabs.add(((TabItem) element).getAssociatedTab());
        }
        int index = allTabs.indexOf(tab);
        if(index == -1) return null;
        allTabs.remove(index);
        if(allTabs.size() == 0) return null;

        Tab left = (index >= 1) ? allTabs.get(index-1) : null;
        Tab right = (index < allTabs.size()) ? allTabs.get(index) : null;
        if(left == null) return right;
        if(right == null) return left;

        return (left.openedTimeStamp > right.openedTimeStamp) ? left : right;
    }
}
