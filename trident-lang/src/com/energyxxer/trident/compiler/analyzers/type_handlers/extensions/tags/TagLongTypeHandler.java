package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.tags;

import com.energyxxer.commodore.functionlogic.nbt.NumericNBTTag;
import com.energyxxer.commodore.functionlogic.nbt.TagLong;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberNotFoundException;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentTypeManager;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentMethod;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeHandler;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.TridentMethod.HelperMethods.assertOfType;

public class TagLongTypeHandler implements TypeHandler<TagLong> {
    static final TridentMethod CONSTRUCTOR = TagLongTypeHandler::constructTagLong;

    @Override
    public Object getMember(TagLong object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(TagLong object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public <F> F cast(TagLong object, Class<F> targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(object != null && Number.class.isAssignableFrom(targetType)) {
            Number number = ((NumericNBTTag) object).getValue();
            if(targetType == Integer.class || targetType == int.class) return (F) (Integer)number.intValue();
            if(targetType == Double.class || targetType == double.class) return (F) (Double)number.doubleValue();
        }
        throw new ClassCastException();
    }

    @Override
    public Class<TagLong> getHandledClass() {
        return TagLong.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "tag_long";
    }

    @Override
    public TypeHandler<?> getSuperType() {
        return TridentTypeManager.getHandlerForHandlerClass(NBTTagTypeHandler.class);
    }

    @Override
    public TridentMethod getConstructor() {
        return CONSTRUCTOR;
    }

    @SuppressWarnings("unchecked")
    private static TagLong constructTagLong(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(params.length == 0 || params[0] == null) return new TagLong(0L);
        Object param = assertOfType(params[0], patterns[0], ctx, TagLong.class, Integer.class, Double.class, String.class);
        if(param instanceof String) {
            try {
                return new TagLong(Long.parseLong((String) param));
            } catch(NumberFormatException x) {
                throw new TridentException(TridentException.Source.TYPE_ERROR, x.getMessage(), pattern, ctx);
            }
        } else if(param instanceof Double) {
            return new TagLong((long)(double) param);
        } else {
            return new TagLong((long)(int) param);
        }
    }
}
