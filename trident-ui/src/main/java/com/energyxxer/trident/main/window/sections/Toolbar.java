package com.energyxxer.trident.main.window.sections;

import com.energyxxer.trident.files.FileType;
import com.energyxxer.trident.global.Commons;
import com.energyxxer.trident.global.temp.projects.Project;
import com.energyxxer.trident.main.window.TridentWindow;
import com.energyxxer.trident.ui.ToolbarButton;
import com.energyxxer.trident.ui.ToolbarSeparator;
import com.energyxxer.trident.ui.styledcomponents.StyledLabel;
import com.energyxxer.trident.ui.theme.change.ThemeListenerManager;
import com.energyxxer.xswing.Padding;
import com.energyxxer.xswing.hints.TextHint;

import javax.swing.*;
import java.awt.*;

/**
 * Created by User on 12/15/2016.
 */
public class Toolbar extends JPanel {

    public TextHint hint = TridentWindow.hintManager.createTextHint("");
    private StyledLabel projectLabel;

    public ThemeListenerManager tlm;

    public void setActiveProject(Project project) {
        if(project != null) {
            projectLabel.setText(project.getName());
            projectLabel.setIconName("project");
        } else {
            projectLabel.setText("");
            projectLabel.setIconName(null);
        }
    }
    
    {
        this.tlm = new ThemeListenerManager();
        this.hint.setOutDelay(1);

        int defaultHeight = 29;

        this.setPreferredSize(new Dimension(1, defaultHeight));
        this.setLayout(new BorderLayout());

        JPanel projectIndicator = new JPanel(new GridBagLayout());
        projectIndicator.setOpaque(false);

        this.add(projectIndicator, BorderLayout.WEST);

        projectIndicator.add(new Padding(10));

        projectLabel = new StyledLabel("", "Toolbar.projectIndicator");
        projectLabel.setTextThemeDriven(false);
        projectIndicator.add(projectLabel);

        JPanel buttonBar = new JPanel(new GridBagLayout());
        tlm.addThemeChangeListener(t -> {
            this.setBackground(t.getColor(new Color(235, 235, 235), "Toolbar.background"));
            this.setBorder(BorderFactory.createMatteBorder(0, 0, Math.max(t.getInteger(1,"Toolbar.border.thickness"),0), 0, t.getColor(new Color(200, 200, 200), "Toolbar.border.color")));

            int height = t.getInteger(29, "Toolbar.height");

            this.setPreferredSize(new Dimension(1, height));
        });
        buttonBar.setOpaque(false);
        this.add(buttonBar, BorderLayout.EAST);

        buttonBar.add(new ToolbarSeparator());

        {
            ToolbarButton button = new ToolbarButton("project",tlm);
            button.setHintText("New Project");
            button.addActionListener(e -> FileType.PROJECT.create(null));
            buttonBar.add(button);
        }

        {
            ToolbarButton button = new ToolbarButton("save",tlm);
            button.setHintText("Save File");
            buttonBar.add(button);
        }

        {
            ToolbarButton button = new ToolbarButton("save_all",tlm);
            button.setHintText("Save All Files");
            buttonBar.add(button);
        }

        buttonBar.add(new ToolbarSeparator());

        {
            ToolbarButton button = new ToolbarButton("world",tlm);
            button.setHintText("New Global Unit");
            buttonBar.add(button);
        }

        buttonBar.add(new ToolbarSeparator());

        {
            ToolbarButton button = new ToolbarButton("entity",tlm);
            button.setHintText("New Entity");
            buttonBar.add(button);
        }

        {
            ToolbarButton button = new ToolbarButton("item",tlm);
            button.setHintText("New Item");
            buttonBar.add(button);
        }

        {
            ToolbarButton button = new ToolbarButton("feature",tlm);
            button.setHintText("New Feature");
            buttonBar.add(button);
        }

        {
            ToolbarButton button = new ToolbarButton("class",tlm);
            button.setHintText("New Class");
            buttonBar.add(button);
        }

        buttonBar.add(new ToolbarSeparator());

        {
            ToolbarButton button = new ToolbarButton("export",tlm);
            button.setHintText("Compile");
            button.addActionListener(e -> Commons.compileActive());
            buttonBar.add(button);
        }

        buttonBar.add(new Padding(10));
    }
}
