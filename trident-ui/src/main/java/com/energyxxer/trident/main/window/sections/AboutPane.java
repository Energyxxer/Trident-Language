package com.energyxxer.trident.main.window.sections;

import com.energyxxer.trident.main.window.TridentWindow;
import com.energyxxer.trident.ui.decorationpane.DecorationObject;
import com.energyxxer.trident.ui.decorationpane.DecorationPane;
import com.energyxxer.util.ImageManager;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.net.URI;

import static com.energyxxer.trident.main.Trident.VERSION;

public class AboutPane extends DecorationPane {

    public static final AboutPane INSTANCE = new AboutPane();

    private AboutPane() {
        super(TridentWindow.jframe, new Dimension(700, 460), ImageManager.load("/assets/logo/about.png"));

        objects.add(new DecorationObject(470, 320, 0, 0) {
            @Override
            public void paint(Graphics g) {
                g.setColor(new Color(187, 187, 187));
                g.setFont(g.getFont().deriveFont(32f));
                g.drawString("v." + VERSION.toString(), this.x, this.y);
            }
        });

        objects.add(new DecorationObject(12, 405, 32, 32) {
            private Image icon = ImageManager.load("/assets/icons/other/discord.png");
            private boolean rollover = false;
            private boolean pressed = false;
            private int padding = 5;

            @Override
            public void paint(Graphics g) {
                if(pressed || rollover) {
                    g.setColor(new Color(255, 255, 255, (pressed) ? 32 : 16));
                    g.fillOval(this.x-padding, this.y-padding, this.w+2*padding, this.h+2*padding);
                }
                g.drawImage(icon, this.x, this.y, this.w, this.h, null);
            }

            @Override
            public Cursor getCursor() {
                return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
            }

            @Override
            public String getToolTipText() {
                return "Join the Official Community Discord";
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI("https://discord.gg/VpfA3c6"));
                } catch (Exception x) {
                    x.printStackTrace();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                rollover = true;
            }

            @Override
            public void mouseExited(MouseEvent e) {
                rollover = false;
            }

            @Override
            public void mousePressed(MouseEvent e) {
                pressed = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                pressed = false;
            }
        });

        objects.add(new DecorationObject(700 - 12 - 32, 405, 32, 32) {
            private Image icon = ImageManager.load("/assets/icons/other/energyxxer.png");
            private boolean rollover = false;
            private boolean pressed = false;
            private int padding = 5;

            @Override
            public void paint(Graphics g) {
                if(pressed || rollover) {
                    g.setColor(new Color(255, 255, 255, (pressed) ? 32 : 16));
                    g.fillOval(this.x-padding, this.y-padding, this.w+2*padding, this.h+2*padding);
                }
                g.drawImage(icon, this.x, this.y, this.w, this.h, null);
            }

            @Override
            public Cursor getCursor() {
                return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
            }

            @Override
            public String getToolTipText() {
                return "Visit Energyxxer's Website";
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI("http://energyxxer.com"));
                } catch (Exception x) {
                    x.printStackTrace();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                rollover = true;
            }

            @Override
            public void mouseExited(MouseEvent e) {
                rollover = false;
            }

            @Override
            public void mousePressed(MouseEvent e) {
                pressed = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                pressed = false;
            }
        });

        revalidate();
        repaint();
    }
}
