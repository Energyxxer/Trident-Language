package com.energyxxer.trident.compiler.commands.parsers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.semantics.TridentFile;

public class NullType implements VariableTypeHandler {
    @Override
    public Object getMember(Object object, String member, TokenPattern pattern, TridentFile file, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(Object object, Object index, TokenPattern pattern, TridentFile file, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(Object object, Class targetType, TokenPattern pattern, TridentFile file) {
        if(targetType == String.class) {
            return "null";
        }
        return null;
    }
}
