package com.energyxxer.trident.compiler.lexer;

import com.energyxxer.enxlex.lexical_analysis.profiles.*;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenSection;
import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.util.StringLocation;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.energyxxer.trident.compiler.lexer.TridentTokens.*;

public class TridentLexerProfile extends LexerProfile {

    public TridentLexerProfile() {

        contexts.add(new LexerContext() {

            private Pattern regex = Pattern.compile("([+-]?\\d+(\\.\\d+)?)([bdfsL]?)", Pattern.CASE_INSENSITIVE);

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

                    if(type == TYPED_NUMBER) obtainedType = type;
                    else if(type == REAL_NUMBER && obtainedType == INTEGER_NUMBER) obtainedType = REAL_NUMBER;

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

        contexts.add(new LexerContext() {

            private Pattern regex = Pattern.compile("(\\d+(\\.\\d+)?[tsd]?)");

            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                Matcher matcher = regex.matcher(str);

                if(matcher.lookingAt()) {
                    int length = matcher.end();
                    return new ScannerContextResponse(true, str.substring(0,length), TridentTokens.TIME);
                } else return new ScannerContextResponse(false);
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Collections.singletonList(TIME);
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
        contexts.add(new StringTypeMatchLexerContext(new String[] { ".", ",", ":", "=", "(", ")", "[", "]", "{", "}", "~", "^", "!", "#" },
                new TokenType[] { DOT, COMMA, COLON, EQUALS, BRACE, BRACE, BRACE, BRACE, BRACE, BRACE, TILDE, CARET, NOT, HASH }
                ));

        contexts.add(new StringMatchLexerContext(TridentTokens.SCOREBOARD_OPERATOR, "%=", "*=", "+=", "-=", "/=", "<", "=", ">", "><"));

        contexts.add(new LexerContext() {
            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                return new ScannerContextResponse(false);
            }

            @Override
            public ScannerContextResponse analyzeExpectingType(String str, TokenType type, LexerProfile profile) {
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
            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                return new ScannerContextResponse(false);
            }

            @Override
            public ScannerContextResponse analyzeExpectingType(String str, TokenType type, LexerProfile profile) {
                for(int i = 0; i < str.length(); i++) {
                    char c = str.charAt(i);
                    if(c == '\n') return new ScannerContextResponse(false);
                    if(!Character.isWhitespace(c)) return new ScannerContextResponse(true, "", TridentTokens.LINE_GLUE);
                }
                return new ScannerContextResponse(true, "", TridentTokens.LINE_GLUE);
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Collections.singletonList(LINE_GLUE);
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

        contexts.add(new LexerContext() {

            private String acceptedNamespaceChars = "[a-z0-9_\\.-]";
            private String acceptedPathChars = "[a-z0-9_/\\.-]";

            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                return new ScannerContextResponse(false);
            }

            @Override
            public ScannerContextResponse analyzeExpectingType(String str, TokenType type, LexerProfile profile) {
                boolean namespaceFound = false;
                int nonNamespaceCharIndex = -1;
                int index = 0;
                for(char c : str.toCharArray()) {
                    boolean validNs = (""+c).matches(acceptedNamespaceChars);
                    boolean validPt = (""+c).matches(acceptedPathChars);
                    if(!validNs && !validPt) {
                        if(!namespaceFound && c == ':') {
                            namespaceFound = true;
                            if(nonNamespaceCharIndex >= 0) break;
                        } else break;
                    } else {
                        if(!namespaceFound && !validNs) {
                            if(nonNamespaceCharIndex <= 0) nonNamespaceCharIndex = index;
                        } else if(namespaceFound && !validPt) {
                            break;
                        }
                    }
                    if(Character.isWhitespace(c)) break;
                    index++;
                }
                if(nonNamespaceCharIndex >= 0) index = nonNamespaceCharIndex;
                if(index == 0) return new ScannerContextResponse(false);
                else return new ScannerContextResponse(true, str.substring(0, index), RESOURCE_LOCATION);
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Collections.singletonList(RESOURCE_LOCATION);
            }
        });

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
                if(str.startsWith("#:") && type == DIRECTIVE_HEADER) {
                    return new ScannerContextResponse(true, str.substring(0, 2), DIRECTIVE_HEADER);
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
            @Override
            public ScannerContextResponse analyze(String str, LexerProfile profile) {
                return new ScannerContextResponse(false);
            }

            private String extractIdentifierA(String str) {
                int i = 0;
                while (i < str.length() && Character.toString(str.charAt(i)).matches("[a-zA-Z0-9._\\-+]")) {
                    i++;
                }
                return str.substring(0, i);
            }

            @Override
            public ScannerContextResponse analyzeExpectingType(String str, TokenType type, LexerProfile profile) {
                str = extractIdentifierA(str);
                if(str.length() == 0 || str.length() > 3) return new ScannerContextResponse(false);

                ArrayList<Character> possibleChars = new ArrayList<>();
                possibleChars.add('x');
                possibleChars.add('y');
                possibleChars.add('z');
                for(int i = 0; i < str.length(); i++) {
                    char c = str.charAt(i);
                    if(possibleChars.contains(c)) {
                        possibleChars.remove((Character)c);
                    } else {
                        return new ScannerContextResponse(false);
                    }
                }

                return new ScannerContextResponse(true, str, SWIZZLE);
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Collections.singletonList(SWIZZLE);
            }
        });

        contexts.add(new IdentifierLexerContext(IDENTIFIER_TYPE_A, "[a-zA-Z0-9._\\-+]"));
        contexts.add(new IdentifierLexerContext(IDENTIFIER_TYPE_B, "[^@\\s]\\S*"));
        contexts.add(new IdentifierLexerContext(IDENTIFIER_TYPE_C, "\\S*"));

        contexts.add(new StringMatchLexerContext(DIRECTIVE_ON_KEYWORD, "compile"));
        contexts.add(new StringMatchLexerContext(KEYWORD, "register", "mark", "do", "while", "within", "using"));
        contexts.add(new StringMatchLexerContext(SYNTACTIC_SUGAR, "isset"));
        contexts.add(new StringMatchLexerContext(BOOLEAN, "true", "false"));
        contexts.add(new StringMatchLexerContext(COMMAND_HEADER, "advancement,bossbar,clear,clone,data,defaultgamemode,difficulty,drop,effect,enchant,experience,execute,fill,function,gamemode,gamerule,give,help,kill,list,locate,me,msg,particle,playsound,recipe,replaceitem,say,scoreboard,seed,schedule,setblock,setworldspawn,spawnpoint,spreadplayers,stopsound,summon,tag,team,teleport,tell,tellraw,time,title,tp,trigger,w,weather,whitelist,worldborder,xp".split(",")));
        contexts.add(new StringMatchLexerContext(MODIFIER_HEADER, "align,anchored,as,at,facing,if,in,positioned,rotated,store,unless".split(",")));

        contexts.add(new StringMatchLexerContext(SYMBOL, "*", "<=", ">=", "<", ">", "!=", "="));

        contexts.add(new StringMatchLexerContext(VARIABLE_MARKER_START, "${"));
        contexts.add(new StringMatchLexerContext(VARIABLE_MARKER_END, "}"));

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
