package com.energyxxer.trident.main;

import com.energyxxer.trident.global.Preferences;
import com.energyxxer.trident.global.Resources;
import com.energyxxer.trident.global.TabManager;
import com.energyxxer.trident.global.temp.projects.ProjectManager;
import com.energyxxer.trident.main.window.TridentWindow;
import com.energyxxer.trident.util.Version;
import com.energyxxer.util.ImageManager;
import com.energyxxer.util.logger.Debug;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class Trident {
	public static Trident trident;
	public static final Version VERSION = new Version(0,1,2);

	public static TridentWindow window;

	private Trident() {
		window = new TridentWindow();
	}

	public static void main(String[] args) {
		Debug.addStream(System.out);
		try {
            Debug.addStream(new FileOutputStream(new File(Preferences.LOG_FILE_PATH)));
        } catch(FileNotFoundException x) {
		    Debug.log("Unable to open log file '" + Preferences.LOG_FILE_PATH + "', will not log to it until restarted");
        }
		Debug.log("Running on Java " + System.getProperty("java.version"));

		JFrame splash = new JFrame();
		splash.setSize(new Dimension(700, 410));
		Point center = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
		center.x -= 700/2;
		center.y -= 410/2;
		splash.setLocation(center);
		splash.setUndecorated(true);
		splash.setVisible(true);
		splash.setContentPane(new JComponent() {
			@Override
			protected void paintComponent(Graphics g) {
				g.drawImage(ImageManager.load("/assets/logo/splash.png"), 0,0,this.getWidth(),this.getHeight(), null);

				Graphics2D g2d = (Graphics2D) g;
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.setColor(new Color(187, 187, 187));
				g.setFont(g.getFont().deriveFont(32f));
				g.drawString("v." + VERSION.toString(), 512, 320);
				g.dispose();
			}
		});
		splash.revalidate();
		splash.setIconImage(ImageManager.load("/assets/logo/logo.png").getScaledInstance(32, 32, Image.SCALE_SMOOTH));

		Resources.load();

		trident = new Trident();

		TridentWindow.setVisible(true);

        splash.setVisible(false);
        splash.dispose();

        if(Preferences.get("workspace_dir", null) == null) {
            WorkspaceDialog.prompt();
        }

        ProjectManager.loadWorkspace();

		TridentWindow.welcomePane.tipScreen.start(1000);
		TabManager.openSavedTabs();
		TridentWindow.projectExplorer.openExplorerTree();
	}

}
