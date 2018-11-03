package com.energyxxer.trident.compiler.lexer;

import com.energyxxer.enxlex.lexical_analysis.profiles.LexerContext;
import com.energyxxer.enxlex.lexical_analysis.profiles.LexerProfile;
import com.energyxxer.enxlex.lexical_analysis.profiles.ScannerContextResponse;
import com.energyxxer.enxlex.lexical_analysis.token.Token;

public class TridentLexerProfile extends LexerProfile {

    public TridentLexerProfile() {
        contexts.add(new LexerContext() {
            @Override
            public ScannerContextResponse analyze(String str) {
                if(!str.startsWith("#")) return new ScannerContextResponse(false);
                if(str.startsWith("#:")) return new ScannerContextResponse(true, str.substring(0, 2), TridentTokens.DIRECTIVE_HEADER);
                if(str.contains("\n")) {
                    return new ScannerContextResponse(true, str.substring(0, str.indexOf("\n")), TridentTokens.COMMENT);
                } else return new ScannerContextResponse(true, str, TridentTokens.COMMENT);
            }

            @Override
            public ContextCondition getCondition() {
                return ContextCondition.LINE_START;
            }
        });
    }

    @Override
    public void putHeaderInfo(Token header) {
        header.attributes.put("TYPE","tdn");
        header.attributes.put("DESC","Trident Function File");
    }
}
