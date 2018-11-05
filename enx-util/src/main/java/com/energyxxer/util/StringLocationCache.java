package com.energyxxer.util;

import java.util.HashMap;

public class StringLocationCache {
    private HashMap<Integer, Integer> lineLocations = new HashMap<>();
    //              (line)   (index)

    public String text;

    public StringLocationCache() {
        clear();
    }

    public StringLocationCache(String text) {
        setText(text);
    }

    public void setText(String text) {
        this.text = text;
        clear();
    }

    public void clear() {
        lineLocations.clear();
        lineLocations.put(0,0);
    }

    public StringLocation getLocationForOffset(int index) {
        int line = 0;
        int lineStart = 0;
        for(int l = 0; l < lineLocations.size(); l++) {
            Integer loc = lineLocations.get(l);
            if(loc == null) {
                //Concurrent modification occurred.
                break;
            }
            if(loc < index) {
                line = l;
                lineStart = loc;
            } else if(loc == index) {
                return new StringLocation(index, l+1, 1);
            } else {
                break;
            }
        }

        String str = text.substring(lineStart);

        int column = 0;

        for(int i = 0; i < str.length(); i++) {
            if(lineStart + i == index) {
                return new StringLocation(index, line + 1, column + 1);
            }
            if(str.charAt(i) == '\n') {
                line++;
                column = 0;
                lineLocations.putIfAbsent(line, lineStart + i + 1);
            } else {
                column++;
            }
        }
        return new StringLocation(index, line + 1, column + 1);
    }

    public void prepopulate() {
        getLocationForOffset(text.length());
    }
}
