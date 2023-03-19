package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.selector_args;

import com.energyxxer.commodore.functionlogic.selector.arguments.TypeArgument;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.types.defaults.EntityType;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.TypeHandlerMemberCollection;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;
import com.energyxxer.prismarine.typesystem.functions.natives.NativeFunctionAnnotations;
import com.energyxxer.prismarine.typesystem.generics.GenericSupplier;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.trident.worker.tasks.SetupModuleTask;

public class TypeArgumentTypeHandler implements TypeHandler<TypeArgument> {
    private TypeHandlerMemberCollection<TypeArgument> members;

    private final PrismarineTypeSystem typeSystem;

    public TypeArgumentTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public void staticTypeSetup(PrismarineTypeSystem typeSystem, ISymbolContext globalCtx) {
        members = new TypeHandlerMemberCollection<>(typeSystem, globalCtx);
        members.setNotFoundPolicy(TypeHandlerMemberCollection.MemberNotFoundPolicy.THROW_EXCEPTION);
        members.putReadOnlyField("value", v -> new ResourceLocation(v.type));
        members.putReadOnlyField("negated", v -> v.negated);

        try {
            members.setConstructor(TypeArgumentTypeHandler.class.getMethod("construct", ResourceLocation.class, Boolean.class, TokenPattern.class, ISymbolContext.class));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public Object getMember(TypeArgument object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        return members.getMember(object, member, pattern, ctx);
    }

    @Override
    public Object getIndexer(TypeArgument object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(TypeArgument object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        return null;
    }

    @Override
    public Class<TypeArgument> getHandledClass() {
        return TypeArgument.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "selector_argument_type";
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
    public static TypeArgument construct(ResourceLocation value, @NativeFunctionAnnotations.NullableArg Boolean negated, TokenPattern<?> p, ISymbolContext ctx) {
        Type type;
        if(value.isTag) {
            type = ctx.get(SetupModuleTask.INSTANCE).getNamespace(value.namespace).tags.entityTypeTags.get(value.body);
        } else {
            type = ctx.get(SetupModuleTask.INSTANCE).getNamespace(value.namespace).types.entity.get(value.body);
            if(type == null) throw new PrismarineException(TridentExceptionUtil.Source.COMMAND_ERROR, "No such tag '" + value + "' for category '" + EntityType.CATEGORY + "'", p, ctx);
        }
        return new TypeArgument(type, Boolean.TRUE.equals(negated));
    }
}
