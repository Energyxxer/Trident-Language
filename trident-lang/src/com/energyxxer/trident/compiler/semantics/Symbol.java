package com.energyxxer.trident.compiler.semantics;

import org.jetbrains.annotations.Nullable;

public class Symbol {

    public enum SymbolVisibility {
        GLOBAL, LOCAL, PRIVATE
    }
    private String name;

    private final SymbolVisibility visibility;
    private Object value;
    public Symbol(String name) {
        this(name, SymbolVisibility.LOCAL);
    }

    public Symbol(String name, SymbolVisibility visibility) {
        this(name, visibility, null);
    }

    public Symbol(String name, SymbolVisibility visibility, Object value) {
        this.name = name;
        this.visibility = visibility;
        if(value != null) setValue(value);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SymbolVisibility getVisibility() {
        return visibility;
    }

    @Nullable
    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
