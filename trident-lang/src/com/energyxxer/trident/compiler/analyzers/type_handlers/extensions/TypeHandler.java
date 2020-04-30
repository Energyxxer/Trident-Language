package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerGroup;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentTypeManager;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentMethod;
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
public interface TypeHandler<T> {
    Object getMember(T object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol);

    Object getIndexer(T object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol);

    Object cast(T object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx);

    default Object coerce(T object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        return null;
    }

    default boolean canCoerce(Object object, TypeHandler into) {
        return false;
    }

    default Iterator<?> getIterator(T object) {
        return null;
    }

    Class<T> getHandledClass();
    default boolean isPrimitive() {
        return true;
    }
    String getTypeIdentifier();
    default boolean isSelfHandler() {
        return this.getClass() == getHandledClass();
    }

    default boolean isInstance(Object obj) {
        return getHandledClass().isInstance(obj);
    }

    default TypeHandler<?> getSuperType() {
        return null;
    }

    default TridentMethod getConstructor(TokenPattern<?> pattern, ISymbolContext ctx) {
        return null;
    }

    default boolean isStaticHandler() {
        return TridentTypeManager.isStaticPrimitiveHandler(this);
    }

    default TypeHandler getStaticHandler() {
        if(isStaticHandler()) return TridentTypeManager.getTypeHandlerTypeHandler();
        return TridentTypeManager.getPrimitiveHandlerForShorthand(getTypeIdentifier());
    }
}
