package com.energyxxer.trident.compiler.commands.parsers.constructs.selectors;

import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.functionlogic.selector.arguments.NBTArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.nbtmapper.PathContext;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.parsers.constructs.NBTParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;

import static com.energyxxer.nbtmapper.tags.PathProtocol.ENTITY;

@ParserMember(key = "nbt")
public class NBTArgumentParser implements SimpleSelectorArgumentParser {
    @Override
    public SelectorArgument parseSingle(TokenPattern<?> pattern, TridentCompiler compiler) {
        TagCompound nbt = NBTParser.parseCompound(pattern.find("NBT_COMPOUND"), compiler);
        PathContext context = new PathContext().setIsSetting(true).setProtocol(ENTITY);
        NBTParser.analyzeTag(nbt, context, pattern.find("NBT_COMPOUND"), compiler);
        return new NBTArgument(nbt, pattern.find("NEGATED") != null);
    }
}
