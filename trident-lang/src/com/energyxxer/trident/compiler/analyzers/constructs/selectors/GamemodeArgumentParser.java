package com.energyxxer.trident.compiler.analyzers.constructs.selectors;

import com.energyxxer.commodore.functionlogic.selector.arguments.GamemodeArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.commodore.types.defaults.GamemodeType;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.nbtmapper.PathContext;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "gamemode")
public class GamemodeArgumentParser implements SimpleSelectorArgumentParser {
    @Override
    public SelectorArgument parseSingle(TokenPattern<?> pattern, ISymbolContext ctx, PathContext pathContext) {
        return new GamemodeArgument(CommonParsers.parseType(pattern.find("GAMEMODE.GAMEMODE_ID"), ctx, GamemodeType.CATEGORY), pattern.find("NEGATED") != null);
    }
}
