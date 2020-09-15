package com.energyxxer.enxlex.pattern_matching.matching;

import com.energyxxer.enxlex.lexical_analysis.Lexer;
import com.energyxxer.enxlex.pattern_matching.TokenMatchResponse;

import java.util.ArrayList;
import java.util.Arrays;

public class TokenGlue extends TokenPatternMatch {

    private ArrayList<TokenPatternMatch> required = new ArrayList<>();
    private ArrayList<TokenPatternMatch> ignored = new ArrayList<>();

    public TokenGlue(boolean required, TokenPatternMatch... patterns) {
        if(required)
            this.required.addAll(Arrays.asList(patterns));
        else
            this.ignored.addAll(Arrays.asList(patterns));
    }

    @Override
    public TokenMatchResponse match(int index, Lexer lexer) {
        lexer.setCurrentIndex(index);
        if(lexer.getLookingIndexTrimmed() == index) {
            boolean matched = true;
            for(TokenPatternMatch ignored : this.ignored) {
                TokenMatchResponse match = ignored.match(index, lexer);
                if(match.matched) {
                    matched = false;
                    break;
                }
            }
            if(matched && !this.required.isEmpty()) {
                boolean valid = false;
                for(TokenPatternMatch required : this.required) {
                    TokenMatchResponse match = required.match(index, lexer);
                    if(match.matched) {
                        valid = true;
                        break;
                    }
                }
                matched = valid;
            }
            return new TokenMatchResponse(matched, null, 0, null);
        }
        return new TokenMatchResponse(false, lexer.retrieveAnyToken(), 0, this, null);
    }

    @Override
    public String deepToString(int levels) {
        return null;
    }

    @Override
    public String toTrimmedString() {
        return "Anything but whitespace";
    }
}
