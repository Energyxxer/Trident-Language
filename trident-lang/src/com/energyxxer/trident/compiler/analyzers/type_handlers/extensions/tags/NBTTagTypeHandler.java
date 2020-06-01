package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.tags;

import com.energyxxer.commodore.functionlogic.nbt.NBTTag;
import com.energyxxer.commodore.functionlogic.nbt.NumericNBTTag;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberNotFoundException;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentTypeManager;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeHandler;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

public class NBTTagTypeHandler implements TypeHandler<NBTTag> {
    @Override
    public Object getMember(NBTTag object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(NBTTag object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(NBTTag object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(object instanceof NumericNBTTag) {
            switch(TridentTypeManager.getInternalTypeIdentifierForType(targetType)) {
                case "primitive(int)": return ((NumericNBTTag) object).getValue().intValue();
                case "primitive(real)": return ((NumericNBTTag) object).getValue().doubleValue();
            }
        }
        throw new ClassCastException();
    }

    @Override
    public Class<NBTTag> getHandledClass() {
        return NBTTag.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "nbt_value";
    }
}
