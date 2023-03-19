package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.debug.DebugFunctionCommand;
import com.energyxxer.commodore.functionlogic.commands.debug.DebugReportCommand;
import com.energyxxer.commodore.functionlogic.commands.debug.DebugStartCommand;
import com.energyxxer.commodore.functionlogic.commands.debug.DebugStopCommand;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.lexer.TridentSuggestionTags;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class DebugCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"debug"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                TridentProductions.commandHeader("debug"),
                choice(
                        group(
                                literal("function"),
                                TridentProductions.resourceLocationFixer,
                                wrapper(productions.getOrCreateStructure("RESOURCE_LOCATION_TAGGED")).setName("FUNCTION_REFERENCE").addTags(TridentSuggestionTags.RESOURCE, TridentSuggestionTags.FUNCTION)
                        ).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                            return new DebugFunctionCommand(CommonParsers.parseFunctionTag((TokenStructure) p.find("FUNCTION_REFERENCE.RESOURCE_LOCATION_TAGGED"), ctx));
                        }),
                        literal("report").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new DebugReportCommand()),
                        literal("start").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new DebugStartCommand()),
                        literal("stop").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new DebugStopCommand())
                ).setName("INNER")
        ).setSimplificationFunctionFind("INNER");
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new UnsupportedOperationException(); //this step is optimized away
    }
}
