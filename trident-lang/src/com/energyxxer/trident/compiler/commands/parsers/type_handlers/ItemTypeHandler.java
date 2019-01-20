package com.energyxxer.trident.compiler.commands.parsers.type_handlers;

import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.item.Item;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.AutoPropertySymbol;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "com.energyxxer.commodore.item.Item")
public class ItemTypeHandler implements VariableTypeHandler<Item> {
    @Override
    public Object getMember(Item object, String member, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        if(member.equals("itemType")) {
            AutoPropertySymbol property = new AutoPropertySymbol<>("itemType", TridentUtil.ResourceLocation.class, () -> new TridentUtil.ResourceLocation(object.getItemType().toString()), value -> {
                if(file.getCompiler().getModule().namespaceExists(value.namespace) && file.getCompiler().getModule().getNamespace(value.namespace).types.item.exists(value.body)) {
                    object.setItemType(file.getCompiler().getModule().getNamespace(value.namespace).types.item.get(value.body));
                } else {
                    file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, value + " is not a valid item type", pattern));
                    throw new EntryParsingException();
                }
            });
            return keepSymbol ? property : property.getValue();
        }
        if(member.equals("itemTag")) {
            AutoPropertySymbol property = new AutoPropertySymbol<>("itemTag", TagCompound.class, object::getNBT, object::setNbt);
            return keepSymbol ? property : property.getValue();
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
