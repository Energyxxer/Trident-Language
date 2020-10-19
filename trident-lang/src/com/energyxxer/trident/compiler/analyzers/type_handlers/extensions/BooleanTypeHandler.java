package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;

public class BooleanTypeHandler implements TypeHandler<Boolean> {
    private final PrismarineTypeSystem typeSystem;

    public BooleanTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public Object getMember(Boolean object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(Boolean object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(Boolean object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }

    @Override
    public Class<Boolean> getHandledClass() {
        return Boolean.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "boolean";
    }


}
