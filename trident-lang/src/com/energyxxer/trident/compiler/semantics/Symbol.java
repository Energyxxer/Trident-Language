package com.energyxxer.trident.compiler.semantics;

public class Symbol {

    public enum SymbolAccess {
        GLOBAL, LOCAL, PROTECTED
    }

    private final String name;
    private final SymbolAccess access;
    private Object value;

    public Symbol(String name) {
        this(name, SymbolAccess.PROTECTED);
    }

    public Symbol(String name, SymbolAccess access) {
        this.name = name;
        this.access = access;
    }
}
