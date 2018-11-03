package com.energyxxer.trident.util;

import java.awt.*;

public class ImageUtil {

    public static Image fitToSize(Image img, int maxWidth, int maxHeight) {
        Dimension size = new Dimension(img.getWidth(null), img.getHeight(null));
        if(size.width < size.height) {
            size.width = Math.max(1,(int) Math.round(maxWidth * (double) size.width / size.height));
            size.height = maxHeight;
        } else if(size.height < size.width) {
            size.height = Math.max(1,(int) Math.round(maxHeight * (double) size.height / size.width));
            size.width = maxWidth;
        } else size = new Dimension(maxWidth, maxHeight);
        return img.getScaledInstance(size.width,size.height, Image.SCALE_SMOOTH);
    }

    private ImageUtil() {
    }
}
