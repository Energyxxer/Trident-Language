package com.energyxxer.trident.compiler.morelang;

import com.energyxxer.enxlex.lexical_analysis.token.TokenType;

import java.lang.reflect.Field;

public class MoreLangTokens {
    public static TokenType
            COMMENT,
            KEYWORD,
            IDENTIFIER,
            NUMBER,
            STRING_LITERAL,
            BRACE,
            DOT,
            COMMA,
            SEMICOLON,
            OPERATOR,
            ARROW,
            EQUALS;

    static {
        Field[] fields = MoreLangTokens.class.getFields();
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
