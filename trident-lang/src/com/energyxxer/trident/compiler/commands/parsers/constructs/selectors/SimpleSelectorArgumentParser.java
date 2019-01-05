package com.energyxxer.trident.compiler.commands.parsers.constructs.selectors;

import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.semantics.TridentFile;

import java.util.Collection;
import java.util.Collections;

public interface SimpleSelectorArgumentParser extends SelectorArgumentParser {
    SelectorArgument parseSingle(TokenPattern<?> pattern, TridentFile file);

    @Override
    default Collection<SelectorArgument> parse(TokenPattern<?> pattern, TridentFile file) {
        return Collections.singletonList(parseSingle(pattern, file));
    }
}
