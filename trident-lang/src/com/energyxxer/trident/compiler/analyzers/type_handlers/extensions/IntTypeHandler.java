package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.commodore.functionlogic.nbt.*;
import com.energyxxer.commodore.util.DoubleRange;
import com.energyxxer.commodore.util.IntegerRange;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;

public class IntTypeHandler implements TypeHandler<Integer> {

    private final PrismarineTypeSystem typeSystem;

    public IntTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public Object getMember(Integer object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(Integer object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(Integer object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        switch(typeSystem.getInternalTypeIdentifierForType(targetType)) {
            case "primitive(real)": return object.doubleValue();
            case "primitive(int_range)": return new IntegerRange(object, object);
            case "primitive(real_range)": return new DoubleRange(object.doubleValue(), object.doubleValue());
            case "primitive(nbt_value)":
            case "primitive(tag_int)": return new TagInt(object);
            case "primitive(tag_byte)": return new TagByte(object);
            case "primitive(tag_short)": return new TagShort(object);
            case "primitive(tag_float)": return new TagFloat(object);
            case "primitive(tag_double)": return new TagDouble(object);
            case "primitive(tag_long)": return new TagLong(object);
        }
        return null;
    }

    @Override
    public Object coerce(Integer object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        switch(typeSystem.getInternalTypeIdentifierForType(targetType)) {
            case "primitive(real)": {
                return object.doubleValue();
            }
            case "primitive(int_range)": {
                return new IntegerRange(object, object);
            }
            case "primitive(real_range)": {
                return new DoubleRange(object.doubleValue(), object.doubleValue());
            }
        }
        return null;
    }

    @Override
    public boolean canCoerce(Object object, TypeHandler into, ISymbolContext ctx) {
        return object instanceof Integer && (
                into instanceof RealTypeHandler ||
                into instanceof IntRangeTypeHandler ||
                into instanceof RealRangeTypeHandler
        );
    }

    @Override
    public Class<Integer> getHandledClass() {
        return Integer.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "int";
    }
}
