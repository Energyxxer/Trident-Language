package com.energyxxer.trident.sets.java.selector_arguments;

import com.energyxxer.commodore.functionlogic.selector.arguments.DZArgument;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.providers.PatternSwitchProviderUnit;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class DZArgumentParser implements PatternSwitchProviderUnit {
    @Override
    public String[] getSwitchKeys() {
        return new String[] {"dz"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        return group(
                literal("dz").setName("SELECTOR_ARGUMENT_KEY"),
                TridentProductions.equals(),
                wrapper(TridentProductions.real(productions), (v, p, d) -> new DZArgument((double)v))
        ).setSimplificationFunctionContentIndex(2);
    }
}
