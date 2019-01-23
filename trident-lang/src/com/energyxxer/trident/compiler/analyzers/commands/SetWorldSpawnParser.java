package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.setworldspawn.SetWorldSpawnCommand;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@AnalyzerMember(key = "setworldspawn")
public class SetWorldSpawnParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        TokenPattern<?> rawCoords = pattern.find(".COORDINATE_SET");
        return rawCoords != null ? new SetWorldSpawnCommand(CoordinateParser.parse(rawCoords, file)) : new SetWorldSpawnCommand(new CoordinateSet());
    }
}
