package com.energyxxer.trident.compiler.commands.parsers.constructs.selectors;

import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentCompiler;

import java.util.Collection;
import java.util.Collections;

public interface SimpleSelectorArgumentParser extends SelectorArgumentParser {
    SelectorArgument parseSingle(TokenPattern<?> pattern, TridentCompiler compiler);

    @Override
    default Collection<SelectorArgument> parse(TokenPattern<?> pattern, TridentCompiler compiler) {
        return Collections.singletonList(parseSingle(pattern, compiler));
    }
}
