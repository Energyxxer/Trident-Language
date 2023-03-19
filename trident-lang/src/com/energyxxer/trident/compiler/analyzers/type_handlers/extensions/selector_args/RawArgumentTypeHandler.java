package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.selector_args;

import com.energyxxer.commodore.functionlogic.inspection.ExecutionVariableMap;
import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.TypeHandlerMemberCollection;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;
import com.energyxxer.prismarine.typesystem.functions.natives.NativeFunctionAnnotations;
import com.energyxxer.prismarine.typesystem.generics.GenericSupplier;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class RawArgumentTypeHandler implements TypeHandler<RawArgumentTypeHandler.RawSelectorArgument> {
    private TypeHandlerMemberCollection<RawSelectorArgument> members;

    private final PrismarineTypeSystem typeSystem;

    public RawArgumentTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public void staticTypeSetup(PrismarineTypeSystem typeSystem, ISymbolContext globalCtx) {
        members = new TypeHandlerMemberCollection<>(typeSystem, globalCtx);
        members.setNotFoundPolicy(TypeHandlerMemberCollection.MemberNotFoundPolicy.THROW_EXCEPTION);
        members.putReadOnlyField("value", v -> v.value);

        try {
            members.setConstructor(RawArgumentTypeHandler.class.getMethod("construct", String.class, Object.class, TokenPattern.class, ISymbolContext.class));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public Object getMember(RawArgumentTypeHandler.RawSelectorArgument object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        return members.getMember(object, member, pattern, ctx);
    }

    @Override
    public Object getIndexer(RawArgumentTypeHandler.RawSelectorArgument object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(RawArgumentTypeHandler.RawSelectorArgument object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        return null;
    }

    @Override
    public Class<RawArgumentTypeHandler.RawSelectorArgument> getHandledClass() {
        return RawArgumentTypeHandler.RawSelectorArgument.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "selector_argument_raw";
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
    public static RawArgumentTypeHandler.RawSelectorArgument construct(String key, Object value, TokenPattern<?> p, ISymbolContext ctx) {
        return new RawArgumentTypeHandler.RawSelectorArgument(key, value, p, ctx);
    }


    public static class RawSelectorArgument implements SelectorArgument {
        @NotNull
        private String key;
        private Object value;
        private TokenPattern<?> p;
        private ISymbolContext ctx;

        public RawSelectorArgument(@NotNull String key, Object value, TokenPattern<?> p, ISymbolContext ctx) {
            this.key = key;
            this.value = value;
            this.p = p;
            this.ctx = ctx;
        }

        @Override
        public @NotNull String getArgumentString() {
            return key + "=" + ctx.getTypeSystem().castToString(value, p, ctx);
        }

        @Override
        public boolean isRepeatable() {
            return true;
        }

        @Override
        public @NotNull SelectorArgument clone() {
            return new RawSelectorArgument(key, value, p, ctx);
        }

        @Override
        public @NotNull String getKey() {
            return key;
        }

        @Override
        public ExecutionVariableMap getUsedExecutionVariables() {
            return null;
        }

        @Override
        public String toString() {
            return getArgumentString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RawSelectorArgument that = (RawSelectorArgument) o;
            return key.equals(that.key) &&
                    Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, value);
        }
    }
}
