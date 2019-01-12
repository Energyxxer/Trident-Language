package com.energyxxer.trident.compiler.commands.parsers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "java.lang.Integer")
public class IntType implements VariableTypeHandler<Integer> {
    @Override
    public Object getMember(Integer object, String member, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(Integer object, Object index, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(Integer object, Class targetType, TokenPattern<?> pattern, TridentFile file) {
        if(targetType == Double.class) return object.doubleValue();
        throw new ClassCastException();
    }

    @Override
    public Object coerce(Integer object, Class targetType, TokenPattern<?> pattern, TridentFile file) {
        if(targetType == Double.class) return object.doubleValue();
        throw new ClassCastException();
    }
}
