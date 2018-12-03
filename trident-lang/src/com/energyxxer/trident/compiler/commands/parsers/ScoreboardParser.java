package com.energyxxer.trident.compiler.commands.parsers;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.scoreboard.*;
import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.commodore.textcomponents.TextComponent;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.constructs.TextParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "scoreboard")
public class ScoreboardParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "OBJECTIVES": {
                return parseObjectives(inner, file.getCompiler());
            }
            case "PLAYERS": {
                return parsePlayers(inner, file.getCompiler());
            }
            default: {
                file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + inner.getName() + "'", inner));
                return null;
            }
        }
    }

    private Command parseObjectives(TokenPattern<?> pattern, TridentCompiler compiler) {
        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "ADD": {
                String objectiveName = inner.find("OBJECTIVE_NAME").flatten(false);
                String criteria = inner.find("CRITERIA").flatten(false);
                TextComponent displayName = TextParser.parseTextComponent(inner.find(".TEXT_COMPONENT"), compiler);
                Objective objective = compiler.getModule().getObjectiveManager().create(objectiveName, criteria, displayName, true);
                return new ObjectivesAddCommand(objective);
            }
            case "LIST": {
                return new ObjectivesListCommand();
            }
            case "MODIFY": {
                Objective objective = CommonParsers.parseObjective(inner.find("OBJECTIVE"), compiler);
                TokenPattern<?> sub = ((TokenStructure)inner.find("CHOICE")).getContents();
                switch(sub.getName()) {
                    case "DISPLAYNAME": {
                        return new ObjectiveModifyCommand(objective, ObjectiveModifyCommand.ObjectiveModifyKey.DISPLAY_NAME, TextParser.parseTextComponent(sub.find("TEXT_COMPONENT"), compiler));
                    }
                    case "RENDERTYPE": {
                        return new ObjectiveModifyCommand(objective, ObjectiveModifyCommand.ObjectiveModifyKey.RENDER_TYPE, ObjectiveModifyCommand.ObjectiveRenderType.valueOf(sub.find("CHOICE").flatten(false).toUpperCase()));
                    }
                    default: {
                        compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + sub.getName() + "'", sub));
                        return null;
                    }
                }
            }
            case "REMOVE": {
                Objective objective = CommonParsers.parseObjective(inner.find("OBJECTIVE"), compiler);
                return new ObjectivesRemoveCommand(objective);
            }
            case "SETDISPLAY": {
                SetObjectiveDisplayCommand.ScoreDisplay displaySlot = SetObjectiveDisplayCommand.ScoreDisplay.getValueForKey(inner.find("DISPLAY_SLOT").flatten(false));
                TokenPattern<?> objectiveClause = inner.find("OBJECTIVE_CLAUSE");
                if(objectiveClause != null) {
                    return new SetObjectiveDisplayCommand(
                            CommonParsers.parseObjective(objectiveClause.find("OBJECTIVE"), compiler),
                            displaySlot
                    );
                }
                return new SetObjectiveDisplayCommand(displaySlot);
            }
            default: {
                compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + inner.getName() + "'", inner));
                return null;
            }
        }
    }

    private Command parsePlayers(TokenPattern<?> pattern, TridentCompiler compiler) {
        return null;
    }
}
