package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.spreadplayers.SpreadPlayersCommand;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.TridentException;

@AnalyzerMember(key = "spreadplayers")
public class SpreadPlayersParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, ISymbolContext ctx) {
        CoordinateSet pos = CoordinateParser.parse(pattern.find("TWO_COORDINATE_SET"), ctx);
        double spreadDistance = CommonParsers.parseDouble(pattern.find("SPREAD_DISTANCE"), ctx);
        double maxRange = CommonParsers.parseDouble(pattern.find("MAX_RANGE"), ctx);
        boolean respectTeams = pattern.find("RESPECT_TEAMS").flatten(false).equals("true");
        Entity entity = EntityParser.parseEntity(pattern.find("ENTITY"), ctx);
        try {
            return new SpreadPlayersCommand(entity, pos, spreadDistance, maxRange, respectTeams);
        } catch(CommodoreException x) {
            TridentException.handleCommodoreException(x, pattern, ctx)
                    .map(CommodoreException.Source.ENTITY_ERROR, pattern.find("ENTITY"))
                    .map("SPREAD_DISTANCE", pattern.find("SPREAD_DISTANCE"))
                    .map("MAX_RANGE", pattern.find("MAX_RANGE"))
                    .invokeThrow();
            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", pattern, ctx);
        }
    }
}
