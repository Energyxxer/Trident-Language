package com.energyxxer.trident.global;

/**
 * Created by User on 12/15/2016.
 */
public class Status {
    private String message = "";
    private String type = INFO;
    private Float progress = null;

    public static final String INFO = "INFO";
    public static final String WARNING = "WARNING";
    public static final String ERROR = "ERROR";

    public Status() {}

    public Status(String message) {
        this.message = message;
    }

    public Status(String type, String message) {
        this(type, message, null);
    }

    public Status(String type, String message, Float progress) {
        this.type = type;
        this.message = message;
        this.progress = progress;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Float getProgress() {
        return progress;
    }

    public void setProgress(Float progress) {
        this.progress = progress;
    }
}
