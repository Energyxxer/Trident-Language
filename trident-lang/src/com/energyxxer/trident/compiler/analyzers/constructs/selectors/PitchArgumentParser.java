package com.energyxxer.trident.compiler.analyzers.constructs.selectors;

import com.energyxxer.commodore.functionlogic.selector.arguments.PitchArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.nbtmapper.PathContext;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "y_rotation")
public class PitchArgumentParser implements SimpleSelectorArgumentParser {
    @Override
    public SelectorArgument parseSingle(TokenPattern<?> pattern, ISymbolContext ctx, PathContext pathContext) {
        return new PitchArgument(CommonParsers.parseRealRange(pattern, ctx));
    }
}
