package com.energyxxer.trident.ui.editor;

import com.energyxxer.trident.global.TabManager;
import com.energyxxer.trident.global.temp.Lang;
import com.energyxxer.trident.ui.Tab;
import com.energyxxer.trident.ui.display.DisplayModule;
import com.energyxxer.trident.ui.scrollbar.OverlayScrollPaneLayout;
import com.energyxxer.trident.ui.theme.Theme;
import com.energyxxer.trident.ui.theme.ThemeManager;
import com.energyxxer.trident.ui.theme.change.ThemeChangeListener;
import com.energyxxer.trident.util.linenumber.TextLineNumber;
import com.energyxxer.util.logger.Debug;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Display module for the main text editor of the program.
 */
public class TridentEditorModule extends JScrollPane implements DisplayModule, UndoableEditListener, MouseListener, ThemeChangeListener {

	File file;
	Tab associatedTab;

	public TridentEditorComponent editorComponent;
	private TextLineNumber tln;
	protected Theme syntax;

	private ArrayList<String> styles = new ArrayList<>();
	HashMap<String, String[]> parserStyles = new HashMap<>();


    //public long lastToolTip = new Date().getTime();

	public TridentEditorModule(Tab tab, File file) {
		this.file = file;
        this.associatedTab = tab;

        editorComponent = new TridentEditorComponent(this);
        //editorComponent.setBorder(BorderFactory.createEmptyBorder(0,5,0,0));

		JPanel container = new JPanel(new BorderLayout());
		container.add(editorComponent);
        super.setViewportView(container);

        tln = new TextLineNumber(editorComponent, this);
        tln.setPadding(10);

		this.setBorder(BorderFactory.createEmptyBorder());

		KeyStroke saveKeystroke = KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());

		KeyStroke reloadKeystroke = KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0);

		//editorComponent.getInputMap().put(undoKeystroke, "undoKeystroke");
		//editorComponent.getInputMap().put(redoKeystroke, "redoKeystroke");
		editorComponent.getInputMap(WHEN_IN_FOCUSED_WINDOW).put(reloadKeystroke, "reloadKeystroke");
		editorComponent.getInputMap().put(saveKeystroke, "saveKeystroke");

		editorComponent.getActionMap().put("saveKeystroke", new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Tab st = TabManager.getSelectedTab();
				if(st != null) st.save();
			}
		});

		editorComponent.getActionMap().put("reloadKeystroke", new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e) {
				setTextToFileContents();
			}
		});

		// editorComponent.addMouseMotionListener(hints = new EditorHints(this));
		editorComponent.addMouseListener(this);

		this.setRowHeaderView(tln);

		this.setLayout(new OverlayScrollPaneLayout(this));

		this.getVerticalScrollBar().setUnitIncrement(17);
		this.getHorizontalScrollBar().setUnitIncrement(17);

		/*linePainter.addPaintListener(() -> {
			this.getVerticalScrollBar().repaint();
			this.getHorizontalScrollBar().repaint();
		});*/

		addThemeChangeListener();

		setTextToFileContents();
	}

	private void setTextToFileContents() {
		byte[] encoded;
		try {
			encoded = Files.readAllBytes(file.toPath());
			String s = new String(encoded, Charset.forName("UTF-8"));
			setText(s);
			editorComponent.setCaretPosition(0);
			associatedTab.updateSavedValue();
			startEditListeners();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void startEditListeners() {
		editorComponent.getDocument().addUndoableEditListener(this);
	}

	private void clearStyles() {
		for(String key : this.styles) {
			editorComponent.removeStyle(key);
		}
		for(String key : this.parserStyles.keySet()) {
			editorComponent.removeStyle(key);
		}
		this.styles.clear();
		this.parserStyles.clear();
	}

	private void setSyntax(Theme newSyntax) {
		if(newSyntax == null) {
			syntax = null;
			clearStyles();
			return;
		}
		if(newSyntax.getThemeType() != Theme.ThemeType.SYNTAX_THEME) {
			Debug.log("Theme \"" + newSyntax + "\" is not a syntax theme!", Debug.MessageType.ERROR);
			return;
		}

		this.syntax = newSyntax;
		clearStyles();
		for(String value : syntax.getValues().keySet()) {
			if(!value.contains(".")) continue;
			//if(sections.length > 2) continue;

			String name = value.substring(0,value.lastIndexOf("."));
			Style style = editorComponent.getStyle(name);
			if(style == null) {
				style = editorComponent.addStyle(name, null);
				this.styles.add(name);
				if(name.startsWith("$") && name.contains(".")) {
					parserStyles.put(name, name.substring(1).toUpperCase().split("\\."));
				}
			}
			switch(value.substring(value.lastIndexOf(".")+1)) {
				case "foreground": {
					StyleConstants.setForeground(style, syntax.getColor(value));
					break;
				}
				case "background": {
					StyleConstants.setBackground(style, syntax.getColor(value));
					break;
				}
				case "italic": {
					StyleConstants.setItalic(style, syntax.getBoolean(value));
					break;
				}
				case "bold": {
					StyleConstants.setBold(style, syntax.getBoolean(value));
					break;
				}
			}
		}
	}

	public void setText(String text) {
		editorComponent.setText(text);

		editorComponent.highlight();
	}

	public String getText() {
		return editorComponent.getText();
	}

	@Override
	public void undoableEditHappened(UndoableEditEvent e) {
		if (!e.getEdit().getPresentationName().equals("style change")) {
			editorComponent.highlight();
			associatedTab.onEdit();
		}
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {

	}

	public void ensureVisible(int index) {
        try {
            Rectangle view = this.getViewport().getViewRect();
            Rectangle rect = editorComponent.modelToView(index);
            if(rect == null) return;
            rect.width = 2;
            rect.x -= view.x;
            rect.y -= view.y;
            this.getViewport().scrollRectToVisible(rect);
        } catch (BadLocationException x) {
            x.printStackTrace();
        }
    }

	@Override
	public void themeChanged(Theme t) {
		editorComponent.setBackground(t.getColor(Color.WHITE, "Editor.background"));
		setBackground(editorComponent.getBackground());
		editorComponent.setForeground(t.getColor(Color.BLACK, "Editor.foreground","General.foreground"));
		editorComponent.setCaretColor(editorComponent.getForeground());
		editorComponent.setSelectionColor(t.getColor(new Color(50, 100, 175), "Editor.selection.background"));
		editorComponent.setSelectedTextColor(t.getColor(editorComponent.getForeground(), "Editor.selection.foreground"));
		editorComponent.setCurrentLineColor(t.getColor(new Color(235, 235, 235), "Editor.currentLine.background"));
		editorComponent.setFont(new Font(t.getString("Editor.font","default:monospaced"), 0, 12));
		tln.setBackground(t.getColor(new Color(235, 235, 235), "Editor.lineNumber.background"));
		tln.setForeground(t.getColor(new Color(150, 150, 150), "Editor.lineNumber.foreground"));
		//tln current line background
		tln.setCurrentLineForeground(t.getColor(tln.getForeground(), "TridentEditorModule.lineNumber.currentLine.foreground"));
		tln.setBorder(
				BorderFactory.createCompoundBorder(
						BorderFactory.createMatteBorder(
								0,
								0,
								0,
								Math.max(t.getInteger(1,"Editor.lineNumber.border.thickness"),0),
								t.getColor(new Color(200, 200, 200), "Editor.lineNumber.border.color","General.line")
						),
						BorderFactory.createEmptyBorder(
								0,
								0,
								0,
								15
						)
				)
		);
		tln.setFont(new Font(t.getString("TridentEditorModule.lineNumber.font","default:monospaced"),0,12));

		Lang lang = Lang.getLangForFile(file.getPath());
		if(lang != null) {
			setSyntax(ThemeManager.getSyntaxForGUITheme(lang, t));
			editorComponent.highlight();
		}
	}

	@Override
    public void displayCaretInfo() {
        editorComponent.displayCaretInfo();
    }

	@Override
	public Object getValue() {
		return getText().intern().hashCode();
	}

	@Override
	public boolean canSave() {
		return true;
	}

	@Override
	public Object save() {
		PrintWriter writer;
		try {
			writer = new PrintWriter(file, "UTF-8");

			String text = getText();
			if(!text.endsWith("\n")) {
				text = text.concat("\n");
				try {
					editorComponent.getDocument().insertString(text.length()-1,"\n",null);
				} catch(BadLocationException e) {
					e.printStackTrace();
				}
			}
			writer.print(text);
			writer.close();
			return getValue();
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void focus() {
		editorComponent.requestFocus();
	}
}