package com.energyxxer.util;

import java.util.HashMap;

public class StringLocationCache {
    //              (line)   (index)
    private HashMap<Integer, Integer> lineLocations = new HashMap<>();
    private int lastKnownLine = 0;

    public String text;

    public StringLocationCache() {
        clear();
    }

    public StringLocationCache(String text) {
        setText(text);
    }

    public void textChanged(String newText, int changeIndex) {
        this.text = newText;
        if(changeIndex > lineLocations.get(lastKnownLine)) return;
        int line = getLocationForOffset(changeIndex).line-1;
        while(lineLocations.containsKey(line)) {
            lineLocations.remove(line);
            line++;
        }
        if(lineLocations.isEmpty()) {
            lineLocations.put(0,0);
        }
        lastKnownLine = lineLocations.size()-1;
    }

    public void setText(String text) {
        this.text = text;
        clear();
    }

    public void clear() {
        lineLocations.clear();
        lineLocations.put(0,0);
        lastKnownLine = 0;
    }

    public StringLocation getLocationForOffset(int index) {
        int line = 0;
        int lineStart = 0;

        int firstLine = 0; //inclusive
        int lastLine = lineLocations.size()-1; //inclusive

        while(lastLine >= firstLine) {
            if(lastLine <= firstLine) {
                Integer boxedLoc = lineLocations.get(lastLine);
                if(boxedLoc == null) { //Concurrent modification occurred.
                    break;
                }
                line = lastLine;
                lineStart = boxedLoc;
                break;
            } else {
                int pivot = (lastLine + firstLine) / 2;
                Integer boxedLoc = lineLocations.get(pivot);
                if(boxedLoc == null) break;
                int loc = boxedLoc;
                if(loc == index) {
                    return new StringLocation(index, pivot+1, 1);
                } else if(index > loc) {
                    firstLine = pivot;
                    if(lastLine - firstLine <= 1) {
                        lastLine = firstLine;
                    }
                } else {
                    lastLine = pivot-1;
                }
            }
        }

        if(text == null) return new StringLocation(0, 1, 1);

        int column = 0;

        for(int i = lineStart; i < text.length(); i++) {
            if(i == index) {
                return new StringLocation(index, line + 1, column + 1);
            }
            if(text.charAt(i) == '\n') {
                line++;
                column = 0;
                lineLocations.putIfAbsent(line, i + 1);
                lastKnownLine = Math.max(lastKnownLine, line);
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
