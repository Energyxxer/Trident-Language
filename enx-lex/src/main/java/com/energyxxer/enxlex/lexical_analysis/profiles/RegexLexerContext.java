package com.energyxxer.enxlex.lexical_analysis.profiles;

import com.energyxxer.enxlex.lexical_analysis.token.TokenType;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexLexerContext implements LexerContext {
    private final Pattern pattern;
    private final TokenType handledType;
    private boolean parseEagerly = false;

    public RegexLexerContext(Pattern pattern, TokenType handledType, boolean parseEagerly) {
        this.pattern = pattern;
        this.handledType = handledType;
        this.parseEagerly = parseEagerly;
    }

    @Override
    public ScannerContextResponse analyze(String str, int startIndex, LexerProfile profile) {
        return parseEagerly ? analyzeExpectingType(str, startIndex, handledType, profile) : ScannerContextResponse.FAILED;
    }

    @Override
    public ScannerContextResponse analyzeExpectingType(String str, int startIndex, TokenType type, LexerProfile profile) {
        Matcher matcher = pattern.matcher(str).region(startIndex, str.length());

        if(matcher.lookingAt() && matcher.end() - matcher.start() > 0) {
            int length = matcher.end() - matcher.start();
            return new ScannerContextResponse(true, str.substring(startIndex,startIndex+length), handledType);
        } else return ScannerContextResponse.FAILED;
    }

    @Override
    public Collection<TokenType> getHandledTypes() {
        return Collections.singletonList(handledType);
    }
}
