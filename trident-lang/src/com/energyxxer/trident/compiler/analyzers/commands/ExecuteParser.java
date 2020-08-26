package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.EmptyCommand;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCommand;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.commodore.functionlogic.functions.FunctionSection;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerManager;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.plugin.CommandDefinition;
import com.energyxxer.trident.compiler.plugin.PluginCommandParser;
import com.energyxxer.trident.compiler.plugin.TridentPlugin;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.ArrayList;
import java.util.Collection;

@AnalyzerMember(key = "execute")
public class ExecuteParser implements SimpleCommandParser {
    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        ArrayList<ExecuteModifier> modifiers = CommonParsers.parseModifierList(((TokenList) pattern.find("MODIFIER_LIST")), ctx);
        modifiers.addAll(0, ctx.getWritingFile().getWritingModifiers());

        TokenPattern<?> rawEnd = pattern.find("EXECUTE_END");
        TokenPattern<?> rawCommand;
        if(rawEnd != null && (rawCommand = ((TokenStructure) rawEnd).getContents()).getName().equals("CHAINED_COMMAND")) {
            rawCommand = rawCommand.find("COMMAND");
            String commandName = rawCommand.flattenTokens().get(0).value;
            FunctionSection appendTo = ctx.getWritingFile().getFunction();
            CommandParser parser = AnalyzerManager.getAnalyzer(CommandParser.class, commandName);
            if (parser != null) {
                Collection<Command> commands = parser.parse(((TokenStructure) rawCommand).getContents(), ctx, modifiers);
                for(Command command : commands) {
                    if (modifiers.isEmpty()) appendTo.append(command);
                    else appendTo.append(new ExecuteCommand(command, modifiers));
                }
            } else {
                boolean found = false;
                for(TridentPlugin plugin : ctx.getCompiler().getWorker().output.transitivePlugins) {
                    CommandDefinition def = plugin.getCommand(commandName);
                    if(def != null) {
                        new PluginCommandParser().handleCommand(def, rawCommand, modifiers, ctx, appendTo);
                        found = true;
                        break;
                    }
                }
                if(!found) throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown command analyzer for '" + commandName + "'", rawCommand, ctx);
            }
            return null;
        }
        return new ExecuteCommand(new EmptyCommand(), modifiers);
    }
}
