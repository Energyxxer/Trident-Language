package com.energyxxer.trident.ui.dialogs.file_dialogs;

import com.energyxxer.trident.files.FileDefaults;
import com.energyxxer.trident.files.FileType;
import com.energyxxer.trident.global.Commons;
import com.energyxxer.trident.global.Preferences;
import com.energyxxer.trident.global.TabManager;
import com.energyxxer.trident.global.temp.projects.ProjectManager;
import com.energyxxer.trident.main.window.TridentWindow;
import com.energyxxer.trident.ui.modules.FileModuleToken;
import com.energyxxer.trident.ui.styledcomponents.StyledButton;
import com.energyxxer.trident.ui.styledcomponents.StyledDropdownMenu;
import com.energyxxer.trident.ui.styledcomponents.StyledLabel;
import com.energyxxer.trident.ui.styledcomponents.StyledTextField;
import com.energyxxer.trident.ui.theme.change.ThemeListenerManager;
import com.energyxxer.trident.util.FileUtil;
import com.energyxxer.util.logger.Debug;
import com.energyxxer.util.out.Console;
import com.energyxxer.xswing.Padding;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

/**
 * Created by User on 2/10/2017.
 */
public class UnitDialog {

    private static final int WIDTH = 400;
    private static final int HEIGHT = 115;
    private static final int HEIGHT_ERR = 150;

    private static JDialog dialog = new JDialog(TridentWindow.jframe);
    private static JPanel pane;

    private static StyledDropdownMenu<FileType> typeDropdown;
    private static StyledTextField nameField;

    private static JPanel errorPanel;
    private static StyledLabel errorLabel;

    private static StyledButton okButton;

    private static boolean valid = false;

    private static String destination;

    private static ThemeListenerManager tlm = new ThemeListenerManager();

    static {
        pane = new JPanel(new BorderLayout());
        pane.setPreferredSize(new Dimension(WIDTH,HEIGHT));
        tlm.addThemeChangeListener(t ->
            pane.setBackground(t.getColor(new Color(235, 235, 235), "NewUnitDialog.background"))
        );

        //<editor-fold desc="Inner Margin">
        pane.add(new Padding(10), BorderLayout.NORTH);
        pane.add(new Padding(25), BorderLayout.WEST);
        pane.add(new Padding(25), BorderLayout.EAST);
        //</editor-fold>

        //<editor-fold desc="Content Components">
        JPanel content = new JPanel();
        content.setOpaque(false);

        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        StyledLabel kindLabel, nameLabel;
        {

            JPanel entry = new JPanel(new BorderLayout());
            entry.setOpaque(false);
            entry.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

            nameLabel = new StyledLabel("Name: ", "NewUnitDialog");
            nameLabel.setStyle(Font.BOLD);
            entry.add(nameLabel, BorderLayout.WEST);

            nameField = new StyledTextField("", "NewUnitDialog");
            nameField.getDocument().addUndoableEditListener(e -> validateInput());

            entry.add(nameField,  BorderLayout.CENTER);
            content.add(entry);
        }
        content.add(new Padding(5));
        {

            JPanel entry = new JPanel(new BorderLayout());
            entry.setOpaque(false);
            entry.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

            kindLabel = new StyledLabel("Kind: ", "NewUnitDialog");
            kindLabel.setStyle(Font.BOLD);
            entry.add(kindLabel, BorderLayout.WEST);

            typeDropdown = new StyledDropdownMenu<>(new FileType[] {FileType.ENTITY, FileType.ITEM, FileType.CLASS, FileType.ENUM, FileType.FEATURE, FileType.WORLD}, "NewUnitDialog");

            tlm.addThemeChangeListener(t -> {
                typeDropdown.setIcon(FileType.ENTITY, Commons.getIcon("entity"));
                typeDropdown.setIcon(FileType.ITEM, Commons.getIcon("item"));
                typeDropdown.setIcon(FileType.CLASS, Commons.getIcon("class"));
                typeDropdown.setIcon(FileType.ENUM, Commons.getIcon("enum"));
                typeDropdown.setIcon(FileType.FEATURE, Commons.getIcon("feature"));
                typeDropdown.setIcon(FileType.WORLD, Commons.getIcon("world"));
            });

            typeDropdown.addChoiceListener(type -> validateInput());

            entry.add(typeDropdown,  BorderLayout.CENTER);
            content.add(entry);
        }

        int maxLabelWidth = Math.max(kindLabel.getPreferredSize().width, nameLabel.getPreferredSize().width) + 5;
        kindLabel.setPreferredSize(new Dimension(maxLabelWidth, kindLabel.getPreferredSize().height));
        nameLabel.setPreferredSize(new Dimension(maxLabelWidth, nameLabel.getPreferredSize().height));

        content.add(new Padding(5));

        {
            errorPanel = new JPanel();
            errorPanel.setOpaque(false);
            errorPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 0));

            errorLabel = new StyledLabel("", "NewUnitDialog.error");
            errorLabel.setStyle(Font.BOLD);
            errorLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, errorLabel.getPreferredSize().height));
            errorPanel.add(errorLabel);

            content.add(errorPanel);
        }

        {
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttons.setOpaque(false);
            buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

            okButton = new StyledButton("OK");
            okButton.addActionListener(e -> submit());
            buttons.add(okButton);
            StyledButton cancelButton = new StyledButton("Cancel");
            cancelButton.addActionListener(e -> dialog.setVisible(false));

            buttons.add(cancelButton);
            content.add(buttons);
        }

        pane.add(content, BorderLayout.CENTER);
        //</editor-fold>

        //<editor-fold desc="Up/Down events">
        pane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "up");
        pane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "down");

        pane.getActionMap().put("up", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int newIndex = typeDropdown.getValueIndex() - 1;
                if(newIndex < 0) newIndex = typeDropdown.getOptions().size()-1;
                typeDropdown.setValueIndex(newIndex);
            }
        });
        pane.getActionMap().put("down", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int newIndex = typeDropdown.getValueIndex() + 1;
                if(newIndex >= typeDropdown.getOptions().size()) newIndex = 0;
                typeDropdown.setValueIndex(newIndex);
            }
        });
        //</editor-fold>

        //<editor-fold desc="Enter key event">
        pane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "submit");
        pane.getActionMap().put("submit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                submit();
            }
        });
        //</editor-fold>

        dialog.setContentPane(pane);
        dialog.pack();
        dialog.setResizable(false);

        dialog.setTitle("Create New Unit");

        dialog.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
    }

    private static void submit() {
        if(!valid) return;
        FileType type = typeDropdown.getValue();
        String filename;
        filename = nameField.getText().trim();

        String path = destination + File.separator + filename + ".trident";

        File newFile = new File(path);
        try {
            boolean successful = newFile.createNewFile();
            int pos;

            if(successful) {
                PrintWriter writer = new PrintWriter(newFile);
                ProjectManager.setIconFor(newFile, type.toString().toLowerCase());

                //<editor-fold desc="File Template Variables">
                HashMap<String, String> variables = new HashMap<>();
                variables.put("package", FileUtil.getPackage(newFile));
                variables.put("name", filename);
                variables.put("user", Preferences.get("username", "User"));
                variables.put("indent", "    ");

                Date date = new Date();
                variables.put("day", Calendar.getInstance().getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()));
                variables.put("date", Integer.toString(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)));
                variables.put("week", Integer.toString(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)));
                variables.put("month", Integer.toString(Calendar.getInstance().get(Calendar.MONTH) + 1));
                variables.put("year", Integer.toString(Calendar.getInstance().get(Calendar.YEAR)));
                variables.put("timestamp", date.toString());
                //</editor-fold>

                String text = FileDefaults.populateTemplate(FileDefaults.defaults.get(type.toString().toLowerCase()), variables);
                pos = Math.max(0, text.indexOf("$END$"));
                text = text.replace("$END$", "");

                writer.print(text);
                writer.close();
            } else {
                Debug.log("File creation unsuccessful", Debug.MessageType.WARN);
                return;
            }

            if(newFile.exists()) TabManager.openTab(new FileModuleToken(newFile),pos);
            TridentWindow.projectExplorer.refresh();
        } catch (IOException x) {
            x.printStackTrace();
        }
        dialog.setVisible(false);
    }

    public static void create(FileType type, String destination) {
        UnitDialog.destination = destination;
        nameField.setText("");
        typeDropdown.setValue(type);

        Point center = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
        center.x -= dialog.getWidth()/2;
        center.y -= dialog.getHeight()/2;

        dialog.setLocation(center);

        dialog.setVisible(true);
    }

    private static void validateInput() {
        String str = nameField.getText().trim();

        if(str.length() <= 0) {
            valid = false;
            okButton.setEnabled(false);
            displayError(null);
            return;
        }

        //Check if file exists
        valid = !new File(destination + File.separator + str + ".trident").exists();
        if(!valid) displayError("Error: File '" + str + ".trident" + "' already exists at the destination");

        //Check if filename is a valid identifier
        if(valid) {
            valid = typeDropdown.getValue() == FileType.WORLD/* || CraftrLang.isValidIdentifier(str)*/;
            if(!valid) {
                displayError("Error: Not a valid identifier");
            }
        }

        //Check if filename is a valid filename
        if(valid) {
            valid = FileUtil.validateFilename(str);
            if(!valid) {
                displayError("Error: Not a valid file name");
            }
        }
        if(valid) displayError(null);
        okButton.setEnabled(valid);
    }

    private static void displayError(String message) {
        if(message == null) {
            pane.setPreferredSize(new Dimension(WIDTH, HEIGHT));
            errorPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 0));
            errorLabel.setText("");
            dialog.pack();
        } else {
            pane.setPreferredSize(new Dimension(WIDTH, HEIGHT_ERR));
            errorPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            errorLabel.setText(message);
            dialog.pack();
        }
    }
}
