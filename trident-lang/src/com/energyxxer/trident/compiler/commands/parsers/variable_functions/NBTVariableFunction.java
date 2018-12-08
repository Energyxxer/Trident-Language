package com.energyxxer.trident.compiler.commands.parsers.variable_functions;

import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.functionlogic.nbt.TagString;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.custom.entities.CustomEntity;

@ParserMember(key = "nbt")
public class NBTVariableFunction implements VariableFunction {
    @Override
    public Object process(Object value, TokenPattern<?> pattern, TridentCompiler compiler) {
        if(value instanceof CustomEntity) {
            TagCompound nbt = new TagCompound(new TagString("id", ((CustomEntity) value).getDefaultType().toString()));
            if(((CustomEntity) value).getDefaultNBT() != null) nbt = ((CustomEntity) value).getDefaultNBT().merge(nbt);
            return nbt;
        } else {
            compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Type of this variable is not compatible with variable function 'nbt'", pattern));
            throw new EntryParsingException();
        }
    }
}
