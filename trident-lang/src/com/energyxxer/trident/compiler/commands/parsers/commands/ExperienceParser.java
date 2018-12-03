package com.energyxxer.trident.compiler.commands.parsers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.experience.ExperienceAddCommand;
import com.energyxxer.commodore.functionlogic.commands.experience.ExperienceCommand;
import com.energyxxer.commodore.functionlogic.commands.experience.ExperienceQueryCommand;
import com.energyxxer.commodore.functionlogic.commands.experience.ExperienceSetCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.commands.parsers.constructs.EntityParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "experience")
public class ExperienceParser implements CommandParser {
    @ParserMember(key = "xp")
    public static class ExperienceParserAlias extends ExperienceParser implements CommandParser {}

    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        TokenPattern<?> inner = ((TokenStructure)pattern.find("SUBCOMMAND")).getContents();
        switch(inner.getName()) {
            case "ADD": {
                Entity entity = EntityParser.parseEntity(inner.find("ENTITY"), file.getCompiler());
                int amount = Integer.parseInt(inner.find("INTEGER").flatten(false));
                ExperienceCommand.Unit unit = parseUnit(inner.find("UNIT"));
                return new ExperienceAddCommand(entity, amount, unit);
            }
            case "SET": {
                Entity entity = EntityParser.parseEntity(inner.find("ENTITY"), file.getCompiler());
                int amount = Integer.parseInt(inner.find("INTEGER").flatten(false));
                ExperienceCommand.Unit unit = parseUnit(inner.find("UNIT"));
                return new ExperienceSetCommand(entity, amount, unit);
            }
            case "QUERY": {
                Entity entity = EntityParser.parseEntity(inner.find("ENTITY"), file.getCompiler());
                ExperienceCommand.Unit unit = parseUnit(inner.find("UNIT"));
                return new ExperienceQueryCommand(entity, unit);
            }
            default: {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + inner.getName() + "'", inner));
                return null;
            }
        }
    }

    private ExperienceCommand.Unit parseUnit(TokenPattern<?> pattern) {
        return (pattern != null && pattern.flatten(false).equals("levels")) ? ExperienceCommand.Unit.LEVELS : ExperienceCommand.Unit.POINTS;
    }
}
