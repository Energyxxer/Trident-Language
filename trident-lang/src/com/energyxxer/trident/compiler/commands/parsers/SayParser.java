package com.energyxxer.trident.compiler.commands.parsers;

import com.energyxxer.commodore.functionlogic.commands.say.SayCommand;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.CommandParserAnnotation;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@CommandParserAnnotation(headerCommand = "say")
public class SayParser implements CommandParser {
    @Override
    public void parse(TokenPattern<?> pattern, TridentFile file) {
        file.getFunction().append(new SayCommand(pattern.flattenTokens().get(1).value));
    }
}