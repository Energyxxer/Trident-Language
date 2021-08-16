package com.energyxxer.trident.sets.java.selector_arguments;

import com.energyxxer.commodore.functionlogic.selector.arguments.TeamArgument;
import com.energyxxer.commodore.types.defaults.TeamReference;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.providers.PatternSwitchProviderUnit;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class TeamArgumentParser implements PatternSwitchProviderUnit {
    @Override
    public String[] getSwitchKeys() {
        return new String[] {"team"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                literal("team").setName("SELECTOR_ARGUMENT_KEY"),
                TridentProductions.equals(),
                group(TridentProductions.not().setOptional(), wrapperOptional(TridentProductions.identifierA(productions)).setName("TEAM")).setEvaluator((p, d) -> {
                    ISymbolContext ctx = (ISymbolContext) d[0];
                    String rawTeam = (String) p.findThenEvaluate("TEAM", null, ctx);
                    TeamReference team = rawTeam != null ? new TeamReference(rawTeam) : null;
                    return new TeamArgument(team, p.find("NEGATED") != null);
                })
        ).setSimplificationFunctionContentIndex(2);
    }
}
