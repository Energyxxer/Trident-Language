package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.commodore.functionlogic.nbt.TagDouble;
import com.energyxxer.commodore.functionlogic.nbt.TagFloat;
import com.energyxxer.commodore.util.DoubleRange;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberNotFoundException;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentTypeManager;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "java.lang.Double")
public class RealTypeHandler implements TypeHandler<Double> {
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
        switch(TridentTypeManager.getInternalTypeIdentifierForType(targetType)) {
            case "primitive(int)": return object.intValue();
            case "primitive(real_range)": return new DoubleRange(object, object);
            case "primitive(nbt_value)":
            case "primitive(tag_double)": return new TagDouble(object);
            case "primitive(tag_float)": return new TagFloat(object.floatValue());
        }
        throw new ClassCastException();
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
