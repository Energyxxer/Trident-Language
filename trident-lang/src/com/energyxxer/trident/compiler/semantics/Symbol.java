package com.energyxxer.trident.compiler.semantics;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeConstraints;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class Symbol {

    public enum SymbolVisibility {
        GLOBAL(4), PUBLIC(3), LOCAL(2), PRIVATE(1);

        private final int visibilityIndex;

        SymbolVisibility(int visibilityIndex) {
            this.visibilityIndex = visibilityIndex;
        }

        public static SymbolVisibility min(SymbolVisibility a, SymbolVisibility b) {
            if(a == null) return b;
            if(b == null) return a;
            return a.getVisibilityIndex() < b.getVisibilityIndex() ? a : b;
        }

        public static SymbolVisibility max(SymbolVisibility a, SymbolVisibility b) {
            if(a == null) return b;
            if(b == null) return a;
            return a.getVisibilityIndex() > b.getVisibilityIndex() ? a : b;
        }

        public int getVisibilityIndex() {
            return visibilityIndex;
        }
    }

    public enum SymbolModifier {
        STATIC(0b1), FINAL(0b10);
        private final int bit;
        SymbolModifier(int bit) {
            this.bit = bit;
        }

        public int getBit() {
            return bit;
        }
    }

    private String name;

    private final SymbolVisibility visibility;
    private Object value;
    private boolean maySet = true;
    private boolean isFinal = false;
    private boolean isClassFunction = false;

    private TypeConstraints typeConstraints = null;

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

    public TypeConstraints getTypeConstraints() {
        return typeConstraints;
    }

    public void setTypeConstraints(TypeConstraints newConstraints) {
        this.typeConstraints = newConstraints;
    }

    @Nullable
    public Object getValue(TokenPattern<?> pattern, ISymbolContext ctx) {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void setFinal(boolean aFinal) {
        isFinal = aFinal;
    }

    public boolean maySet() {
        return maySet;
    }

    public void setFinalAndLock() {
        setFinal(true);
        maySet = false;
    }

    public void setClassFunction() {
        isClassFunction = true;
        maySet = false;
    }

    public void safeSetValue(Object value, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(maySet) {
            if(typeConstraints != null) {
                typeConstraints.validate(value, pattern, ctx);
                value = typeConstraints.adjustValue(value, pattern, ctx);
            }
            this.value = value;
            if(isFinal) maySet = false;
        } else if(isClassFunction) {
            throw new TridentException(TridentException.Source.TYPE_ERROR, "Cannot assign a value to a class function", pattern, ctx);
        } else {
            throw new TridentException(TridentException.Source.TYPE_ERROR, "Cannot assign a value to a final variable", pattern, ctx);
        }
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
