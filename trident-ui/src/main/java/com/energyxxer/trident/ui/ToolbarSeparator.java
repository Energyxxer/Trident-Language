package com.energyxxer.trident.ui;

import com.energyxxer.trident.ui.theme.change.ThemeListenerManager;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

/**
 * It's literally just a line.
 */
public class ToolbarSeparator extends JComponent {

	private ThemeListenerManager tlm = new ThemeListenerManager();

	private Color left = Color.BLACK;
	private Color right = Color.WHITE;

	public ToolbarSeparator() {
		this.setPreferredSize(new Dimension(15, 25));

		this.setOpaque(true);
		this.setBackground(new Color(0,0,0,0));

		tlm.addThemeChangeListener(t -> {
			left = t.getColor(new Color(150, 150, 150), "Toolbar.separator.dark");
			right = t.getColor(new Color(235, 235, 235), "Toolbar.separator.light");
		});
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		int x = this.getWidth()/2-1;

		g.setColor(left);
		g.fillRect(x,2,1,this.getHeight()-4);
		x++;
		g.setColor(right);
		g.fillRect(x,2,1,this.getHeight()-4);

		g.dispose();
	}
}
