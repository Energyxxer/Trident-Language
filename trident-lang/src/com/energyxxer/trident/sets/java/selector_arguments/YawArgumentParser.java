package com.energyxxer.trident.sets.java.selector_arguments;

import com.energyxxer.commodore.functionlogic.selector.arguments.YawArgument;
import com.energyxxer.commodore.util.DoubleRange;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.providers.PatternSwitchProviderUnit;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class YawArgumentParser implements PatternSwitchProviderUnit {
    @Override
    public String[] getSwitchKeys() {
        return new String[] {"x_rotation"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        return group(
                literal("x_rotation").setName("SELECTOR_ARGUMENT_KEY"),
                TridentProductions.equals(),
                wrapper(productions.getOrCreateStructure("REAL_NUMBER_RANGE"), (v, p, d) -> new YawArgument((DoubleRange) v))
        ).setSimplificationFunctionContentIndex(2);
    }
}
