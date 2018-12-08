package com.energyxxer.trident.compiler.commands.parsers.constructs.selectors;

import com.energyxxer.commodore.functionlogic.selector.arguments.NBTArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.parsers.constructs.NBTParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;

@ParserMember(key = "nbt")
public class NBTArgumentParser implements SimpleSelectorArgumentParser {
    @Override
    public SelectorArgument parseSingle(TokenPattern<?> pattern, TridentCompiler compiler) {
        return new NBTArgument(NBTParser.parseCompound(pattern.find("NBT_COMPOUND"), compiler), pattern.find("NEGATED") != null);
    }
}
