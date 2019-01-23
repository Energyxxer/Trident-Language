package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.RawCommand;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@AnalyzerMember(key = "/")
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
