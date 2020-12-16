package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.commodore.util.IntegerRange;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.TypeHandlerMemberCollection;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;
import com.energyxxer.prismarine.typesystem.functions.natives.NativeFunctionAnnotations;
import com.energyxxer.prismarine.typesystem.generics.GenericSupplier;

public class IntRangeTypeHandler implements TypeHandler<IntegerRange> {
    private TypeHandlerMemberCollection<IntegerRange> members;

    private final PrismarineTypeSystem typeSystem;

    public IntRangeTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public void staticTypeSetup(PrismarineTypeSystem typeSystem, ISymbolContext globalCtx) {
        members = new TypeHandlerMemberCollection<>(typeSystem, globalCtx);
        members.setNotFoundPolicy(TypeHandlerMemberCollection.MemberNotFoundPolicy.THROW_EXCEPTION);

        try {
            members.setConstructor(IntRangeTypeHandler.class.getMethod("construct", Integer.class, Integer.class));

            members.putReadOnlyField("min", IntRangeTypeHandler::getMin);
            members.putReadOnlyField("max", IntRangeTypeHandler::getMax);
            members.putReadOnlyField("range", r -> getMax(r) - getMin(r));

            members.putMethod(IntRangeTypeHandler.class.getMethod("deriveMin", Integer.class, IntegerRange.class));
            members.putMethod(IntRangeTypeHandler.class.getMethod("deriveMax", Integer.class, IntegerRange.class));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public Object getMember(IntegerRange object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        return members.getMember(object, member, pattern, ctx);
    }

    @Override
    public Object getIndexer(IntegerRange object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(IntegerRange range, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }

    @Override
    public Class<IntegerRange> getHandledClass() {
        return IntegerRange.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "int_range";
    }

    @Override
    public PrimitivePrismarineFunction getConstructor(TokenPattern<?> pattern, ISymbolContext ctx, GenericSupplier genericSupplier) {
        return members.getConstructor();
    }



    public static int getMin(IntegerRange range) {
        if(range.getMin() != null) return range.getMin();
        else return Integer.MIN_VALUE;
    }

    public static int getMax(IntegerRange range) {
        if(range.getMax() != null) return range.getMax();
        else return Integer.MAX_VALUE;
    }

    @NativeFunctionAnnotations.NotNullReturn
    public static IntegerRange deriveMin(@NativeFunctionAnnotations.NullableArg Integer newMin, @NativeFunctionAnnotations.ThisArg IntegerRange thiz) {
        return new IntegerRange(newMin, thiz.getMax());
    }

    @NativeFunctionAnnotations.NotNullReturn
    public static IntegerRange deriveMax(@NativeFunctionAnnotations.NullableArg Integer newMax, @NativeFunctionAnnotations.ThisArg IntegerRange thiz) {
        return new IntegerRange(thiz.getMin(), newMax);
    }

    @NativeFunctionAnnotations.NotNullReturn
    public static IntegerRange construct(@NativeFunctionAnnotations.NullableArg Integer min, @NativeFunctionAnnotations.NullableArg Integer max) {
        return new IntegerRange(min, max);
    }
}
