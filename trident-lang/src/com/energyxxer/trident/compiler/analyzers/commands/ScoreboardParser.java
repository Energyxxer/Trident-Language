package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.scoreboard.*;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.score.LocalScore;
import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.commodore.textcomponents.TextComponent;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.constructs.TextParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "scoreboard")
public class ScoreboardParser implements SimpleCommandParser {
    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "OBJECTIVES": {
                return parseObjectives(inner, ctx);
            }
            case "PLAYERS": {
                return parsePlayers(inner, ctx);
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, ctx);
            }
        }
    }

    private Command parseObjectives(TokenPattern<?> pattern, ISymbolContext ctx) {
        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "ADD": {
                String objectiveName = CommonParsers.parseIdentifierA(inner.find("OBJECTIVE_NAME.IDENTIFIER_A"), ctx);
                String criteria = CommonParsers.parseIdentifierB(inner.find("CRITERIA.IDENTIFIER_B"), ctx);
                TextComponent displayName = TextParser.parseTextComponent(inner.find(".TEXT_COMPONENT"), ctx);
                Objective objective;
                if(ctx.getCompiler().getModule().getObjectiveManager().exists(objectiveName)) {
                    objective = ctx.getCompiler().getModule().getObjectiveManager().get(objectiveName);
                } else {
                    objective = ctx.getCompiler().getModule().getObjectiveManager().create(objectiveName, criteria, displayName);
                }
                return new ObjectivesAddCommand(objective);
            }
            case "LIST": {
                return new ObjectivesListCommand();
            }
            case "MODIFY": {
                Objective objective = CommonParsers.parseObjective(inner.find("OBJECTIVE_NAME"), ctx);
                TokenPattern<?> sub = ((TokenStructure)inner.find("CHOICE")).getContents();
                switch(sub.getName()) {
                    case "DISPLAYNAME": {
                        return new ObjectiveModifyCommand(objective, ObjectiveModifyCommand.ObjectiveModifyKey.DISPLAY_NAME, TextParser.parseTextComponent(sub.find("TEXT_COMPONENT"), ctx));
                    }
                    case "RENDERTYPE": {
                        return new ObjectiveModifyCommand(objective, ObjectiveModifyCommand.ObjectiveModifyKey.RENDER_TYPE, ObjectiveModifyCommand.ObjectiveRenderType.valueOf(sub.find("CHOICE").flatten(false).toUpperCase()));
                    }
                    default: {
                        throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + sub.getName() + "'", sub, ctx);
                    }
                }
            }
            case "REMOVE": {
                Objective objective = CommonParsers.parseObjective(inner.find("OBJECTIVE_NAME"), ctx);
                return new ObjectivesRemoveCommand(objective);
            }
            case "SETDISPLAY": {
                Type displaySlot = ctx.getCompiler().getModule().minecraft.types.scoreDisplay.get(CommonParsers.parseIdentifierA(inner.find("DISPLAY_SLOT.IDENTIFIER_A"), ctx));
                TokenPattern<?> objectiveClause = inner.find("OBJECTIVE_CLAUSE");
                if(objectiveClause != null) {
                    return new SetObjectiveDisplayCommand(
                            CommonParsers.parseObjective(objectiveClause.find("OBJECTIVE_NAME"), ctx),
                            displaySlot
                    );
                }
                return new SetObjectiveDisplayCommand(displaySlot);
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, ctx);
            }
        }
    }

    private Command parsePlayers(TokenPattern<?> pattern, ISymbolContext ctx) {
        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "CHANGE": {
                LocalScore score = CommonParsers.parseScore(inner.find("SCORE"), ctx);
                int amount = CommonParsers.parseInt(inner.find("INTEGER"), ctx);

                if(inner.find("CHOICE.LITERAL_SET") != null) return new ScoreSet(score, amount);
                if(inner.find("CHOICE.LITERAL_REMOVE") != null) amount *= -1;
                return new ScoreAdd(score, amount);
            }
            case "ENABLE": {
                LocalScore score = CommonParsers.parseScore(inner.find("SCORE"), ctx);
                try {
                    //noinspection ConstantConditions
                    return new TriggerEnable(score.getHolder(), score.getObjective());
                } catch(CommodoreException x) {
                    TridentException.handleCommodoreException(x, inner, ctx)
                            .map(CommodoreException.Source.ENTITY_ERROR, inner.find("SCORE"))
                            .map(CommodoreException.Source.TYPE_ERROR, inner.find("SCORE"))
                            .invokeThrow();
                    throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", inner, ctx);
                }
            }
            case "GET": {
                LocalScore score = CommonParsers.parseScore(inner.find("SCORE"), ctx);
                try {
                    return new ScoreGet(score);
                } catch(CommodoreException x) {
                    TridentException.handleCommodoreException(x, inner, ctx)
                            .map(CommodoreException.Source.ENTITY_ERROR, inner.find("SCORE"))
                            .invokeThrow();
                    throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", inner, ctx);
                }
            }
            case "LIST": {
                Entity entity = EntityParser.parseEntity(inner.find(".ENTITY"), ctx);
                try {
                    return new ScoreList(entity);
                } catch(CommodoreException x) {
                    TridentException.handleCommodoreException(x, inner, ctx)
                            .map(CommodoreException.Source.ENTITY_ERROR, inner.find(".ENTITY"))
                            .invokeThrow();
                }
            }
            case "OPERATION": {
                LocalScore target = CommonParsers.parseScore(inner.find("TARGET_SCORE.SCORE"), ctx);
                LocalScore source = CommonParsers.parseScore(inner.find("SOURCE_SCORE.SCORE"), ctx);
                String rawOperator = inner.find("OPERATOR").flatten(false);
                return new ScorePlayersOperation(target, ScorePlayersOperation.Operation.getOperationForSymbol(rawOperator), source);
            }
            case "RESET": {
                LocalScore score = CommonParsers.parseScore(inner.find("SCORE"), ctx);
                return new ScoreReset(score);
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, ctx);
            }
        }
    }
}
