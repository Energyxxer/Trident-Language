package com.energyxxer.trident.main.window.sections.tools;

import com.energyxxer.trident.main.window.TridentWindow;
import com.energyxxer.trident.ui.explorer.NoticeExplorerMaster;
import com.energyxxer.trident.ui.scrollbar.OverlayScrollPaneLayout;

import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * Created by User on 5/16/2017.
 */
public class NoticeBoard extends ToolBoard {

    private static final int BOARD_HEIGHT = 250;


    public NoticeBoard(ToolBoardMaster parent) {
        super(parent);
        this.setLayout(new BorderLayout());
        //this.setPreferredSize(new Dimension(0, 25));
        //tlm.addThemeChangeListener(t -> this.setBorder(BorderFactory.createMatteBorder(Math.max(t.getInteger(1, "NoticeBoard.header.border.thickness"),0), 0, 0, 0, t.getColor(new Color(200, 200, 200), "NoticeBoard.header.border.color"))));

        //JPanel boardHeader = new JPanel(new BorderLayout());
        //tlm.addThemeChangeListener(t -> boardHeader.setBackground(t.getColor(new Color(235, 235, 235), "NoticeBoard.header.background")));
        //boardHeader.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        //boardHeader.setPreferredSize(new Dimension(0, 25));

        //StyledLabel boardLabel = new StyledLabel("Notice Board", "NoticeBoard.header");
        //boardHeader.add(boardLabel, BorderLayout.WEST);

        //JPanel consoleActionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
        //consoleActionPanel.setOpaque(false);

        //ToolbarButton toggle = new ToolbarButton("toggle", tlm);
        //toggle.setHintText("Toggle Board");
        //toggle.setPreferredHintPos(Hint.LEFT);
        //toggle.setPreferredSize(new Dimension(20,20));

        /*toggle.addActionListener(e -> {
            if (this.getPreferredSize().height == 25) {
                expand();
            } else {
                collapse();
            }
        });*/

        //ToolbarButton clear = new ToolbarButton("clear", tlm);
        //clear.setHintText("Clear Board");
        //clear.setPreferredHintPos(Hint.LEFT);
        //clear.setPreferredSize(new Dimension(20,20));

        //consoleActionPanel.add(clear);
        //consoleActionPanel.add(toggle);
        //boardHeader.add(consoleActionPanel, BorderLayout.EAST);

        //this.add(boardHeader, BorderLayout.NORTH);

        JScrollPane sp = new JScrollPane(TridentWindow.noticeExplorer = new NoticeExplorerMaster());
        sp.setBorder(new EmptyBorder(0,0,0,0));
        sp.setLayout(new OverlayScrollPaneLayout(sp));

        this.add(sp, BorderLayout.CENTER);

        //clear.addActionListener(e -> TridentWindow.noticeExplorer.clear());
        this.setPreferredSize(new Dimension(0, BOARD_HEIGHT));
    }

    public void expand() {
        this.setPreferredSize(new Dimension(0, BOARD_HEIGHT));
        this.revalidate();
        this.repaint();
    }

    public void collapse() {
        this.setPreferredSize(new Dimension(0, 25));
        this.revalidate();
        this.repaint();
    }

    @Override
    public String getName() {
        return "Notice Board";
    }

    @Override
    public String getIconName() {
        return "notices";
    }
}
