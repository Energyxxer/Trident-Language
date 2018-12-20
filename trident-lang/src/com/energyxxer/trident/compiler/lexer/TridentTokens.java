package com.energyxxer.trident.compiler.lexer;

import com.energyxxer.enxlex.lexical_analysis.token.TokenType;

import java.lang.reflect.Field;

public class TridentTokens {
    public static TokenType
            COMMENT, DIRECTIVE_HEADER, DIRECTIVE_ON_KEYWORD,
            DOT, COMMA, COLON, ARROW, EQUALS, HASH, NOT,
            BRACE,
            STRING_LITERAL, BOOLEAN,
            TILDE, CARET,
            INTEGER_NUMBER, REAL_NUMBER, SHORT_REAL_NUMBER, TYPED_NUMBER, TIME,
            VERBATIM_COMMAND,
            KEYWORD,
            SYMBOL, SCOREBOARD_OPERATOR,
            SWIZZLE,
            COMMAND_HEADER,
            MODIFIER_HEADER,
            SELECTOR_HEADER,
            TRAILING_STRING,
            SYNTACTIC_SUGAR,
            SORTING, NUMERIC_DATA_TYPE, SOUND_CHANNEL, ANCHOR,
            IDENTIFIER_TYPE_A, IDENTIFIER_TYPE_B, IDENTIFIER_TYPE_C, IDENTIFIER_TYPE_D, IDENTIFIER_TYPE_X, RESOURCE_LOCATION, CASE_INSENSITIVE_RESOURCE_LOCATION,
            GLUE, LINE_GLUE
    ;

    static {
        Field[] fields = TridentTokens.class.getFields();
        for(Field field : fields) {
            if(field.getType() == TokenType.class) {
                try {
                    field.set(null, new TokenType(field.getName()));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
