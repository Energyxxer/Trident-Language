package com.energyxxer.trident.sets.java.selector_arguments;

import com.energyxxer.commodore.functionlogic.selector.arguments.LevelArgument;
import com.energyxxer.commodore.util.IntegerRange;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.providers.PatternSwitchProviderUnit;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class LevelArgumentParser implements PatternSwitchProviderUnit {
    @Override
    public String[] getSwitchKeys() {
        return new String[] {"level"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        return group(
                literal("level").setName("SELECTOR_ARGUMENT_KEY"),
                TridentProductions.equals(),
                wrapper(productions.getOrCreateStructure("INTEGER_NUMBER_RANGE"), (v, p, d) -> new LevelArgument((IntegerRange) v))
        ).setSimplificationFunctionContentIndex(2);
    }
}
