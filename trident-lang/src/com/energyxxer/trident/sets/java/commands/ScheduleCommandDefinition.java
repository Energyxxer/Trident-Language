package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.schedule.ScheduleClearCommand;
import com.energyxxer.commodore.functionlogic.commands.schedule.ScheduleCommand;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.util.TimeSpan;
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

public class ScheduleCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"schedule"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                TridentProductions.commandHeader("schedule"),
                choice(
                        group(
                                literal("clear"),
                                TridentProductions.resourceLocationFixer,
                                wrapper(productions.getOrCreateStructure("RESOURCE_LOCATION_TAGGED")).setName("FUNCTION_REFERENCE").addTags(TridentSuggestionTags.RESOURCE, TridentSuggestionTags.FUNCTION).addTags("cspn:Function")
                        ).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                            return new ScheduleClearCommand(CommonParsers.parseFunctionTag((TokenStructure) p.find("FUNCTION_REFERENCE.RESOURCE_LOCATION_TAGGED"), ctx));
                        }),
                        group(
                                literal("function"),
                                TridentProductions.resourceLocationFixer,
                                group(productions.getOrCreateStructure("RESOURCE_LOCATION_TAGGED")).setName("FUNCTION_REFERENCE").addTags(TridentSuggestionTags.RESOURCE, TridentSuggestionTags.FUNCTION).addTags("cspn:Function"),
                                productions.getOrCreateStructure("TIME"),
                                enumChoice(ScheduleCommand.ScheduleMode.class).setOptional().setName("SCHEDULE_MODE")
                        ).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                            Type function = CommonParsers.parseFunctionTag((TokenStructure) p.find("FUNCTION_REFERENCE.RESOURCE_LOCATION_TAGGED"), ctx);
                            TimeSpan time = (TimeSpan) p.find("TIME").evaluate(ctx, null);
                            ScheduleCommand.ScheduleMode mode = (ScheduleCommand.ScheduleMode) p.findThenEvaluate("SCHEDULE_MODE", ScheduleCommand.ScheduleMode.APPEND, ctx, null);
                            return new ScheduleCommand(function, time, mode);
                        })
                ).setName("INNER")
        ).setSimplificationFunctionFind("INNER");
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new UnsupportedOperationException(); //this step is optimized away
    }
}
