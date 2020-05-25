package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.commodore.util.IntegerRange;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberNotFoundException;
import com.energyxxer.trident.compiler.analyzers.type_handlers.NativeMethodWrapper;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentFunction;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "com.energyxxer.commodore.util.IntegerRange")
public class IntRangeTypeHandler implements TypeHandler<IntegerRange> {
    private static final TridentFunction CONSTRUCTOR = new NativeMethodWrapper<>(
            "new int_range",
            ((instance, params) -> new IntegerRange((Integer)params[0], (Integer)params[1])),
            Integer.class,
            Integer.class
    ).setNullable(0).setNullable(1).createForInstance(null);

    @Override
    public Object getMember(IntegerRange object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if(member.equals("min")) {
            return object.getMin();
        }
        if(member.equals("max")) {
            return object.getMax();
        }
        if(member.equals("range")) {
            return getMax(object) - getMin(object);
        }
        if(member.equals("deriveMin")) {
            return (TridentFunction) (params, patterns, pattern1, file1) -> {
                if(params.length != 1) {
                    throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Method 'deriveMin' requires 1 parameter, instead found " + params.length, pattern, ctx);
                }

                Integer newMin = params[0] != null ? TridentFunction.HelperMethods.assertOfClass(params[0], patterns[0], ctx, Integer.class) : null;

                return new IntegerRange(newMin, object.getMax());
            };
        }
        if(member.equals("deriveMax")) {
            return (TridentFunction) (params, patterns, pattern1, file1) -> {
                if(params.length != 1) {
                    throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Method 'deriveMax' requires 1 parameter, instead found " + params.length, pattern, ctx);
                }

                Integer newMax = params[0] != null ? TridentFunction.HelperMethods.assertOfClass(params[0], patterns[0], ctx, Integer.class) : null;

                return new IntegerRange(object.getMin(), newMax);
            };
        }
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(IntegerRange object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    private int getMin(IntegerRange range) {
        if(range.getMin() != null) return range.getMin();
        else return Integer.MIN_VALUE;
    }

    private int getMax(IntegerRange range) {
        if(range.getMax() != null) return range.getMax();
        else return Integer.MAX_VALUE;
    }

    @SuppressWarnings("unchecked")
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
    public TridentFunction getConstructor(TokenPattern<?> pattern, ISymbolContext ctx) {
        return CONSTRUCTOR;
    }
}
