package com.energyxxer.trident.compiler.analyzers.constructs.selectors;

import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.commodore.functionlogic.selector.arguments.ScoreArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.commodore.util.NumberRange;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.TridentException;

@AnalyzerMember(key = "scores")
public class ScoresArgumentParser implements SimpleSelectorArgumentParser {
    @Override
    public SelectorArgument parseSingle(TokenPattern<?> pattern, ISymbolContext ctx) {
        TokenList scoreList = (TokenList) pattern.find("SCORE_LIST");

        ScoreArgument scores = new ScoreArgument();

        if(scoreList != null) {
            for(TokenPattern<?> rawArg : scoreList.getContents()) {
                if(rawArg.getName().equals("SCORE_ENTRY")) {
                    Objective objective = CommonParsers.parseObjective(rawArg.find("OBJECTIVE_NAME"), ctx);
                    NumberRange<Integer> range;

                    TokenPattern<?> valueInner = ((TokenStructure) rawArg.find("SCORE_VALUE")).getContents();
                    switch(valueInner.getName()) {
                        case "INTEGER_NUMBER_RANGE":
                            range = CommonParsers.parseIntRange(valueInner, ctx);
                            break;
                        case "ISSET":
                            range = new NumberRange<>(Integer.MIN_VALUE, null);
                            break;
                        default: {
                            throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + valueInner.getName() + "'", valueInner, ctx);
                        }
                    }

                    scores.put(objective, range);
                }
            }
        }

        return scores;
    }
}
