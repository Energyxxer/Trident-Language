package com.energyxxer.trident.compiler.commands.parsers.modifiers;

import com.energyxxer.commodore.functionlogic.commands.execute.*;
import com.energyxxer.commodore.functionlogic.commands.scoreboard.ScoreComparison;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.nbt.path.NBTPath;
import com.energyxxer.commodore.functionlogic.score.LocalScore;
import com.energyxxer.commodore.util.NumberRange;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.commands.parsers.constructs.EntityParser;
import com.energyxxer.trident.compiler.commands.parsers.constructs.NBTParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;

public class ConditionalParser implements ModifierParser {
    @ParserMember(key = "if")
    public static class IfParser extends ConditionalParser implements ModifierParser {}
    @ParserMember(key = "unless")
    public static class UnlessParser extends ConditionalParser implements ModifierParser {}

    @Override
    public ExecuteModifier parse(TokenPattern<?> pattern, TridentCompiler compiler) {
        ExecuteCondition.ConditionType conditionType =
                pattern.find("HEADER").flatten(false).equals("if") ?
                        ExecuteCondition.ConditionType.IF :
                        ExecuteCondition.ConditionType.UNLESS;

        TokenPattern<?> subject = ((TokenStructure) pattern.find("SUBJECT")).getContents();
        switch(subject.getName()) {
            case "ENTITY_CONDITION": {
                return new ExecuteConditionEntity(conditionType, EntityParser.parseEntity(subject.find("ENTITY"), compiler));
            }
            case "BLOCK_CONDITION": {
                return new ExecuteConditionBlock(conditionType, CoordinateParser.parse(subject.find("COORDINATE_SET"), compiler), CommonParsers.parseBlock(subject.find("BLOCK_TAGGED"), compiler));
            }
            case "SCORE_CONDITION": {
                LocalScore scoreA = new LocalScore(CommonParsers.parseObjective(subject.find("OBJECTIVE"), compiler), EntityParser.parseEntity(subject.find("ENTITY"), compiler));
                TokenStructure choice = (TokenStructure) subject.find("CHOICE");
                String branchName = choice.getContents().getName();
                switch(branchName) {
                    case "COMPARISON": {
                        LocalScore scoreB = new LocalScore(CommonParsers.parseObjective(choice.find("OBJECTIVE"), compiler), EntityParser.parseEntity(choice.find("ENTITY"), compiler));
                        String rawOp = choice.find("OPERATOR").flatten(false);
                        return new ExecuteConditionScoreComparison(conditionType, scoreA, ScoreComparison.getValueForSymbol(rawOp), scoreB);
                    }
                    case "MATCHES": {
                        NumberRange<Integer> range = CommonParsers.parseIntRange(choice.find("INTEGER_NUMBER_RANGE"));
                        return new ExecuteConditionScoreMatch(conditionType, scoreA, range);
                    }
                    case "ISSET": {
                        return new ExecuteConditionScoreMatch(conditionType, scoreA, new NumberRange<>(Integer.MIN_VALUE, null));
                    }
                    default: {
                        compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + branchName + "'", choice));
                        return null;
                    }
                }
            }
            case "REGION_CONDITION": {
                CoordinateSet from = CoordinateParser.parse(subject.find("FROM.COORDINATE_SET"), compiler);
                CoordinateSet to = CoordinateParser.parse(subject.find("TO.COORDINATE_SET"), compiler);
                CoordinateSet template = CoordinateParser.parse(subject.find("TEMPLATE.COORDINATE_SET"), compiler);

                return new ExecuteConditionRegion(conditionType, from, to, template, subject.find("AIR_POLICY").flatten(false).equals("masked") ? ExecuteConditionRegion.AirPolicy.MASKED : ExecuteConditionRegion.AirPolicy.ALL);
            }
            case "DATA_CONDITION": {
                NBTPath path = NBTParser.parsePath(subject.find("NBT_PATH"));

                TokenPattern<?> dataSubject = ((TokenStructure)subject.find("CHOICE")).getContents();
                switch(dataSubject.getName()) {
                    case "BLOCK_SUBJECT": return new ExecuteConditionDataBlock(conditionType, CoordinateParser.parse(dataSubject.find("COORDINATE_SET"), compiler), path);
                    case "ENTITY_SUBJECT": return new ExecuteConditionDataEntity(conditionType, EntityParser.parseEntity(dataSubject.find("ENTITY"), compiler), path);
                    default: {
                        compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + dataSubject.getName() + "'", dataSubject));
                        return null;
                    }
                }
            }
            default: {
                compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + subject.getName() + "'", subject));
                return null;
            }
        }
    }
}
