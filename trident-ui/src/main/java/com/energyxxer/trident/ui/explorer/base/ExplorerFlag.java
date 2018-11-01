package com.energyxxer.trident.ui.explorer.base;

/**
 * Created by User on 2/11/2017.
 */
public class ExplorerFlag {
    public static final ExplorerFlag
            DEBUG_WIDTH = new ExplorerFlag("Debug Width"),
            DYNAMIC_ROW_HEIGHT = new ExplorerFlag("Dynamic Row Height");

    private final String displayName;

    public ExplorerFlag(String displayName) {
        this.displayName = displayName;
    }
}
