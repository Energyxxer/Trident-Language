package com.energyxxer.trident.ui.navbar;

import com.energyxxer.trident.global.Commons;
import com.energyxxer.trident.ui.HintStylizer;
import com.energyxxer.xswing.hints.TextHint;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public class NavigationItem extends NavigationElement {

    private String icon = null;
    private String hintText = null;

    private ArrayList<Runnable> actions = new ArrayList<>();

    public NavigationItem(NavigatorMaster master, String icon) {
        super(master);
        this.icon = icon;
    }

    @Override
    public Rectangle render(Graphics g, Point p) {

        int width = 30;
        int height = 30;

        g.setColor((this.rollover || this.selected) ? master.getColorMap().get("item.rollover.background") : master.getColorMap().get("item.background"));
        g.fillRect(p.x, p.y, width, height);
        if(this.selected) {
            g.setColor(master.getColorMap().get("item.selected.background"));

            switch(master.getSelectionStyle()) {
                case "FULL": {
                    g.fillRect(p.x, p.y, width, height);
                    break;
                }
                case "LINE_LEFT": {
                    g.fillRect(p.x, p.y, master.getSelectionLineThickness(), height);
                    break;
                }
                case "LINE_RIGHT": {
                    g.fillRect(p.x + width - master.getSelectionLineThickness(), p.y, master.getSelectionLineThickness(), height);
                    break;
                }
                case "LINE_TOP": {
                    g.fillRect(p.x, p.y, width, master.getSelectionLineThickness());
                    break;
                }
                case "LINE_BOTTOM": {
                    g.fillRect(p.x, p.y + height - master.getSelectionLineThickness(), width, master.getSelectionLineThickness());
                    break;
                }
            }
        }

        if(icon != null) g.drawImage(Commons.getIcon(icon).getScaledInstance(16, 16, Image.SCALE_SMOOTH), p.x+(width-16)/2, p.y+(height-16)/2, null);
        this.setBounds(new Rectangle(p.x,p.y,width,height));
        return this.getBounds();
    }

    public String getHintText() {
        return hintText;
    }

    public void setHintText(String hintText) {
        this.hintText = hintText;
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if(e.getButton() == MouseEvent.BUTTON1) {
            master.setSelected(this);
            actions.forEach(Runnable::run);
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {
        TextHint hint = master.getHint();
        Rectangle bounds = this.getBounds();

        if(hintText != null && bounds != null && !hint.isShowing()) {
            hint.setText(hintText);
            Point loc = new Point(e.getXOnScreen()-e.getX(),e.getYOnScreen()-e.getY());
            loc.x += bounds.x+bounds.width/2;
            loc.y += bounds.y+bounds.height/2;
            HintStylizer.style(hint);
            hint.show(loc, this::isRollover);
        }
    }

    public void addSelectAction(Runnable action) {
        this.actions.add(action);
    }

    public void removeSelectAction(Runnable action) {
        this.actions.remove(action);
    }
}
