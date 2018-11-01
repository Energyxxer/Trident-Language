package com.energyxxer.enxlex.lexical_analysis.profiles;

/**
 * Created by User on 5/17/2017.
 */
public class ScannerContextException extends RuntimeException {

    private final int index;

    public ScannerContextException(String message, int index) {
        super(message);
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
