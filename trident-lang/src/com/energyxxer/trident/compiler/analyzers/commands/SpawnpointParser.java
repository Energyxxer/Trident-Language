package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.spawnpoint.SpawnpointCommand;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.rotation.RotationUnit;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "spawnpoint")
public class SpawnpointParser implements SimpleCommandParser {
    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        Entity entity = EntityParser.parseEntity(pattern.find(".ENTITY"), ctx);
        TokenPattern<?> rawCoords = pattern.find("..COORDINATE_SET");
        CoordinateSet pos = rawCoords != null ? CoordinateParser.parse(rawCoords, ctx) : null;

        TokenPattern<?> rawAngle = pattern.find("..ANGLE.MIXABLE_COORDINATE");
        RotationUnit angle = null;
        if(rawAngle != null) {
            angle = CoordinateParser.parseRotationUnit(rawAngle, ctx);
        }

        try {
            return new SpawnpointCommand(entity, pos, angle);
        } catch(CommodoreException x) {
            TridentException.handleCommodoreException(x, pattern, ctx)
                    .map(CommodoreException.Source.ENTITY_ERROR, pattern.tryFind(".ENTITY"))
                    .invokeThrow();
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }
}
