package com.energyxxer.trident.compiler.commands.parsers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.worldborder.*;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "worldborder")
public class WorldBorderParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "GET": {
                return new WorldBorderGetWidth();
            }
            case "CHANGE": {
                double distance = CommonParsers.parseDouble(inner.find("DISTANCE"), file);
                int seconds = 0;
                if(inner.find("TIME") != null) seconds = CommonParsers.parseInt(inner.find("TIME"), file);

                if(inner.find("CHOICE.LITERAL_ADD") != null) return new WorldBorderAddDistance(distance, seconds);
                else return new WorldBorderSetDistance(distance, seconds);
            }
            case "DAMAGE": {
                double damageOrDistance = CommonParsers.parseDouble(inner.find("DAMAGE_OR_DISTANCE"), file);
                if(inner.find("CHOICE.LITERAL_AMOUNT") != null) return new WorldBorderSetDamageAmount(damageOrDistance);
                else return new WorldBorderSetDamageBuffer(damageOrDistance);
            }
            case "WARNING": {
                int distanceOrTime = CommonParsers.parseInt(inner.find("DISTANCE_OR_TIME"), file);
                if(inner.find("CHOICE.LITERAL_DISTANCE") != null) return new WorldBorderSetWarningDistance(distanceOrTime);
                else return new WorldBorderSetWarningTime(distanceOrTime);
            }
            case "CENTER": {
                return new WorldBorderSetCenter(CoordinateParser.parse(inner.find("TWO_COORDINATE_SET"), file));
            }
            default: {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + inner.getName() + "'", inner));
                return null;
            }
        }
    }
}
