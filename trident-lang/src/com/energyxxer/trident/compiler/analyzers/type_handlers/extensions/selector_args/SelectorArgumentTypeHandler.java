package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.selector_args;

import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.TypeHandlerMemberCollection;

public class SelectorArgumentTypeHandler implements TypeHandler<SelectorArgument> {
    private TypeHandlerMemberCollection<SelectorArgument> members;

    @Override
    public void staticTypeSetup(PrismarineTypeSystem typeSystem, ISymbolContext globalCtx) {
        members = new TypeHandlerMemberCollection<>(typeSystem, globalCtx);
        members.setNotFoundPolicy(TypeHandlerMemberCollection.MemberNotFoundPolicy.THROW_EXCEPTION);
        members.putReadOnlyField("key", SelectorArgument::getKey);
    }

    private final PrismarineTypeSystem typeSystem;

    public SelectorArgumentTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public Object getMember(SelectorArgument object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        return members.getMember(object, member, pattern, ctx);
    }

    @Override
    public Object getIndexer(SelectorArgument object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(SelectorArgument object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        return null;
    }

    @Override
    public Class<SelectorArgument> getHandledClass() {
        return SelectorArgument.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "selector_argument";
    }
}
