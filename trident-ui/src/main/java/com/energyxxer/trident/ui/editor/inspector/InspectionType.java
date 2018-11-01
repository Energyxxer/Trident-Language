package com.energyxxer.trident.ui.editor.inspector;

/**
 * Created by User on 1/1/2017.
 */
public enum InspectionType {

    SUGGESTION("suggestion"), WARNING("warning", true), ERROR("error", true);

    public String key;
    public boolean line;

    InspectionType(String key) {
        this.key = key;
        line = false;
    }

    InspectionType(String key, boolean line) {
        this.key = key;
        this.line = line;
    }
}
