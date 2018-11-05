package com.energyxxer.enxlex.lexical_analysis.profiles;

import com.energyxxer.enxlex.lexical_analysis.token.TokenType;

import java.util.Collection;
import java.util.Collections;

public class StringMatchLexerContext implements LexerContext {
    private final TokenType type;
    private final String[] strings;

    public StringMatchLexerContext(TokenType type, String... strings) {
        this.type = type;
        this.strings = strings;
    }

    @Override
    public ScannerContextResponse analyze(String str, LexerProfile profile) {
        for(String match : strings) {
            if(str.startsWith(match) && (str.length() == match.length() || !(profile.canMerge(str.charAt(match.length()-1), str.charAt(match.length()))))) return new ScannerContextResponse(true, match, type);
        }
        return new ScannerContextResponse(false);
    }

    @Override
    public Collection<TokenType> getHandledTypes() {
        return Collections.singletonList(type);
    }
}
