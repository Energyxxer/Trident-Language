package com.energyxxer.trident.compiler.commands.parsers.type_handlers;

import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "com.energyxxer.commodore.functionlogic.nbt.TagCompound")
public class CompoundType implements VariableTypeHandler<TagCompound> {
    @Override
    public Object getMember(TagCompound object, String member, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        if(member.equals("toDictionary")) {
            return new MethodWrapper<TagCompound>("toDictionary", ((instance, params) -> NBTToDictionary.convert(instance, file))).createForInstance(object);
        }
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(TagCompound object, Object index, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <F> F cast(TagCompound object, Class<F> targetType, TokenPattern<?> pattern, TridentFile file) {
        throw new ClassCastException();
    }
}
