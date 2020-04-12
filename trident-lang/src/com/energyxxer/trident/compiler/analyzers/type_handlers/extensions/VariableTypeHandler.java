package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerGroup;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.Iterator;

@AnalyzerGroup(
        classes="BlockTypeHandler," +
                "BooleanTypeHandler," +
                "CoordinateTypeHandler," +
                "EntityTypeHandler," +
                "IntRangeTypeHandler," +
                "IntTypeHandler," +
                "ItemTypeHandler," +
                "NBTPathTypeHandler," +
                "NullTypeHandler," +
                "RealRangeTypeHandler," +
                "RealTypeHandler," +
                "ResourceTypeHandler," +
                "StringTypeHandler," +
                "tags.TagCompoundTypeHandler," +
                "tags.TagListTypeHandler," +
                "TextComponentTypeHandler"
)
public interface VariableTypeHandler<T> {
    Object getMember(T object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol);

    Object getIndexer(T object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol);

    @SuppressWarnings("unchecked")
    <F> F cast(T object, Class<F> targetType, TokenPattern<?> pattern, ISymbolContext ctx);

    default Object coerce(T object, Class targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        return null;
    }

    default Iterator<?> getIterator(T object) {
        return null;
    }

    Class<T> getHandledClass();

    default boolean isPrimitive() {
        return true;
    }
    String getPrimitiveShorthand();
    default boolean isSelfHandler() {
        return this.getClass() == getHandledClass();
    }
    default boolean isInstance(Object obj) {
        return getHandledClass().isInstance(obj);
    }

    default VariableTypeHandler<?> getSuperType() {
        return null;
    }

}
