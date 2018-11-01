package com.energyxxer.trident.main;

import com.energyxxer.trident.global.Preferences;
import com.energyxxer.trident.main.window.TridentWindow;
import com.energyxxer.trident.ui.styledcomponents.StyledButton;
import com.energyxxer.trident.ui.styledcomponents.StyledFileField;
import com.energyxxer.trident.ui.styledcomponents.StyledLabel;
import com.energyxxer.trident.ui.theme.change.ThemeChangeListener;
import com.energyxxer.xswing.Padding;
import com.energyxxer.xswing.XFileField;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

/**
 * Allows the user to choose a workspace location for their projects.
 */
public class WorkspaceDialog {
    private static boolean initialized = false;

    private static final int WIDTH = 400;
    private static final int HEIGHT = 140;

    private static JDialog dialog = new JDialog(TridentWindow.jframe);

    private static JPanel pane = new JPanel(new BorderLayout());
    private static JPanel content = new JPanel(new BorderLayout());
    private static JPanel instructions = new JPanel();
    private static JPanel input = new JPanel(new FlowLayout(FlowLayout.CENTER));
    private static StyledFileField field = new StyledFileField(XFileField.OPEN_DIRECTORY, new File(Preferences.DEFAULT_WORKSPACE_PATH), "WorkspaceDialog");

    private static JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    private static StyledButton okay = new StyledButton("OK","WorkspaceDialog");
    private static StyledButton cancel = new StyledButton("Cancel","WorkspaceDialog");

    private static boolean valid = false;

    private static void initialize() {
        pane.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        pane.add(new Padding(10),BorderLayout.NORTH);
        pane.add(new Padding(25),BorderLayout.WEST);
        pane.add(new Padding(25),BorderLayout.EAST);
        pane.add(new Padding(10),BorderLayout.SOUTH);
        pane.add(content, BorderLayout.CENTER);

        content.setOpaque(false);
        instructions.setOpaque(false);
        instructions.add(
                new StyledLabel(
                        "<html>Specify the desired workspace directory.<br>This is where all your projects are going to be saved.</html>","WorkspaceDialog"),
                FlowLayout.LEFT);
        content.add(instructions, BorderLayout.NORTH);
        ThemeChangeListener.addThemeChangeListener(t ->
                pane.setBackground(t.getColor(new Color(235, 235, 235), "WorkspaceDialog.background","Dialog.background"))
        );

        input.setOpaque(false);
        input.add(field, BorderLayout.CENTER);

        field.getField().getDocument().addUndoableEditListener(e -> validateInput());

        content.add(input, BorderLayout.CENTER);

        buttons.setOpaque(false);
        buttons.add(okay);
        buttons.add(cancel);
        content.add(buttons, BorderLayout.SOUTH);

        okay.addActionListener(e -> submit());

        cancel.addActionListener(e -> dialog.setVisible(false));


        //<editor-fold desc="Enter key event">
        content.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "submit");
        content.getActionMap().put("submit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                submit();
            }
        });
        //</editor-fold>

        dialog.setContentPane(pane);
        dialog.setResizable(false);

        dialog.setTitle("Setup Workspace");

        dialog.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);

        initialized = true;
    }

    private static void submit() {
        if(!valid) return;

        field.getFile().mkdirs();
        Preferences.put("workspace_dir",field.getFile().getAbsolutePath());
        TridentWindow.projectExplorer.refresh();
        dialog.setVisible(false);
    }

    private static void validateInput() {
        valid = true;
        okay.setEnabled(valid);
    }

    public static void prompt() {
        SwingUtilities.invokeLater(() -> {
            if (!initialized) {
                initialize();
            }

            field.setFile(new File(Preferences.get("workspace_dir",Preferences.DEFAULT_WORKSPACE_PATH)));
            validateInput();
            dialog.pack();

            Point center = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
            center.x -= dialog.getWidth()/2;
            center.y -= dialog.getHeight()/2;

            dialog.setLocation(center);

            dialog.setVisible(true);
        });
    }
}