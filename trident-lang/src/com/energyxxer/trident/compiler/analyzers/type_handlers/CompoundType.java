package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

import java.util.HashMap;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.VariableMethod.HelperMethods.assertOfType;

@AnalyzerMember(key = "com.energyxxer.commodore.functionlogic.nbt.TagCompound")
public class CompoundType implements VariableTypeHandler<TagCompound> {
    private static HashMap<String, MemberWrapper<TagCompound>> members = new HashMap<>();

    static {
        try {
            members.put("merge", new MethodWrapper<>(TagCompound.class.getMethod("merge", TagCompound.class)));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object getMember(TagCompound object, String member, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        if(member.equals("toDictionary")) {
            return new MethodWrapper<TagCompound>("toDictionary", ((instance, params) -> NBTToDictionary.convert(instance, pattern, file))).createForInstance(object);
        }
        MemberWrapper<TagCompound> result = members.get(member);
        if(result == null) throw new MemberNotFoundException();
        return result.unwrap(object);
    }

    @Override
    public Object getIndexer(TagCompound object, Object index, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        String key = assertOfType(index, pattern, file, String.class);
        if(object.contains(key)) return object.get(key);
        else return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <F> F cast(TagCompound object, Class<F> targetType, TokenPattern<?> pattern, TridentFile file) {
        throw new ClassCastException();
    }
}
