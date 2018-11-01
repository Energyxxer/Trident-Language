package com.energyxxer.trident.main.window;

import com.energyxxer.trident.global.Commons;
import com.energyxxer.trident.global.Status;
import com.energyxxer.trident.global.TabManager;
import com.energyxxer.trident.main.Trident;
import com.energyxxer.trident.main.window.sections.*;
import com.energyxxer.trident.main.window.sections.MenuBar;
import com.energyxxer.trident.main.window.sections.tools.ConsoleBoard;
import com.energyxxer.trident.main.window.sections.tools.NoticeBoard;
import com.energyxxer.trident.main.window.sections.tools.ToolBoardMaster;
import com.energyxxer.trident.ui.explorer.NoticeExplorerMaster;
import com.energyxxer.trident.ui.explorer.ProjectExplorerMaster;
import com.energyxxer.trident.ui.tablist.TabListMaster;
import com.energyxxer.trident.ui.theme.Theme;
import com.energyxxer.trident.ui.theme.ThemeManager;
import com.energyxxer.trident.ui.theme.change.ThemeChangeListener;
import com.energyxxer.trident.ui.theme.change.ThemeListenerManager;
import com.energyxxer.util.ImageManager;
import com.energyxxer.util.logger.Debug;
import com.energyxxer.xswing.hints.HintManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Literally what it sounds like.
 */
public class TridentWindow {
	private static final Dimension defaultSize = new Dimension(1200, 800);
	public static final Font defaultFont = new JLabel().getFont();

	public static JFrame jframe;

	public static ProjectExplorerMaster projectExplorer;
	public static NoticeExplorerMaster noticeExplorer;

	public static ToolBoardMaster toolBoard;

	public static NoticeBoard noticeBoard;
	public static ConsoleBoard consoleBoard;

	public static MenuBar menuBar;
	public static Toolbar toolbar;
	public static Sidebar sidebar;
	public static EditArea editArea;
	public static WelcomePane welcomePane;

	public static StatusBar statusBar;

	public static HintManager hintManager = new HintManager(jframe);

	public static TabListMaster tabList;

	private ThemeListenerManager tlm = new ThemeListenerManager();

    public TridentWindow() {
		jframe = new JFrame();
		setTitle("");
		jframe.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		tlm.addThemeChangeListener(t -> jframe.getContentPane().setBackground(t.getColor(new Color(215, 215, 215), "Window.background")));

		jframe.setJMenuBar(menuBar = new MenuBar());

		jframe.setLayout(new BorderLayout());

		tabList = new TabListMaster();

		welcomePane = new WelcomePane();

		jframe.getContentPane().add(toolbar = new Toolbar(), BorderLayout.NORTH);

		JPanel mainContent = new JPanel(new BorderLayout());
		jframe.getContentPane().add(mainContent, BorderLayout.CENTER);
		mainContent.add(sidebar = new Sidebar(), BorderLayout.WEST);
		mainContent.add(editArea = new EditArea(), BorderLayout.CENTER);

		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher((e) -> {
            if((e.getKeyCode() == KeyEvent.VK_X || Character.toLowerCase(e.getKeyChar()) == 'x') && e.getModifiers() == InputEvent.SHIFT_MASK + InputEvent.ALT_MASK) {
                if(e.getID() == KeyEvent.KEY_PRESSED) {
                    Commons.compileActive();
                }
                return true;
            } else if((e.getKeyCode() == KeyEvent.VK_W || Character.toLowerCase(e.getKeyChar()) == 'w') && e.getModifiers() == InputEvent.CTRL_MASK) {
                if(e.getID() == KeyEvent.KEY_PRESSED) {
                    TabManager.closeSelectedTab();
                }
                return true;
            }
            return false;
        });

		toolBoard = new ToolBoardMaster();
		mainContent.add(toolBoard, BorderLayout.SOUTH);

		noticeBoard = new NoticeBoard(toolBoard);
		consoleBoard = new ConsoleBoard(toolBoard);
		toolBoard.setLastOpenedBoard(noticeBoard);

		jframe.getContentPane().add(statusBar = new StatusBar(), BorderLayout.SOUTH);

		/*JWindow w = new JWindow(jframe);
		w.setLocation(500,500);
		w.setBackground(new Color(0,0,0,0));

		w.setContentPane(new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				//super.paintComponent(g);
				g.setColor(new Color(100,255,255,50));
				g.fillRect(0,0,this.getWidth()/2,this.getHeight());
			}
		});
		//w.setOpacity(0);
		//w.getContentPane().add(new JLabel("Hello World"));
		w.setSize(100,20);
		w.setVisible(true);*/

		//PopupFactory.getSharedInstance().getPopup(jframe, test, 500, 500).show();

		//toolbar.setToolTipText("a");

		jframe.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				terminate();
			}
		});

		jframe.setExtendedState(JFrame.MAXIMIZED_BOTH);
		jframe.setSize(defaultSize);
		jframe.setPreferredSize(defaultSize);

		List<Image> icons = new ArrayList<>();
		icons.add(
				ImageManager.load("/assets/logo/logo_icon.png").getScaledInstance(16, 16, java.awt.Image.SCALE_SMOOTH));
		icons.add(ImageManager.load("/assets/logo/logo.png").getScaledInstance(32, 32, java.awt.Image.SCALE_SMOOTH));
		jframe.setIconImages(icons);

		GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
		Point center = env.getCenterPoint();
		center.x -= jframe.getWidth() / 2;
		center.y -= jframe.getHeight() / 2;
		jframe.setLocation(center);

		tlm.addThemeChangeListener(t -> {
			UIManager.put("ToolTip.background",t.getColor(Color.WHITE, "Tooltip.background"));
			UIManager.put("ToolTip.foreground",t.getColor(Color.BLACK, "Tooltip.foreground"));
			int borderThickness = Math.max(t.getInteger(1,"Tooltip.border.thickness"),0);
			UIManager.put("ToolTip.border",BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(borderThickness, borderThickness, borderThickness, borderThickness, t.getColor(Color.BLACK, "Tooltip.border.color")),BorderFactory.createEmptyBorder(3,5,3,5)));
		});
	}

	public static void setVisible(boolean b) {
		jframe.setVisible(b);
	}

	public static boolean isVisible() {
		return jframe.isVisible();
	}

	public static void setTheme(Theme t) {
		if(statusBar != null && !t.equals(ThemeManager.currentGUITheme)) {
			Status themeSetStatus = new Status("Theme set to: " + t.getName());

			setStatus(themeSetStatus);

			new Timer().schedule(new TimerTask() {
				public void run() {
					TridentWindow.dismissStatus(themeSetStatus);
				}
			}, 5000);
		}

		ThemeChangeListener.dispatchThemeChange(t);
	}

    public static Theme getTheme() {
        return ThemeManager.currentGUITheme;
    }

	public static void setStatus(String text) {
		statusBar.setStatus(text);
	}

	public static void setStatus(Status status) {
		statusBar.setStatus(status);
	}

	public static void setTitle(String title) {
		jframe.setTitle(title + ((title.length() > 0) ? " - " : "") + "Trident " + Trident.VERSION);
	}

	public static void clearTitle() {
		setTitle("");
	}

	public static void dismissStatus(Status status) {
		statusBar.dismissStatus(status);
	}

    public static void close() {
		jframe.dispatchEvent(new WindowEvent(jframe, WindowEvent.WINDOW_CLOSING));
	}

	private static void terminate() {
		Debug.log("Terminating...");
		TabManager.saveOpenTabs();
		projectExplorer.saveExplorerTree();
		jframe.dispose();
		System.exit(0);
	}
}