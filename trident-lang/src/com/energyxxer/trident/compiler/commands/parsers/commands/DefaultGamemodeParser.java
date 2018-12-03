package com.energyxxer.trident.compiler.commands.parsers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.defaultgamemode.DefaultGamemodeCommand;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "defaultgamemode")
public class DefaultGamemodeParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        return new DefaultGamemodeCommand(file.getCompiler().getModule().minecraft.types.gamemode.get(pattern.flattenTokens().get(1).value));
    }
}