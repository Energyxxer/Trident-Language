package com.energyxxer.trident.sets.java.selector_arguments;

import com.energyxxer.commodore.functionlogic.selector.arguments.SortArgument;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.providers.PatternSwitchProviderUnit;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class SortArgumentParser implements PatternSwitchProviderUnit {
    @Override
    public String[] getSwitchKeys() {
        return new String[] {"sort"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        return group(
                literal("sort").setName("SELECTOR_ARGUMENT_KEY"),
                TridentProductions.equals(),
                wrapper(enumChoice(SortArgument.SortMode.class), (v, p, d) -> new SortArgument((SortArgument.SortMode) v))
        ).setSimplificationFunctionContentIndex(2);
    }
}
