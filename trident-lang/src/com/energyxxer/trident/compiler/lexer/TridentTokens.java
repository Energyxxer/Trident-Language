package com.energyxxer.trident.compiler.lexer;

import com.energyxxer.enxlex.lexical_analysis.token.TokenType;

import java.lang.reflect.Field;

public class TridentTokens {
    public static TokenType
            COMMENT, DIRECTIVE_HEADER,
            DOT, COMMA, COLON, SEMICOLON,
            BRACE,
            NEWLINE,
            STRING_LITERAL,
            TILDE, CARET,
            INTEGER_NUMBER, REAL_NUMBER, TYPED_NUMBER,
            VERBATIM_COMMAND,
            SYMBOL
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
