package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.team.*;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.textcomponents.TextColor;
import com.energyxxer.commodore.textcomponents.TextComponent;
import com.energyxxer.commodore.types.defaults.TeamReference;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.constructs.TextParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@AnalyzerMember(key = "team")
public class TeamParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "ADD": {
                TeamReference team = new TeamReference(inner.find("TEAM").flatten(false));
                TextComponent displayName = TextParser.parseTextComponent(inner.find("DISPLAY_NAME.TEXT_COMPONENT"), file);
                return new TeamCreateCommand(team, displayName);
            }
            case "EMPTY": {
                TeamReference team = new TeamReference(inner.find("TEAM").flatten(false));
                return new TeamEmptyCommand(team);
            }
            case "JOIN": {
                TeamReference team = new TeamReference(inner.find("TEAM").flatten(false));
                Entity entity = EntityParser.parseEntity(inner.find("SUBJECT.ENTITY"), file);
                return new TeamJoinCommand(team, entity);
            }
            case "LEAVE": {
                Entity entity = EntityParser.parseEntity(inner.find("ENTITY"), file);
                return new TeamLeaveCommand(entity);
            }
            case "LIST": {
                TokenPattern<?> rawTeam = inner.find(".TEAM");
                if(rawTeam != null) return new TeamListCommand(new TeamReference(rawTeam.flatten(false)));
                else return new TeamListCommand();
            }
            case "MODIFY": {
                TeamReference team = new TeamReference(inner.find("TEAM").flatten(false));
                return parseModify(inner.find("TEAM_OPTIONS"), file, team);
            }
            case "REMOVE": {
                TeamReference team = new TeamReference(inner.find("TEAM").flatten(false));
                return new TeamRemoveCommand(team);
            }
            default: {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + inner.getName() + "'", inner));
                return null;
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
            return new TeamModifyCommand(team, key, TextColor.valueOf(inner.find("TEXT_COLOR").flatten(false).toUpperCase()));
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
        file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Something went wrong with team option " + pattern, pattern));
        return null;
    }
}
