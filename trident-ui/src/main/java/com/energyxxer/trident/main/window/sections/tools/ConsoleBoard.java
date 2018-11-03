package com.energyxxer.trident.main.window.sections.tools;

import com.energyxxer.trident.global.TabManager;
import com.energyxxer.trident.ui.modules.FileModuleToken;
import com.energyxxer.trident.ui.scrollbar.OverlayScrollBarUI;
import com.energyxxer.trident.ui.theme.change.ThemeListenerManager;
import com.energyxxer.util.logger.Debug;
import com.energyxxer.util.out.Console;
import com.energyxxer.util.out.ConsoleOutputStream;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

/**
 * Created by User on 12/15/2016.
 */
public class ConsoleBoard extends ToolBoard {

    private static final int CONSOLE_HEIGHT = 200;

    private ThemeListenerManager tlm = new ThemeListenerManager();

    public ConsoleBoard(ToolBoardMaster parent) {
        super(parent);
        this.setLayout(new BorderLayout());
        this.setPreferredSize(new Dimension(0, CONSOLE_HEIGHT));
        tlm.addThemeChangeListener(t -> this.setBorder(BorderFactory.createMatteBorder(Math.max(t.getInteger(1, "Console.header.border.thickness"),0), 0, 0, 0, t.getColor(new Color(200, 200, 200), "Console.header.border.color"))));

        //JPanel consoleHeader = new JPanel(new BorderLayout());
        //tlm.addThemeChangeListener(t -> consoleHeader.setBackground(t.getColor(new Color(235, 235, 235), "Console.header.background")));
        //consoleHeader.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        //consoleHeader.setPreferredSize(new Dimension(0, 25));

        //StyledLabel consoleLabel = new StyledLabel("Console", "Console.header");
        //consoleHeader.add(consoleLabel, BorderLayout.WEST);

        //JPanel consoleActionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
        //consoleActionPanel.setOpaque(false);

        /*ToolbarButton toggle = new ToolbarButton("toggle", tlm);
        toggle.setHintText("Toggle Console");
        toggle.setPreferredHintPos(Hint.LEFT);
        toggle.setPreferredSize(new Dimension(20,20));

        toggle.addActionListener(e -> {
            if (this.getPreferredSize().height == 25) {
                this.setPreferredSize(new Dimension(0, CONSOLE_HEIGHT));
            } else {
                this.setPreferredSize(new Dimension(0, 25));
            }
            this.revalidate();
            this.repaint();
        });


        ToolbarButton clear = new ToolbarButton("clear", tlm);
        clear.setHintText("Clear Console");
        clear.setPreferredHintPos(Hint.LEFT);
        clear.setPreferredSize(new Dimension(20,20));

        consoleActionPanel.add(clear);
        consoleActionPanel.add(toggle);
        consoleHeader.add(consoleActionPanel, BorderLayout.EAST);

        this.add(consoleHeader, BorderLayout.NORTH);*/

        JTextPane console = new JTextPane();
        console.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        tlm.addThemeChangeListener(t -> {
            console.setBackground(t.getColor(Color.WHITE, "Console.background"));
            console.setSelectionColor(t.getColor(new Color(50, 100, 175), "Console.selection.background","General.textfield.selection.background"));
            console.setSelectedTextColor(t.getColor(Color.BLACK, "Console.selection.foreground","General.textfield.selection.foreground","Console.foreground","General.foreground"));
            console.setFont(new Font(t.getString("Console.font","Editor.font","default:monospaced"), 0, 12));
            console.setForeground(t.getColor(Color.BLACK, "Console.foreground"));

            if(console.getStyle("warning") != null) console.removeStyle("warning");
            if(console.getStyle("error") != null) console.removeStyle("error");

            Style warningStyle = console.addStyle("warning", null);
            StyleConstants.setForeground(warningStyle, t.getColor(new Color(255, 140, 0), "Console.warning"));

            Style errorStyle = console.addStyle("error", null);
            StyleConstants.setForeground(errorStyle, t.getColor(new Color(200,50,50), "Console.error"));

            Style debugStyle = console.addStyle("debug", null);
            StyleConstants.setForeground(debugStyle, new Color(104,151,187));
        });
        /*clear.addActionListener(e -> {
            try {
                console.getDocument().remove(0,console.getDocument().getLength());
            } catch(BadLocationException x) {}
        });*/
        console.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                AttributeSet hyperlink = console.getStyledDocument().getCharacterElement(console.viewToModel(e.getPoint())).getAttributes();
                if(hyperlink.containsAttribute("IS_HYPERLINK",true)) {
                    String path = (String) hyperlink.getAttribute("PATH");
                    int location = Integer.parseInt((String) hyperlink.getAttribute("LOCATION"));
                    int length = Integer.parseInt((String) hyperlink.getAttribute("LENGTH"));

                    TabManager.openTab(new FileModuleToken(new File(path)), location, length);
                }
            }
        });
        console.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                AttributeSet hyperlink = console.getStyledDocument().getCharacterElement(console.viewToModel(e.getPoint())).getAttributes();

                console.setCursor(Cursor.getPredefinedCursor((hyperlink.containsAttribute("IS_HYPERLINK",true)) ? Cursor.HAND_CURSOR : Cursor.TEXT_CURSOR));
            }
        });
        console.setEditable(false);
        console.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        //console.setEditorKit(JEditorPane.createEditorKitForContentType("text/html"));

        //tlm.addThemeChangeListener(t -> textConsoleOut.update());

        Debug.addStream(new ConsoleOutputStream(console));
        /*Console.addInfoStream(new ConsoleOutputStream(console));
        Console.addWarnStream(new ConsoleOutputStream(console,"warning"));
        Console.addErrStream(new ConsoleOutputStream(console,"error"));
        Console.addDebugStream(new ConsoleOutputStream(console,"debug"));*/

        //consoleOut = new PrintStream(textConsoleOut);
        //System.setOut(new PrintStream(new MultiOutputStream(consoleOut, System.out)));
        //System.setErr(new PrintStream(new MultiOutputStream(consoleOut, System.err)));

        JScrollPane consoleScrollPane = new JScrollPane(console);

        //consoleScrollPane.setLayout(new OverlayScrollPaneLayout());

        consoleScrollPane.getVerticalScrollBar().setUI(new OverlayScrollBarUI(consoleScrollPane));
        consoleScrollPane.getHorizontalScrollBar().setUI(new OverlayScrollBarUI(consoleScrollPane));
        consoleScrollPane.getVerticalScrollBar().setOpaque(false);
        consoleScrollPane.getHorizontalScrollBar().setOpaque(false);

        tlm.addThemeChangeListener(t -> {
            consoleScrollPane.setBackground(console.getBackground());
            consoleScrollPane.setBorder(BorderFactory.createMatteBorder(Math.max(t.getInteger("Console.header.border.thickness"),0), 0, 0, 0, t.getColor(new Color(200, 200, 200), "Console.header.border.color")));
        });

        this.add(consoleScrollPane, BorderLayout.CENTER);
    }

    @Override
    public String getName() {
        return "Console";
    }

    @Override
    public String getIconName() {
        return "console";
    }
}
