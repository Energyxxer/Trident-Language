package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.tags;

import com.energyxxer.commodore.functionlogic.nbt.TagLong;
import com.energyxxer.commodore.functionlogic.nbt.TagLongArray;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberNotFoundException;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentFunction;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentTypeManager;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeHandler;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

public class TagLongArrayTypeHandler implements TypeHandler<TagLongArray> {
    private static final TridentFunction CONSTRUCTOR = (params, patterns, pattern, ctx) -> constructTagLongArray(params, patterns, pattern, ctx);

    @Override
    public Object getMember(TagLongArray object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(TagLongArray object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(TagLongArray object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        if ("primitive(list)".equals(TridentTypeManager.getInternalTypeIdentifierForType(targetType))) {
            return new ListObject(object.getAllTags());
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
    public TridentFunction getConstructor(TokenPattern<?> pattern, ISymbolContext ctx) {
        return CONSTRUCTOR;
    }

    private static TagLongArray constructTagLongArray(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext ctx) {
        if(params.length == 0 || params[0] == null) return new TagLongArray();
        ListObject list = TridentFunction.HelperMethods.assertOfClass(params[0], patterns[0], ctx, ListObject.class);

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
