package com.energyxxer.enxlex.lexical_analysis.profiles;

import com.energyxxer.enxlex.lexical_analysis.token.TokenType;

import java.util.Collection;
import java.util.Collections;

public class StringMatchLexerContext implements LexerContext {
    private final TokenType type;
    private final String[] strings;
    private boolean onlyWhenExpected = false;

    public StringMatchLexerContext(TokenType type, String... strings) {
        this.type = type;
        this.strings = strings;
    }

    @Override
    public ScannerContextResponse analyze(String str, int startIndex, LexerProfile profile) {
        return !onlyWhenExpected ? analyzeExpectingType(str, startIndex, type, profile) : ScannerContextResponse.FAILED;
    }

    @Override
    public ScannerContextResponse analyzeExpectingType(String str, int startIndex, TokenType type, LexerProfile profile) {
        for(String match : strings) {
            if(str.startsWith(match, startIndex) && (str.length() == startIndex+match.length() || !(profile.canMerge(str.charAt(startIndex+match.length()-1), str.charAt(startIndex+match.length()))))) return new ScannerContextResponse(true, match, type);
        }
        return ScannerContextResponse.FAILED;
    } //substring done

    @Override
    public Collection<TokenType> getHandledTypes() {
        return Collections.singletonList(type);
    }

    public StringMatchLexerContext setOnlyWhenExpected(boolean onlyWhenExpected) {
        this.onlyWhenExpected = onlyWhenExpected;
        return this;
    }
}
