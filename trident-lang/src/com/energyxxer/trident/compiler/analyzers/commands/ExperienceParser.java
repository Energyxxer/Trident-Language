package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.experience.ExperienceAddCommand;
import com.energyxxer.commodore.functionlogic.commands.experience.ExperienceCommand;
import com.energyxxer.commodore.functionlogic.commands.experience.ExperienceQueryCommand;
import com.energyxxer.commodore.functionlogic.commands.experience.ExperienceSetCommand;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.TridentException;

@AnalyzerMember(key = "experience")
public class ExperienceParser implements SimpleCommandParser {
    @AnalyzerMember(key = "xp")
    public static class ExperienceParserAlias extends ExperienceParser implements SimpleCommandParser {}

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        TokenPattern<?> inner = ((TokenStructure)pattern.find("SUBCOMMAND")).getContents();
        switch(inner.getName()) {
            case "ADD": {
                Entity entity = EntityParser.parseEntity(inner.find("ENTITY"), ctx);
                int amount = CommonParsers.parseInt(inner.find("INTEGER"), ctx);
                ExperienceCommand.Unit unit = parseUnit(inner.find("UNIT"));
                return new ExperienceAddCommand(entity, amount, unit);
            }
            case "SET": {
                Entity entity = EntityParser.parseEntity(inner.find("ENTITY"), ctx);
                int amount = CommonParsers.parseInt(inner.find("INTEGER"), ctx);
                ExperienceCommand.Unit unit = parseUnit(inner.find("UNIT"));
                return new ExperienceSetCommand(entity, amount, unit);
            }
            case "QUERY": {
                Entity entity = EntityParser.parseEntity(inner.find("ENTITY"), ctx);
                ExperienceCommand.Unit unit = parseUnit(inner.find("UNIT"));
                return new ExperienceQueryCommand(entity, unit);
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, ctx);
            }
        }
    }

    private ExperienceCommand.Unit parseUnit(TokenPattern<?> pattern) {
        return (pattern != null && pattern.flatten(false).equals("levels")) ? ExperienceCommand.Unit.LEVELS : ExperienceCommand.Unit.POINTS;
    }
}
