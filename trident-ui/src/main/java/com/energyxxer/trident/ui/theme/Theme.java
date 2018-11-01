package com.energyxxer.trident.ui.theme;

import com.energyxxer.trident.main.window.TridentWindow;

import java.awt.Color;
import java.awt.Font;
import java.util.HashMap;

import static com.energyxxer.trident.ui.theme.Theme.ThemeType.GUI_THEME;

public class Theme {
	private final String name;
	private final ThemeType themeType;
	private final HashMap<String, Object> values;

	public enum ThemeType {
		GUI_THEME("gui"), SYNTAX_THEME("syntax");

		public String subdirectory;

		ThemeType(String subdirectory) {
			this.subdirectory = subdirectory;
		}
	}

	public Theme(String name) {
		this(name, new HashMap<>());
	}
	public Theme(String name, HashMap<String, Object> values) {
		this(GUI_THEME, name, values);
	}
	public Theme(ThemeType type, String name, HashMap<String, Object> values) {
		this.name = name;
		this.values = values;
		this.themeType = type;
	}

	protected void put(String key, Object value) {
		values.put(key,value);
	}

	public Object get(Object defaultValue, String... keys) {
		for(String key : keys) {
			Object value = values.get(key);
			if(value != null) return value;
		}
		return defaultValue;
	}

	//Colors

	public Color getColor(Color defaultValue, String... keys) {
		for(String key : keys) {
			Object value = values.get(key);
			if(value != null && value instanceof Color) return (Color) value;
		}
		return defaultValue;
	}

	public Color getColor(String... keys) { return getColor(null, keys); }

	//Booleans

	public boolean getBoolean(boolean defaultValue, String... keys) {
		for(String key : keys) {
			Object value = values.get(key);
			if(value != null && value instanceof Boolean) return (Boolean) value;
		}
		return defaultValue;
	}

	public boolean getBoolean(String... keys) { return getBoolean(false, keys); }

	//Strings

	public String getString(String... keys) {
		String defaultValue = null;
		for(String key : keys) {
			if(key.startsWith("default:")) {
				defaultValue = key.substring("default:".length());
				continue;
			}
			Object value = values.get(key);
			if(value != null && value instanceof String) return (String) value;
		}
		return defaultValue;
	}

	//Integers

	public int getInteger(int defaultValue, String... keys) {
		for(String key : keys) {
			Object value = values.get(key);
			if(value != null && value instanceof Integer) return (Integer) value;
		}
		return defaultValue;
	}

	public int getInteger(String... keys) {
		return getInteger(0, keys);
	}

	//Fonts

	public Font getFont(Font defaultValue, String... keys) {
		String[] names = new String[keys.length];
		String[] sizes = new String[keys.length];
		String[] bolds = new String[keys.length];
		String[] italics = new String[keys.length];

		for(int i = 0; i < keys.length; i++) {
			names[i] = keys[i] + ".font";
			sizes[i] = keys[i] + ".fontSize";
			bolds[i] = keys[i] + ".bold";
			italics[i] = keys[i] + ".italic";
		}

		String name = this.getString(names);
		if(name == null) name = defaultValue.getName();
		int size = this.getInteger(defaultValue.getSize(), sizes);
		if(size < 0) size = 14;
		boolean bold = this.getBoolean(defaultValue.isBold(), bolds);
		boolean italic = this.getBoolean(defaultValue.isItalic(), italics);

		return new Font(name, (bold ? Font.BOLD : Font.PLAIN) + (italic ? Font.ITALIC : Font.PLAIN), size);
	}

	public Font getFont(String... keys) {
		return getFont(TridentWindow.defaultFont, keys);
	}

	//Other

	public String getName() {return name;}

	public ThemeType getThemeType() {
		return themeType;
	}

	public HashMap<String, Object> getValues() {
		return values;
	}

	@Override
	public String toString() {return name;}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Theme theme = (Theme) o;

		return name != null ? name.equals(theme.name) : theme.name == null;
	}
}