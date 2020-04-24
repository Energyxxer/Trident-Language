package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.tags;

import com.energyxxer.commodore.functionlogic.nbt.*;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberNotFoundException;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentTypeManager;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentMethod;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeHandler;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.TridentMethod.HelperMethods.assertOfType;

public class TagByteArrayTypeHandler implements TypeHandler<TagByteArray> {
    private static final TridentMethod CONSTRUCTOR = TagByteArrayTypeHandler::constructTagByteArray;

    @Override
    public Object getMember(TagByteArray object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(TagByteArray object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <F> F cast(TagByteArray object, Class<F> targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(targetType == ListObject.class) {
            return (F) new ListObject(object.getAllTags());
        }
        throw new ClassCastException();
    }

    @Override
    public Class<TagByteArray> getHandledClass() {
        return TagByteArray.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "tag_byte_array";
    }

    @Override
    public TypeHandler<?> getSuperType() {
        return TridentTypeManager.getHandlerForHandlerClass(NBTTagTypeHandler.class);
    }

    @Override
    public TridentMethod getConstructor() {
        return CONSTRUCTOR;
    }

    private static TagByteArray constructTagByteArray(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(params.length == 0 || params[0] == null) return new TagByteArray();
        ListObject list = assertOfType(params[0], patterns[0], ctx, ListObject.class);

        TagByteArray arr = new TagByteArray();

        for(Object obj : list) {
            Object checked = assertOfType(obj, patterns[0], ctx, Integer.class, TagByte.class);
            if(checked instanceof TagByte) {
                arr.add((TagByte) checked);
            } else {
                arr.add(new TagByte((int) checked));
            }
        }

        return arr;
    }
}
