package com.energyxxer.trident.compiler.commands.parsers.type_handlers;

import com.energyxxer.commodore.util.NumberRange;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;

@ParserMember(key = "com.energyxxer.commodore.util.NumberRange<Integer>")
public class IntRangeType implements VariableTypeHandler<NumberRange<Integer>> {
    @Override
    public Object getMember(NumberRange<Integer> object, String member, TokenPattern<?> pattern, TridentCompiler compiler, boolean keepSymbol) {
        if(member.equals("min")) {
            return getMin(object);
        }
        if(member.equals("max")) {
            return getMax(object);
        }
        if(member.equals("range")) {
            return getMax(object) - getMin(object);
        }
        return null;
    }

    @Override
    public Object getIndexer(NumberRange<Integer> object, Object index, TokenPattern<?> pattern, TridentCompiler compiler, boolean keepSymbol) {
        return null;
    }

    private int getMin(NumberRange<Integer> range) {
        if(range.getMin() != null) return range.getMin();
        else return Integer.MIN_VALUE;
    }

    private int getMax(NumberRange<Integer> range) {
        if(range.getMax() != null) return range.getMax();
        else return Integer.MAX_VALUE;
    }

    @Override
    public Object cast(NumberRange<Integer> range, Class targetType, TokenPattern<?> pattern, TridentCompiler compiler) {
        if(targetType == String.class) return range.toString();
        return null;
    }
}
