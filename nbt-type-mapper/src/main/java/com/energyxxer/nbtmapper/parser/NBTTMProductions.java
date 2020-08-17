package com.energyxxer.nbtmapper.parser;

import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenGroupMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenItemMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenListMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenStructureMatch;

import static com.energyxxer.nbtmapper.parser.NBTTMTokens.*;

public class NBTTMProductions {
    public static final TokenPatternMatch FILE;
    public static final TokenPatternMatch ENTRY;
    private static final TokenStructureMatch TYPE;
    
    static {

        TYPE = struct("TYPE");

        TokenStructureMatch KEY = choice(ofType(NBTTMTokens.KEY), ofType(STRING_LITERAL), ofType(WILDCARD)).setName("KEY");

        TokenStructureMatch FLAG = struct("FLAG");
        FLAG.add(ofType(IDENTIFIER)); //boolean, text_component, resource_location...
        FLAG.add(group(matchItem(IDENTIFIER, "type"), brace("("), ofType(HASH).setOptional().setName("IS_TAG"), ofType(IDENTIFIER).setName("DEFINITION_CATEGORY"), brace(")")).setName("TYPE_FLAG"));
        FLAG.add(group(matchItem(IDENTIFIER, "one_of"), brace("("), list(ofType(STRING_LITERAL).setName("OPTION"), comma()).setName("OPTION_LIST"), brace(")")).setName("ONE_OF_FLAG"));

        TokenPatternMatch FLAGS = group(brace("("), list(FLAG, comma()).setName("FLAG_LIST"), brace(")")).setOptional().setName("FLAGS");

        TokenGroupMatch COMPOUND = group(
                brace("{"),
                list(
                        group(KEY, colon(), TYPE).setName("COMPOUND_INNER"),
                        comma()
                ).setOptional().setName("COMPOUND_INNER_LIST"),
                brace("}"), FLAGS
        ).setName("COMPOUND");

        TokenGroupMatch LIST = group(
                brace("["),
                TYPE,
                brace("]"), FLAGS
        ).setName("LIST");

        TokenGroupMatch ARRAY = group(
                brace("["),
                ofType(ARRAY_TYPE).setName("ARRAY_TYPE"),
                ofType(SEMICOLON),
                brace("]"), FLAGS
        ).setName("ARRAY");

        TYPE.add(group(ofType(PRIMITIVE_TYPE).setName("PRIMITIVE_NAME"), FLAGS).setName("PRIMITIVE"));
        TYPE.add(COMPOUND);
        TYPE.add(LIST);
        TYPE.add(ARRAY);
        TokenGroupMatch REFERENCE_S = group(ofType(REFERENCE).setName("REFERENCE_NAME"), FLAGS).setName("REFERENCE");
        TYPE.add(REFERENCE_S);

        ENTRY = choice(
                group(ofType(REFERENCE).setName("TYPE_NAME"), colon(), TYPE).setName("ROOT_TYPE"),
                ofType(COMMENT).setName("COMMENT")
        ).setName("ENTRY");

        FILE = group(
                list(ENTRY).setOptional().setName("ENTRIES"),
                ofType(TokenType.END_OF_FILE)
        );
    }

    static TokenItemMatch matchItem(TokenType type, String text) {
        return new TokenItemMatch(type, text).setName("ITEM_MATCH");
    }

    static TokenItemMatch brace(String brace) {
        return matchItem(BRACE, brace);
    }

    static TokenItemMatch colon() {
        return ofType(COLON);
    }

    static TokenItemMatch comma() {
        return ofType(COMMA).setName("COMMA");
    }
    
    static TokenItemMatch ofType(TokenType type) {
        return new TokenItemMatch(type);
    }

    static TokenStructureMatch struct(String name) {
        return new TokenStructureMatch(name);
    }

    static TokenStructureMatch choice(TokenPatternMatch... options) {
        if(options.length == 0) throw new IllegalArgumentException("Need one or more options for choice");
        TokenStructureMatch s = struct("CHOICE");
        for(TokenPatternMatch option : options) {
            s.add(option);
        }
        return s;
    }

    static TokenGroupMatch optional() {
        return new TokenGroupMatch(true);
    }

    static TokenGroupMatch group(TokenPatternMatch... items) {
        TokenGroupMatch g = new TokenGroupMatch();
        for(TokenPatternMatch item : items) {
            g.append(item);
        }
        return g;
    }

    static TokenListMatch list(TokenPatternMatch pattern) {
        return list(pattern, null);
    }

    static TokenListMatch list(TokenPatternMatch pattern, TokenPatternMatch separator) {
        return new TokenListMatch(pattern, separator);
    }

    static TokenGroupMatch optional(TokenPatternMatch... items) {
        TokenGroupMatch g = group(items);
        g.setOptional();
        return g;
    }
}
