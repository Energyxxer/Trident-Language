package com.energyxxer.trident.ui.theme.change;

import com.energyxxer.trident.main.window.TridentWindow;
import com.energyxxer.util.Disposable;
import com.energyxxer.trident.ui.theme.Theme;

import java.util.ArrayList;

public interface ThemeChangeListener extends Disposable {
	
	ArrayList<ThemeChangeListener> listeners = new ArrayList<ThemeChangeListener>();

	static void addThemeChangeListener(ThemeChangeListener l) {
		addThemeChangeListener(l, false);
	}

	static void addThemeChangeListener(ThemeChangeListener l, boolean priority) {
		if(priority) listeners.add(0, l);
		else listeners.add(l);

		l.themeChanged(TridentWindow.getTheme());
	}

	default void addThemeChangeListener() {
		addThemeChangeListener(this);
	}
	
	static void dispatchThemeChange(Theme t) {
		for(int i = 0; i < listeners.size(); i++) {
			listeners.get(i).themeChanged(t);
		}
	}
	
	void themeChanged(Theme t);

	default void dispose() {
		listeners.remove(this);
	}
}
