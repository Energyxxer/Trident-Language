package com.energyxxer.trident.ui.display;

/**
 * Created by User on 2/8/2017.
 */
public interface DisplayModule {
    void displayCaretInfo();
    Object getValue();
    boolean canSave();
    Object save();
    void focus();
}
