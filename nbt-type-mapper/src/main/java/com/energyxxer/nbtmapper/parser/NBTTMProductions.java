package com.energyxxer.nbtmapper.parser;

import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.*;

import static com.energyxxer.nbtmapper.parser.NBTTMTokens.*;

public class NBTTMProductions {
    public static final LazyTokenPatternMatch FILE;
    public static final LazyTokenPatternMatch ENTRY;
    private static final LazyTokenStructureMatch TYPE;
    
    static {

        TYPE = struct("TYPE");

        LazyTokenStructureMatch KEY = choice(ofType(NBTTMTokens.KEY), ofType(STRING_LITERAL), ofType(WILDCARD)).setName("KEY");

        LazyTokenGroupMatch COMPOUND = group(
                brace("{"),
                list(
                        group(KEY, colon(), TYPE).setName("COMPOUND_INNER"),
                        comma()
                ).setOptional().setName("COMPOUND_INNER_LIST"),
                brace("}")
        ).setName("COMPOUND");

        LazyTokenGroupMatch LIST = group(
                brace("["),
                TYPE,
                brace("]")
        ).setName("LIST");

        LazyTokenStructureMatch FLAG = struct("FLAG");
        FLAG.add(ofType(IDENTIFIER)); //boolean, text_component, resource_location...
        FLAG.add(group(matchItem(IDENTIFIER, "type"), brace("("), ofType(HASH).setOptional().setName("IS_TAG"), ofType(DEFINITION_CATEGORY).setName("DEFINITION_CATEGORY"), brace(")")).setName("TYPE_FLAG"));
        FLAG.add(group(matchItem(IDENTIFIER, "one_of"), brace("("), list(ofType(STRING_LITERAL).setName("OPTION"), comma()).setName("OPTION_LIST"), brace(")")).setName("ONE_OF_FLAG"));

        LazyTokenPatternMatch FLAGS = group(brace("("), list(FLAG, comma()).setName("FLAG_LIST"), brace(")")).setOptional().setName("FLAGS");

        TYPE.add(group(ofType(PRIMITIVE_TYPE).setName("PRIMITIVE_NAME"), FLAGS).setName("PRIMITIVE"));
        TYPE.add(COMPOUND);
        TYPE.add(LIST);
        LazyTokenGroupMatch REFERENCE_S = group(ofType(REFERENCE).setName("REFERENCE_NAME"), FLAGS).setName("REFERENCE");
        TYPE.add(REFERENCE_S);

        ENTRY = choice(
                group(ofType(REFERENCE).setName("TYPE_NAME"), colon(), choice(COMPOUND, REFERENCE_S).setName("TYPE")).setName("ROOT_TYPE"),
                ofType(COMMENT).setName("COMMENT")
        ).setName("ENTRY");

        FILE = group(
                list(ENTRY).setOptional().setName("ENTRIES"),
                ofType(TokenType.END_OF_FILE)
        );
    }

    static LazyTokenItemMatch matchItem(TokenType type, String text) {
        return new LazyTokenItemMatch(type, text).setName("ITEM_MATCH");
    }

    static LazyTokenItemMatch brace(String brace) {
        return matchItem(BRACE, brace);
    }

    static LazyTokenItemMatch colon() {
        return ofType(COLON);
    }

    static LazyTokenItemMatch comma() {
        return ofType(COMMA).setName("COMMA");
    }
    
    static LazyTokenItemMatch ofType(TokenType type) {
        return new LazyTokenItemMatch(type);
    }

    static LazyTokenStructureMatch struct(String name) {
        return new LazyTokenStructureMatch(name);
    }

    static LazyTokenStructureMatch choice(LazyTokenPatternMatch... options) {
        if(options.length == 0) throw new IllegalArgumentException("Need one or more options for choice");
        LazyTokenStructureMatch s = struct("CHOICE");
        for(LazyTokenPatternMatch option : options) {
            s.add(option);
        }
        return s;
    }

    static LazyTokenGroupMatch optional() {
        return new LazyTokenGroupMatch(true);
    }

    static LazyTokenGroupMatch group(LazyTokenPatternMatch... items) {
        LazyTokenGroupMatch g = new LazyTokenGroupMatch();
        for(LazyTokenPatternMatch item : items) {
            g.append(item);
        }
        return g;
    }

    static LazyTokenListMatch list(LazyTokenPatternMatch pattern) {
        return list(pattern, null);
    }

    static LazyTokenListMatch list(LazyTokenPatternMatch pattern, LazyTokenPatternMatch separator) {
        return new LazyTokenListMatch(pattern, separator);
    }

    static LazyTokenGroupMatch optional(LazyTokenPatternMatch... items) {
        LazyTokenGroupMatch g = group(items);
        g.setOptional();
        return g;
    }
}
