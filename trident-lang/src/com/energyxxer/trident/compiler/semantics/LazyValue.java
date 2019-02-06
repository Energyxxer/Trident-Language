package com.energyxxer.trident.compiler.semantics;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.VariableMethod.HelperMethods.assertOfType;

public class LazyValue {
    private TokenPattern<?> pattern;
    private final ISymbolContext ctx;
    private boolean keepSymbol;
    private Object value = null;
    private boolean evaluated = false;

    public LazyValue(TokenPattern<?> pattern, ISymbolContext ctx) {
        this(pattern, ctx, false);
    }

    public LazyValue(TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        this.pattern = pattern;
        this.ctx = ctx;
        this.keepSymbol = keepSymbol;
    }

    public Object getValue() {
        if(!evaluated) {
            value = InterpolationManager.parse(pattern, ctx, keepSymbol);
            evaluated = true;
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue(Class<T> expected) {
        if(!evaluated) {
            value = InterpolationManager.parse(pattern, ctx, expected);
            evaluated = true;
            return (T) value;
        } else return assertOfType(value, pattern, ctx, expected);
    }
}
