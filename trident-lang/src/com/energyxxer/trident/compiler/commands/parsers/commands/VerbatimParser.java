package com.energyxxer.trident.compiler.commands.parsers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.RawCommand;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.parsers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "/")
public class VerbatimParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        TokenPattern<?> iblock = pattern.find("INTERPOLATION_BLOCK");
        if(iblock != null) {
            return new RawCommand(InterpolationManager.parse(iblock, file, String.class));
        } else {
            return new RawCommand(pattern.flatten(false).substring(1).trim());
        }
    }
}
