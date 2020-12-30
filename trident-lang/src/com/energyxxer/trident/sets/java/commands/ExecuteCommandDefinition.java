package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.EmptyCommand;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCommand;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.enxlex.lexical_analysis.inspections.CodeReplacementAction;
import com.energyxxer.enxlex.lexical_analysis.inspections.Inspection;
import com.energyxxer.enxlex.lexical_analysis.inspections.InspectionSeverity;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenGroup;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.CommandDefinition;
import com.energyxxer.trident.worker.tasks.SetupWritingStackTask;
import com.energyxxer.util.StringBounds;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static com.energyxxer.prismarine.PrismarineProductions.*;
import static com.energyxxer.trident.compiler.TridentProductions.sameLine;
import static com.energyxxer.trident.compiler.lexer.TridentTokens.COMMAND_HEADER;

public class ExecuteCommandDefinition implements CommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"execute"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        return group(
                TridentProductions.commandHeader("execute"),
                productions.getOrCreateStructure("MODIFIER_LIST"),
                choice(
                        literal("noop").setEvaluator((p, d) -> Collections.singletonList(new EmptyCommand())),
                        group(
                                literal("run"),
                                productions.getOrCreateStructure("COMMAND")
                        ).setSimplificationFunctionContentIndex(1).setName("CHAINED_COMMAND").addTags("cspn:Chained Command"),
                        fail(group(sameLine(), ofType(COMMAND_HEADER)).addProcessor((p, l) -> {
                            String commandName = ((TokenGroup) p).getContents()[1].flatten(false);
                            if(!"run".equals(commandName) && !"noop".equals(commandName) && l.getInspectionModule() != null) {
                                StringBounds bounds = p.getStringBounds();
                                Inspection inspection = new Inspection("Probable bug: No 'run' nor 'noop' keyword; these two commands are separate")
                                        .setBounds(bounds.start.index, bounds.end.index)
                                        .setSeverity(InspectionSeverity.WARNING);

                                int commandStartIndex = ((TokenGroup)p).getContents()[1].getStringLocation().index;

                                inspection.addAction(
                                        new CodeReplacementAction("Insert 'run'")
                                                .setReplacementStartIndex(bounds.start.index)
                                                .setReplacementEndIndex(commandStartIndex)
                                                .setReplacementText(" run ")
                                );
                                inspection.addAction(
                                        new CodeReplacementAction("Insert 'noop'")
                                                .setReplacementStartIndex(bounds.start.index)
                                                .setReplacementEndIndex(commandStartIndex)
                                                .setReplacementText(" noop ")
                                );
                                inspection.addAction(
                                        new CodeReplacementAction("Insert newline")
                                        .setReplacementStartIndex(bounds.start.index)
                                        .setReplacementEndIndex(commandStartIndex)
                                        .setReplacementText("\n\bINDENT\b")
                                );

                                l.getInspectionModule().addInspection(inspection);
                            }
                        })).setOptional()
                ).setName("EXECUTE_END").setOptional()
        );
    }

    @Override
    public Collection<Command> parse(TokenPattern<?> pattern, ISymbolContext ctx, Collection<ExecuteModifier> ignore) {
        ArrayList<ExecuteModifier> modifiers = (ArrayList<ExecuteModifier>) pattern.findThenEvaluateLazyDefault("MODIFIER_LIST", ArrayList::new, ctx);
        modifiers.addAll(0, ctx.get(SetupWritingStackTask.INSTANCE).getWritingFile().getWritingModifiers());
        Object returnedCommands = pattern.findThenEvaluateLazyDefault("EXECUTE_END", () -> {
            if (modifiers.isEmpty()) {
                return Collections.emptyList();
            } else {
                return Collections.singletonList(new EmptyCommand());
            }
        }, ctx, modifiers);
        if(returnedCommands instanceof Command) {
            returnedCommands = Collections.singletonList((Command)returnedCommands);
        }
        Collection<Command> commands = (Collection<Command>) returnedCommands;
        if (modifiers.isEmpty()) return commands;

        ArrayList<Command> outCommands = new ArrayList<>();
        for (Command command : commands) {
            outCommands.add(new ExecuteCommand(command, modifiers));
        }
        return outCommands;
    }
}
