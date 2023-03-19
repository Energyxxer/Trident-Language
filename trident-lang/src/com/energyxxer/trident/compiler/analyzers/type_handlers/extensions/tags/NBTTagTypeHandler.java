package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.tags;

import com.energyxxer.commodore.functionlogic.nbt.NBTTag;
import com.energyxxer.commodore.functionlogic.nbt.NumericNBTTag;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;

public class NBTTagTypeHandler implements TypeHandler<NBTTag> {

    private final PrismarineTypeSystem typeSystem;

    public NBTTagTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

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
            switch(typeSystem.getInternalTypeIdentifierForType(targetType)) {
                case "primitive(int)": return ((NumericNBTTag) object).getValue().intValue();
                case "primitive(real)": return ((NumericNBTTag) object).getValue().doubleValue();
            }
        }
        return null;
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
