package com.energyxxer.xswing;

import javax.swing.*;
import java.awt.*;

/**
 * Created by User on 2/11/2017.
 */
public class Padding extends JPanel {

    public Padding() {
        this.setOpaque(false);
    }

    public Padding(int size) {
        this();
        Dimension dim = new Dimension(size, size);
        this.setPreferredSize(dim);
        this.setMaximumSize(dim);
    }
}
