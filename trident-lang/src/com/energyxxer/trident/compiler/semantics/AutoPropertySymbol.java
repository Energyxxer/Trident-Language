package com.energyxxer.trident.compiler.semantics;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AutoPropertySymbol<T> extends Symbol {

    public interface Getter<T> {
        T get();
    }
    public interface Setter<T> {
        void set(T value);
    }

    @NotNull
    private final Class<T> cls;
    private final Getter<T> getter;
    private final Setter<T> setter;

    public AutoPropertySymbol(String name, @NotNull Class<T> cls, Getter<T> getter, Setter<T> setter) {
        super(name);
        this.cls = cls;
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public @Nullable Object getValue(TokenPattern<?> pattern, ISymbolContext ctx) {
        return getter.get();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setValue(Object value) {
        if(cls.isInstance(value)) {
            setter.set((T) value);
        }
    }
}
