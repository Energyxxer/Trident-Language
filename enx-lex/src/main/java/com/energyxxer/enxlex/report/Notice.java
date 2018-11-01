package com.energyxxer.enxlex.report;

import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;

/**
 * Created by User on 5/15/2017.
 */
public class Notice {
    private NoticeType type;
    private String message;
    private String formattedPath;

    private String filePath;
    private int locationIndex;
    private int locationLength;

    private String label;

    public Notice(NoticeType type, String message) {
        this(null, type, message);
    }

    public Notice(String label, NoticeType type, String message) {
        this(label, type, message, (String) null);
    }

    public Notice(NoticeType type, String message, TokenPattern<?> pattern) {
        this(null, type, message, pattern);
    }

    public Notice(String label, NoticeType type, String message, TokenPattern<?> pattern) {
        this(label, type, message, (pattern != null) ? pattern.getFormattedPath() : null);
    }

    public Notice(NoticeType type, String message, Token token) {
        this(null, type, message, token);
    }

    public Notice(String label, NoticeType type, String message, Token token) {
        this(label, type, message, (token != null) ? token.getFormattedPath() : null);
    }

    public Notice(NoticeType type, String message, String formattedPath) {
        this(null, type, message, formattedPath);
    }

    public Notice(String label, NoticeType type, String message, String formattedPath) {
        this.type = type;
        this.message = message;
        this.setFormattedPath(formattedPath);
        if(label != null) this.label = label;
    }

    public NoticeType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getFormattedPath() {
        return formattedPath;
    }

    public void setType(NoticeType type) {
        this.type = type;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setFormattedPath(String formattedPath) {
        this.formattedPath = formattedPath;
        if(formattedPath != null) {
            String[] segments = formattedPath.split("\b");
            this.filePath = segments[1];
            this.locationIndex = Integer.parseInt(segments[2]);
            this.locationLength = Integer.parseInt(segments[3]);

            this.label = this.filePath;
        }
    }

    public String getFilePath() {
        return filePath;
    }

    public int getLocationIndex() {
        return locationIndex;
    }

    public int getLocationLength() {
        return locationLength;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return message + ((formattedPath != null) ? ("\n    at " + formattedPath) : "");
    }
}
