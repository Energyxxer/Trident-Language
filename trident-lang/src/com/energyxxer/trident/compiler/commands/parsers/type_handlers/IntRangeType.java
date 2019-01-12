package com.energyxxer.trident.compiler.commands.parsers.type_handlers;

import com.energyxxer.commodore.util.NumberRange;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

import static com.energyxxer.trident.compiler.commands.parsers.type_handlers.VariableMethod.HelperMethods.assertOfType;

@ParserMember(key = "com.energyxxer.commodore.util.NumberRange<Integer>")
public class IntRangeType implements VariableTypeHandler<NumberRange<Integer>> {
    @Override
    public Object getMember(NumberRange<Integer> object, String member, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        if(member.equals("min")) {
            return getMin(object);
        }
        if(member.equals("max")) {
            return getMax(object);
        }
        if(member.equals("range")) {
            return getMax(object) - getMin(object);
        }
        if(member.equals("setMin")) {
            return (VariableMethod) (params, patterns, pattern1, file1) -> {
                if(params.length != 1) {
                    file1.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Method 'setMin' requires 1 parameter, instead found " + params.length, pattern));
                    throw new EntryParsingException();
                }

                int newMin = assertOfType(params[0], patterns[0], file, Integer.class);

                return new NumberRange<>(newMin, object.getMax());
            };
        }
        if(member.equals("setMax")) {
            return (VariableMethod) (params, patterns, pattern1, file1) -> {
                if(params.length != 1) {
                    file1.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Method 'setMax' requires 1 parameter, instead found " + params.length, pattern));
                    throw new EntryParsingException();
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

    @Override
    public Object cast(NumberRange<Integer> range, Class targetType, TokenPattern<?> pattern, TridentFile file) {
        throw new ClassCastException();
    }
}
