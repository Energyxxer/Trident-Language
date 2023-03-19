package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.selector_args;

import com.energyxxer.commodore.functionlogic.selector.arguments.PredicateArgument;
import com.energyxxer.commodore.types.defaults.PredicateReference;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.TypeHandlerMemberCollection;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;
import com.energyxxer.prismarine.typesystem.functions.natives.NativeFunctionAnnotations;
import com.energyxxer.prismarine.typesystem.generics.GenericSupplier;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.worker.tasks.SetupModuleTask;

public class PredicateArgumentTypeHandler implements TypeHandler<PredicateArgument> {
    private TypeHandlerMemberCollection<PredicateArgument> members;

    private final PrismarineTypeSystem typeSystem;

    public PredicateArgumentTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public void staticTypeSetup(PrismarineTypeSystem typeSystem, ISymbolContext globalCtx) {
        members = new TypeHandlerMemberCollection<>(typeSystem, globalCtx);
        members.setNotFoundPolicy(TypeHandlerMemberCollection.MemberNotFoundPolicy.THROW_EXCEPTION);
        members.putReadOnlyField("value", v -> v.predicate != null ? new ResourceLocation(v.predicate) : null);
        members.putReadOnlyField("negated", v -> v.negated);

        try {
            members.setConstructor(PredicateArgumentTypeHandler.class.getMethod("construct", ResourceLocation.class, Boolean.class, TokenPattern.class, ISymbolContext.class));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public Object getMember(PredicateArgument object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        return members.getMember(object, member, pattern, ctx);
    }

    @Override
    public Object getIndexer(PredicateArgument object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(PredicateArgument object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        return null;
    }

    @Override
    public Class<PredicateArgument> getHandledClass() {
        return PredicateArgument.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "selector_argument_predicate";
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
    public static PredicateArgument construct(@NativeFunctionAnnotations.NullableArg ResourceLocation value, @NativeFunctionAnnotations.NullableArg Boolean negated, TokenPattern<?> p, ISymbolContext ctx) {
        return new PredicateArgument(value != null ? new PredicateReference(ctx.get(SetupModuleTask.INSTANCE).getNamespace(value.namespace), value.body) : null, Boolean.TRUE.equals(negated));
    }
}
