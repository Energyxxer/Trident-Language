package com.energyxxer.trident.compiler.commands.parsers;

import com.energyxxer.commodore.functionlogic.commands.defaultgamemode.DefaultGamemodeCommand;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.CommandParserAnnotation;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@CommandParserAnnotation(headerCommand = "defaultgamemode")
public class DefaultGamemodeParser implements CommandParser {
    @Override
    public void parse(TokenPattern<?> pattern, TridentFile file) {
        file.getFunction().append(new DefaultGamemodeCommand(file.getCompiler().getModule().minecraft.types.gamemode.get(pattern.flattenTokens().get(1).value)));
    }
}