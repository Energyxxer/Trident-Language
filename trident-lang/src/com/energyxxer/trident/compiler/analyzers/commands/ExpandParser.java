package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCommand;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerManager;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.ArrayList;
import java.util.Collection;

@AnalyzerMember(key = "expand")
public class ExpandParser implements CommandParser {
    @Override
    public Collection<Command> parse(TokenPattern<?> pattern, ISymbolContext ctx) {
        ArrayList<Command> commands = new ArrayList<>();

        TokenList commandPatterns = ((TokenList) pattern.find("COMMANDS"));

        for(TokenPattern<?> inner : commandPatterns.getContents()) {
            TokenPattern<?> expandEntry = ((TokenStructure) inner.find("EXPAND_ENTRY")).getContents();
            if(expandEntry.getName().equals("COMMENT")) continue;
            inner = expandEntry;
            ArrayList<ExecuteModifier> modifiers = CommonParsers.parseModifierList((TokenList) inner.find("MODIFIERS"), ctx);

            TokenPattern<?> commandPattern = inner.find("COMMAND");
            CommandParser parser = AnalyzerManager.getAnalyzer(CommandParser.class, commandPattern.flattenTokens().get(0).value);
            if (parser != null) {
                Collection<Command> innerCommands = parser.parse(((TokenStructure) commandPattern).getContents(), ctx);
                if(modifiers.isEmpty()) {
                    commands.addAll(innerCommands);
                } else {
                    for(Command innerCommand : innerCommands) {
                        commands.add(new ExecuteCommand(innerCommand, modifiers));
                    }
                }
            } else {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown command analyzer for '" + commandPattern.flattenTokens().get(0).value + "'", commandPattern, ctx);
            }
        }

        return commands;
    }
}
