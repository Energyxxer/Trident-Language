package com.energyxxer.trident.sets.java.selector_arguments;

import com.energyxxer.commodore.functionlogic.selector.arguments.SortArgument;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.providers.PatternSwitchProviderUnit;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class SortArgumentParser implements PatternSwitchProviderUnit<ISymbolContext> {
    @Override
    public String[] getSwitchKeys() {
        return new String[] {"sort"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                literal("sort").setName("SELECTOR_ARGUMENT_KEY"),
                TridentProductions.equals(),
                wrapper(enumChoice(SortArgument.SortMode.class), (Object v, TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new SortArgument((SortArgument.SortMode) v))
        ).setSimplificationFunctionContentIndex(2);
    }
}
