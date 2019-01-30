package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.scoreboard.*;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.score.LocalScore;
import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.commodore.textcomponents.TextComponent;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.constructs.TextParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@AnalyzerMember(key = "scoreboard")
public class ScoreboardParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "OBJECTIVES": {
                return parseObjectives(inner, file);
            }
            case "PLAYERS": {
                return parsePlayers(inner, file);
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, file);
            }
        }
    }

    private Command parseObjectives(TokenPattern<?> pattern, TridentFile file) {
        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "ADD": {
                String objectiveName = CommonParsers.parseIdentifierA(inner.find("OBJECTIVE_NAME.IDENTIFIER_A"), file);
                String criteria = inner.find("CRITERIA").flatten(false);
                TextComponent displayName = TextParser.parseTextComponent(inner.find(".TEXT_COMPONENT"), file);
                Objective objective;
                if(file.getCompiler().getModule().getObjectiveManager().contains(objectiveName)) {
                    objective = file.getCompiler().getModule().getObjectiveManager().get(objectiveName);
                } else {
                    objective = file.getCompiler().getModule().getObjectiveManager().create(objectiveName, criteria, displayName, true);
                }
                return new ObjectivesAddCommand(objective);
            }
            case "LIST": {
                return new ObjectivesListCommand();
            }
            case "MODIFY": {
                Objective objective = CommonParsers.parseObjective(inner.find("OBJECTIVE"), file);
                TokenPattern<?> sub = ((TokenStructure)inner.find("CHOICE")).getContents();
                switch(sub.getName()) {
                    case "DISPLAYNAME": {
                        return new ObjectiveModifyCommand(objective, ObjectiveModifyCommand.ObjectiveModifyKey.DISPLAY_NAME, TextParser.parseTextComponent(sub.find("TEXT_COMPONENT"), file));
                    }
                    case "RENDERTYPE": {
                        return new ObjectiveModifyCommand(objective, ObjectiveModifyCommand.ObjectiveModifyKey.RENDER_TYPE, ObjectiveModifyCommand.ObjectiveRenderType.valueOf(sub.find("CHOICE").flatten(false).toUpperCase()));
                    }
                    default: {
                        throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + sub.getName() + "'", sub, file);
                    }
                }
            }
            case "REMOVE": {
                Objective objective = CommonParsers.parseObjective(inner.find("OBJECTIVE"), file);
                return new ObjectivesRemoveCommand(objective);
            }
            case "SETDISPLAY": {
                SetObjectiveDisplayCommand.ScoreDisplay displaySlot = SetObjectiveDisplayCommand.ScoreDisplay.getValueForKey(inner.find("DISPLAY_SLOT").flatten(false));
                TokenPattern<?> objectiveClause = inner.find("OBJECTIVE_CLAUSE");
                if(objectiveClause != null) {
                    return new SetObjectiveDisplayCommand(
                            CommonParsers.parseObjective(objectiveClause.find("OBJECTIVE"), file),
                            displaySlot
                    );
                }
                return new SetObjectiveDisplayCommand(displaySlot);
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, file);
            }
        }
    }

    private Command parsePlayers(TokenPattern<?> pattern, TridentFile file) {
        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "CHANGE": {
                Entity entity = EntityParser.parseEntity(inner.find("ENTITY"), file);
                Objective objective = CommonParsers.parseObjective(inner.find("OBJECTIVE"), file);
                int amount = CommonParsers.parseInt(inner.find("INTEGER"), file);

                if(inner.find("CHOICE.LITERAL_SET") != null) return new ScoreSet(new LocalScore(entity, objective), amount);
                if(inner.find("CHOICE.LITERAL_REMOVE") != null) amount *= -1;
                return new ScoreAdd(new LocalScore(entity, objective), amount);
            }
            case "ENABLE": {
                Entity entity = EntityParser.parseEntity(inner.find("ENTITY"), file);
                Objective objective = CommonParsers.parseObjective(inner.find("OBJECTIVE"), file);
                try {
                    return new TriggerEnable(entity, objective);
                } catch(CommodoreException x) {
                    TridentException.handleCommodoreException(x, inner, file)
                            .map(CommodoreException.Source.ENTITY_ERROR, inner.find("ENTITY"))
                            .map(CommodoreException.Source.TYPE_ERROR, inner.find("OBJECTIVE"))
                            .invokeThrow();
                    throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", inner, file);
                }
            }
            case "GET": {
                Entity entity = EntityParser.parseEntity(inner.find("ENTITY"), file);
                Objective objective = CommonParsers.parseObjective(inner.find("OBJECTIVE"), file);
                try {
                    return new ScoreGet(new LocalScore(entity, objective));
                } catch(CommodoreException x) {
                    TridentException.handleCommodoreException(x, inner, file)
                            .map(CommodoreException.Source.ENTITY_ERROR, inner.find("ENTITY"))
                            .invokeThrow();
                    throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", inner, file);
                }
            }
            case "LIST": {
                Entity entity = EntityParser.parseEntity(inner.find(".ENTITY"), file);
                try {
                    return new ScoreList(entity);
                } catch(CommodoreException x) {
                    TridentException.handleCommodoreException(x, inner, file)
                            .map(CommodoreException.Source.ENTITY_ERROR, inner.find(".ENTITY"))
                            .invokeThrow();
                }
            }
            case "OPERATION": {
                LocalScore target = new LocalScore(
                        EntityParser.parseEntity(inner.find("TARGET.ENTITY"), file),
                        CommonParsers.parseObjective(inner.find("TARGET_OBJECTIVE"), file)
                );
                LocalScore source = new LocalScore(
                        EntityParser.parseEntity(inner.find("SOURCE.ENTITY"), file),
                        CommonParsers.parseObjective(inner.find("SOURCE_OBJECTIVE"), file)
                );
                String rawOperator = inner.find("OPERATOR").flatten(false);
                return new ScorePlayersOperation(target, ScorePlayersOperation.Operation.getOperationForSymbol(rawOperator), source);
            }
            case "RESET": {
                Entity entity = EntityParser.parseEntity(inner.find("TARGET.ENTITY"), file);
                TokenPattern<?> objectiveClause = inner.find("OBJECTIVE_CLAUSE");
                if(objectiveClause != null) {
                    Objective objective = CommonParsers.parseObjective(objectiveClause.find("OBJECTIVE"), file);
                    if(entity != null) {
                        return new ScoreReset(entity, objective);
                    } else {
                        return new ScoreReset(objective);
                    }
                }

                if(entity != null) return new ScoreReset(entity);
                else return new ScoreReset();
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, file);
            }
        }
    }
}
