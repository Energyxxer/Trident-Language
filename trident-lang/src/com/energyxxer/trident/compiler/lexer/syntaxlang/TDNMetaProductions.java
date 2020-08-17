package com.energyxxer.trident.compiler.lexer.syntaxlang;

import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenGroupMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenItemMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenListMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenStructureMatch;

import static com.energyxxer.trident.compiler.lexer.syntaxlang.TDNMetaLexerProfile.*;

public class TDNMetaProductions {
    public static final TokenStructureMatch FILE;

    static {
        FILE = new TokenStructureMatch("FILE");

        TokenStructureMatch STATEMENT = new TokenStructureMatch("STATEMENT");

        TokenStructureMatch VALUE = new TokenStructureMatch("VALUE");
        TokenStructureMatch ROOT_VALUE = new TokenStructureMatch("ROOT_VALUE");

        ROOT_VALUE.add(ofType(IDENTIFIER).setName("ROOT_IDENTIFIER"));
        ROOT_VALUE.add(ofType(FUNCTION).setName("ROOT_FUNCTION"));
        ROOT_VALUE.add(ofType(STRING_LITERAL).setName("ROOT_STRING_LITERAL"));
        ROOT_VALUE.add(ofType(BOOLEAN).setName("ROOT_BOOLEAN"));

        TokenStructureMatch MEMBER_ACCESS = new TokenStructureMatch("MEMBER_ACCESS");
        MEMBER_ACCESS.add(group(ofType(DOT), ofType(FUNCTION).setName("FUNCTION_NAME")).setName("FUNCTION_MEMBER"));
        MEMBER_ACCESS.add(group(stringMatch(BRACE, "("), list(VALUE, new TokenItemMatch(COMMA)).setOptional().setName("ARGUMENT_LIST"), stringMatch(BRACE, ")")).setName("FUNCTION_CALL"));

        VALUE.add(group(ROOT_VALUE, list(MEMBER_ACCESS).setOptional().setName("MEMBER_ACCESS_LIST")).setName("MEMBER_ACCESS_VALUE"));

        STATEMENT.add(group(stringMatch(KEYWORD, "define"), ofType(IDENTIFIER).setName("DEFINITION_NAME"), ofType(EQUALS), VALUE, ofType(SEMICOLON)).setName("DEFINE_STATEMENT"));

        TokenGroupMatch RETURN_STATEMENT = group(stringMatch(KEYWORD, "return"), VALUE, ofType(SEMICOLON)).setName("RETURN_STATEMENT");

        FILE.add(group(list(STATEMENT).setOptional().setName("STATEMENT_LIST"), RETURN_STATEMENT, ofType(TokenType.END_OF_FILE)));
    }

    private static TokenGroupMatch group(TokenPatternMatch... patterns) {
        TokenGroupMatch g = new TokenGroupMatch();
        for(TokenPatternMatch p : patterns) {
            g.append(p);
        }
        return g;
    }

    private static TokenListMatch list(TokenPatternMatch main) {
        return list(main, null);
    }

    private static TokenListMatch list(TokenPatternMatch main, TokenPatternMatch separator) {
        return new TokenListMatch(main, separator);
    }

    private static TokenGroupMatch optional(TokenPatternMatch p) {
        return new TokenGroupMatch(true).append(p);
    }

    private static TokenItemMatch ofType(TokenType type) {
        return new TokenItemMatch(type);
    }

    public static TokenItemMatch stringMatch(TokenType type, String value) {
        return new TokenItemMatch(type, value);
    }
}
