package com.energyxxer.enxlex.lexical_analysis.profiles;

import com.energyxxer.enxlex.lexical_analysis.token.TokenSection;
import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.util.StringLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

public class StringLiteralLexerContext implements LexerContext {
    private final String delimiters;
    private final TokenType handledType;

    public StringLiteralLexerContext(String delimiters, TokenType handledType) {
        this.delimiters = delimiters;
        this.handledType = handledType;
    }

    @Override
    public ScannerContextResponse analyze(String str, int startIndex, LexerProfile profile) {
        if(str.length() - startIndex <= 0) return ScannerContextResponse.FAILED;
        char startingCharacter = str.charAt(startIndex);

        if(delimiters.contains(Character.toString(startingCharacter))) {

            String errorMessage = null;

            StringBuilder token = new StringBuilder(Character.toString(startingCharacter));
            StringLocation end = new StringLocation(1,0,1);

            HashMap<TokenSection, String> escapedChars = new HashMap<>();

            for(int i = startIndex+1; i < str.length(); i++) {
                char c = str.charAt(i);

                if(c == '\n') {
                    end.line++;
                    end.column = 0;
                } else {
                    end.column++;
                }
                end.index++;

                if(c == '\n') {
                    ScannerContextResponse response = new ScannerContextResponse(true, token.toString(), end, handledType, escapedChars);
                    response.setError("Illegal line end in string literal", i-startIndex, 1);
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
                                escapedChars.put(new TokenSection(i - startIndex,6), "string_literal.escape");
                            }
                        } else {
                            escapedChars.put(new TokenSection(i - startIndex,2), "string_literal.escape");
                        }
                    }
                    token.append(escapedChar);
                    i++;
                } else if(c == startingCharacter) {
                    ScannerContextResponse response = new ScannerContextResponse(true, token.toString(), end, handledType, escapedChars);
                    if(errorMessage != null) {
                        response.setError(errorMessage, 0, i - startIndex + 1);
                    }
                    return response;
                }
            }
            //Unexpected end of input
            ScannerContextResponse response = new ScannerContextResponse(true, token.toString(), end, handledType, escapedChars);
            response.setError("Unexpected end of input", str.length() - startIndex-1, 1);
            return response;
        } else return ScannerContextResponse.FAILED;
    }

    @Override
    public Collection<TokenType> getHandledTypes() {
        return Collections.singletonList(handledType);
    }
}
