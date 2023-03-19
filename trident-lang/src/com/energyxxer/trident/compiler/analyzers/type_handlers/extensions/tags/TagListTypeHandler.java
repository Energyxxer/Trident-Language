package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.tags;

import com.energyxxer.commodore.functionlogic.nbt.NBTTag;
import com.energyxxer.commodore.functionlogic.nbt.TagList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.TypeHandlerMemberCollection;
import com.energyxxer.trident.compiler.analyzers.type_handlers.NBTToDictionary;

import java.util.Iterator;

public class TagListTypeHandler implements TypeHandler<TagList> {
    private TypeHandlerMemberCollection<TagList> members;
    private final PrismarineTypeSystem typeSystem;

    public TagListTypeHandler(PrismarineTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    @Override
    public PrismarineTypeSystem getTypeSystem() {
        return typeSystem;
    }

    @Override
    public void staticTypeSetup(PrismarineTypeSystem typeSystem, ISymbolContext globalCtx) {
        members = new TypeHandlerMemberCollection<>(typeSystem, globalCtx);
        members.setNotFoundPolicy(TypeHandlerMemberCollection.MemberNotFoundPolicy.THROW_EXCEPTION);

        try {
            members.putMethod(TagList.class.getMethod("merge", TagList.class));
            members.putReadOnlyField("length", TagList::size);
            members.putMethod("toList", NBTToDictionary.class.getMethod("convert", NBTTag.class, TokenPattern.class, ISymbolContext.class));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object getMember(TagList object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        return members.getMember(object, member, pattern, ctx, keepSymbol);
    }

    @Override
    public Object getIndexer(TagList object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        PrismarineTypeSystem.assertOfClass(index, pattern, ctx, Integer.class);
        int realIndex = (int) index;
        if(realIndex < 0 || realIndex >= object.size()) {
            throw new PrismarineException(PrismarineException.Type.INTERNAL_EXCEPTION, "Index out of bounds: " + index + "; Length: " + object.size(), pattern, ctx);
        }
        return object.getAllTags().get(realIndex);
    }

    @Override
    public Object cast(TagList object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        return null;
    }

    @Override
    public Iterator<?> getIterator(TagList object, TokenPattern<?> pattern, ISymbolContext ctx) {
        return object.getAllTags().iterator();
    }

    @Override
    public Class<TagList> getHandledClass() {
        return TagList.class;
    }

    @Override
    public String getTypeIdentifier() {
        return "tag_list";
    }

    @Override
    public TypeHandler<?> getSuperType() {
        return typeSystem.getHandlerForHandlerClass(NBTTagTypeHandler.class);
    }
}
