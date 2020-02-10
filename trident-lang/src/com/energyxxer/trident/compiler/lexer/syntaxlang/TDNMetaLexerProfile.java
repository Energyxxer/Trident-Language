package com.energyxxer.trident.compiler.lexer.syntaxlang;

import com.energyxxer.enxlex.lexical_analysis.profiles.LexerContext;
import com.energyxxer.enxlex.lexical_analysis.profiles.LexerProfile;
import com.energyxxer.enxlex.lexical_analysis.profiles.ScannerContextResponse;
import com.energyxxer.enxlex.lexical_analysis.profiles.StringTypeMatchLexerContext;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenSection;
import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.util.StringLocation;

import java.util.*;

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
        contexts.add(new LexerContext() {

            String delimiters = "\"'";

            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                if(str.length() <= 0) return new ScannerContextResponse(false);
                char startingCharacter = str.charAt(0);

                if(delimiters.contains(Character.toString(startingCharacter))) {

                    String errorMessage = null;

                    StringBuilder token = new StringBuilder(Character.toString(startingCharacter));
                    StringLocation end = new StringLocation(1,0,1);

                    HashMap<TokenSection, String> escapedChars = new HashMap<>();

                    for(int i = 1; i < str.length(); i++) {
                        char c = str.charAt(i);

                        if(c == '\n') {
                            end.line++;
                            end.column = 0;
                        } else {
                            end.column++;
                        }
                        end.index++;

                        if(c == '\n') {
                            ScannerContextResponse response = new ScannerContextResponse(true, token.toString(), end, STRING_LITERAL, escapedChars);
                            response.setError("Illegal line end in string literal", i, 1);
                            return response;
                        }
                        token.append(c);
                        if(c == '\\') {
                            if(i >= str.length()-1) {
                                break; //Unexpected end of input
                            }
                            char escapedChar = str.charAt(i+1);
                            if(!"bfnrtu\\\"'".contains(escapedChar+"")) {
                                errorMessage = "Illegal escape character in string literal";
                            } else {
                                if(escapedChar == 'u') {
                                    if(str.length() - i+2 < 4 || !str.substring(i+2, i+2+4).matches("[0-9A-Fa-f]{4}")) {
                                        errorMessage = "Illegal escape character in string literal";
                                    } else {
                                        escapedChars.put(new TokenSection(i,6), "string_literal.escape");
                                    }
                                } else {
                                    escapedChars.put(new TokenSection(i,2), "string_literal.escape");
                                }
                            }
                            token.append(escapedChar);
                            i++;
                        } else if(c == startingCharacter) {
                            ScannerContextResponse response = new ScannerContextResponse(true, token.toString(), end, STRING_LITERAL, escapedChars);
                            if(errorMessage != null) {
                                response.setError(errorMessage, 0, i+1);
                            }
                            return response;
                        }
                    }
                    //Unexpected end of input
                    ScannerContextResponse response = new ScannerContextResponse(true, token.toString(), end, STRING_LITERAL, escapedChars);
                    response.setError("Unexpected end of input", str.length()-1, 1);
                    return response;
                } else return new ScannerContextResponse(false);
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Collections.singletonList(STRING_LITERAL);
            }
        });

        //Comments
        contexts.add(new LexerContext() {
            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                if(!str.startsWith("//")) return new ScannerContextResponse(false);
                if(str.contains("\n")) {
                    return handleComment(str.substring(0, str.indexOf("\n")));
                } else return handleComment(str);
            }

            private ScannerContextResponse handleComment(String str) {
                HashMap<TokenSection, String> sections = new HashMap<>();
                int todoIndex = str.toUpperCase(Locale.ENGLISH).indexOf("TODO");
                if(todoIndex >= 0) {
                    int todoEnd = str.indexOf("\n");
                    if(todoEnd < 0) todoEnd = str.length();
                    sections.put(new TokenSection(todoIndex, todoEnd-todoIndex), "comment.todo");
                }
                return new ScannerContextResponse(true, str, COMMENT, sections);
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Collections.singletonList(COMMENT);
            }
        });

        contexts.add(new LexerContext() {

            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                int i = 0;
                while(i < str.length() && (
                        (i == 0 && str.substring(i,i+1).matches("[a-zA-Z_]")
                                ||
                                (i > 0 && str.substring(i,i+1).matches("[a-zA-Z0-9_]"))
                        ))) {
                    i++;
                }
                str = str.substring(0, i);
                if(i > 0) {
                    TokenType type = IDENTIFIER;
                    if(functionNames.contains(str)) type = FUNCTION;
                    else if(keywords.contains(str)) type = KEYWORD;
                    return new ScannerContextResponse(true, str, type);
                }
                return new ScannerContextResponse(false);
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Arrays.asList(IDENTIFIER, FUNCTION, KEYWORD);
            }
        });
    }

    @Override
    public void putHeaderInfo(Token header) {
        header.attributes.put("TYPE","tdnmeta");
        header.attributes.put("DESC","Trident Meta Syntax File");
    }
}
