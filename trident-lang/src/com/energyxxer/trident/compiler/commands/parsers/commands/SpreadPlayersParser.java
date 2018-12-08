package com.energyxxer.trident.compiler.commands.parsers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.spreadplayers.SpreadPlayersCommand;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.commands.parsers.constructs.EntityParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "spreadplayers")
public class SpreadPlayersParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        CoordinateSet pos = CoordinateParser.parse(pattern.find("TWO_COORDINATE_SET"), file.getCompiler());
        double spreadDistance = CommonParsers.parseDouble(pattern.find("SPREAD_DISTANCE"), file.getCompiler());
        double maxRange = CommonParsers.parseDouble(pattern.find("MAX_RANGE"), file.getCompiler());
        boolean respectTeams = pattern.find("RESPECT_TEAMS").flatten(false).equals("true");
        Entity entity = EntityParser.parseEntity(pattern.find("ENTITY"), file.getCompiler());
        return new SpreadPlayersCommand(entity, pos, spreadDistance, maxRange, respectTeams);
    }
}
