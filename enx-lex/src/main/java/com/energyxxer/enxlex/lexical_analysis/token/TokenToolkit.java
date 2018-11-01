package com.energyxxer.enxlex.lexical_analysis.token;

import com.energyxxer.enxlex.pattern_matching.TokenMatchResponse;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by User on 1/1/2017.
 */
public class TokenToolkit {

    public static ArrayList<TokenPattern<?>> search(List<Token> tokens, TokenPatternMatch m) {

        ArrayList<TokenPattern<?>> matches = new ArrayList<>();

        for(int i = 0; i < tokens.size(); i++) {
            TokenMatchResponse response = m.match(tokens.subList(i,tokens.size()));
            if(response.matched) {
                if(response.pattern != null) matches.add(response.pattern);
                i += response.length-1;
            }
        }

        return matches;
    }
}
