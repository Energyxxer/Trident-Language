package com.energyxxer.trident.ui;

import com.energyxxer.trident.global.temp.projects.Project;
import com.energyxxer.trident.global.temp.projects.ProjectManager;
import com.energyxxer.trident.main.window.TridentWindow;
import com.energyxxer.trident.ui.audio.AudioPlayer;
import com.energyxxer.trident.ui.display.DisplayModule;
import com.energyxxer.trident.ui.editor.TridentEditorModule;
import com.energyxxer.trident.ui.imageviewer.ImageViewer;
import com.energyxxer.trident.ui.tablist.TabItem;

import javax.swing.*;
import java.io.File;
import java.util.Date;

/**
 * Concept of an open tab in the interface. Contains a component that represents
 * the clickable tab element.
 */
public class Tab {
	private Project linkedProject;
	public String path;
	public DisplayModule module;
	private Object savedValue;
	public boolean visible = true;

	public long openedTimeStamp;
	private boolean saved = true;
	private String name;
	private TabItem tabItem;

	@Override
	public String toString() {
		return "Tab [title=" + getName() + ", path=" + path + ", visible=" + visible + "]";
	}

	public Tab(String path) {
		this.path = path;
		this.linkedProject = ProjectManager.getAssociatedProject(new File(path));
		if(path.endsWith(".png")) {
			module = new ImageViewer(this);
		} else if(path.endsWith(".ogg") || path.endsWith(".mp3") || path.endsWith(".mid")) {
			module = new AudioPlayer(this);
		} else {
			module = new TridentEditorModule(this);
		}
		savedValue = module.getValue();

		openedTimeStamp = new Date().getTime();

		this.name = new File(path).getName();
	}

	private File getFile() {
		return new File(path);
	}

	public void onSelect() {
		openedTimeStamp = new Date().getTime();
		module.focus();
		module.displayCaretInfo();
	}

	public void onEdit() {
		this.setSaved(savedValue == null || savedValue.equals(module.getValue()));
	}

	public void updateName() {
		tabItem.updateName();
	}
	
	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public boolean isActive() {
		return this.tabItem != null && this.tabItem.isSelected();
	}

	public void save() {
		if(!module.canSave()) return;

		Object val = module.save();
		if(val != null) {
			savedValue = val;
			setSaved(true);
		}
	}

	public Project getLinkedProject() {
		return linkedProject;
	}

	public JComponent getModuleComponent() {
		return (JComponent) module;
	}

	public boolean isSaved() {
		return saved;
	}

	public void setSaved(boolean saved) {
		if(this.saved != saved) {
			this.saved = saved;
			updateList();
		}
	}

	private void updateList() {
		TridentWindow.tabList.repaint();
	}

	public String getName() {
		return name;
	}

	public TabItem getLinkedTabItem() {
		return tabItem;
	}

	public void linkTabItem(TabItem tabItem) {
		this.tabItem = tabItem;
	}
}
