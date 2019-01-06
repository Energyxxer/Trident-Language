package com.energyxxer.trident.compiler.commands.parsers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "java.lang.Boolean")
public class BooleanType implements VariableTypeHandler<Boolean> {
    @Override
    public Object getMember(Boolean object, String member, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(Boolean object, Object index, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(Boolean object, Class targetType, TokenPattern<?> pattern, TridentFile file) {
        if(targetType == String.class) return String.valueOf(object);
        throw new ClassCastException();
    }
}
