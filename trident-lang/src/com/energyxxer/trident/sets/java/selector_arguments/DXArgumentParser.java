package com.energyxxer.trident.sets.java.selector_arguments;

import com.energyxxer.commodore.functionlogic.selector.arguments.DXArgument;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.providers.PatternSwitchProviderUnit;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class DXArgumentParser implements PatternSwitchProviderUnit {
    @Override
    public String[] getSwitchKeys() {
        return new String[] {"dx"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                literal("dx").setName("SELECTOR_ARGUMENT_KEY"),
                TridentProductions.equals(),
                wrapper(TridentProductions.real(productions), (v, p, d) -> new DXArgument((double)v))
        ).setSimplificationFunctionContentIndex(2);
    }
}
