package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.commodore.functionlogic.nbt.*;
import com.energyxxer.commodore.util.DoubleRange;
import com.energyxxer.commodore.util.IntegerRange;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberNotFoundException;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentTypeManager;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "java.lang.Integer")
public class IntTypeHandler implements TypeHandler<Integer> {
    @Override
    public Object getMember(Integer object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(Integer object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object cast(Integer object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        switch(TridentTypeManager.getInternalTypeIdentifierForType(targetType)) {
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
        throw new ClassCastException();
    }

    @Override
    public Object coerce(Integer object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        if("primitive(real)".equals(TridentTypeManager.getInternalTypeIdentifierForType(targetType))) {
            return object.doubleValue();
        }
        return null;
    }

    @Override
    public boolean canCoerce(Object object, TypeHandler into) {
        return object instanceof Integer && (
                    into instanceof RealTypeHandler
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
