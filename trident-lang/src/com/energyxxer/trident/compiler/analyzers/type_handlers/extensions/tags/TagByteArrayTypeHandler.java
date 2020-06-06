package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.tags;

import com.energyxxer.commodore.functionlogic.nbt.*;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberNotFoundException;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentFunction;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentTypeManager;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeHandler;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.TridentFunction.HelperMethods.assertOfClass;

public class TagByteArrayTypeHandler implements TypeHandler<TagByteArray> {
    private static final TridentFunction CONSTRUCTOR = (params, patterns, pattern, ctx) -> constructTagByteArray(params, patterns, pattern, ctx);

    @Override
    public Object getMember(TagByteArray object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(TagByteArray object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(TagByteArray object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        if ("primitive(list)".equals(TridentTypeManager.getInternalTypeIdentifierForType(targetType))) {
            return new ListObject(object.getAllTags());
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
    public TridentFunction getConstructor(TokenPattern<?> pattern, ISymbolContext ctx) {
        return CONSTRUCTOR;
    }

    private static TagByteArray constructTagByteArray(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(params.length == 0 || params[0] == null) return new TagByteArray();
        ListObject list = TridentFunction.HelperMethods.assertOfClass(params[0], patterns[0], ctx, ListObject.class);

        TagByteArray arr = new TagByteArray();

        for(Object obj : list) {
            Object checked = assertOfClass(obj, patterns[0], ctx, Integer.class, TagByte.class);
            if(checked instanceof TagByte) {
                arr.add((TagByte) checked);
            } else {
                arr.add(new TagByte((int) checked));
            }
        }

        return arr;
    }
}
