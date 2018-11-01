package com.energyxxer.trident.util.linenumber;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyleConstants;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.HashMap;

public class TextLineNumber extends JPanel
		implements CaretListener, DocumentListener, AdjustmentListener
{
	public static final float LEFT = 0.0f;
	public static final float CENTER = 0.5f;
	public static final float RIGHT = 1.0f;

	private final int HEIGHT = getPreferredSize().height;

	private JTextComponent component;
	private JScrollPane scrollPane;

	private Color currentLineForeground = null;
	private Border customBorder = new EmptyBorder(0,0,0,0);
	private float digitAlignment;
	private int minimumDigits;
	private int padding;

	private HashMap<String, FontMetrics> fonts;

	private int lastDigits = 0;
	private int lastHeight = 0;
	private int lastLine = -1;

	public TextLineNumber(JTextComponent component, JScrollPane scrollPane) {
		this(component, scrollPane, 3);
	}

	public TextLineNumber(JTextComponent component, JScrollPane scrollPane, int minimumDigits) {
		this.component = component;
		this.scrollPane = scrollPane;
		this.minimumDigits = minimumDigits;
		digitAlignment = RIGHT;
		setPadding(5);
		component.getDocument().addDocumentListener(this);
		component.addCaretListener( this );
		scrollPane.getVerticalScrollBar().addAdjustmentListener(this);
		setOpaque(false);
	}

	public void setPadding(int padding) {
		this.padding = padding;
		updateWidth();
	}

	private int getLineCount() {
		Element root = component.getDocument().getDefaultRootElement();
		return root.getElementCount();
	}

	private void updateWidth() {

		int digits = Math.max(String.valueOf(getLineCount()).length(), minimumDigits);

		if(digits != lastDigits) {
			int digitWidth = this.getFontMetrics(this.getFont()).charWidth('0');
			int width = padding + (digitWidth * digits) + padding;

			setPreferredSize(new Dimension(width, 0));
		}
	}

	public void setCurrentLineForeground(Color currentLineForeground) {
		this.currentLineForeground = currentLineForeground;
	}

	private Color getCurrentLineForeground() {
		return (currentLineForeground != null) ? currentLineForeground : getForeground();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		g.setColor(getBackground());
		g.fillRect(0,0,this.getWidth(),this.getHeight());

		try {
			FontMetrics fontMetrics = component.getFontMetrics( component.getFont() );
			int lineHeight = component.modelToView(0).height;
			int availableWidth = getSize().width - (2 * padding);

			Rectangle viewport = scrollPane.getViewport().getViewRect();

			int start = component.modelToView(component.viewToModel(new Point(0, viewport.y))).y - viewport.y; //b1
			int startLine = getLineNumberFor(component.viewToModel(new Point(0, viewport.y))); //a1
			int endLine = getLineCount();

			int caretPosition = getLineNumberFor(component.viewToModel(new Point(0,component.modelToView(component.getCaretPosition()).y)));

			for (int n = startLine; n <= endLine; n++) {
				String label = String.valueOf(n);
				int stringWidth = fontMetrics.stringWidth(label);
				int x = getOffsetX(availableWidth, stringWidth) + padding;
				int y = (start + ((n-startLine+1)*lineHeight)) - (lineHeight/4);

				if(n == caretPosition) g.setColor(getCurrentLineForeground());
				else g.setColor(getForeground());

				g.drawString(label, x, y);
			}

		} catch(BadLocationException e) {
			e.printStackTrace();
		}
	}

	private boolean isCurrentLine(int rowStartOffset)
	{
		int caretPosition = component.getCaretPosition();
		Element root = component.getDocument().getDefaultRootElement();

		return root.getElementIndex(rowStartOffset) == root.getElementIndex(caretPosition);
	}

	protected int getLineNumberFor(int offset) {
		return component.getDocument().getDefaultRootElement().getElementIndex(offset) + 1;
	}

	private int getOffsetX(int availableWidth, int stringWidth)
	{
		return (int)((availableWidth - stringWidth) * digitAlignment);
	}

	/*
	 *  Determine the Y offset for the current row
	 */
	private int getOffsetY(int rowStartOffset, FontMetrics fontMetrics)
			throws BadLocationException
	{
		//  Get the bounding rectangle of the row

		Rectangle r = component.modelToView( rowStartOffset );
		int lineHeight = fontMetrics.getHeight();
		int y = r.y + r.height;
		int descent = 0;

		//  The text needs to be positioned above the bottom of the bounding
		//  rectangle based on the descent of the font(s) contained on the row.

		if (r.height == lineHeight)  // default font is being used
		{
			descent = fontMetrics.getDescent();
		}
		else  // We need to check all the attributes for font changes
		{
			if (fonts == null)
				fonts = new HashMap<String, FontMetrics>();

			Element root = component.getDocument().getDefaultRootElement();
			int index = root.getElementIndex( rowStartOffset );
			Element line = root.getElement( index );

			for (int i = 0; i < line.getElementCount(); i++)
			{
				Element child = line.getElement(i);
				AttributeSet as = child.getAttributes();
				String fontFamily = (String)as.getAttribute(StyleConstants.FontFamily);
				Integer fontSize = (Integer)as.getAttribute(StyleConstants.FontSize);
				String key = fontFamily + fontSize;

				FontMetrics fm = fonts.get( key );

				if (fm == null)
				{
					Font font = new Font(fontFamily, Font.PLAIN, fontSize);
					fm = component.getFontMetrics( font );
					fonts.put(key, fm);
				}

				descent = Math.max(descent, fm.getDescent());
			}
		}

		return y - descent;
	}

	@Override
	public void caretUpdate(CaretEvent e) {
		//  Get the line the caret is positioned on

		int caretPosition = component.getCaretPosition();
		Element root = component.getDocument().getDefaultRootElement();
		int currentLine = root.getElementIndex( caretPosition );

		//  Need to repaint so the correct line number can be highlighted

		if (lastLine != currentLine)
		{
			documentChanged();
			lastLine = currentLine;
		}
	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		documentChanged();
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		documentChanged();
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
		documentChanged();
	}

	private void documentChanged()
	{
		//  View of the component has not been updated at the time
		//  the DocumentEvent is fired
		repaint();/*
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void start()
			{
				try
				{
					int endPos = component.getDocument().getLength();
					Rectangle rect = component.modelToView(endPos);

					if (rect != null && rect.y != lastHeight)
					{
						updateWidth();
						repaint();
						lastHeight = rect.y;
					}
				}
				catch (BadLocationException ex) {  nothing to do  }
			}
		});*/
	}

	@Override
	public void adjustmentValueChanged(AdjustmentEvent e) {
		documentChanged();
	}
}