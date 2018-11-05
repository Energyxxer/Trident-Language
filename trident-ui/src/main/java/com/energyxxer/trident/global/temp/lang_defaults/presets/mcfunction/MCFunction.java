package com.energyxxer.trident.global.temp.lang_defaults.presets.mcfunction;

import com.energyxxer.enxlex.lexical_analysis.token.TokenType;

public class MCFunction {

    public static final TokenType
            SELECTOR_HEADER = new TokenType("SELECTOR_HEADER"), // @a, @e, @r, @p, @s
            BRACE = new TokenType("BRACE"), // (, ), {, }...
            DOT = new TokenType("DOT"), // .
            COMMA = new TokenType("COMMA"), // {1[,] 2[,]...}
            COLON = new TokenType("COLON"), // case 8[:]
            EQUALS = new TokenType("EQUALS"), // =
            TILDE = new TokenType("TILDE"), // ~
            CARET = new TokenType("CARET"), // ^
            INTEGER_NUMBER = new TokenType("INTEGER_NUMBER"), // 1
            REAL_NUMBER = new TokenType("REAL_NUMBER"), // 0.1
            TYPED_NUMBER = new TokenType("TYPED_NUMBER"), // 0.1f
            STRING_LITERAL = new TokenType("STRING_LITERAL"), // "STRING LITERAL"
            //BOOLEAN = new TokenType("BOOLEAN"), // true, false
            COMMENT = new TokenType("COMMENT_S"),
            LOWERCASE_IDENTIFIER = new TokenType("LOWERCASE_IDENTIFIER"), // anything else
            MIXED_IDENTIFIER = new TokenType("MIXED_IDENTIFIER"), // Anything else
            SYMBOL = new TokenType("SYMBOL"); // Anything else

    public static final TokenType[] ALL_TYPES = {SELECTOR_HEADER, BRACE, DOT, COMMA, COLON, EQUALS, TILDE, CARET, INTEGER_NUMBER, REAL_NUMBER, TYPED_NUMBER, STRING_LITERAL, COMMENT, LOWERCASE_IDENTIFIER, MIXED_IDENTIFIER, SYMBOL};
}
