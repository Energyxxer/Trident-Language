package com.energyxxer.trident.compiler.semantics;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.VariableTypeHandler;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class Symbol {
    public enum SymbolVisibility {
        GLOBAL, LOCAL, PRIVATE;
    }
    private String name;
    private final SymbolVisibility visibility;
    private Object value;
    private VariableTypeHandler typeConstraint = null;
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

    public void setTypeConstraint(VariableTypeHandler typeConstraint) {
    }

    @Nullable
    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void safeSetValue(Object value, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(typeConstraint != null) {

        }
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Symbol symbol = (Symbol) o;
        return Objects.equals(name, symbol.name) &&
                visibility == symbol.visibility &&
                Objects.equals(value, symbol.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, visibility, value);
    }
}
