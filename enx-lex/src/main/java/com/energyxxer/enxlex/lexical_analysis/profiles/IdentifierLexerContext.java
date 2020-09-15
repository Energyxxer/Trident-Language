package com.energyxxer.enxlex.lexical_analysis.profiles;

import com.energyxxer.enxlex.lexical_analysis.token.TokenType;

import java.util.Collection;
import java.util.Collections;

public class IdentifierLexerContext implements LexerContext {
    private final TokenType type;
    private final String regex;
    private final String firstRegex;
    private boolean onlyWhenExpected = true;

    public IdentifierLexerContext(TokenType type, String regex) {
        this(type, regex, null);
    }

    public IdentifierLexerContext(TokenType type, String regex, String firstRegex) {
        this.type = type;
        this.regex = regex;
        this.firstRegex = firstRegex;
    }

    @Override
    public ScannerContextResponse analyze(String str, int startIndex, LexerProfile profile) {
        return !onlyWhenExpected ? analyzeExpectingType(str, startIndex, type, profile) : ScannerContextResponse.FAILED;
    }

    @Override
    public ScannerContextResponse analyzeExpectingType(String str, int startIndex, TokenType type, LexerProfile profile) {
        int i = startIndex;
        while(i < str.length() &&
                (
                        (
                                (i > startIndex || firstRegex == null) && Character.toString(str.charAt(i)).matches(regex)
                        )
                                ||
                        (i == startIndex && firstRegex != null && Character.toString(str.charAt(i)).matches(firstRegex)))) {
            i++;
        }
        if(i > startIndex) return new ScannerContextResponse(true, str.substring(startIndex, i), type);
        return ScannerContextResponse.FAILED;
    } //substring done

    @Override
    public Collection<TokenType> getHandledTypes() {
        return Collections.singletonList(type);
    }

    public IdentifierLexerContext setOnlyWhenExpected(boolean onlyWhenExpected) {
        this.onlyWhenExpected = onlyWhenExpected;
        return this;
    }
}
