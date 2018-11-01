package com.energyxxer.trident.ui;

import com.energyxxer.trident.global.Commons;
import com.energyxxer.trident.main.window.TridentWindow;
import com.energyxxer.trident.ui.theme.change.ThemeListenerManager;
import com.energyxxer.util.Constant;
import com.energyxxer.xswing.hints.Hint;
import com.energyxxer.xswing.hints.TextHint;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * Represents a single button in the toolbar.
 */
public class ToolbarButton extends JButton implements MouseListener, MouseMotionListener {

    private static final int MARGIN = 1;
    private static final int BORDER_THICKNESS = 1;
    //public static final int SIZE = 25;

    private Color background = Color.GRAY;
    private Color rolloverBackground = Color.GRAY;
    private Color pressedBackground = Color.GRAY;
    private Color border = Color.BLACK;
    private Color rolloverBorder = Color.BLACK;
    private Color pressedBorder = Color.BLACK;

	private String hintText = "";
	private Constant preferredHintPos = Hint.BELOW;

	private boolean rollover = false;

	private String icon;
	private int iconSize;

	private boolean sizeValid = false;

	public ToolbarButton(String icon, ThemeListenerManager tlm) {
		this.setContentAreaFilled(false);
		this.setOpaque(false);
		this.setBackground(new Color(0,0,0,0));

		this.icon = icon;

		//this.setPreferredSize(new Dimension(25, 25));
		this.setBorder(BorderFactory.createEmptyBorder());

		tlm.addThemeChangeListener(t -> {
            this.background = t.getColor(Color.GRAY, "Toolbar.button.background", "General.button.background");
            this.rolloverBackground = t.getColor(Color.GRAY, "Toolbar.button.hover.background", "General.button.hover.background", "Toolbar.button.background", "General.button.background");
            this.pressedBackground = t.getColor(Color.GRAY, "Toolbar.button.pressed.background", "General.button.pressed.background", "Toolbar.button.hover.background", "General.button.hover.background", "Toolbar.button.background", "General.button.background");
            this.border = t.getColor(Color.BLACK, "Toolbar.button.border.color", "General.button.border.color");
            this.rolloverBorder = t.getColor(Color.BLACK, "Toolbar.button.hover.border.color", "General.button.hover.border.color", "Toolbar.button.border.color", "General.button.border.color");
            this.pressedBorder = t.getColor(Color.BLACK, "Toolbar.button.pressed.border.color", "General.button.pressed.border.color", "Toolbar.button.hover.border.color", "General.button.hover.border.color", "Toolbar.button.border.color", "General.button.border.color");

			this.setForeground(t.getColor(Color.BLACK, "Toolbar.button.foreground", "General.button.foreground", "General.foreground"));
			this.setFont(t.getFont( "Toolbar.button", "General.button", "General"));

			updateSize();
        });

		this.setFocusPainted(false);

		this.addMouseListener(this);
		this.addMouseMotionListener(this);

		updateSize();
	}

	@Override
	protected void paintComponent(Graphics g) {
		if(!sizeValid) {
			updateSize();
			this.getParent().revalidate();
			this.repaint();
			return;
		}
		Graphics2D g2 = (Graphics2D) g;

		Composite previousComposite = g2.getComposite();

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(getModel().isPressed() ? pressedBackground : getModel().isRollover() ? rolloverBackground : background);
        g.fillRect(MARGIN+BORDER_THICKNESS,MARGIN+BORDER_THICKNESS,this.getWidth()-2*MARGIN-2*BORDER_THICKNESS,this.getHeight()-2*MARGIN-2*BORDER_THICKNESS);
        g.setColor(getModel().isPressed() ? pressedBorder : getModel().isRollover() ? rolloverBorder : border);
        g.fillRect(MARGIN,MARGIN,this.getWidth()-2*MARGIN-BORDER_THICKNESS,BORDER_THICKNESS);
        g.fillRect(this.getWidth()-MARGIN-BORDER_THICKNESS,MARGIN,BORDER_THICKNESS,this.getHeight()-2*MARGIN-BORDER_THICKNESS);
        g.fillRect(MARGIN+BORDER_THICKNESS,this.getHeight()-MARGIN-BORDER_THICKNESS,this.getWidth()-2*MARGIN-BORDER_THICKNESS,BORDER_THICKNESS);
        g.fillRect(MARGIN,MARGIN+BORDER_THICKNESS,BORDER_THICKNESS,this.getHeight()-2*MARGIN-BORDER_THICKNESS);

		g2.setComposite(previousComposite);

		super.paintComponent(g);
	}

	public String getHintText() {
		return hintText;
	}

	public void setHintText(String hintText) {
		this.hintText = hintText;
	}

	public Constant getPreferredHintPos() {
		return preferredHintPos;
	}

	public void setPreferredHintPos(Constant preferredPos) {
		this.preferredHintPos = preferredPos;
	}

	@Override
	public void setText(String text) {
		super.setText(text);
		updateSize();
	}

	private Dimension getBestSize() {
		int width = 0;
		int height = 0;

		if(this.getGraphics() == null) {
			sizeValid = false;
			return this.getPreferredSize();
		}

		FontMetrics metrics = this.getGraphics().getFontMetrics(this.getFont());

		width += iconSize;
		width += 9;

		String text = this.getText();
		if(text != null && text.length() > 0) {
			width += 6;
			width += metrics.stringWidth(getText());

			height += Math.max(metrics.getHeight() + metrics.getAscent() + metrics.getDescent(),iconSize+9);
		} else {
			height += iconSize;
			height += 9;
		}

		sizeValid = true;
		return new Dimension(width, height);
	}

	private void updateSize() {
		this.setPreferredSize(adjustSize(getBestSize()));
		updateIcon();
	}

	private void updateIcon() {
		if(icon != null) {
			int newIconSize = (Math.max(this.getBestSize().height,25)/25)*16;
			if(newIconSize != iconSize) {
				this.setIcon(new ImageIcon(Commons.getIcon(icon).getScaledInstance(newIconSize, newIconSize, Image.SCALE_SMOOTH)));
				iconSize = newIconSize;
				updateSize();
			}
		}
	}

	private static Dimension adjustSize(Dimension size) {
		size.width = Math.max(size.width,25);
		size.height = Math.max(size.height,25);
		size.height /= 25;
		size.height *= 25;
		return size;
	}

	@Override
	public void setSize(int width, int height) {
		super.setSize(Math.max(width,25), Math.max(height,25));
	}

	@Override
	public void mouseClicked(MouseEvent e) {

	}

	@Override
	public void mousePressed(MouseEvent e) {

	}

	@Override
	public void mouseReleased(MouseEvent e) {

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
	public void mouseDragged(MouseEvent e) {

	}

	@Override
	public void mouseMoved(MouseEvent e) {
		rollover = true;
		TextHint hint = TridentWindow.toolbar.hint;
		if(!hint.isShowing()) {
			hint.setText(hintText);
			hint.setPreferredPos(this.preferredHintPos);
			Point point = this.getLocationOnScreen();
			point.x += this.getWidth()/2;
			point.y += this.getHeight()/2;
			HintStylizer.style(hint);
			hint.show(point, () -> rollover && this.isShowing());
		}
	}
}
