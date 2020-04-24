package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.tags;

import com.energyxxer.commodore.functionlogic.nbt.TagLong;
import com.energyxxer.commodore.functionlogic.nbt.TagLongArray;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberNotFoundException;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentTypeManager;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentMethod;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeHandler;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.TridentMethod.HelperMethods.assertOfType;

public class TagLongArrayTypeHandler implements TypeHandler<TagLongArray> {
    private static final TridentMethod CONSTRUCTOR = TagLongArrayTypeHandler::constructTagLongArray;

    @Override
    public Object getMember(TagLongArray object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(TagLongArray object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <F> F cast(TagLongArray object, Class<F> targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(targetType == ListObject.class) {
            return (F) new ListObject(object.getAllTags());
        }
        throw new ClassCastException();
    }

    @Override
    public Class<TagLongArray> getHandledClass() {
        return TagLongArray.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "tag_long_array";
    }

    @Override
    public TypeHandler<?> getSuperType() {
        return TridentTypeManager.getHandlerForHandlerClass(NBTTagTypeHandler.class);
    }

    @Override
    public TridentMethod getConstructor(TokenPattern<?> pattern, ISymbolContext ctx) {
        return CONSTRUCTOR;
    }

    private static TagLongArray constructTagLongArray(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(params.length == 0 || params[0] == null) return new TagLongArray();
        ListObject list = assertOfType(params[0], patterns[0], ctx, ListObject.class);

        TagLongArray arr = new TagLongArray();

        for(Object obj : list) {
            if(obj instanceof TagLong) {
                arr.add((TagLong) obj);
            } else {
                arr.add((TagLong) TagLongTypeHandler.CONSTRUCTOR.call(new Object[] {obj}, patterns, pattern, ctx));
            }
        }

        return arr;
    }
}
