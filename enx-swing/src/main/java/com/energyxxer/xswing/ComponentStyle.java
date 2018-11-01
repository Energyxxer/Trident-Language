package com.energyxxer.xswing;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class ComponentStyle {
	public Color background = SystemDefaults.BACKGROUND;
	public Color foreground = SystemDefaults.FOREGROUND;
	public Font font = SystemDefaults.FONT;
	public Border border = SystemDefaults.BORDER;
	
	public ComponentStyle() {
		background = SystemDefaults.BACKGROUND;
		foreground = SystemDefaults.FOREGROUND;
		font = SystemDefaults.FONT;
		border = SystemDefaults.BORDER;
	}
	
	public ComponentStyle(Object ignore) {
		background = null;
		foreground = null;
		font = null;
		border = null;
	}
	
	public static final ComponentStyle DEFAULT = new ComponentStyle();
	
	public void applyStyle(JComponent c) {
		if(background != null) c.setBackground(background);
		if(foreground != null) c.setForeground(foreground);
		if(font != null) c.setFont(font);
		if(border != null) {c.setBorder(border);} else {c.setBorder(BorderFactory.createEmptyBorder(0,0,1,0));}
	}
	
	public Color getBackground() {
		return background;
	}
	public void setBackground(Color background) {
		this.background = background;
		//return this;
	}
	public Color getForeground() {
		return foreground;
	}
	public void setForeground(Color foreground) {
		this.foreground = foreground;
		//return this;
	}
	public Font getFont() {
		return font;
	}
	public void setFont(Font font) {
		this.font = font;
		//return this;
	}
	public Border getBorder() {
		return border;
	}
	public void setBorder(Border border) {
		this.border = border;
		//return this;
	}
	
}
