package com.energyxxer.trident.compiler.commands.parsers.modifiers;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCondition;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteConditionBlock;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteConditionEntity;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.commodore.functionlogic.score.LocalScore;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.commands.parsers.constructs.EntityParser;
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
                return new ExecuteConditionBlock(conditionType, CoordinateParser.parse(subject.find("COORDINATE_SET")), CommonParsers.parseBlock(subject.find("BLOCK_TAGGED"), compiler));
            }
            case "SCORE_CONDITION": {
                LocalScore scoreA = new LocalScore(CommonParsers.parseObjective(subject.find("OBJECTIVE"), compiler), EntityParser.parseEntity(subject.find("ENTITY"), compiler));

                break;
            }
        }

        return null;
    }
}
