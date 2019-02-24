package com.energyxxer.trident.compiler.analyzers.modifiers;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.execute.*;
import com.energyxxer.commodore.functionlogic.commands.scoreboard.ScoreComparison;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.commodore.functionlogic.score.LocalScore;
import com.energyxxer.commodore.util.NumberRange;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.constructs.NBTParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.TridentException;

public class ConditionalParser implements SimpleModifierParser {
    @AnalyzerMember(key = "if")
    public static class IfParser extends ConditionalParser implements SimpleModifierParser {}
    @AnalyzerMember(key = "unless")
    public static class UnlessParser extends ConditionalParser implements SimpleModifierParser {}

    @Override
    public ExecuteModifier parseSingle(TokenPattern<?> pattern, ISymbolContext ctx) {
        ExecuteCondition.ConditionType conditionType =
                pattern.find("HEADER").flatten(false).equals("if") ?
                        ExecuteCondition.ConditionType.IF :
                        ExecuteCondition.ConditionType.UNLESS;

        TokenPattern<?> subject = ((TokenStructure) pattern.find("SUBJECT")).getContents();
        switch(subject.getName()) {
            case "ENTITY_CONDITION": {
                return new ExecuteConditionEntity(conditionType, EntityParser.parseEntity(subject.find("ENTITY"), ctx));
            }
            case "BLOCK_CONDITION": {
                return new ExecuteConditionBlock(conditionType, CoordinateParser.parse(subject.find("COORDINATE_SET"), ctx), CommonParsers.parseBlock(subject.find("BLOCK_TAGGED"), ctx));
            }
            case "SCORE_CONDITION": {
                LocalScore scoreA = new LocalScore(CommonParsers.parseObjective(subject.find("OBJECTIVE_NAME"), ctx), EntityParser.parseEntity(subject.find("ENTITY"), ctx));
                TokenStructure choice = (TokenStructure) subject.find("CHOICE");
                String branchName = choice.getContents().getName();
                try {
                    switch(branchName) {
                        case "COMPARISON": {
                            LocalScore scoreB = new LocalScore(CommonParsers.parseObjective(choice.find("OBJECTIVE_NAME"), ctx), EntityParser.parseEntity(choice.find("ENTITY"), ctx));
                            String rawOp = choice.find("OPERATOR").flatten(false);
                            return new ExecuteConditionScoreComparison(conditionType, scoreA, ScoreComparison.getValueForSymbol(rawOp), scoreB);
                        }
                        case "MATCHES": {
                            NumberRange<Integer> range = CommonParsers.parseIntRange(choice.find("INTEGER_NUMBER_RANGE"), ctx);
                            return new ExecuteConditionScoreMatch(conditionType, scoreA, range);
                        }
                        case "ISSET": {
                            return new ExecuteConditionScoreMatch(conditionType, scoreA, new NumberRange<>(Integer.MIN_VALUE, null));
                        }
                        default: {
                            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + branchName + "'", choice, ctx);
                        }
                    }
                } catch(CommodoreException x) {
                    TridentException.handleCommodoreException(x, pattern, ctx)
                            .map("TARGET_ENTITY", subject.find("ENTITY"))
                            .map("SOURCE_ENTITY", choice.find("ENTITY"))
                            .invokeThrow();
                }
            }
            case "REGION_CONDITION": {
                CoordinateSet from = CoordinateParser.parse(subject.find("FROM.COORDINATE_SET"), ctx);
                CoordinateSet to = CoordinateParser.parse(subject.find("TO.COORDINATE_SET"), ctx);
                CoordinateSet template = CoordinateParser.parse(subject.find("TEMPLATE.COORDINATE_SET"), ctx);

                return new ExecuteConditionRegion(conditionType, from, to, template, subject.find("AIR_POLICY").flatten(false).equals("masked") ? ExecuteConditionRegion.AirPolicy.MASKED : ExecuteConditionRegion.AirPolicy.ALL);
            }
            case "DATA_CONDITION": {
                NBTPath path = NBTParser.parsePath(subject.find("NBT_PATH"), ctx);

                TokenPattern<?> dataSubject = ((TokenStructure)subject.find("CHOICE")).getContents();
                try {
                    switch(dataSubject.getName()) {
                        case "BLOCK_SUBJECT": return new ExecuteConditionDataBlock(conditionType, CoordinateParser.parse(dataSubject.find("COORDINATE_SET"), ctx), path);
                        case "ENTITY_SUBJECT": return new ExecuteConditionDataEntity(conditionType, EntityParser.parseEntity(dataSubject.find("ENTITY"), ctx), path);
                        default: {
                            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + dataSubject.getName() + "'", dataSubject, ctx);
                        }
                    }
                } catch(CommodoreException x) {
                    TridentException.handleCommodoreException(x, pattern, ctx)
                            .map(CommodoreException.Source.ENTITY_ERROR, dataSubject.find("ENTITY"))
                            .invokeThrow();
                }
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + subject.getName() + "'", subject, ctx);
            }
        }
    }
}
