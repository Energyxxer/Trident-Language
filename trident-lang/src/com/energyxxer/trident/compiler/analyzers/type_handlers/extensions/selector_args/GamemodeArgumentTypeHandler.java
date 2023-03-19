package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.selector_args;

import com.energyxxer.commodore.functionlogic.selector.arguments.GamemodeArgument;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.TypeHandlerMemberCollection;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;
import com.energyxxer.prismarine.typesystem.functions.natives.NativeFunctionAnnotations;
import com.energyxxer.prismarine.typesystem.generics.GenericSupplier;
import com.energyxxer.trident.worker.tasks.SetupModuleTask;

public class GamemodeArgumentTypeHandler implements TypeHandler<GamemodeArgument> {
    private TypeHandlerMemberCollection<GamemodeArgument> members;

    private final PrismarineTypeSystem typeSystem;

    public GamemodeArgumentTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public void staticTypeSetup(PrismarineTypeSystem typeSystem, ISymbolContext globalCtx) {
        members = new TypeHandlerMemberCollection<>(typeSystem, globalCtx);
        members.setNotFoundPolicy(TypeHandlerMemberCollection.MemberNotFoundPolicy.THROW_EXCEPTION);
        members.putReadOnlyField("value", v -> v.gamemode.toStringExcludeMinecraftNamespace());
        members.putReadOnlyField("negated", v -> v.negated);

        try {
            members.setConstructor(GamemodeArgumentTypeHandler.class.getMethod("construct", String.class, Boolean.class, ISymbolContext.class));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public Object getMember(GamemodeArgument object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        return members.getMember(object, member, pattern, ctx);
    }

    @Override
    public Object getIndexer(GamemodeArgument object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(GamemodeArgument object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        return null;
    }

    @Override
    public Class<GamemodeArgument> getHandledClass() {
        return GamemodeArgument.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "selector_argument_gamemode";
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
    public static GamemodeArgument construct(String value, @NativeFunctionAnnotations.NullableArg Boolean negated, ISymbolContext ctx) {
        Type type = ctx.get(SetupModuleTask.INSTANCE).minecraft.types.gamemode.get(value);
        return new GamemodeArgument(type, Boolean.TRUE.equals(negated));
    }
}
