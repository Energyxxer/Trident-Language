package com.energyxxer.trident.ui.editor.inspector;

import com.energyxxer.util.StringBounds;

/**
 * Created by User on 1/1/2017.
 */
public class InspectionItem {

    StringBounds bounds;
    InspectionType type;
    String message;

    public InspectionItem(InspectionType type, String message, StringBounds bounds) {
        this.bounds = bounds;
        this.message = message;
        this.type = type;
    }

    @Override
    public String toString() {
        return "InspectionItem{" +
                "bounds=" + bounds +
                ", type=" + type +
                ", message='" + message + '\'' +
                '}';
    }
}
