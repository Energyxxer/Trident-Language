package com.energyxxer.trident.compiler.lexer;

import com.energyxxer.enxlex.lexical_analysis.profiles.LexerContext;
import com.energyxxer.enxlex.lexical_analysis.profiles.LexerProfile;
import com.energyxxer.enxlex.lexical_analysis.profiles.ScannerContextResponse;
import com.energyxxer.enxlex.lexical_analysis.profiles.StringMatchLexerContext;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenType;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.energyxxer.trident.compiler.lexer.TridentTokens.*;

public class TridentLexerProfile extends LexerProfile {

    public TridentLexerProfile() {
        contexts.add(new LexerContext() {
            @Override
            public ScannerContextResponse analyze(String str) {
                if(!str.startsWith("#")) return new ScannerContextResponse(false);
                if(str.startsWith("#:")) return new ScannerContextResponse(true, str.substring(0, 2), DIRECTIVE_HEADER);
                if(str.contains("\n")) {
                    return new ScannerContextResponse(true, str.substring(0, str.indexOf("\n")), COMMENT);
                } else return new ScannerContextResponse(true, str, COMMENT);
            }

            @Override
            public ScannerContextResponse analyzeExpectingType(String str, TokenType type) {
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
            public ScannerContextResponse analyze(String str) {
                Matcher matcher = regex.matcher(str);

                if(matcher.lookingAt()) {
                    int length = matcher.end();
                    return new ScannerContextResponse(true, str.substring(0,length), (Character.isLetter(str.charAt(length-1)) ? TridentTokens.TYPED_NUMBER : ((str.substring(0, length).contains(".")) ? TridentTokens.REAL_NUMBER : TridentTokens.INTEGER_NUMBER)));
                } else return new ScannerContextResponse(false);
            }

            @Override
            public ScannerContextResponse analyzeExpectingType(String str, TokenType type) {
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

        contexts.add(new LexerContext() {

            String[] patterns = { ".", ",", ":", "=", "(", ")", "[", "]", "{", "}", "~", "^" };
            TokenType[] types = { TridentTokens.DOT, TridentTokens.COMMA, TridentTokens.COLON, TridentTokens.BRACE, TridentTokens.BRACE, TridentTokens.BRACE, TridentTokens.BRACE, TridentTokens.BRACE, TridentTokens.BRACE, TridentTokens.TILDE, TridentTokens.CARET };

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

            String header = "/";

            @Override
            public ScannerContextResponse analyze(String str) {
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

        contexts.add(new StringMatchLexerContext(DIRECTIVE_ON_KEYWORD, "compile", "load", "tick"));
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
