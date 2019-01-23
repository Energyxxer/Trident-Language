package com.energyxxer.trident.compiler.analyzers.type_handlers;

public class MemberNotFoundException extends RuntimeException {
    public MemberNotFoundException() {
    }

    public MemberNotFoundException(String message) {
        super(message);
    }
}
