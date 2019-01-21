package com.energyxxer.trident.compiler.semantics;

import org.jetbrains.annotations.Nullable;

public class Symbol {



    public enum SymbolAccess {
        GLOBAL, LOCAL, PROTECTED;
    }
    private String name;

    private final SymbolAccess access;
    private Object value;
    public Symbol(String name) {
        this(name, SymbolAccess.PROTECTED);
    }

    public Symbol(String name, SymbolAccess access) {
        this(name, access, null);
    }

    public Symbol(String name, SymbolAccess access, Object value) {
        this.name = name;
        this.access = access;
        setValue(value);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SymbolAccess getAccess() {
        return access;
    }

    @Nullable
    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
