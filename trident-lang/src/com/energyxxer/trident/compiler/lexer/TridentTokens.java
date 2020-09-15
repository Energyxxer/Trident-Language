package com.energyxxer.trident.compiler.lexer;

import com.energyxxer.enxlex.lexical_analysis.token.TokenType;

import java.lang.reflect.Field;

public class TridentTokens {
    public static TokenType
            // #...
            COMMENT,
            // @
            DIRECTIVE_HEADER,
            // . , : -> = # !
            DOT, COMMA, COLON, ARROW, EQUALS, HASH, NOT,
            // ( ) { } [ ]
            BRACE,
            // "string literal"
            STRING_LITERAL,
            // true, false
            BOOLEAN,
            // ~ ^
            TILDE, CARET,
            // 0, 0.1, .1, 0.1f, 0.10000, 5t
            INTEGER_NUMBER, REAL_NUMBER, SHORT_REAL_NUMBER, TYPED_NUMBER, JSON_NUMBER, TIME,
            // 1-1-1-1-1
            UUID,
            // literally anything with or without spaces that doesn't begin with a $
            VERBATIM_COMMAND,
            // /
            VERBATIM_COMMAND_HEADER,
            // do, if, else, for, switch, case, throw, return, entity, item...
            KEYWORD,
            // null
            NULL,
            // * $
            SYMBOL,
            // += -= *= /= %=
            SCOREBOARD_OPERATOR,
            // + - * / %
            COMPILER_OPERATOR,
            // + - ++ -- ! ~
            COMPILER_PREFIX_OPERATOR,
            // ++ --
            COMPILER_POSTFIX_OPERATOR,
            // xyz
            SWIZZLE,
            // give, clear, say, setblock, fill, execute...
            COMMAND_HEADER,
            // as, at, align, positioned...
            MODIFIER_HEADER,
            // @p, @e, @a, @r, @s
            SELECTOR_HEADER,
            // literally anything
            TRAILING_STRING, SAY_STRING, WHITESPACE,
            // isset, update
            CUSTOM_COMMAND_KEYWORD,
            //Objective names, tags, etc.:
            IDENTIFIER_TYPE_A,
            //Player names (cannot begin with @ nor $) (very greedy string):
            IDENTIFIER_TYPE_B,
            //Player names in pointers (cannot begin with @ nor $, nor contain -<>) (very greedy string):
            IDENTIFIER_TYPE_B_LIMITED,
            //advancement criteria (very greedy string):
            IDENTIFIER_TYPE_C,
            //NBT path keys (same as B except doesn't allow dots, double quotes, square braces, curly braces nor angle braces):
            IDENTIFIER_TYPE_D,
            //Pretty much Java identifiers. There are no reserved keywords
            IDENTIFIER_TYPE_X,
            //primitive types
            PRIMITIVE_TYPE,
            //minecraft:trident/resource_locations
            RESOURCE_LOCATION,
            //Control tokens (zero-width):
            //Returns true if there is no whitespace ahead
            GLUE,
            //Returns true if the next non-whitespace character is in the same line as the previous
            LINE_GLUE,
            //Returns true, always. Use whenever post-processing requires the location of a token that may or may not exist
            EMPTY_TOKEN,
            //Returns false, always. Use whenever post-processing requires the location of a token that may or may not exist
            NO_TOKEN
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
