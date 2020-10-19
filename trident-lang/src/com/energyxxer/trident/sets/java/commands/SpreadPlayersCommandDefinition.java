package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.spreadplayers.SpreadPlayersCommand;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.suggestions.SuggestionTags;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class SpreadPlayersCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"spreadplayers"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        return group(
                TridentProductions.commandHeader("spreadplayers"),
                productions.getOrCreateStructure("TWO_COORDINATE_SET"),
                TridentProductions.real(productions).setName("SPREAD_DISTANCE").addTags("cspn:Spread Distance"),
                TridentProductions.real(productions).setName("MAX_RANGE").addTags("cspn:Max Range"),
                TridentProductions.versionLimited(productions.worker, "command.spreadplayers.under", false, optional(literal("under"), TridentProductions.integer(productions).setName("MAX_HEIGHT").addTags("cspn:Max Height")).setSimplificationFunctionContentIndex(1).setName("UNDER_CLAUSE")),
                TridentProductions.rawBoolean().setName("RESPECT_TEAMS").addTags(SuggestionTags.ENABLED, "cspn:Respect Teams?"),
                productions.getOrCreateStructure("ENTITY")
        );
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        CoordinateSet pos = (CoordinateSet) pattern.find("TWO_COORDINATE_SET").evaluate(ctx);
        double spreadDistance = (double) pattern.find("SPREAD_DISTANCE").evaluate(ctx);
        double maxRange = (double) pattern.find("MAX_RANGE").evaluate(ctx);
        boolean respectTeams = (boolean) pattern.find("RESPECT_TEAMS").evaluate(ctx);
        Entity entity = (Entity) pattern.find("ENTITY").evaluate(ctx);

        Integer under = (Integer) pattern.findThenEvaluate("UNDER_CLAUSE", null, ctx);

        try {
            if (under != null) {
                return new SpreadPlayersCommand(entity, pos, spreadDistance, maxRange, respectTeams, under);
            } else {
                return new SpreadPlayersCommand(entity, pos, spreadDistance, maxRange, respectTeams);
            }
        } catch (CommodoreException x) {
            TridentExceptionUtil.handleCommodoreException(x, pattern, ctx)
                    .map(CommodoreException.Source.ENTITY_ERROR, pattern.tryFind("ENTITY"))
                    .map("SPREAD_DISTANCE", pattern.tryFind("SPREAD_DISTANCE"))
                    .map("MAX_RANGE", pattern.tryFind("MAX_RANGE"))
                    .map("MAX_HEIGHT", pattern.tryFind("UNDER_CLAUSE.MAX_HEIGHT"))
                    .invokeThrow();
            throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }
}
