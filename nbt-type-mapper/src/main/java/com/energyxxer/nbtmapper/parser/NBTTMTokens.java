package com.energyxxer.nbtmapper.parser;

import com.energyxxer.enxlex.lexical_analysis.token.TokenType;

public class NBTTMTokens {
    public static final TokenType BRACE = new TokenType("BRACE"); // {} [] ()
    public static final TokenType COLON = new TokenType("COLON"); // :
    public static final TokenType COMMA = new TokenType("COMMA"); // ,
    public static final TokenType HASH = new TokenType("HASH"); // #
    public static final TokenType WILDCARD = new TokenType("WILDCARD"); // *
    public static final TokenType KEY = new TokenType("KEY"); // Air, FallDistance...
    public static final TokenType STRING_LITERAL = new TokenType("STRING_LITERAL"); // "minecraft:stone" (for things like known recipes)
    public static final TokenType REFERENCE = new TokenType("REFERENCE"); // $ENTITY, $BLOCK_ENTITY
    public static final TokenType PRIMITIVE_TYPE = new TokenType("PRIMITIVE_TYPE"); // Byte, Short, String, Int...
    public static final TokenType IDENTIFIER = new TokenType("IDENTIFIER"); // boolean, text_component, type
    public static final TokenType DEFINITION_CATEGORY = new TokenType("DEFINITION_CATEGORY"); // entity, item, particle, dimension...

    public static final TokenType COMMENT = new TokenType("COMMENT", false); // # player:

}