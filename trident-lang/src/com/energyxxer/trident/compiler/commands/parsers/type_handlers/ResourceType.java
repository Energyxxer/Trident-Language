package com.energyxxer.trident.compiler.commands.parsers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "com.energyxxer.trident.compiler.TridentUtil\$ResourceLocation")
public class ResourceType implements VariableTypeHandler<TridentUtil.ResourceLocation> {
    @Override
    public Object getMember(TridentUtil.ResourceLocation object, String member, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(TridentUtil.ResourceLocation object, Object index, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object cast(TridentUtil.ResourceLocation object, Class targetType, TokenPattern<?> pattern, TridentFile file) {
        throw new ClassCastException();
    }
}
