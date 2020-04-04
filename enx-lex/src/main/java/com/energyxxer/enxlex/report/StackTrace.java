package com.energyxxer.enxlex.report;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;

import java.util.ArrayList;
import java.util.List;

public class StackTrace {

    public static class StackTraceElement {
        String message;
        TokenPattern<?> pattern;

        public StackTraceElement(String message, TokenPattern<?> pattern) {
            this.message = message;
            this.pattern = pattern;
        }

        @Override
        public String toString() {
            return message + " (" + pattern.getFile().getName() + ":" + pattern.getStringLocation().line + ")";
        }

        public String getMessage() {
            return message;
        }

        public TokenPattern<?> getPattern() {
            return pattern;
        }
    }

    private ArrayList<StackTraceElement> elements;

    public StackTrace(List<StackTraceElement> elements) {
        this.elements = new ArrayList<>(elements);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(StackTraceElement elem : elements) {
            sb.append("    ");
            sb.append(elem.toString());
            sb.append("\n");
        }
        if(sb.length() > 0) {
            sb.setLength(sb.length()-1);
        }
        return sb.toString();
    }

    public ArrayList<StackTraceElement> getElements() {
        return elements;
    }
}
