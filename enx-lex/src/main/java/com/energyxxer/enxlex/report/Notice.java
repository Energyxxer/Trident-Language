package com.energyxxer.enxlex.report;

import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;

/**
 * Created by User on 5/15/2017.
 */
public class Notice {
    private NoticeType type;
    private String message;
    private String extendedMessage;
    private String formattedPath;

    private String filePath;
    private int locationIndex;
    private int locationLength;

    private String group;

    public Notice(NoticeType type, String message) {
        this(null, type, message);
    }

    public Notice(String group, NoticeType type, String message) {
        this(group, type, message, (String) null);
    }

    public Notice(NoticeType type, String message, TokenPattern<?> pattern) {
        this(type, message, message, pattern);
    }

    public Notice(NoticeType type, String message, String extendedMessage, TokenPattern<?> pattern) {
        this(null, type, message, extendedMessage, pattern);
    }

    public Notice(String group, NoticeType type, String message, TokenPattern<?> pattern) {
        this(group, type, message, message, pattern);
    }

    public Notice(String group, NoticeType type, String message, String extendedMessage, TokenPattern<?> pattern) {
        this(group, type, message, extendedMessage, (pattern != null) ? pattern.getFormattedPath() : null);
    }

    public Notice(NoticeType type, String message, Token token) {
        this(null, type, message, token);
    }

    public Notice(String group, NoticeType type, String message, Token token) {
        this(group, type, message, (token != null) ? token.getFormattedPath() : null);
    }

    public Notice(NoticeType type, String message, String formattedPath) {
        this(null, type, message, formattedPath);
    }

    public Notice(String group, NoticeType type, String message, String formattedPath) {
        this(group, type, message, message, formattedPath);
    }

    public Notice(String group, NoticeType type, String message, String extendedMessage, String formattedPath) {
        this.type = type;
        this.message = message;
        this.extendedMessage = extendedMessage;
        this.setFormattedPath(formattedPath);
        if(group != null) this.group = group;
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

            this.group = this.filePath;
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

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Override
    public String toString() {
        return message + ((formattedPath != null) ? ("\n    at " + formattedPath) : "");
    }

    public String getExtendedMessage() {
        return extendedMessage;
    }

    public void setExtendedMessage(String extendedMessage) {
        this.extendedMessage = extendedMessage;
    }
}
