package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.setworldspawn.SetWorldSpawnCommand;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.rotation.RotationUnit;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "setworldspawn")
public class SetWorldSpawnParser implements SimpleCommandParser {
    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        TokenPattern<?> rawCoords = pattern.find(".COORDINATE_SET");

        TokenPattern<?> rawAngle = pattern.find(".ANGLE.MIXABLE_COORDINATE");
        RotationUnit angle = null;
        if(rawAngle != null) {
            angle = CoordinateParser.parseRotationUnit(rawAngle, ctx);
        }

        return rawCoords != null ? new SetWorldSpawnCommand(CoordinateParser.parse(rawCoords, ctx), angle) : new SetWorldSpawnCommand(new CoordinateSet(), angle);
    }
}
