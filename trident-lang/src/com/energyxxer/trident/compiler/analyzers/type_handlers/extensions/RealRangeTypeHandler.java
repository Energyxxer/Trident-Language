package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.commodore.util.DoubleRange;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.TypeHandlerMemberCollection;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;
import com.energyxxer.prismarine.typesystem.functions.natives.NativeFunctionAnnotations;
import com.energyxxer.prismarine.typesystem.generics.GenericSupplier;

public class RealRangeTypeHandler implements TypeHandler<DoubleRange> {
    private TypeHandlerMemberCollection<DoubleRange> members;

    private final PrismarineTypeSystem typeSystem;

    public RealRangeTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public void staticTypeSetup(PrismarineTypeSystem typeSystem, ISymbolContext globalCtx) {
        members = new TypeHandlerMemberCollection<>(typeSystem, globalCtx);
        members.setNotFoundPolicy(TypeHandlerMemberCollection.MemberNotFoundPolicy.THROW_EXCEPTION);

        try {
            members.setConstructor(RealRangeTypeHandler.class.getMethod("construct", Double.class, Double.class));

            members.putReadOnlyField("min", RealRangeTypeHandler::getMin);
            members.putReadOnlyField("max", RealRangeTypeHandler::getMax);
            members.putReadOnlyField("range", r -> getMax(r) - getMin(r));

            members.putMethod(RealRangeTypeHandler.class.getMethod("deriveMin", Double.class, DoubleRange.class));
            members.putMethod(RealRangeTypeHandler.class.getMethod("deriveMax", Double.class, DoubleRange.class));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public Object getMember(DoubleRange object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        return members.getMember(object, member, pattern, ctx, keepSymbol);
    }

    @Override
    public Object getIndexer(DoubleRange object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(DoubleRange range, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }

    @Override
    public Class<DoubleRange> getHandledClass() {
        return DoubleRange.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "real_range";
    }

    @Override
    public PrimitivePrismarineFunction getConstructor(TokenPattern<?> pattern, ISymbolContext ctx, GenericSupplier genericSupplier) {
        return members.getConstructor();
    }

    public static double getMin(DoubleRange range) {
        if(range.getMin() != null) return range.getMin();
        else return Double.NEGATIVE_INFINITY;
    }

    public static double getMax(DoubleRange range) {
        if(range.getMax() != null) return range.getMax();
        else return Double.POSITIVE_INFINITY;
    }

    @NativeFunctionAnnotations.NotNullReturn
    public static DoubleRange deriveMin(@NativeFunctionAnnotations.NullableArg Double newMin, @NativeFunctionAnnotations.ThisArg DoubleRange thiz) {
        return new DoubleRange(newMin, thiz.getMax());
    }

    @NativeFunctionAnnotations.NotNullReturn
    public static DoubleRange deriveMax(@NativeFunctionAnnotations.NullableArg Double newMax, @NativeFunctionAnnotations.ThisArg DoubleRange thiz) {
        return new DoubleRange(thiz.getMin(), newMax);
    }

    @NativeFunctionAnnotations.NotNullReturn
    public static DoubleRange construct(@NativeFunctionAnnotations.NullableArg Double min, @NativeFunctionAnnotations.NullableArg Double max) {
        return new DoubleRange(min, max);
    }
}
