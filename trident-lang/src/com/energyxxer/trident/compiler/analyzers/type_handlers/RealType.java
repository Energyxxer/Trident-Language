package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.commodore.functionlogic.nbt.*;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "java.lang.Double")
public class RealType implements VariableTypeHandler<java.lang.Double> {
    @Override
    public Object getMember(Double object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(Double object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <F> F cast(Double object, Class<F> targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(targetType == Integer.class || targetType == int.class) return (F) (Integer) object.intValue();
        if(targetType == NBTTag.class || targetType == TagDouble.class) return (F) new TagDouble(object);
        if(targetType == TagFloat.class) return (F) new TagFloat(object.floatValue());
        throw new ClassCastException();
    }

    @Override
    public Object coerce(Double object, Class targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(targetType == NBTTag.class || targetType == TagDouble.class) return new TagDouble(object);
        if(targetType == TagFloat.class) return new TagFloat(object.floatValue());
        throw new ClassCastException();
    }
}
