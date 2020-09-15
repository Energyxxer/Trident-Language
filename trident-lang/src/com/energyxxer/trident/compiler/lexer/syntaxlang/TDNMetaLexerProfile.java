package com.energyxxer.trident.compiler.lexer.syntaxlang;

import com.energyxxer.enxlex.lexical_analysis.profiles.*;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenType;

import java.util.Arrays;
import java.util.List;

public class TDNMetaLexerProfile extends LexerProfile {

    private static final List<String> functionNames = Arrays.asList("ofType", "stringMatch", "literal", "group", "optional", "list", "choice", "name", "hint", "storeVar", "storeFlat", "noToken", "brace");
    private static final List<String> keywords = Arrays.asList("return", "define");

    public TDNMetaLexerProfile() {
        this.initialize();
    }

    public static final TokenType DOT = new TokenType("DOT");
    public static final TokenType COMMA = new TokenType("COMMA");
    public static final TokenType COLON = new TokenType("COLON");
    public static final TokenType SEMICOLON = new TokenType("SEMICOLON");
    public static final TokenType EQUALS = new TokenType("EQUALS");
    public static final TokenType BRACE = new TokenType("BRACE");
    public static final TokenType TILDE = new TokenType("TILDE");
    public static final TokenType CARET = new TokenType("CARET");
    public static final TokenType NOT = new TokenType("NOT");
    public static final TokenType HASH = new TokenType("HASH");
    public static final TokenType STRING_LITERAL = new TokenType("STRING_LITERAL");
    public static final TokenType BOOLEAN = new TokenType("BOOLEAN");
    public static final TokenType IDENTIFIER = new TokenType("IDENTIFIER");
    public static final TokenType FUNCTION = new TokenType("FUNCTION");
    public static final TokenType KEYWORD = new TokenType("KEYWORD");
    public static final TokenType COMMENT = new TokenType("COMMENT", false);

    private void initialize() {

        contexts.add(new StringTypeMatchLexerContext(new String[] { ".", ",", ":", ";", "=", "(", ")", "[", "]", "{", "}", "<", ">", "~", "^", "!", "#", "true", "false" },
                new TokenType[] { DOT, COMMA, COLON, SEMICOLON, EQUALS, BRACE, BRACE, BRACE, BRACE, BRACE, BRACE, BRACE, BRACE, TILDE, CARET, NOT, HASH, BOOLEAN, BOOLEAN }
        ));

        //String literals
        contexts.add(new StringLiteralLexerContext("\"'", STRING_LITERAL));

        //Comments
        contexts.add(new CommentLexerContext("//", COMMENT));

        contexts.add(new IdentifierLexerContext(IDENTIFIER, "[a-zA-Z0-9_]", "[a-zA-Z_]").setOnlyWhenExpected(false));
    }

    @Override
    public void putHeaderInfo(Token header) {
        header.attributes.put("TYPE","tdnmeta");
        header.attributes.put("DESC","Trident Meta Syntax File");
    }

    @Override
    public boolean filter(Token token) {
        if(token.type == IDENTIFIER) {
            if(functionNames.contains(token.value)) token.type = FUNCTION;
            else if(keywords.contains(token.value)) token.type = KEYWORD;
        }
        return false;
    }
}
