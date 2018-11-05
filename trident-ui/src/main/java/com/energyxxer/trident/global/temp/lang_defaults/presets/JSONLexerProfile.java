package com.energyxxer.trident.global.temp.lang_defaults.presets;

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

/**
 * Created by User on 2/6/2017.
 */
public class JSONLexerProfile extends LexerProfile {

    /**
     * Holds the previous token for multi-token analysis.
     * */
    private Token tokenBuffer = null;

    private static final TokenType
            BRACE = new TokenType("BRACE"), // (, ), {, }...
            COMMA = new TokenType("COMMA"), // {1[,] 2[,]...}
            COLON = new TokenType("COLON"), // case 8[:]
            NUMBER = new TokenType("NUMBER"), // 0.1f
            STRING_LITERAL = new TokenType("STRING_LITERAL"), // "STRING LITERAL"
            BOOLEAN = new TokenType("BOOLEAN"); // true, false

    /**
     * Creates a JSON Analysis Profile.
     * */
    public JSONLexerProfile() {
        //String
        LexerContext stringContext = new LexerContext() {

            String delimiters = "\"'";

            @Override
            public ScannerContextResponse analyze(String str) {
                if (str.length() <= 0) return new ScannerContextResponse(false);
                char startingCharacter = str.charAt(0);

                if (delimiters.contains(Character.toString(startingCharacter))) {

                    StringBuilder token = new StringBuilder(Character.toString(startingCharacter));

                    HashMap<TokenSection, String> escapedChars = new HashMap<>();

                    for (int i = 1; i < str.length(); i++) {
                        char c = str.charAt(i);

                        if (c == '\n') {
                            ScannerContextResponse response = new ScannerContextResponse(true, token.toString(), new StringLocation(i, 0, i), STRING_LITERAL, escapedChars);
                            response.setError("Illegal line end in string literal", i, 1);
                            return response;
                        }
                        token.append(c);
                        if (c == '\\') {
                            token.append(str.charAt(i + 1));
                            escapedChars.put(new TokenSection(i, 2), "string_literal.escape");
                            i++;
                        } else if (c == startingCharacter) {
                            return new ScannerContextResponse(true, token.toString(), STRING_LITERAL, escapedChars);
                        }
                    }
                    //Unexpected end of input
                    ScannerContextResponse response = new ScannerContextResponse(true, token.toString(), new StringLocation(str.length(), 0, str.length()), STRING_LITERAL, escapedChars);
                    response.setError("Unexpected end of input", str.length() - 1, 1);
                    return response;
                } else return new ScannerContextResponse(false);
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Collections.singletonList(STRING_LITERAL);
            }
        };
        //Numbers
        LexerContext numberContext = new LexerContext() {

            private Pattern regex = Pattern.compile("(-?\\d+(\\.\\d+)?)");

            @Override
            public ScannerContextResponse analyze(String str) {
                Matcher matcher = regex.matcher(str);

                if (matcher.lookingAt()) {
                    int length = matcher.end();
                    return new ScannerContextResponse(true, str.substring(0, length), NUMBER);
                } else return new ScannerContextResponse(false);
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Collections.singletonList(NUMBER);
            }
        };
        //Braces
        LexerContext braceContext = new LexerContext() {
            @Override
            public ScannerContextResponse analyze(String str) {
                if (str.length() <= 0) return new ScannerContextResponse(false);
                if ("[]{}".contains(str.substring(0, 1))) {
                    return new ScannerContextResponse(true, str.substring(0, 1), BRACE);
                }
                return new ScannerContextResponse(false);
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Collections.singletonList(BRACE);
            }
        };

        //Misc
        LexerContext miscellaneousContext = new LexerContext() {

            String[] patterns = { ",", ":" };
            TokenType[] types = { COMMA, COLON };

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
        };

        ArrayList<LexerContext> jsonContexts = new ArrayList<>();
        jsonContexts.add(stringContext);
        jsonContexts.add(braceContext);
        jsonContexts.add(miscellaneousContext);
        jsonContexts.add(numberContext);
        this.contexts = jsonContexts;
    }

    @Override
    public boolean canMerge(char ch0, char ch1) {
        return Character.isJavaIdentifierPart(ch0) && Character.isJavaIdentifierPart(ch1);
    }

    @Override
    public boolean filter(Token token) {
        if(token.type == TokenType.UNKNOWN) {
            if(token.value.equals("true") || token.value.equals("false")) {
                token.type = BOOLEAN;
            }
        }
        if(token.type == STRING_LITERAL) {
            if(tokenBuffer != null) this.stream.write(tokenBuffer, true);
            tokenBuffer = token;
            return true;
        }
        if(token.type == COLON && tokenBuffer != null) {
            tokenBuffer.attributes.put("IS_PROPERTY",true);
            this.stream.write(tokenBuffer, true);
            tokenBuffer = null;
            return false;
        }
        if(tokenBuffer != null) {
            this.stream.write(tokenBuffer, true);
            tokenBuffer = null;
        }
        return false;
    }

    @Override
    public void putHeaderInfo(Token header) {
        header.attributes.put("TYPE","json");
        header.attributes.put("DESC","JavaScript Object Notation File");
    }
}
