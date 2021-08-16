package com.energyxxer.trident.sets.java.selector_arguments;

import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.commodore.functionlogic.selector.arguments.ScoreArgument;
import com.energyxxer.commodore.util.IntegerRange;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.providers.PatternSwitchProviderUnit;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;

import static com.energyxxer.prismarine.PrismarineProductions.*;
import static com.energyxxer.trident.compiler.lexer.TridentTokens.CUSTOM_COMMAND_KEYWORD;

public class ScoresArgumentParser implements PatternSwitchProviderUnit {
    @Override
    public String[] getSwitchKeys() {
        return new String[] {"scores"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {

        TokenPatternMatch scoreArgumentBlock = group(
                TridentProductions.brace("{"),
                list(
                        group(
                                productions.getOrCreateStructure("OBJECTIVE_NAME"),
                                TridentProductions.equals(),
                                choice(
                                        matchItem(CUSTOM_COMMAND_KEYWORD, "isset").setName("ISSET").setEvaluator((p, d) -> new IntegerRange(null, null)),
                                        productions.getOrCreateStructure("INTEGER_NUMBER_RANGE")
                                ).setName("SCORE_VALUE")
                        ).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            ScoreArgument scores = (ScoreArgument) d[1];

                            Objective objective = (Objective) p.find("OBJECTIVE_NAME").evaluate(ctx, Objective.class);

                            IntegerRange range = (IntegerRange) p.find("SCORE_VALUE").evaluate(ctx);
                            scores.put(objective, range);
                            return null;
                        }),
                        TridentProductions.comma()
                ).setOptional().setName("SCORE_LIST"),
                TridentProductions.brace("}")
        ).setEvaluator((p, d) -> {
            ISymbolContext ctx = (ISymbolContext) d[0];
            TokenList scoreList = (TokenList) p.find("SCORE_LIST");

            ScoreArgument scores = new ScoreArgument();

            if(scoreList != null) {
                for(TokenPattern<?> entry : scoreList.getContentsExcludingSeparators()) {
                    entry.evaluate(ctx, scores);
                }
            }

            return scores;
        });

        return group(
                literal("scores").setName("SELECTOR_ARGUMENT_KEY"),
                TridentProductions.equals(),
                scoreArgumentBlock
        ).setSimplificationFunctionContentIndex(2);
    }
}
