package com.energyxxer.trident.compiler.commands.parsers;

import com.energyxxer.commodore.functionlogic.commands.seed.SeedCommand;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.CommandParserAnnotation;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@CommandParserAnnotation(headerCommand = "seed")
public class SeedParser implements CommandParser {
    @Override
    public void parse(TokenPattern<?> pattern, TridentFile file) {
        file.getFunction().append(new SeedCommand());
    }
}
