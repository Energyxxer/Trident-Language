package com.energyxxer.util.out;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.io.PrintStream;
import java.util.HashMap;

/**
 * Created by User on 1/8/2017.
 */
public class ConsoleOutputStream extends PrintStream {

    private final JTextPane component;
    public String style = "";

    private static final int NONE = 0,
                             PATH = 1,
                             LOCATION = 2,
                             LENGTH = 3,
                             TEXT = 4;

    private HashMap<String, StringBuilder> hyperLinkElements = new HashMap<>();

    private int hyperLinkStage = NONE;

    public ConsoleOutputStream(final JTextPane component) {
        super(System.out);
        this.component = component;
    }

    public ConsoleOutputStream(JTextPane component, String style) {
        super(System.out);
        this.component = component;
        this.style = style;
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }

    public void setStyle(String style) {
        this.style = style;
    }

    @Override
    public void print(String string) {
        for(char c : string.toCharArray()) {
            print(c);
        }
    }

    @Override
    public void print(char c) {
        if (c == '\r')
            return;

        //Hyperlink Syntax:
        // \b PATH \b LOCATION \b LENGTH \b TEXT \b

        switch(hyperLinkStage) {
            case NONE: {
                if(c == '\b') {
                    hyperLinkStage++;
                    hyperLinkElements.put("PATH",new StringBuilder());
                    hyperLinkElements.put("LOCATION",new StringBuilder());
                    hyperLinkElements.put("LENGTH",new StringBuilder());
                    hyperLinkElements.put("TEXT",new StringBuilder());
                } else {
                    SwingUtilities.invokeLater(() -> {
                        try {
                            component.getStyledDocument().insertString(component.getStyledDocument().getLength(), c + "", (style == null) ? null : component.getStyle(style));
                        } catch(BadLocationException e) {}
                    });
                }
                break;
            }
            case PATH: {
                if(c == '\b') {
                    hyperLinkStage++;
                } else {
                    hyperLinkElements.get("PATH").append(c);
                }
                break;
            }
            case LOCATION: {
                if(c == '\b') {
                    hyperLinkStage++;
                } else if(Character.isDigit(c)) {
                    hyperLinkElements.get("LOCATION").append(c);
                }
                break;
            }
            case LENGTH: {
                if(c == '\b') {
                    hyperLinkStage++;
                } else if(Character.isDigit(c)) {
                    hyperLinkElements.get("LENGTH").append(c);
                }
                break;
            }
            case TEXT: {
                if(c == '\b') {
                    //Finalize
                    hyperLinkStage = NONE;

                    final String path = hyperLinkElements.get("PATH").toString();
                    final String location = hyperLinkElements.get("LOCATION").toString();
                    final String length = hyperLinkElements.get("LENGTH").toString();
                    final String text = hyperLinkElements.get("TEXT").toString();
                    final Style hyperlink = component.addStyle("hyperlink:"+path+"?"+location+"&"+length, null);
                    hyperlink.addAttribute("IS_HYPERLINK",true);
                    hyperlink.addAttribute("PATH",path);
                    hyperlink.addAttribute("LOCATION",location);
                    hyperlink.addAttribute("LENGTH",length);
                    StyleConstants.setForeground(hyperlink,new Color(50, 100, 175));
                    StyleConstants.setUnderline(hyperlink, true);
                    SwingUtilities.invokeLater(() -> {
                        try {
                            component.getStyledDocument().insertString(
                                    component.getStyledDocument().getLength(),
                                    text,
                                    hyperlink
                            );
                        } catch(BadLocationException x) {}
                    });

                } else {
                    hyperLinkElements.get("TEXT").append(c);
                }
                break;
            }
        }
    }
}
