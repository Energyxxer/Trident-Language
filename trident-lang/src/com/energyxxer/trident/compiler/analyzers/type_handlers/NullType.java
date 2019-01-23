package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.semantics.TridentFile;

public class NullType implements VariableTypeHandler<Object> {
    @Override
    public Object getMember(Object object, String member, TokenPattern pattern, TridentFile file, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(Object object, Object index, TokenPattern pattern, TridentFile file, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <F> F cast(Object object, Class<F> targetType, TokenPattern<?> pattern, TridentFile file) {
        return null;
    }
}
