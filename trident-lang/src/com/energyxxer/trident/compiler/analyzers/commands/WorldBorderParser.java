package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.worldborder.*;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.TridentException;

@AnalyzerMember(key = "worldborder")
public class WorldBorderParser implements SimpleCommandParser {
    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "GET": {
                return new WorldBorderGetWidth();
            }
            case "CHANGE": {
                double distance = CommonParsers.parseDouble(inner.find("DISTANCE"), ctx);
                int seconds = 0;
                if(inner.find("TIME") != null) seconds = CommonParsers.parseInt(inner.find("TIME"), ctx);

                try {
                    if(inner.find("CHOICE.LITERAL_ADD") != null) return new WorldBorderAddDistance(distance, seconds);
                    else return new WorldBorderSetDistance(distance, seconds);
                } catch(CommodoreException x) {
                    TridentException.handleCommodoreException(x, pattern, ctx)
                            .map("DISTANCE", inner.tryFind("DISTANCE"))
                            .map("TIME", inner.tryFind("TIME"))
                            .invokeThrow();
                }
            }
            case "DAMAGE": {
                double damageOrDistance = CommonParsers.parseDouble(inner.find("DAMAGE_OR_DISTANCE"), ctx);
                try {
                    if(inner.find("CHOICE.LITERAL_AMOUNT") != null) return new WorldBorderSetDamageAmount(damageOrDistance);
                    else return new WorldBorderSetDamageBuffer(damageOrDistance);
                } catch(CommodoreException x) {
                    TridentException.handleCommodoreException(x, pattern, ctx)
                            .map(CommodoreException.Source.NUMBER_LIMIT_ERROR, pattern.tryFind("DAMAGE_OR_DISTANCE"))
                            .invokeThrow();
                }
            }
            case "WARNING": {
                int distanceOrTime = CommonParsers.parseInt(inner.find("DISTANCE_OR_TIME"), ctx);
                try {
                    if(inner.find("CHOICE.LITERAL_DISTANCE") != null) return new WorldBorderSetWarningDistance(distanceOrTime);
                    else return new WorldBorderSetWarningTime(distanceOrTime);
                } catch(CommodoreException x) {
                    TridentException.handleCommodoreException(x, pattern, ctx)
                            .map(CommodoreException.Source.NUMBER_LIMIT_ERROR, inner.tryFind("DISTANCE_OR_TIME"))
                            .invokeThrow();
                }
            }
            case "CENTER": {
                return new WorldBorderSetCenter(CoordinateParser.parse(inner.find("TWO_COORDINATE_SET"), ctx));
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + pattern.getName() + "'", pattern, ctx);
            }
        }
    }
}
