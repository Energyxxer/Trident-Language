package com.energyxxer.trident.ui.dialogs.file_dialogs;

import com.energyxxer.trident.files.FileType;
import com.energyxxer.trident.global.TabManager;
import com.energyxxer.trident.main.window.TridentWindow;
import com.energyxxer.trident.ui.modules.FileModuleToken;
import com.energyxxer.trident.ui.styledcomponents.StyledButton;
import com.energyxxer.trident.ui.styledcomponents.StyledIcon;
import com.energyxxer.trident.ui.styledcomponents.StyledLabel;
import com.energyxxer.trident.ui.styledcomponents.StyledTextField;
import com.energyxxer.trident.ui.theme.change.ThemeListenerManager;
import com.energyxxer.util.FileUtil;
import com.energyxxer.util.logger.Debug;
import com.energyxxer.xswing.Padding;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

/**
 * Created by User on 2/10/2017.
 */
public class UnitDialog {

    private static final int WIDTH = 400;
    private static final int HEIGHT = 115;
    private static final int HEIGHT_ERR = 150;

    private static JDialog dialog = new JDialog(TridentWindow.jframe);
    private static JPanel pane;

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

        //<editor-fold desc="Icon">
        JPanel iconPanel = new JPanel(new BorderLayout());
        iconPanel.setOpaque(false);
        iconPanel.setPreferredSize(new Dimension(73, 48));
        iconPanel.add(new Padding(25), BorderLayout.WEST);
        iconPanel.setBorder(new EmptyBorder(0, 0, 0, 2));
        iconPanel.add(new StyledIcon("trident_file", 48, 48, Image.SCALE_SMOOTH));
        pane.add(iconPanel, BorderLayout.WEST);
        //</editor-fold>

        //<editor-fold desc="Inner Margin">
        pane.add(new Padding(10), BorderLayout.NORTH);
        pane.add(new Padding(25), BorderLayout.EAST);
        //</editor-fold>

        //<editor-fold desc="Content Components">
        JPanel content = new JPanel();
        content.setOpaque(false);

        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        {

            JPanel entry = new JPanel(new BorderLayout());
            entry.setOpaque(false);
            entry.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

            StyledLabel nameLabel = new StyledLabel("Enter new Trident Function name:", "NewUnitDialog");
            nameLabel.setStyle(Font.PLAIN);
            nameLabel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
            nameLabel.setHorizontalTextPosition(JLabel.LEFT);
            entry.add(nameLabel, BorderLayout.CENTER);

            content.add(entry);
        }
        {
            JPanel entry = new JPanel(new BorderLayout());
            entry.setOpaque(false);
            entry.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

            nameField = new StyledTextField("", "NewUnitDialog");
            nameField.getDocument().addUndoableEditListener(e -> validateInput());

            entry.add(nameField, BorderLayout.CENTER);

            content.add(entry);
        }

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

        content.add(new Padding(5));

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
        String filename;
        filename = nameField.getText().trim();
        String path = destination + File.separator + filename;
        if(!path.endsWith(".tdn")) path += ".tdn";

        File newFile = new File(path);
        try {
            boolean successful = newFile.createNewFile();
            int pos;

            if(successful) {
                /*PrintWriter writer = new PrintWriter(newFile);

                //<editor-fold desc="File Template Variables">
                HashMap<String, String> variables = new HashMap<>();
                variables.put("path", ProjectUtil.getPackage(newFile));
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
                pos = Math.max(0, text.indexOf("\$END$"));
                text = text.replace("\$END$", "");

                writer.print(text);
                writer.close();*/
            } else {
                Debug.log("File creation unsuccessful", Debug.MessageType.WARN);
                return;
            }

            if(newFile.exists()) TabManager.openTab(new FileModuleToken(newFile), 0);
            TridentWindow.projectExplorer.refresh();
        } catch (IOException x) {
            x.printStackTrace();
        }
        dialog.setVisible(false);
    }

    public static void create(FileType type, String destination) {
        UnitDialog.destination = destination;
        nameField.setText("");

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
            valid = true; //TODO: check valid function name
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
