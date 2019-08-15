package com.energyxxer.trident.compiler.analyzers.constructs.selectors;

import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.functionlogic.selector.arguments.NBTArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.nbtmapper.PathContext;
import com.energyxxer.trident.compiler.analyzers.constructs.NBTParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "nbt")
public class NBTArgumentParser implements SimpleSelectorArgumentParser {
    @Override
    public SelectorArgument parseSingle(TokenPattern<?> pattern, ISymbolContext ctx, PathContext pathContext) {
        TagCompound nbt = NBTParser.parseCompound(pattern.find("NBT_COMPOUND"), ctx);
        NBTParser.analyzeTag(nbt, pathContext, pattern.find("NBT_COMPOUND"), ctx);
        return new NBTArgument(nbt, pattern.find("NEGATED") != null);
    }
}
