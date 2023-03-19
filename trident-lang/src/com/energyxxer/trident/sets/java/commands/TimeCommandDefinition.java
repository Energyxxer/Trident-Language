package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.time.TimeAddCommand;
import com.energyxxer.commodore.functionlogic.commands.time.TimeQueryCommand;
import com.energyxxer.commodore.functionlogic.commands.time.TimeSetCommand;
import com.energyxxer.commodore.util.TimeSpan;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class TimeCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"time"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                TridentProductions.commandHeader("time"),
                choice(
                        group(literal("query"), enumChoice(TimeQueryCommand.TimeCounter.class).setName("TIME_COUNTER")).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new TimeQueryCommand((TimeQueryCommand.TimeCounter) p.find("TIME_COUNTER").evaluate(ctx, null))),
                        group(literal("set"), choice(
                                wrapper(productions.getOrCreateStructure("TIME"), (Object v, TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new TimeSetCommand((TimeSpan) v)),
                                wrapper(enumChoice(TimeSetCommand.TimeOfDay.class), (Object v, TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new TimeSetCommand((TimeSetCommand.TimeOfDay) v))
                        )).setSimplificationFunctionContentIndex(1),
                        group(literal("add"), productions.getOrCreateStructure("TIME")).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new TimeAddCommand((TimeSpan) p.find("TIME").evaluate(ctx, null)))
                ).setName("INNER")
        ).setSimplificationFunctionFind("INNER");
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new UnsupportedOperationException(); //this step is optimized away
    }
}
