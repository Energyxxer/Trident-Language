package com.energyxxer.trident.compiler.lexer;

import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.LazyTokenItemMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.LazyTokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.LazyTokenStructureMatch;

import static com.energyxxer.trident.compiler.lexer.TridentProductions.*;
import static com.energyxxer.trident.compiler.lexer.TridentTokens.BOOLEAN;
import static com.energyxxer.trident.compiler.lexer.TridentTokens.IDENTIFIER_TYPE_X;

public class SubTridentProductions {
    public static final LazyTokenPatternMatch COMPILE_BLOCK_INNER;

    private static final LazyTokenStructureMatch STATEMENT;
    private static final LazyTokenStructureMatch VALUE;
    private static final LazyTokenStructureMatch EXPRESSION;
    private static final LazyTokenStructureMatch DATA_TYPE;

    static {
        STATEMENT = struct("STATEMENT");
        COMPILE_BLOCK_INNER = list(group(STATEMENT, list(ofType(TokenType.NEWLINE)).setOptional())).setOptional().setName("COMPILE_BLOCK_INNER");

        DATA_TYPE = choice(
                "integer",
                "real",
                "string",
                "bool",
                "entity",
                "block",
                "item",
                "coordinates",
                "nbt_compound",
                "nbt_path",
                "text_component"
        );

        VALUE = choice(
                VARIABLE_MARKER,
                integer(),
                real(),
                ofType(BOOLEAN).setName("BOOLEAN"),
                string(),
                identifierX(),
                ENTITY,
                BLOCK,
                ITEM,
                TEXT_COMPONENT,
                NBT_COMPOUND,
                COORDINATE_SET
        ).setName("VALUE");

        EXPRESSION = choice(
                group(VALUE, keyword("as"), DATA_TYPE).setName("CAST"),
                group(choice(identifierX(),VARIABLE_MARKER).setName("VARIABLE_CHOICE"), symbol("="), VALUE).setName("ASSIGNMENT")
        );

        VALUE.add(EXPRESSION);

        STATEMENT.add(group(keyword("var"), identifierX(), optional(symbol("="), VALUE).setName("INITIALIZATION")).setName("VARIABLE_DECLARATION"));
        STATEMENT.add(group(keyword("append"), COMMAND).setName("COMMAND_APPEND"));
    }


    static LazyTokenItemMatch identifierX() {
        return ofType(IDENTIFIER_TYPE_X).setName("IDENTIFIER");
    }
}
