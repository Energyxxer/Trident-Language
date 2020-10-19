package com.energyxxer.trident.compiler.molang;

import com.energyxxer.enxlex.lexical_analysis.token.TokenType;

import java.lang.reflect.Field;

public class MoLangTokens {
    public static TokenType
            // #...
            COMMENT,
            // @
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
        Field[] fields = MoLangTokens.class.getFields();
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
