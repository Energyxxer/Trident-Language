package com.energyxxer.trident.compiler.analyzers.constructs.selectors;

import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.Collection;
import java.util.Collections;

public interface SimpleSelectorArgumentParser extends SelectorArgumentParser {
    SelectorArgument parseSingle(TokenPattern<?> pattern, ISymbolContext ctx);

    @Override
    default Collection<SelectorArgument> parse(TokenPattern<?> pattern, ISymbolContext ctx) {
        return Collections.singletonList(parseSingle(pattern, ctx));
    }
}
