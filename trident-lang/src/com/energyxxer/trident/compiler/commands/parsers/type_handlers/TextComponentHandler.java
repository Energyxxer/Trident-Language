package com.energyxxer.trident.compiler.commands.parsers.type_handlers;

import com.energyxxer.commodore.textcomponents.TextComponent;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "com.energyxxer.commodore.textcomponents.TextComponent")
public class TextComponentHandler implements VariableTypeHandler<TextComponent> {
    @Override
    public Object getMember(TextComponent object, String member, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(TextComponent object, Object index, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <F> F cast(TextComponent object, Class<F> targetType, TokenPattern<?> pattern, TridentFile file) {
        throw new ClassCastException();
    }
}
