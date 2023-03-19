package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.selector_args;

import com.energyxxer.commodore.functionlogic.selector.arguments.TeamArgument;
import com.energyxxer.commodore.types.defaults.TeamReference;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.TypeHandlerMemberCollection;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;
import com.energyxxer.prismarine.typesystem.functions.natives.NativeFunctionAnnotations;
import com.energyxxer.prismarine.typesystem.generics.GenericSupplier;

public class TeamArgumentTypeHandler implements TypeHandler<TeamArgument> {
    private TypeHandlerMemberCollection<TeamArgument> members;

    private final PrismarineTypeSystem typeSystem;

    public TeamArgumentTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public void staticTypeSetup(PrismarineTypeSystem typeSystem, ISymbolContext globalCtx) {
        members = new TypeHandlerMemberCollection<>(typeSystem, globalCtx);
        members.setNotFoundPolicy(TypeHandlerMemberCollection.MemberNotFoundPolicy.THROW_EXCEPTION);
        members.putReadOnlyField("value", v -> v.team != null ? v.team.toString() : null);
        members.putReadOnlyField("negated", v -> v.negated);

        try {
            members.setConstructor(TeamArgumentTypeHandler.class.getMethod("construct", String.class, Boolean.class));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public Object getMember(TeamArgument object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        return members.getMember(object, member, pattern, ctx);
    }

    @Override
    public Object getIndexer(TeamArgument object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(TeamArgument object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        return null;
    }

    @Override
    public Class<TeamArgument> getHandledClass() {
        return TeamArgument.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "selector_argument_team";
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
    public static TeamArgument construct(@NativeFunctionAnnotations.NullableArg String value, @NativeFunctionAnnotations.NullableArg Boolean negated) {
        return new TeamArgument(value != null ? new TeamReference(value) : null, Boolean.TRUE.equals(negated));
    }
}
