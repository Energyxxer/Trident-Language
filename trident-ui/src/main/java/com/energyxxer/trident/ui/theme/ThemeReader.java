package com.energyxxer.trident.ui.theme;

import com.energyxxer.trident.util.FileUtil;
import com.energyxxer.trident.util.LineReader;
import com.energyxxer.trident.util.Range;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by User on 12/13/2016.
 */
public class ThemeReader {

    private HashMap<String, Object> themeValues;

    private int currentLine = 0;
    private String line = null;

    public Theme read(Theme.ThemeType type, String name) throws ThemeParserException {
        try {
            return read(type, name, LineReader.read("/resources/themes/" + type.subdirectory + '/' + name + ".properties"));
        } catch(IOException x) {
            throw new ThemeParserException(x.getMessage(),0,"");
        }
    }

    public Theme read(Theme.ThemeType type, File file) throws ThemeParserException {
        try {
            return read(type, FileUtil.stripExtension(file.getName()), LineReader.read(file));
        } catch(IOException x) {
            throw new ThemeParserException(x.getMessage(),0,"");
        }
    }

    private Theme read(Theme.ThemeType type, String name, List<String> lines) throws ThemeParserException {
        currentLine = 0;
        themeValues = new HashMap<>();
        for(String rawLine : lines) {
            currentLine++;
            line = rawLine.trim();
            if(line.length() == 0) continue;
            if(line.startsWith("#")) continue;
            if(!line.contains("=")) throw new ThemeParserException("Couldn't find key/value separator.",currentLine,line);

            String key = line.substring(0,line.indexOf("=")).trim();
            String valueString = line.substring(line.indexOf("=")+1).trim();

            if(key.length() == 0) throw new ThemeParserException("Couldn't find key.",currentLine,line);
            if(valueString.length() == 0) throw new ThemeParserException("Couldn't find value.",currentLine,line);

            Object value = parseValue(valueString);
            themeValues.put(key,value);
        }
        return new Theme(type, name, themeValues);
    }

    private Object parseValue(String value) throws ThemeParserException {

        if(value.startsWith("rgb(")) {
            if(value.endsWith(")")) {
                String inner = value.substring(4,value.length()-1);
                String[] entryList = inner.split(",");
                if(!new Range(3,4).contains(entryList.length)) throw new ThemeParserException("Color values take only 3 or 4 arguments.",currentLine,line);
                int[] colorValues = { 0, 0, 0, 255 };
                for(int i = 0; i < 4; i++) {
                    if(i >= entryList.length) break;
                    String entry = entryList[i].trim();
                    try {
                        if(i < 3) {
                            int colorValue = Integer.parseInt(entry);
                            if(colorValue < 0 || colorValue > 255) throw new ThemeParserException("Color values must be in the 0 and 255 range.",currentLine,line);
                            colorValues[i] = colorValue;
                        } else {
                            float colorValue = Float.parseFloat(entry);
                            if(colorValue < 0 || colorValue > 1) throw new ThemeParserException("Color alpha must be between 0 and 1",currentLine,line);
                            colorValues[i] = Math.round(colorValue*255);
                        }
                    } catch(NumberFormatException e) {
                        throw new ThemeParserException("Expected a number, instead got \"" + entryList[i].trim() + "\"",currentLine,line);
                    }
                }
                return new Color(colorValues[0],colorValues[1],colorValues[2],colorValues[3]);
            } else throw new ThemeParserException("Couldn't find end of color literal.",currentLine,line);
        } else if(value.startsWith("#")) {
            try {
                if(value.substring(1).length() == 3) {
                    String fullColor = "" + value.charAt(1) + value.charAt(1) + value.charAt(2) + value.charAt(2) + value.charAt(3) + value.charAt(3);
                    return new Color(Integer.parseInt(fullColor,16));
                } else if(value.substring(1).length() == 6) {
                    return new Color(Integer.parseInt(value.substring(1),16));
                } else {
                    throw new ThemeParserException("Expected a hexadecimal color, instead got \"" + value.substring(1) + "\"",currentLine,line);
                }
            } catch(NumberFormatException e) {
                throw new ThemeParserException("Expected a hexadecimal color, instead got \"" + value.substring(1) + "\"",currentLine,line);
            }
        } else if(value.startsWith("@")) {
            if(value.length() == 1) throw new ThemeParserException("Unterminated key reference. Expected name.",currentLine,line);
            Object obj = themeValues.get(value.substring(1));
            if(obj == null) throw new ThemeParserException("Invalid key reference. \"" + value.substring(1) + "\" hasn't been defined at this point in the file",currentLine,line);
            return obj;
        } else if(Arrays.asList("true","false").contains(value)) {
            return Boolean.valueOf(value);
        } else if(value.matches("^[-+]?\\d+$")) {
            return Integer.parseInt(value);
        } else if(value.matches("^[-+]?\\d+(\\.\\d++)?$")) {
            return Float.parseFloat(value);
        } else {
            return value;
        }
    }

}
