package com.energyxxer.trident.compiler.commands.parsers.constructs.selectors;

import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.commodore.functionlogic.selector.arguments.ScoreArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.commodore.util.NumberRange;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;

@ParserMember(key = "scores")
public class ScoresArgumentParser implements SelectorArgumentParser {
    @Override
    public SelectorArgument parse(TokenPattern<?> pattern, TridentCompiler compiler) {
        TokenList scoreList = (TokenList) pattern.find("SCORE_LIST");

        ScoreArgument scores = new ScoreArgument();

        if(scoreList != null) {
            for(TokenPattern<?> rawArg : scoreList.getContents()) {
                if(rawArg.getName().equals("SCORE_ENTRY")) {
                    Objective objective = CommonParsers.parseObjective(rawArg.find("OBJECTIVE_NAME"), compiler);
                    NumberRange<Integer> range = CommonParsers.parseIntRange(rawArg.find("INTEGER_NUMBER_RANGE"));
                    scores.put(objective, range);
                }
            }
        }

        return scores;
    }
}
