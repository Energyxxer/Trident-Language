package com.energyxxer.trident.compiler.commands;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CommandParserAnnotation {
    String headerCommand();
}
