package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.commodore.functionlogic.nbt.NBTTag;
import com.energyxxer.commodore.functionlogic.nbt.NumericNBTTag;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberNotFoundException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

public class NBTTagTypeHandler implements VariableTypeHandler<NBTTag> {
    @Override
    public Object getMember(NBTTag object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(NBTTag object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <F> F cast(NBTTag object, Class<F> targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(object instanceof NumericNBTTag && Number.class.isAssignableFrom(targetType)) {
            Number number = ((NumericNBTTag) object).getValue();
            if(targetType == Integer.class || targetType == int.class) return (F) (Integer)number.intValue();
            if(targetType == Double.class || targetType == double.class) return (F) (Double)number.doubleValue();
        }
        throw new ClassCastException();
    }

    @Override
    public Class<NBTTag> getHandledClass() {
        return NBTTag.class;
    }

    @Override
    public String getPrimitiveShorthand() {
        return "nbt_tag";
    }
}
