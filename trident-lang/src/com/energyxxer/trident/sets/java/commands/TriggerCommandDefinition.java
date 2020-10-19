package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.trigger.TriggerCommand;
import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class TriggerCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"trigger"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        return group(
                TridentProductions.commandHeader("trigger"),
                productions.getOrCreateStructure("OBJECTIVE_NAME"),
                optional(enumChoice(TriggerCommand.Action.class).setName("TRIGGER_ACTION"), TridentProductions.integer(productions)).setName("INNER")
        );
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        Objective objective = (Objective) pattern.find("OBJECTIVE_NAME").evaluate(ctx, Objective.class);
        TriggerCommand.Action action = (TriggerCommand.Action) pattern.findThenEvaluate("INNER.TRIGGER_ACTION", TriggerCommand.Action.ADD, ctx);
        int amount = (int) pattern.findThenEvaluate("INNER.INTEGER", 1, ctx);
        try {
            return new TriggerCommand(objective, action, amount);
        } catch (CommodoreException x) {
            TridentExceptionUtil.handleCommodoreException(x, pattern, ctx)
                    .map(CommodoreException.Source.TYPE_ERROR, pattern.tryFind("OBJECTIVE_NAME"))
                    .invokeThrow();
            throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }
}
