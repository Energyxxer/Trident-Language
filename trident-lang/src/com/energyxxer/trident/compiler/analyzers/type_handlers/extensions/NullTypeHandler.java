package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberNotFoundException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

public class NullTypeHandler implements TypeHandler<Object> {
    @Override
    public Object getMember(Object object, String member, TokenPattern pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(Object object, Object index, TokenPattern pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <F> F cast(Object object, Class<F> targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        return null;
    }

    @Override
    public Class<Object> getHandledClass() {
        return null;
    }

    @Override
    public String getTypeIdentifier() {
        return "null";
    }

    @Override
    public boolean isInstance(Object obj) {
        return true;
    }
}
