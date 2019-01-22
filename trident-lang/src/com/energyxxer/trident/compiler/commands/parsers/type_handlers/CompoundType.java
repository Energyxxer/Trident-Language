package com.energyxxer.trident.compiler.commands.parsers.type_handlers;

import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

import static com.energyxxer.trident.compiler.commands.parsers.type_handlers.VariableMethod.HelperMethods.assertOfType;

@ParserMember(key = "com.energyxxer.commodore.functionlogic.nbt.TagCompound")
public class CompoundType implements VariableTypeHandler<TagCompound> {
    @Override
    public Object getMember(TagCompound object, String member, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        if(member.equals("toDictionary")) {
            return new MethodWrapper<TagCompound>("toDictionary", ((instance, params) -> NBTToDictionary.convert(instance, pattern, file))).createForInstance(object);
        }
        try {
            if(member.equals("merge")) {
                return new MethodWrapper<TagCompound>(TagCompound.class.getMethod("merge", TagCompound.class)).createForInstance(object);
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        throw new MemberNotFoundException();
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
