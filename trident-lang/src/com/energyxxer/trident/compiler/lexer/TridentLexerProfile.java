package com.energyxxer.trident.compiler.lexer;

import com.energyxxer.enxlex.lexical_analysis.profiles.*;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenSection;
import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.util.StringLocation;
import com.energyxxer.util.logger.Debug;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.energyxxer.trident.compiler.lexer.TridentTokens.*;

public class TridentLexerProfile extends LexerProfile {

    public TridentLexerProfile() {
        contexts.add(new LexerContext() {
            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                if(!str.startsWith("#")) return new ScannerContextResponse(false);
                if(str.startsWith("#:")) return new ScannerContextResponse(true, str.substring(0, 2), DIRECTIVE_HEADER);
                if(str.contains("\n")) {
                    return new ScannerContextResponse(true, str.substring(0, str.indexOf("\n")), COMMENT);
                } else return new ScannerContextResponse(true, str, COMMENT);
            }

            @Override
            public ScannerContextResponse analyzeExpectingType(String str, TokenType type, LexerProfile profile) {
                if(!str.startsWith("#")) return new ScannerContextResponse(false);
                if(str.startsWith("#:")) {
                    if(type == DIRECTIVE_HEADER) {
                        return new ScannerContextResponse(true, str.substring(0, 2), DIRECTIVE_HEADER);
                    } else {
                        return new ScannerContextResponse(false);
                    }
                }
                if(str.contains("\n")) {
                    return new ScannerContextResponse(true, str.substring(0, str.indexOf("\n")), COMMENT);
                } else return new ScannerContextResponse(true, str, COMMENT);
            }

            @Override
            public ContextCondition getCondition() {
                return ContextCondition.LINE_START;
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Arrays.asList(COMMENT, DIRECTIVE_HEADER);
            }
        });

        contexts.add(new LexerContext() {

            private Pattern regex = Pattern.compile("([+-]?\\d+(\\.\\d+)?[bdfsL]?)");

            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                Matcher matcher = regex.matcher(str);

                if(matcher.lookingAt()) {
                    int length = matcher.end();
                    return new ScannerContextResponse(true, str.substring(0,length), (Character.isLetter(str.charAt(length-1)) ? TridentTokens.TYPED_NUMBER : ((str.substring(0, length).contains(".")) ? TridentTokens.REAL_NUMBER : TridentTokens.INTEGER_NUMBER)));
                } else return new ScannerContextResponse(false);
            }

            @Override
            public ScannerContextResponse analyzeExpectingType(String str, TokenType type, LexerProfile profile) {
                Matcher matcher = regex.matcher(str);

                if(matcher.lookingAt()) {
                    int length = matcher.end();

                    TokenType obtainedType = Character.isLetter(str.charAt(length-1)) ? TridentTokens.TYPED_NUMBER : ((str.substring(0, length).contains(".")) ? TridentTokens.REAL_NUMBER : TridentTokens.INTEGER_NUMBER);

                    if(type == REAL_NUMBER && obtainedType == INTEGER_NUMBER) obtainedType = REAL_NUMBER;

                    if(type == obtainedType) {
                        return new ScannerContextResponse(true, str.substring(0,length), type);
                    } else return new ScannerContextResponse(false);
                } else return new ScannerContextResponse(false);
            }

            @Override
            public ContextCondition getCondition() {
                return ContextCondition.LEADING_WHITESPACE;
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Arrays.asList(TridentTokens.TYPED_NUMBER, TridentTokens.INTEGER_NUMBER, TridentTokens.REAL_NUMBER);
            }
        });

        /*contexts.add(new LexerContext() {

            String[] patterns = { ".", ",", ":", "(", ")", "[", "]", "{", "}", "~", "^" };
            TokenType[] types = { TridentTokens.DOT, TridentTokens.COMMA, TridentTokens.COLON, TridentTokens.BRACE, TridentTokens.BRACE, TridentTokens.BRACE, TridentTokens.BRACE, TridentTokens.BRACE, TridentTokens.BRACE, TridentTokens.TILDE, TridentTokens.CARET };

            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
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
        });*/
        contexts.add(new StringTypeMatchLexerContext(new String[] { ".", ",", ":", "=", "(", ")", "[", "]", "{", "}", "~", "^", "!" },
                new TokenType[] { TridentTokens.DOT, TridentTokens.COMMA, TridentTokens.COLON, TridentTokens.EQUALS, TridentTokens.BRACE, TridentTokens.BRACE, TridentTokens.BRACE, TridentTokens.BRACE, TridentTokens.BRACE, TridentTokens.BRACE, TridentTokens.TILDE, TridentTokens.CARET, TridentTokens.NOT }
                ));

        contexts.add(new LexerContext() {
            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                return new ScannerContextResponse(false);
            }

            @Override
            public ScannerContextResponse analyzeExpectingType(String str, TokenType type, LexerProfile profile) {
                Debug.log("Checking for glue in '" + str.substring(0, 5) + "'");
                if(str.length() > 0 && Character.isWhitespace(str.charAt(0))) return new ScannerContextResponse(false);
                return new ScannerContextResponse(true, "", TridentTokens.GLUE);
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Collections.singletonList(GLUE);
            }

            @Override
            public boolean ignoreLeadingWhitespace() {
                return false;
            }
        });

        contexts.add(new LexerContext() {

            String header = "/";

            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                if(str.length() <= 0) return new ScannerContextResponse(false);
                if(!str.startsWith(header)) return new ScannerContextResponse(false);

                if(str.contains("\n")) {
                    return new ScannerContextResponse(true, str.substring(0, str.indexOf("\n")), VERBATIM_COMMAND);
                } else return new ScannerContextResponse(true, str, VERBATIM_COMMAND);
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Collections.singletonList(VERBATIM_COMMAND);
            }
        });

        contexts.add(new LexerContext() {

            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                return new ScannerContextResponse(false );
            }

            @Override
            public ScannerContextResponse analyzeExpectingType(String str, TokenType type, LexerProfile profile) {
                if(str.contains("\n")) {
                    return new ScannerContextResponse(true, str.substring(0, str.indexOf("\n")), TRAILING_STRING);
                } else return new ScannerContextResponse(true, str, TRAILING_STRING);
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Collections.singletonList(TRAILING_STRING);
            }
        });

        contexts.add(new LexerContext() {

            String delimiters = "\"";

            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
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
                            ScannerContextResponse response = new ScannerContextResponse(true, token.toString(), end, TridentTokens.STRING_LITERAL, escapedChars);
                            response.setError("Illegal line end in string literal", i, 1);
                            return response;
                        }
                        token.append(c);
                        if(c == '\\') {
                            token.append(str.charAt(i+1));
                            escapedChars.put(new TokenSection(i,2), "string_literal.escape");
                            i++;
                        } else if(c == startingCharacter) {
                            return new ScannerContextResponse(true, token.toString(), end, TridentTokens.STRING_LITERAL, escapedChars);
                        }
                    }
                    //Unexpected end of input
                    ScannerContextResponse response = new ScannerContextResponse(true, token.toString(), end, TridentTokens.STRING_LITERAL, escapedChars);
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

            private String headers = "pears";

            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
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

        contexts.add(new StringMatchLexerContext(DIRECTIVE_ON_KEYWORD, "compile", "load", "tick"));
        contexts.add(new StringMatchLexerContext(KEYWORD, "register", "mark", "do", "while", "within", "using"));
        contexts.add(new StringMatchLexerContext(BOOLEAN, "true", "false"));
        contexts.add(new StringMatchLexerContext(COMMAND_HEADER, "advancement,bossbar,clear,clone,data,defaultgamemode,difficulty,effect,enchant,experience,execute,fill,function,gamemode,gamerule,give,help,kill,list,locate,me,particle,playsound,recipe,replaceitem,say,scoreboard,seed,setblock,setworldspawn,spawnpoint,spreadplayers,stopsound,summon,tag,team,teleport,tell,tellraw,time,title,tp,trigger,weather,whitelist,worldborder".split(",")));

        contexts.add(new StringMatchLexerContext(SORTING, "nearest", "farthest", "arbitrary", "random"));
        contexts.add(new StringMatchLexerContext(NUMERIC_DATA_TYPE, "byte", "double", "float", "int", "long", "short"));
        contexts.add(new StringMatchLexerContext(SOUND_CHANNEL, "ambient", "block", "hostile", "master", "music", "neutral", "player", "record", "voice", "weather"));
        contexts.add(new StringMatchLexerContext(ANCHOR, "feet", "eyes"));

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
        header.attributes.put("TYPE","tdn");
        header.attributes.put("DESC","Trident Function File");
    }
}
