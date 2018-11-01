package com.energyxxer.trident.ui.dialogs;

import com.energyxxer.trident.main.window.TridentWindow;
import com.energyxxer.trident.ui.styledcomponents.StyledButton;
import com.energyxxer.trident.ui.styledcomponents.StyledLabel;
import com.energyxxer.trident.ui.theme.change.ThemeListenerManager;
import com.energyxxer.xswing.Padding;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Created by User on 2/11/2017.
 */
public class OptionDialog {

    private static final int WIDTH = 400;
    private static final int HEIGHT = 110;

    public String result = null;

    private ThemeListenerManager tlm = new ThemeListenerManager();

    public OptionDialog(String title, String query, String[] options) {
        JDialog dialog = new JDialog(TridentWindow.jframe);

        JPanel pane = new JPanel(new BorderLayout());
        pane.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        tlm.addThemeChangeListener(t ->
                pane.setBackground(t.getColor(new Color(235, 235, 235), "OptionDialog.background"))
        );

        pane.add(new Padding(10), BorderLayout.NORTH);
        pane.add(new Padding(25), BorderLayout.WEST);
        pane.add(new Padding(25), BorderLayout.EAST);
        pane.add(new Padding(10), BorderLayout.SOUTH);

        {
            JPanel content = new JPanel(new BorderLayout());
            content.setOpaque(false);

            StyledLabel label = new StyledLabel(query, "OptionDialog");
            label.setStyle(Font.BOLD);
            content.add(label, BorderLayout.CENTER);

            {
                JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                buttons.setOpaque(false);
                buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

                for(String option : options) {
                    StyledButton button = new StyledButton(option,"OptionDialog");
                    button.addActionListener(e -> {
                        result = option;
                        dialog.setVisible(false);
                    });
                    buttons.add(button);
                }

                content.add(buttons, BorderLayout.SOUTH);
            }

            pane.add(content, BorderLayout.CENTER);
        }

        pane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "submit");
        pane.getActionMap().put("submit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                result = options[0];
                dialog.setVisible(false);
            }
        });

        dialog.setContentPane(pane);
        dialog.pack();

        dialog.setTitle(title);

        Point center = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
        center.x -= dialog.getWidth()/2;
        center.y -= dialog.getHeight()/2;

        dialog.setLocation(center);

        dialog.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);

        dialog.setVisible(true);
    }
}
