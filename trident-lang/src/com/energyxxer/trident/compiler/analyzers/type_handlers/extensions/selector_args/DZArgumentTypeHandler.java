package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.selector_args;

import com.energyxxer.commodore.functionlogic.selector.arguments.DZArgument;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.TypeHandlerMemberCollection;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;
import com.energyxxer.prismarine.typesystem.functions.natives.NativeFunctionAnnotations;
import com.energyxxer.prismarine.typesystem.generics.GenericSupplier;

public class DZArgumentTypeHandler implements TypeHandler<DZArgument> {
    private TypeHandlerMemberCollection<DZArgument> members;

    private final PrismarineTypeSystem typeSystem;

    public DZArgumentTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public void staticTypeSetup(PrismarineTypeSystem typeSystem, ISymbolContext globalCtx) {
        members = new TypeHandlerMemberCollection<>(typeSystem, globalCtx);
        members.setNotFoundPolicy(TypeHandlerMemberCollection.MemberNotFoundPolicy.THROW_EXCEPTION);
        members.putReadOnlyField("value", v -> v.value);

        try {
            members.setConstructor(DZArgumentTypeHandler.class.getMethod("construct", double.class));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public Object getMember(DZArgument object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        return members.getMember(object, member, pattern, ctx);
    }

    @Override
    public Object getIndexer(DZArgument object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(DZArgument object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        return null;
    }

    @Override
    public Class<DZArgument> getHandledClass() {
        return DZArgument.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "selector_argument_dz";
    }

    @Override
    public TypeHandler<?> getSuperType() {
        return typeSystem.getHandlerForHandlerClass(SelectorArgumentTypeHandler.class);
    }

    @Override
    public PrimitivePrismarineFunction getConstructor(TokenPattern<?> pattern, ISymbolContext ctx, GenericSupplier genericSupplier) {
        return members.getConstructor();
    }

    @NativeFunctionAnnotations.NotNullReturn
    public static DZArgument construct(double value) {
        return new DZArgument(value);
    }
}
