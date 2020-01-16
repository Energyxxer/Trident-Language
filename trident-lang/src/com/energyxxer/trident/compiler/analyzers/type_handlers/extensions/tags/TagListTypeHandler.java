package com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.tags;

import com.energyxxer.commodore.functionlogic.nbt.TagList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.*;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.VariableTypeHandler;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.HashMap;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.VariableMethod.HelperMethods.assertOfType;

@AnalyzerMember(key = "com.energyxxer.commodore.functionlogic.nbt.TagList")
public class TagListTypeHandler implements VariableTypeHandler<TagList> {
    private static HashMap<String, MemberWrapper<TagList>> members = new HashMap<>();

    static {
        try {
            members.put("merge", new MethodWrapper<>(TagList.class.getMethod("merge", TagList.class)));
            members.put("length", new FieldWrapper<>(TagList::size));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object getMember(TagList object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        if(member.equals("toList")) {
            return new MethodWrapper<TagList>("toList", ((instance, params) -> NBTToDictionary.convert(instance, pattern, ctx))).createForInstance(object);
        }
        MemberWrapper<TagList> result = members.get(member);
        if(result == null) throw new MemberNotFoundException();
        return result.unwrap(object);
    }

    @Override
    public Object getIndexer(TagList object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        assertOfType(index, pattern, ctx, Integer.class);
        int realIndex = (int) index;
        if(realIndex < 0 || realIndex >= object.size()) {
            throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Index out of bounds: " + index + "; Length: " + object.size(), pattern, ctx);
        }
        return object.getAllTags().get(realIndex);
    }

    @Override
    public <F> F cast(TagList object, Class<F> targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }
}
