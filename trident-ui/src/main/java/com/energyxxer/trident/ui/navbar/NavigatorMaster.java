package com.energyxxer.trident.ui.navbar;

import com.energyxxer.trident.main.window.TridentWindow;
import com.energyxxer.util.Constant;
import com.energyxxer.xswing.hints.TextHint;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.HashMap;

public class NavigatorMaster extends JComponent implements MouseListener, MouseMotionListener {

    public static final Constant HORIZONTAL = new Constant("HORIZONTAL");
    public static final Constant VERTICAL = new Constant("VERTICAL");

    private Constant orientation = HORIZONTAL;

    private ArrayList<NavigationElement> elements = new ArrayList<>();

    protected HashMap<String, Color> colors = new HashMap<>();

    protected String selectionStyle = "FULL";
    protected int selectionLineThickness = 2;

    protected NavigationElement rolloverElement = null;
    protected NavigationElement selectedElement = null;

    protected TextHint hint = TridentWindow.hintManager.createTextHint("Sample Text");

    public NavigatorMaster() {
        this(HORIZONTAL);
    }

    public NavigatorMaster(Constant orientation) {
        this.setOrientation(orientation);

        this.addMouseListener(this);
        this.addMouseMotionListener(this);

        this.setPreferredSize(new Dimension(30, 30));
    }

    public Constant getOrientation() {
        return orientation;
    }

    public void setOrientation(Constant orientation) {
        if(orientation != HORIZONTAL && orientation != VERTICAL) throw new IllegalArgumentException("Navigator orientation must be either NavigatorMaster.HORIZONTAL or NavigatorMaster.VERTICAL. Found: '" + orientation.toString() + "'");
        this.orientation = orientation;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(colors.get("background"));
        g.fillRect(0,0,getWidth(),getHeight());

        Rectangle rect = null;
        Point p = new Point();

        for(NavigationElement elem : elements) {
            Rectangle drawn = elem.render(g.create(), p);
            if(drawn != null) {
                if(rect == null) rect = drawn;
                else rect = rect.union(drawn);

                if(orientation == HORIZONTAL) {
                    p = new Point(rect.x+rect.width,rect.y);
                } else {
                    p = new Point(rect.x,rect.y+rect.height);
                }
            }
        }

        Dimension size = (rect != null) ? new Dimension(rect.width+2*rect.x,rect.height+2*rect.y) : new Dimension();

        if(size.width < 5) size.width = 5;
        if(size.height < 5) size.height = 5;

        if(!this.getPreferredSize().equals(size)) {
            this.setPreferredSize(size);
        }
    }

    public void addElement(NavigationElement elem) {
        elements.add(elem);
    }

    public void removeElement(NavigationElement elem) {
        elements.remove(elem);
    }

    public void setSelected(NavigationElement elem) {
        if(selectedElement != null) {
            selectedElement.selected = false;
            selectedElement = null;
        }
        if(elem != null) elem.selected = true;
        selectedElement = elem;
        repaint();
    }

    protected NavigationElement getElementAtPos(Point p) {
        for(NavigationElement elem : elements) {
            if(elem.getBounds() != null && elem.getBounds().contains(p)) return elem;
        }
        return null;
    }

    protected void setRollover(NavigationElement elem) {
        if(rolloverElement != null) {
            rolloverElement.rollover = false;
            rolloverElement = null;
        }
        if(elem != null) elem.rollover = true;
        rolloverElement = elem;
        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        NavigationElement element = getElementAtPos(e.getPoint());
        if(element != null) element.mouseClicked(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        NavigationElement element = getElementAtPos(e.getPoint());
        if(element != null) element.mousePressed(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        NavigationElement element = getElementAtPos(e.getPoint());
        if(element != null) element.mouseReleased(e);
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {
        if(rolloverElement != null) rolloverElement.mouseExited(e);
        setRollover(null);
    }

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {
        NavigationElement element = getElementAtPos(e.getPoint());
        if(rolloverElement != element) {
            if(rolloverElement != null) rolloverElement.mouseExited(e);
            if(element != null) element.mouseEntered(e);
        }
        setRollover(element);
        if(element != null) element.mouseMoved(e);
    }

    public HashMap<String, Color> getColorMap() {
        return colors;
    }

    public String getSelectionStyle() {
        return selectionStyle;
    }

    public void setSelectionStyle(String selectionStyle) {
        this.selectionStyle = selectionStyle;
    }

    public int getSelectionLineThickness() {
        return selectionLineThickness;
    }

    public void setSelectionLineThickness(int selectionLineThickness) {
        this.selectionLineThickness = selectionLineThickness;
    }

    public TextHint getHint() {
        return hint;
    }
}