package com.energyxxer.trident.compiler.commands.parsers.type_handlers;

import com.energyxxer.commodore.item.Item;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "com.energyxxer.commodore.item.Item")
public class ItemTypeHandler implements VariableTypeHandler<Item> {
    @Override
    public Object getMember(Item object, String member, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        if(member.equals("itemType")) {
            return new TridentUtil.ResourceLocation(object.getItemType().toString());
        }
        if(member.equals("tag")) {
            return object.getNBT();
        }
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(Item object, Object index, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <F> F cast(Item object, Class<F> targetType, TokenPattern<?> pattern, TridentFile file) {
        throw new ClassCastException();
    }
}
