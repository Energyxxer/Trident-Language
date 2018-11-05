package com.energyxxer.trident.global.temp.lang_defaults.presets;

import com.energyxxer.enxlex.lexical_analysis.profiles.LexerContext;
import com.energyxxer.enxlex.lexical_analysis.profiles.ScannerContextResponse;
import com.energyxxer.enxlex.lexical_analysis.profiles.LexerProfile;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Created by User on 4/8/2017.
 */
public class PropertiesLexerProfile extends LexerProfile {

    private static final TokenType
            COMMENT = new TokenType("COMMENT",false),
            KEY = new TokenType("KEY"),
            SEPARATOR = new TokenType("SEPARATOR"),
            VALUE = new TokenType("VALUE");

    private TokenType stage = KEY;

    /**
     * Creates a JSON Analysis Profile.
     * */
    public PropertiesLexerProfile() {
        LexerContext propertyContext = new LexerContext() {
            @Override
            public ScannerContextResponse analyze(String str) {
                if(str.trim().length() <= 0) return new ScannerContextResponse(false);
                if(str.startsWith("\n")) {
                    stage = KEY;
                    return new ScannerContextResponse(false);
                }
                if(stage == KEY) {
                    if(str.trim().startsWith("#")) {
                        StringBuilder comment = new StringBuilder();
                        for(char ch : str.toCharArray()) {
                            if(ch == '\n') break;
                            comment.append(ch);
                        }
                        return new ScannerContextResponse(true, comment.toString(), COMMENT);
                    } else {
                        StringBuilder key = new StringBuilder();
                        for(char ch : str.toCharArray()) {
                            if(ch == '=') {
                                stage = SEPARATOR;
                                break;
                            } else if(ch == '\n') {
                                break;
                            } else key.append(ch);
                        }
                        return new ScannerContextResponse(true, key.toString(), KEY);
                    }
                } else if(stage == SEPARATOR) {
                    stage = VALUE;
                    return new ScannerContextResponse(true, "=", SEPARATOR);
                } else if(stage == VALUE) {
                    StringBuilder value = new StringBuilder();
                    for(char ch : str.toCharArray()) {
                        if(ch == '\n') {
                            break;
                        } else value.append(ch);
                    }
                    stage = KEY;
                    return new ScannerContextResponse(true, value.toString(), VALUE);
                }
                return null;
            }

            @Override
            public Collection<TokenType> getHandledTypes() {
                return Arrays.asList(KEY, SEPARATOR, VALUE, COMMENT);
            }
        };

        ArrayList<LexerContext> propertiesContexts = new ArrayList<>();
        propertiesContexts.add(propertyContext);
        this.contexts = propertiesContexts;
    }

    @Override
    public void putHeaderInfo(Token header) {
        header.attributes.put("TYPE","properties");
        header.attributes.put("DESC","Java Properties File");
    }
}
