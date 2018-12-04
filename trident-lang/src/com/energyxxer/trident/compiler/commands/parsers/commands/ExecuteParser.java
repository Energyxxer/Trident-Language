package com.energyxxer.trident.compiler.commands.parsers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.EmptyCommand;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCommand;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserManager;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.commands.parsers.modifiers.ModifierParser;
import com.energyxxer.trident.compiler.semantics.TridentFile;

import java.util.ArrayList;

@ParserMember(key = "execute")
public class ExecuteParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        ArrayList<ExecuteModifier> modifiers = new ArrayList<>();

        TokenPattern<?> rawList = pattern.find("MODIFIER_LIST");
        if(rawList instanceof TokenList) {
            TokenList list = (TokenList) rawList;
            for(TokenPattern<?> inner : list.getContents()) {
                ModifierParser parser = ParserManager.getParser(ModifierParser.class, inner.flattenTokens().get(0).value);;
                if(parser != null) {
                    ExecuteModifier modifier = parser.parse(inner, file.getCompiler());
                    if(modifier != null) modifiers.add(modifier);
                }
            }
        }

        TokenPattern<?> rawCommand = pattern.find("CHAINED_COMMAND.COMMAND");
        if(rawCommand != null) {
            CommandParser parser = ParserManager.getParser(CommandParser.class, rawCommand.flattenTokens().get(0).value);
            if(parser != null) {
                Command command = parser.parse(rawCommand, file);
                if(command != null) return new ExecuteCommand(command, modifiers);
            }
        }
        return new ExecuteCommand(new EmptyCommand(), modifiers);
    }
}
