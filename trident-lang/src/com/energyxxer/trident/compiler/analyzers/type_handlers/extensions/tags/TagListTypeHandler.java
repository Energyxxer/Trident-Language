package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.tags;

import com.energyxxer.commodore.functionlogic.nbt.TagList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.*;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeHandler;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.HashMap;
import java.util.Iterator;

@AnalyzerMember(key = "com.energyxxer.commodore.functionlogic.nbt.TagList")
public class TagListTypeHandler implements TypeHandler<TagList> {
    private static HashMap<String, MemberWrapper<TagList>> members = new HashMap<>();

    static {
        try {
            members.put("merge", new NativeMethodWrapper<>(TagList.class.getMethod("merge", TagList.class)));
            members.put("length", new FieldWrapper<>(TagList::size));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object getMember(TagList object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if(member.equals("toList")) {
            return new NativeMethodWrapper<TagList>("toList", ((instance, params) -> NBTToDictionary.convert(instance, pattern, ctx))).createForInstance(object);
        }
        MemberWrapper<TagList> result = members.get(member);
        if(result == null) throw new MemberNotFoundException();
        return result.unwrap(object);
    }

    @Override
    public Object getIndexer(TagList object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        TridentFunction.HelperMethods.assertOfClass(index, pattern, ctx, Integer.class);
        int realIndex = (int) index;
        if(realIndex < 0 || realIndex >= object.size()) {
            throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Index out of bounds: " + index + "; Length: " + object.size(), pattern, ctx);
        }
        return object.getAllTags().get(realIndex);
    }

    @Override
    public Object cast(TagList object, TypeHandler targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }

    @Override
    public Iterator<?> getIterator(TagList object) {
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
        return TridentTypeManager.getHandlerForHandlerClass(NBTTagTypeHandler.class);
    }
}
