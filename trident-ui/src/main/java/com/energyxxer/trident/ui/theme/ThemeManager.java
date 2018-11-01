package com.energyxxer.trident.ui.theme;

import com.energyxxer.util.logger.Debug;
import com.energyxxer.util.out.Console;
import com.energyxxer.trident.global.temp.Lang;
import com.energyxxer.trident.global.Preferences;
import com.energyxxer.trident.global.Resources;
import com.energyxxer.trident.main.window.TridentWindow;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by User on 12/13/2016.
 */
public class ThemeManager {

    private static HashMap<String, Theme> gui_themes = new HashMap<>();
    private static HashMap<String, Theme> syntax_themes = new HashMap<>();

    private static Theme nullTheme = new Theme("null");

    public static Theme currentGUITheme = new Theme("null");

    public static void loadAll() {
        gui_themes.clear();
        syntax_themes.clear();

        ThemeReader tr = new ThemeReader();
        for(String file : Resources.indexes.get("GUI Themes")) {
            try {
                Theme theme = tr.read(Theme.ThemeType.GUI_THEME, file);
                if(theme != null) gui_themes.put(theme.getName(),theme);
            } catch(ThemeParserException e) {
                Debug.log(e.getMessage(), Debug.MessageType.WARN);
            }
        }

        for(String file : Resources.indexes.get("Syntax Themes")) {
            try {
                Theme theme = tr.read(Theme.ThemeType.SYNTAX_THEME, file);
                if(theme != null) syntax_themes.put(theme.getName(),theme);
            } catch(ThemeParserException e) {
                Debug.log(e.getMessage(), Debug.MessageType.WARN);
            }
        }

        //Read theme directory

        String themeDirPath = System.getProperty("user.home") + File.separator + "Trident" + File.separator + "resources" + File.separator + "themes" + File.separator;

        File themeDir = new File(themeDirPath);
        if(themeDir.exists()) {
            File guiThemeDirectory = new File(themeDirPath + "gui");
            if(guiThemeDirectory.exists()) {
                File[] files = guiThemeDirectory.listFiles();
                if(files != null) {
                    for(File file : files) {
                        try {
                            Theme theme = tr.read(Theme.ThemeType.GUI_THEME, file);
                            if(theme != null) gui_themes.put(theme.getName(),theme);
                        } catch(ThemeParserException e) {
                            Debug.log(e.getMessage(), Debug.MessageType.ERROR);
                        }
                    }
                }

            } else guiThemeDirectory.mkdir();

            File syntaxThemeDirectory = new File(themeDirPath + "syntax");
            if(syntaxThemeDirectory.exists()) {
                File[] files = syntaxThemeDirectory.listFiles();
                if(files != null) {
                    for(File file : files) {
                        try {
                            Theme theme = tr.read(Theme.ThemeType.SYNTAX_THEME, file);
                            if(theme != null) syntax_themes.put(theme.getName(),theme);
                        } catch(ThemeParserException e) {
                            Debug.log(e.getMessage(), Debug.MessageType.WARN);
                        }
                    }
                }

            } else syntaxThemeDirectory.mkdir();
        } else themeDir.mkdirs();

        setGUITheme(Preferences.get("theme"));
    }

    public static HashMap<String, Theme> getGUIThemes() {
        return gui_themes;
    }

    public static List<Theme> getGUIThemesAsList() {
        return new ArrayList<>(gui_themes.values());
    }

    public static Theme[] getGUIThemesAsArray() {
        return getGUIThemesAsList().toArray(new Theme[0]);
    }

    public static Theme getGUITheme(String name) {
        return (name.equals("null")) ? null : gui_themes.get(name);
    }

    public static Theme getSyntaxTheme(String name) {
        return (name == null || name.equals("null")) ? null : syntax_themes.get(name);
    }

    public static void setGUITheme(String name) {
        if(name != null && name.equals("null")) {
            Preferences.put("theme","null");
            TridentWindow.setTheme(nullTheme);
            currentGUITheme = nullTheme;
            return;
        }

        Theme theme = getGUITheme(name);
        if(theme == null) theme = nullTheme;

        Preferences.put("theme",theme.getName());
        TridentWindow.setTheme(theme);
        currentGUITheme = theme;

    }

    public static Theme getSyntaxForGUITheme(Lang lang, Theme guiTheme) {
        String s = guiTheme.getString("Syntax." + lang.toString().toLowerCase());
        return getSyntaxTheme(s);
    }
}
