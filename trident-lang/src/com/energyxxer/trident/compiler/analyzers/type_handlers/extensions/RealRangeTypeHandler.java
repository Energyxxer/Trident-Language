package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.commodore.util.DoubleRange;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberNotFoundException;
import com.energyxxer.trident.compiler.analyzers.type_handlers.VariableMethod;
import com.energyxxer.trident.compiler.analyzers.type_handlers.VariableTypeHandler;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.VariableMethod.HelperMethods.assertOfType;

@AnalyzerMember(key = "com.energyxxer.commodore.util.DoubleRange")
public class RealRangeTypeHandler implements VariableTypeHandler<DoubleRange> {
    @Override
    public Object getMember(DoubleRange object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
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
            return (VariableMethod) (params, patterns, pattern1, file1) -> {
                if(params.length != 1) {
                    throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Method 'deriveMin' requires 1 parameter, instead found " + params.length, pattern, ctx);
                }

                Double newMin = params[0] != null ? assertOfType(params[0], patterns[0], ctx, Double.class) : null;

                return new DoubleRange(newMin, object.getMax());
            };
        }
        if(member.equals("deriveMax")) {
            return (VariableMethod) (params, patterns, pattern1, file1) -> {
                if(params.length != 1) {
                    throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Method 'deriveMax' requires 1 parameter, instead found " + params.length, pattern, ctx);
                }

                Double newMax = params[0] != null ? assertOfType(params[0], patterns[0], ctx, Double.class) : null;

                return new DoubleRange(object.getMin(), newMax);
            };
        }
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(DoubleRange object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    private double getMin(DoubleRange range) {
        if(range.getMin() != null) return range.getMin();
        else return Double.NEGATIVE_INFINITY;
    }

    private double getMax(DoubleRange range) {
        if(range.getMax() != null) return range.getMax();
        else return Double.POSITIVE_INFINITY;
    }

    @Override
    public <F> F cast(DoubleRange range, Class<F> targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }
}
