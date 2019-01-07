package com.energyxxer.trident.compiler.commands.parsers.variable_functions;

import com.energyxxer.commodore.functionlogic.nbt.TagByte;
import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.functionlogic.nbt.TagString;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.custom.entities.CustomEntity;
import com.energyxxer.trident.compiler.semantics.custom.items.CustomItem;

@ParserMember(key = "nbt")
public class NBTVariableFunction implements VariableFunction {
    @Override
    public Object process(Object value, TokenPattern<?> pattern, TridentCompiler compiler) {
        if(value instanceof CustomEntity) {
            TagCompound nbt = new TagCompound(new TagString("id", ((CustomEntity) value).getDefaultType().toString()));
            nbt = ((CustomEntity) value).getDefaultNBT().merge(nbt);
            return nbt;
        } else if(value instanceof CustomItem) {
            TagCompound nbt = new TagCompound(
                    new TagString("id", ((CustomItem) value).getDefaultType().toString()),
                    new TagByte("Count", 1));
            if(((CustomItem) value).getDefaultNBT() != null) {
                TagCompound tag = ((CustomItem) value).getDefaultNBT().clone();
                tag.setName("tag");
                nbt = new TagCompound(tag).merge(nbt);
            }
            return nbt;
        } else {
            compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Type of this variable is not compatible with variable function 'nbt'", pattern));
            throw new EntryParsingException();
        }
    }
}
