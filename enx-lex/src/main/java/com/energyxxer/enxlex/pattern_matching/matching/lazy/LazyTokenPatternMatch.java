package com.energyxxer.enxlex.pattern_matching.matching.lazy;

import com.energyxxer.enxlex.lexical_analysis.LazyLexer;
import com.energyxxer.enxlex.pattern_matching.TokenMatchResponse;
import com.energyxxer.enxlex.pattern_matching.matching.GeneralTokenPatternMatch;
import com.energyxxer.util.Stack;

public abstract class LazyTokenPatternMatch extends GeneralTokenPatternMatch {

    public TokenMatchResponse match(int index, LazyLexer lexer) {
        return match(index, lexer, new Stack());
    }

    public abstract TokenMatchResponse match(int index, LazyLexer lexer, Stack st);
}
