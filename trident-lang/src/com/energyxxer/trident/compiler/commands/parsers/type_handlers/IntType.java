package com.energyxxer.trident.compiler.commands.parsers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;

@ParserMember(key = "java.lang.Integer")
public class IntType implements VariableTypeHandler<Integer> {
    @Override
    public Object getMember(Integer object, String member, TokenPattern<?> pattern, TridentCompiler compiler, boolean keepSymbol) {
        return null;
    }

    @Override
    public Object getIndexer(Integer object, Object index, TokenPattern<?> pattern, TridentCompiler compiler, boolean keepSymbol) {
        return null;
    }

    @Override
    public Object cast(Integer object, Class targetType, TokenPattern<?> pattern, TridentCompiler compiler) {
        if(targetType == String.class) return Integer.toString(object);
        if(targetType == Double.class) return object.doubleValue();
        return null;
    }
}
