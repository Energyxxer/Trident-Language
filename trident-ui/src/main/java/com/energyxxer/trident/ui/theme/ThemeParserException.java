package com.energyxxer.trident.ui.theme;

/**
 * Created by User on 12/13/2016.
 */
class ThemeParserException extends Throwable {
    ThemeParserException(String message, int lineNumber, String line) {
        super(message + "\n\tat line " + lineNumber + ":\n\t" + line);
    }
}
