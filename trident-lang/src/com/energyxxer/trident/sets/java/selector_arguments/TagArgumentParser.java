package com.energyxxer.trident.sets.java.selector_arguments;

import com.energyxxer.commodore.functionlogic.selector.arguments.TagArgument;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.providers.PatternSwitchProviderUnit;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class TagArgumentParser implements PatternSwitchProviderUnit {
    @Override
    public String[] getSwitchKeys() {
        return new String[] {"tag"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                literal("tag").setName("SELECTOR_ARGUMENT_KEY"),
                TridentProductions.equals(),
                group(TridentProductions.not().setOptional(), wrapperOptional(TridentProductions.identifierA(productions)).setName("TAG")).setEvaluator((p, d) -> {
                    ISymbolContext ctx = (ISymbolContext) d[0];
                    return new TagArgument((String) p.findThenEvaluate("TAG", "", ctx), p.find("NEGATED") != null);
                })
        ).setSimplificationFunctionContentIndex(2);
    }
}
