package com.energyxxer.trident.compiler.commands.parsers.constructs.selectors;

import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserGroup;

import java.util.Collection;

@ParserGroup
public interface SelectorArgumentParser {
    Collection<SelectorArgument> parse(TokenPattern<?> pattern, TridentCompiler compiler);
}
