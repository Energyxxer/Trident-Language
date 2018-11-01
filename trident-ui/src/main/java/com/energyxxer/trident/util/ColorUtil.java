package com.energyxxer.trident.util;

import java.awt.Color;

public class ColorUtil {
	public static Color add(Color c1, Color c2) {
		
		float a = c2.getAlpha()/255f;
		int r = (int) (c1.getRed() + (c2.getRed() * a));
		int g = (int) (c1.getGreen() + (c2.getGreen() * a));
		int b = (int) (c1.getBlue() + (c2.getBlue() * a));

		r = Math.min(255, r);
		g = Math.min(255, g);
		b = Math.min(255, b);
		
		return new Color(r, g, b);
	}
	
	public static String toCSS(Color c) {
		int r = c.getRed();
		int g = c.getGreen();
		int b = c.getBlue();
		int a = c.getAlpha();
		if(a == 255) {
			return "rgb(" + r + "," + g + "," + b + ")";
		} else {
			return "rgba(" + r + "," + g + "," + b + "," + ((float) a /255) + ")";
		}
	}

	private ColorUtil() {}
}
