package com.energyxxer.enxlex.pattern_matching.matching;

import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.pattern_matching.TokenMatchResponse;
import com.energyxxer.util.Stack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    public TokenMatchResponse match(List<Token> tokens) {
        return match(tokens, null, new Stack());
    }

    @Override
    public TokenMatchResponse match(List<Token> tokens, Token lastToken, Stack st) {
        if(lastToken == null || tokens.isEmpty() || lastToken.loc.index + lastToken.value.length() == tokens.get(0).loc.index) {
            boolean matched = true;
            for(TokenPatternMatch ignored : this.ignored) {
                TokenMatchResponse match = ignored.match(tokens, lastToken, st);
                if(match.matched) {
                    matched = false;
                    break;
                }
            }
            if(matched && !this.required.isEmpty()) {
                boolean valid = false;
                for(TokenPatternMatch required : this.required) {
                    TokenMatchResponse match = required.match(tokens, lastToken, st);
                    if(match.matched) {
                        valid = true;
                        break;
                    }
                }
                matched = valid;
            }
            return new TokenMatchResponse(matched, null, 0, null);
        }
        return new TokenMatchResponse(false, lastToken, 0, this, null);
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
