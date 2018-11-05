package com.energyxxer.trident.compiler.lexer;

import com.energyxxer.enxlex.lexical_analysis.token.TokenType;

import java.lang.reflect.Field;

public class TridentTokens {;
    public static TokenType
            COMMENT, DIRECTIVE_HEADER, DIRECTIVE_ON_KEYWORD,
            DOT, COMMA, COLON, SEMICOLON, EQUALS, NOT,
            BRACE,
            NEWLINE,
            STRING_LITERAL, BOOLEAN,
            TILDE, CARET,
            INTEGER_NUMBER, REAL_NUMBER, TYPED_NUMBER,
            VERBATIM_COMMAND,
            SYMBOL,
            KEYWORD,
            COMMAND_HEADER,
            SELECTOR_HEADER,
            TRAILING_STRING,
            SORTING, NUMERIC_DATA_TYPE, SOUND_CHANNEL, ANCHOR,
            GLUE
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
