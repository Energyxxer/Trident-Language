package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.time.TimeAddCommand;
import com.energyxxer.commodore.functionlogic.commands.time.TimeQueryCommand;
import com.energyxxer.commodore.functionlogic.commands.time.TimeSetCommand;
import com.energyxxer.commodore.util.TimeSpan;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class TimeCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"time"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        return group(
                TridentProductions.commandHeader("time"),
                choice(
                        group(literal("query"), enumChoice(TimeQueryCommand.TimeCounter.class).setName("TIME_COUNTER")).setEvaluator((p, d) -> new TimeQueryCommand((TimeQueryCommand.TimeCounter) p.find("TIME_COUNTER").evaluate())),
                        group(literal("set"), choice(
                                wrapper(productions.getOrCreateStructure("TIME"), (v, p, d) -> new TimeSetCommand((TimeSpan) v)),
                                wrapper(enumChoice(TimeSetCommand.TimeOfDay.class), (v, p, d) -> new TimeSetCommand((TimeSetCommand.TimeOfDay) v))
                        )).setSimplificationFunctionContentIndex(1),
                        group(literal("add"), productions.getOrCreateStructure("TIME")).setEvaluator((p, d) -> new TimeAddCommand((TimeSpan) p.find("TIME").evaluate((ISymbolContext) d[0])))
                ).setName("INNER")
        ).setSimplificationFunctionFind("INNER");
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new UnsupportedOperationException(); //this step is optimized away
    }
}
