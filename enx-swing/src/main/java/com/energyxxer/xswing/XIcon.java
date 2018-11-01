package com.energyxxer.xswing;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Created by User on 2/11/2017.
 */
public class XIcon extends JPanel {

    private BufferedImage image = null;

    public XIcon() {
    }

    public XIcon(Image image) {
        this((BufferedImage) image);
    }

    public XIcon(BufferedImage image) {
        this.image = image;
    }

    {
        this.setOpaque(false);
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        this.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if(image != null) g.drawImage(image, 0, 0, null);
    }
}
