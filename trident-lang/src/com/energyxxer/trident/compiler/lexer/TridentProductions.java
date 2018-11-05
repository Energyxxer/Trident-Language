package com.energyxxer.trident.compiler.lexer;

import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.LazyTokenGroupMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.LazyTokenItemMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.LazyTokenListMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.LazyTokenStructureMatch;

public class TridentProductions {

    public static final LazyTokenStructureMatch FILE;
    public static final LazyTokenStructureMatch ENTRY;

    public static final LazyTokenItemMatch COMMENT;
    public static final LazyTokenItemMatch VERBATIM_COMMAND;
    public static final LazyTokenGroupMatch DIRECTIVE;
    //public static final LazyTokenItemMatch INSTRUCTION;
    //public static final LazyTokenItemMatch COMMAND;


    static {
        FILE = new LazyTokenStructureMatch("FILE");
        ENTRY = new LazyTokenStructureMatch("ENTRY");

        COMMENT = new LazyTokenItemMatch(TridentTokens.COMMENT);
        VERBATIM_COMMAND = new LazyTokenItemMatch(TridentTokens.VERBATIM_COMMAND);

        DIRECTIVE = new LazyTokenGroupMatch();
        DIRECTIVE.setName("DIRECTIVE");
        {
            DIRECTIVE.append(new LazyTokenItemMatch(TridentTokens.DIRECTIVE_HEADER));
            DIRECTIVE.append(verbatimMatch("on"));
            DIRECTIVE.append(ofType(TridentTokens.DIRECTIVE_ON_KEYWORD));
        }

        ENTRY.add(COMMENT);
        ENTRY.add(DIRECTIVE);
        ENTRY.add(VERBATIM_COMMAND);

        {
            LazyTokenGroupMatch separator = new LazyTokenGroupMatch(true);
            separator.append(new LazyTokenListMatch(TokenType.NEWLINE, true));
            LazyTokenListMatch l = new LazyTokenListMatch(new LazyTokenGroupMatch(true).append(ENTRY), separator, true);
            FILE.add(l);
        }
    }

    private static LazyTokenItemMatch verbatimMatch(String text) {
        return new LazyTokenItemMatch(TokenType.UNKNOWN, text);
    }

    private static LazyTokenItemMatch ofType(TokenType type) {
        return new LazyTokenItemMatch(type);
    }

    private static LazyTokenStructureMatch struct(String name) {
        return new LazyTokenStructureMatch(name);
    }
}
