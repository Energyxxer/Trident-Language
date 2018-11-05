package com.energyxxer.enxlex.lexical_analysis.profiles;

import com.energyxxer.enxlex.lexical_analysis.token.TokenType;

import java.util.Arrays;
import java.util.Collection;

public class StringTypeMatchLexerContext implements LexerContext {
    private final String[] strings;
    private final TokenType[] types;

    public StringTypeMatchLexerContext(String[] strings, TokenType[] types) {
        this.strings = strings;
        this.types = types;
    }

    @Override
    public ScannerContextResponse analyze(String str) {
        for(int i = 0; i < strings.length; i++) {
            if(str.startsWith(strings[i])) return new ScannerContextResponse(true, strings[i], types[i]);
        }
        return new ScannerContextResponse(false);
    }

    @Override
    public ScannerContextResponse analyzeExpectingType(String str, TokenType type) {
        for(int i = 0; i < strings.length; i++) {
            if(type == types[i] && str.startsWith(strings[i])) {
                return new ScannerContextResponse(true, strings[i], types[i]);
            }
        }
        return new ScannerContextResponse(false);
    }

    @Override
    public Collection<TokenType> getHandledTypes() {
        return Arrays.asList(types);
    }
}
