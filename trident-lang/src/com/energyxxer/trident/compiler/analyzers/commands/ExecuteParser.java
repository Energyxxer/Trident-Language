package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.EmptyCommand;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCommand;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerManager;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.modifiers.ModifierParser;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;

import java.util.ArrayList;
import java.util.Collection;

@AnalyzerMember(key = "execute")
public class ExecuteParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        ArrayList<ExecuteModifier> modifiers = new ArrayList<>();

        TokenPattern<?> rawList = pattern.find("MODIFIER_LIST");
        if(rawList instanceof TokenList) {
            TokenList list = (TokenList) rawList;
            for(TokenPattern<?> inner : list.getContents()) {
                ModifierParser parser = AnalyzerManager.getAnalyzer(ModifierParser.class, inner.flattenTokens().get(0).value);
                if(parser != null) {
                    Collection<ExecuteModifier> modifier = parser.parse(inner, file);
                    modifiers.addAll(modifier);
                } else {
                    throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown modifier analyzer for '" + inner.flattenTokens().get(0).value + "'", inner, file);
                }
            }
        }

        TokenPattern<?> rawCommand = pattern.find("CHAINED_COMMAND.COMMAND");
        if(rawCommand != null) {
            CommandParser parser = AnalyzerManager.getAnalyzer(CommandParser.class, rawCommand.flattenTokens().get(0).value);
            if(parser != null) {
                Command command = parser.parse((TokenPattern<?>) (rawCommand.getContents()), file);
                if(command != null) return new ExecuteCommand(command, modifiers);
            } else {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown command analyzer for '" + rawCommand.flattenTokens().get(0).value + "'", rawCommand, file);
            }
        }
        return new ExecuteCommand(new EmptyCommand(), modifiers);
    }
}
