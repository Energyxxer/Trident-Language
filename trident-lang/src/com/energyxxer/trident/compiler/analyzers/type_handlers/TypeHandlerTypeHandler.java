package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeHandler;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

public class TypeHandlerTypeHandler implements TypeHandler<TypeHandler> {

    @Override
    public Object getMember(TypeHandler object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(TypeHandler object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(TypeHandler object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }

    @Override
    public Class<TypeHandler> getHandledClass() {
        return TypeHandler.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "type_definition";
    }

    @Override
    public boolean isInstance(Object obj) {
        return obj instanceof TypeHandler && ((TypeHandler) obj).isStaticHandler();
    }
}
