package com.energyxxer.trident.global.temp.lang_defaults.presets;

import com.energyxxer.trident.global.temp.lang_defaults.presets.mcfunction.MCFunction;
import com.energyxxer.enxlex.lexical_analysis.profiles.LexerContext;
import com.energyxxer.enxlex.lexical_analysis.profiles.ScannerContextResponse;
import com.energyxxer.enxlex.lexical_analysis.profiles.LexerProfile;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenSection;
import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.util.StringLocation;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.energyxxer.trident.global.temp.lang_defaults.presets.mcfunction.MCFunction.*;

public class MCFunctionLexerProfile extends LexerProfile {

    public MCFunctionLexerProfile() {

        ArrayList<LexerContext> contexts = new ArrayList<>();

        contexts.add(new LexerContext() {

            private String headers = "pears";

            @Override
            public ScannerContextResponse analyze(String str) {
                if(str.length() < 2) return new ScannerContextResponse(false);
                if(!str.startsWith("@")) return new ScannerContextResponse(false);
                if(headers.contains(str.charAt(1) + "")) {
                    return new ScannerContextResponse(true, str.substring(0,2), SELECTOR_HEADER);
                }
                return new ScannerContextResponse(false);
            }

            @Override
            public ContextCondition getCondition() {
                return ContextCondition.LEADING_WHITESPACE;
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Collections.singletonList(SELECTOR_HEADER);
            }
        });
        
        contexts.add(new LexerContext() {

            String delimiters = "\"";

            @Override
            public ScannerContextResponse analyze(String str) {
                if(str.length() <= 0) return new ScannerContextResponse(false);
                char startingCharacter = str.charAt(0);

                if(delimiters.contains(Character.toString(startingCharacter))) {

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
                            ScannerContextResponse response = new ScannerContextResponse(true, token.toString(), end, MCFunction.STRING_LITERAL, escapedChars);
                            response.setError("Illegal line end in string literal", i, 1);
                            return response;
                        }
                        token.append(c);
                        if(c == '\\') {
                            token.append(str.charAt(i+1));
                            escapedChars.put(new TokenSection(i,2), "string_literal.escape");
                            i++;
                        } else if(c == startingCharacter) {
                            return new ScannerContextResponse(true, token.toString(), end, MCFunction.STRING_LITERAL, escapedChars);
                        }
                    }
                    //Unexpected end of input
                    ScannerContextResponse response = new ScannerContextResponse(true, token.toString(), end, MCFunction.STRING_LITERAL, escapedChars);
                    response.setError("Unexpected end of input", str.length()-1, 1);
                    return response;
                } else return new ScannerContextResponse(false);
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Collections.singletonList(STRING_LITERAL);
            }
        });

        contexts.add(new LexerContext() {
            @Override
            public ScannerContextResponse analyze(String str) {
                if(!str.startsWith("#")) return new ScannerContextResponse(false);
                if(str.contains("\n")) {
                    return new ScannerContextResponse(true, str.substring(0, str.indexOf("\n")), MCFunction.COMMENT);
                } else return new ScannerContextResponse(true, str, MCFunction.COMMENT);
            }

            @Override
            public ContextCondition getCondition() {
                return ContextCondition.LINE_START;
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Collections.singletonList(COMMENT);
            }
        });
        
        contexts.add(new LexerContext() {

            String[] patterns = { ".", ",", ":", "=", "(", ")", "[", "]", "{", "}", "~", "^" };
            TokenType[] types = { MCFunction.DOT, MCFunction.COMMA, MCFunction.COLON, MCFunction.EQUALS, MCFunction.BRACE, MCFunction.BRACE, MCFunction.BRACE, MCFunction.BRACE, MCFunction.BRACE, MCFunction.BRACE, MCFunction.TILDE, MCFunction.CARET };

            @Override
            public ScannerContextResponse analyze(String str) {
                if(str.length() <= 0) return new ScannerContextResponse(false);
                for(int i = 0; i < patterns.length; i++) {
                    if(str.startsWith(patterns[i])) {
                        return new ScannerContextResponse(true, patterns[i], types[i]);
                    }
                }
                return new ScannerContextResponse(false);
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Arrays.asList(types);
            }
        });
        
        contexts.add(new LexerContext() {

            private Pattern regex = Pattern.compile("([+-]?\\d+(\\.\\d+)?[bdfsL]?)");

            @Override
            public ScannerContextResponse analyze(String str) {
                Matcher matcher = regex.matcher(str);

                if(matcher.lookingAt()) {
                    int length = matcher.end();
                    return new ScannerContextResponse(true, str.substring(0,length), (Character.isLetter(str.charAt(length-1)) ? MCFunction.TYPED_NUMBER : ((str.substring(0, length).contains(".")) ? MCFunction.REAL_NUMBER : MCFunction.INTEGER_NUMBER)));
                } else return new ScannerContextResponse(false);
            }

            @Override
            public ContextCondition getCondition() {
                return ContextCondition.LEADING_WHITESPACE;
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Arrays.asList(TYPED_NUMBER, INTEGER_NUMBER, REAL_NUMBER);
            }
        });
        
        this.contexts = contexts;
    }

    @Override
    public boolean filter(Token token) {
        if(token.type == TokenType.UNKNOWN) {
            if(Character.isJavaIdentifierPart(token.value.charAt(0))) {
                if(token.value.equals(token.value.toLowerCase())) {
                    token.type = MCFunction.LOWERCASE_IDENTIFIER;
                } else {
                    token.type = MCFunction.MIXED_IDENTIFIER;
                }
            } else {
                token.type = MCFunction.SYMBOL;
            }
        }
        return false;
    }

    @Override
    public boolean canMerge(char ch0, char ch1) {
        return isValidIdentifierPart(ch0) && isValidIdentifierPart(ch1);
    }

    private boolean isValidIdentifierPart(char ch) {
        return ch != '$' && Character.isJavaIdentifierPart(ch);
    }

    @Override
    public boolean useNewlineTokens() {
        return true;
    }

    @Override
    public void putHeaderInfo(Token header) {
        header.attributes.put("TYPE","mcfunction");
        header.attributes.put("DESC","Minecraft Function File");
    }
}
