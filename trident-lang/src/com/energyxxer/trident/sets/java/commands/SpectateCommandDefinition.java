package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.spectate.SpectateStartCommand;
import com.energyxxer.commodore.functionlogic.commands.spectate.SpectateStopCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;

import static com.energyxxer.prismarine.PrismarineProductions.group;
import static com.energyxxer.prismarine.PrismarineProductions.optional;

public class SpectateCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"spectate"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                TridentProductions.commandHeader("spectate"),
                optional(
                        TridentProductions.sameLine(),
                        TridentProductions.noToken().addTags("cspn:Target"),
                        productions.getOrCreateStructure("ENTITY"),
                        optional(
                                TridentProductions.sameLine(),
                                TridentProductions.noToken().addTags("cspn:Spectator"),
                                productions.getOrCreateStructure("ENTITY")
                        ).setName("INNER")
                ).setName("INNER")
        );
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        try {
            if (pattern.find("INNER") == null) return new SpectateStopCommand();
            Entity target = (Entity) pattern.find("INNER.ENTITY").evaluate(ctx, null);
            Entity spectator = (Entity) pattern.findThenEvaluate("INNER.INNER.ENTITY", null, ctx, null);
            return new SpectateStartCommand(target, spectator);
        } catch (CommodoreException x) {
            TridentExceptionUtil.handleCommodoreException(x, pattern, ctx)
                    .map("TARGET", pattern.tryFind("INNER.ENTITY"))
                    .map("SPECTATOR", pattern.tryFind("INNER.INNER.ENTITY"))
                    .invokeThrow();
        }
        throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
    }
}
