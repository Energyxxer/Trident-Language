package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.CommandGroup;
import com.energyxxer.commodore.functionlogic.commands.EmptyCommand;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCommand;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerManager;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.ArrayList;
import java.util.Collection;

@AnalyzerMember(key = "execute")
public class ExecuteParser implements SimpleCommandParser {
    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        ArrayList<ExecuteModifier> modifiers = CommonParsers.parseModifierList(((TokenList) pattern.find("MODIFIER_LIST")), ctx);

        TokenPattern<?> rawCommand = pattern.find("CHAINED_COMMAND.COMMAND");
        if(rawCommand != null) {
            CommandParser parser = AnalyzerManager.getAnalyzer(CommandParser.class, rawCommand.flattenTokens().get(0).value);
            if(parser != null) {
                Collection<Command> commands = parser.parse((TokenPattern<?>) (rawCommand.getContents()), ctx, modifiers);
                if(!commands.isEmpty()) {
                    if(commands.size() == 1) {
                        return new ExecuteCommand(commands.toArray(new Command[0])[0], modifiers);
                    } else {
                        CommandGroup group = new CommandGroup(ctx.getWritingFile().getFunction());
                        for(Command command : commands) {
                            group.append(command);
                        }
                        return new ExecuteCommand(group, modifiers);
                    }
                }
            } else {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown command analyzer for '" + rawCommand.flattenTokens().get(0).value + "'", rawCommand, ctx);
            }
        }
        return new ExecuteCommand(new EmptyCommand(), modifiers);
    }
}
