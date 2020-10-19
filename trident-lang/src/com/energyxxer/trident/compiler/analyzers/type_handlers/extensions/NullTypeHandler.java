package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;

public class NullTypeHandler implements TypeHandler<Object> {

    private final PrismarineTypeSystem typeSystem;

    public NullTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public Object getMember(Object object, String member, TokenPattern pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(Object object, Object index, TokenPattern pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(Object object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
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
