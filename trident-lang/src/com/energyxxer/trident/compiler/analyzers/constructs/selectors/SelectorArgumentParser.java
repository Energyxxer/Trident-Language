package com.energyxxer.trident.compiler.analyzers.constructs.selectors;

import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerGroup;
import com.energyxxer.trident.compiler.semantics.TridentFile;

import java.util.Collection;

@AnalyzerGroup
public interface SelectorArgumentParser {
    Collection<SelectorArgument> parse(TokenPattern<?> pattern, TridentFile file);
}
