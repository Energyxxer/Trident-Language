package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.commodore.functionlogic.nbt.TagDouble;
import com.energyxxer.commodore.functionlogic.nbt.TagFloat;
import com.energyxxer.commodore.util.DoubleRange;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;

public class RealTypeHandler implements TypeHandler<Double> {

    private final PrismarineTypeSystem typeSystem;

    public RealTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public Object getMember(Double object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(Double object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(Double object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        switch(typeSystem.getInternalTypeIdentifierForType(targetType)) {
            case "primitive(int)": return object.intValue();
            case "primitive(real_range)": return new DoubleRange(object, object);
            case "primitive(nbt_value)":
            case "primitive(tag_double)": return new TagDouble(object);
            case "primitive(tag_float)": return new TagFloat(object.floatValue());
        }
        return null;
    }

    @Override
    public Object coerce(Double object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        if ("primitive(real_range)".equals(typeSystem.getInternalTypeIdentifierForType(targetType))) {
            return new DoubleRange(object, object);
        }
        return null;
    }

    @Override
    public boolean canCoerce(Object object, TypeHandler into, ISymbolContext ctx) {
        return object instanceof Double && (into instanceof RealRangeTypeHandler);
    }

    @Override
    public boolean isInstance(Object obj) {
        return obj instanceof Double;
    }

    @Override
    public Class<Double> getHandledClass() {
        return Double.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "real";
    }
}
