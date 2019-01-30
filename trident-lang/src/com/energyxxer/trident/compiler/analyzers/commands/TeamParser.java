package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.team.*;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.textcomponents.TextColor;
import com.energyxxer.commodore.textcomponents.TextComponent;
import com.energyxxer.commodore.types.defaults.TeamReference;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.constructs.TextParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@AnalyzerMember(key = "team")
public class TeamParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "ADD": {
                TeamReference team = new TeamReference(CommonParsers.parseIdentifierA(inner.find("TEAM.IDENTIFIER_A"), file));
                TextComponent displayName = TextParser.parseTextComponent(inner.find("DISPLAY_NAME.TEXT_COMPONENT"), file);
                return new TeamCreateCommand(team, displayName);
            }
            case "EMPTY": {
                TeamReference team = new TeamReference(CommonParsers.parseIdentifierA(inner.find("TEAM.IDENTIFIER_A"), file));
                return new TeamEmptyCommand(team);
            }
            case "JOIN": {
                TeamReference team = new TeamReference(CommonParsers.parseIdentifierA(inner.find("TEAM.IDENTIFIER_A"), file));
                Entity entity = EntityParser.parseEntity(inner.find("SUBJECT.ENTITY"), file);
                return new TeamJoinCommand(team, entity);
            }
            case "LEAVE": {
                Entity entity = EntityParser.parseEntity(inner.find("ENTITY"), file);
                return new TeamLeaveCommand(entity);
            }
            case "LIST": {
                TokenPattern<?> rawTeam = inner.find(".TEAM.IDENTIFIER_A");
                if(rawTeam != null) return new TeamListCommand(new TeamReference(CommonParsers.parseIdentifierA(rawTeam, file)));
                else return new TeamListCommand();
            }
            case "MODIFY": {
                TeamReference team = new TeamReference(CommonParsers.parseIdentifierA(inner.find("TEAM.IDENTIFIER_A"), file));
                return parseModify(inner.find("TEAM_OPTIONS"), file, team);
            }
            case "REMOVE": {
                TeamReference team = new TeamReference(CommonParsers.parseIdentifierA(inner.find("TEAM.IDENTIFIER_A"), file));
                return new TeamRemoveCommand(team);
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, file);
            }
        }
    }

    private Command parseModify(TokenPattern<?> pattern, TridentFile file, TeamReference team) {
        TokenPattern<?> inner = ((TokenStructure)pattern).getContents();
        TeamModifyCommand.TeamModifyKey key = TeamModifyCommand.TeamModifyKey.getValueForKey(inner.flattenTokens().get(0).value);

        Class valueClass = key.getValueClass();
        if(valueClass == Boolean.class) {
            return new TeamModifyCommand(team, key, inner.find("BOOLEAN").flatten(false).equals("true"));
        } else if(valueClass == TextColor.class) {
            String argument = inner.find("TEAM_COLOR").flatten(false).toUpperCase();
            TextColor value = argument.equals("RESET") ? null : TextColor.valueOf(argument);
            return new TeamModifyCommand(team, key, value);
        } else if(valueClass == TextComponent.class) {
            return new TeamModifyCommand(team, key, TextParser.parseTextComponent(inner.find("TEXT_COMPONENT"), file));
        } else if(valueClass == TeamModifyCommand.AppliesTo.class) {
            String rawValue = inner.find("CHOICE").flatten(false);
            for(TeamModifyCommand.AppliesTo rule : TeamModifyCommand.AppliesTo.values()) {
                String shouldBeKey = rule.getValueString().replace("%", key.getValueVerb());
                if(shouldBeKey.equals(rawValue)) {
                    if(key.isTeamValueInverted()) rule = rule.getTeamInverted();
                    return new TeamModifyCommand(team, key, rule);
                }
            }
        }
        throw new TridentException(TridentException.Source.IMPOSSIBLE, "Something went wrong with team modify: " + key, pattern, file);
    }
}
