package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.tags;

import com.energyxxer.commodore.functionlogic.nbt.TagInt;
import com.energyxxer.commodore.functionlogic.nbt.TagIntArray;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberNotFoundException;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentTypeManager;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentMethod;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeHandler;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.TridentMethod.HelperMethods.assertOfType;

public class TagIntArrayTypeHandler implements TypeHandler<TagIntArray> {
    private static final TridentMethod CONSTRUCTOR = TagIntArrayTypeHandler::constructTagIntArray;

    @Override
    public Object getMember(TagIntArray object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(TagIntArray object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <F> F cast(TagIntArray object, Class<F> targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(targetType == ListObject.class) {
            return (F) new ListObject(object.getAllTags());
        }
        throw new ClassCastException();
    }

    @Override
    public Class<TagIntArray> getHandledClass() {
        return TagIntArray.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "tag_int_array";
    }

    @Override
    public TypeHandler<?> getSuperType() {
        return TridentTypeManager.getHandlerForHandlerClass(NBTTagTypeHandler.class);
    }

    @Override
    public TridentMethod getConstructor() {
        return CONSTRUCTOR;
    }

    private static TagIntArray constructTagIntArray(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(params.length == 0 || params[0] == null) return new TagIntArray();
        ListObject list = assertOfType(params[0], patterns[0], ctx, ListObject.class);

        TagIntArray arr = new TagIntArray();

        for(Object obj : list) {
            Object checked = assertOfType(obj, patterns[0], ctx, Integer.class, TagInt.class);
            if(checked instanceof TagInt) {
                arr.add((TagInt) checked);
            } else {
                arr.add(new TagInt((int) checked));
            }
        }

        return arr;
    }
}
