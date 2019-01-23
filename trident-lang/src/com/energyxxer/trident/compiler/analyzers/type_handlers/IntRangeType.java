package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.commodore.util.NumberRange;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.VariableMethod.HelperMethods.assertOfType;

@AnalyzerMember(key = "com.energyxxer.commodore.util.NumberRange<Integer>")
public class IntRangeType implements VariableTypeHandler<NumberRange<Integer>> {
    @Override
    public Object getMember(NumberRange<Integer> object, String member, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
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
                    throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Method 'deriveMin' requires 1 parameter, instead found " + params.length, pattern, file);
                }

                int newMin = assertOfType(params[0], patterns[0], file, Integer.class);

                return new NumberRange<>(newMin, object.getMax());
            };
        }
        if(member.equals("deriveMax")) {
            return (VariableMethod) (params, patterns, pattern1, file1) -> {
                if(params.length != 1) {
                    throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Method 'deriveMax' requires 1 parameter, instead found " + params.length, pattern, file);
                }

                int newMax = assertOfType(params[0], patterns[0], file, Integer.class);

                return new NumberRange<>(object.getMin(), newMax);
            };
        }
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(NumberRange<Integer> object, Object index, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    private int getMin(NumberRange<Integer> range) {
        if(range.getMin() != null) return range.getMin();
        else return Integer.MIN_VALUE;
    }

    private int getMax(NumberRange<Integer> range) {
        if(range.getMax() != null) return range.getMax();
        else return Integer.MAX_VALUE;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <F> F cast(NumberRange<Integer> range, Class<F> targetType, TokenPattern<?> pattern, TridentFile file) {
        throw new ClassCastException();
    }
}
