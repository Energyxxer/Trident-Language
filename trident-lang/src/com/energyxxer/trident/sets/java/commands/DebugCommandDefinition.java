package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.debug.DebugReportCommand;
import com.energyxxer.commodore.functionlogic.commands.debug.DebugStartCommand;
import com.energyxxer.commodore.functionlogic.commands.debug.DebugStopCommand;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class DebugCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"debug"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        return group(
                TridentProductions.commandHeader("debug"),
                choice(
                        literal("report").setEvaluator((p, d) -> new DebugReportCommand()),
                        literal("start").setEvaluator((p, d) -> new DebugStartCommand()),
                        literal("stop").setEvaluator((p, d) -> new DebugStopCommand())
                ).setName("INNER")
        ).setSimplificationFunctionFind("INNER");
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new UnsupportedOperationException(); //this step is optimized away
    }
}
