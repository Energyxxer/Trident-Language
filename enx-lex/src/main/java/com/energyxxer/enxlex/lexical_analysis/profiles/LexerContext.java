package com.energyxxer.enxlex.lexical_analysis.profiles;

import com.energyxxer.enxlex.lexical_analysis.token.TokenType;

import java.util.Collection;

/**
 * Defines sub-routines to analyze special-case tokens.
 */
public interface LexerContext {
    /**
     * Analyzes the given substring, starting at the
     * current position of the EagerLexer, and returns information about the analysis.
     *
     * @param str The substring to analyze.
     *
     * @param profile
     * @return A semanticContext response object containing information about the analysis.
     * */
    ScannerContextResponse analyze(String str, LexerProfile profile);

    default ScannerContextResponse analyzeExpectingType(String str, TokenType type, LexerProfile profile) {
        return analyze(str, profile);
    }

    default ContextCondition getCondition() {
        return ContextCondition.NONE;
    }

    Collection<TokenType> getHandledTypes();

    enum ContextCondition {
        NONE, LINE_START, LEADING_WHITESPACE
    }
}
