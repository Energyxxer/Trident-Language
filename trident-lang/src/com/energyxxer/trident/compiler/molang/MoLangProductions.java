package com.energyxxer.trident.compiler.molang;

import com.energyxxer.prismarine.operators.OperationOrder;
import com.energyxxer.prismarine.operators.OperatorPool;
import com.energyxxer.prismarine.expressions.TokenExpressionMatch;
import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenGroupMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenItemMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenListMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenStructureMatch;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.enxlex.suggestions.SuggestionTags;
import com.energyxxer.util.logger.Debug;

import static com.energyxxer.trident.compiler.molang.MoLangTokens.*;

public class MoLangProductions {
    public static TokenGroupMatch FILE;

    private static TokenStructureMatch ROOT_VALUE;
    private static TokenStructureMatch CONTEXT_VALUE;

    static {
        OperatorPool operatorPool = new OperatorPool();

        operatorPool.addBinaryOperator("*", 2, OperationOrder.LTR);
        operatorPool.addBinaryOperator("/", 2, OperationOrder.LTR);

        operatorPool.addBinaryOperator("+", 3, OperationOrder.LTR);
        operatorPool.addBinaryOperator("-", 3, OperationOrder.LTR);

        operatorPool.addBinaryOperator("<", 5, OperationOrder.LTR);
        operatorPool.addBinaryOperator("<=", 5, OperationOrder.LTR);
        operatorPool.addBinaryOperator(">", 5, OperationOrder.LTR);
        operatorPool.addBinaryOperator(">=", 5, OperationOrder.LTR);
        operatorPool.addBinaryOperator("==", 6, OperationOrder.LTR);
        operatorPool.addBinaryOperator("!=", 6, OperationOrder.LTR);
        operatorPool.addBinaryOperator("&&", 10, OperationOrder.LTR);
        operatorPool.addBinaryOperator("||", 11, OperationOrder.LTR);

        operatorPool.addBinaryOperator("??", 12, OperationOrder.RTL);

        operatorPool.addTernaryOperator("?", ":", 13, OperationOrder.RTL);

        operatorPool.addBinaryOperator("=", 99, OperationOrder.RTL);

        ROOT_VALUE = struct("ROOT_VALUE");
        ROOT_VALUE.add(ofType(NUMBER));
        ROOT_VALUE.add(ofType(STRING_LITERAL));

        ROOT_VALUE.add(stringMatch(IDENTIFIER, "break"));
        ROOT_VALUE.add(stringMatch(IDENTIFIER, "continue"));

        CONTEXT_VALUE = struct("CONTEXT_VALUE");
        ROOT_VALUE.add(CONTEXT_VALUE);

        TokenGroupMatch furtherContext = optional(ofType(ARROW), CONTEXT_VALUE);


        TokenStructureMatch MID_VALUE = struct("MID_VALUE");
        MID_VALUE.add(ROOT_VALUE);

        TokenExpressionMatch EXPRESSION = new TokenExpressionMatch(MID_VALUE, operatorPool, ofType(OPERATOR));
        MID_VALUE.add(group(brace("(").addTags(SuggestionTags.DISABLED), EXPRESSION, brace(")")));


        TokenGroupMatch functionCall = optional(brace("(").addTags(SuggestionTags.DISABLED), list(EXPRESSION, comma()).setOptional(), brace(")"));

        ROOT_VALUE.add(group( //todo check if variable.something->math.pi is valid; if so, add to CONTEXT_VALUE instead
                stringMatch(IDENTIFIER, "Math").addTags(SuggestionTags.DISABLED_INDEX, SuggestionTags.ENABLED),
                ofType(MoLangTokens.DOT),
                ofType(MoLangTokens.IDENTIFIER)
                        .addTags(SuggestionTags.ENABLED, MoLangSuggestionTags.MATH_MEMBER)
                        .addProcessor((p, l) -> {
                            String thisMemberName = p.flatten(false);
                            for(String queryName : MoLang.mathMembers) {
                                if(queryName.equalsIgnoreCase(thisMemberName)) return;
                            }
                            l.getNotices().add(new Notice(NoticeType.WARNING, "No such Math property or function '" + thisMemberName + "'", p));
                        }),
                functionCall
        ));

        CONTEXT_VALUE.add(group(choice("variable", "v").addTags(SuggestionTags.DISABLED_INDEX, SuggestionTags.ENABLED), ofType(MoLangTokens.DOT), ofType(MoLangTokens.IDENTIFIER), furtherContext).setName("VARIABLE"));
        CONTEXT_VALUE.add(group(
                choice("query", "q").addTags(SuggestionTags.DISABLED_INDEX, SuggestionTags.ENABLED),
                ofType(MoLangTokens.DOT),
                ofType(MoLangTokens.IDENTIFIER)
                        .addTags(SuggestionTags.ENABLED, MoLangSuggestionTags.QUERY_NAME)
                        .addProcessor((p, l) -> {
                            String thisQueryName = p.flatten(false);
                            for(String queryName : MoLang.queryNames) {
                                if(queryName.equalsIgnoreCase(thisQueryName)) return;
                            }
                            l.getNotices().add(new Notice(NoticeType.WARNING, "No such query '" + thisQueryName + "'", p));
                        }),
                functionCall,
                furtherContext
        ));

        EXPRESSION.addProcessor((p, l) -> {
            Debug.log(p);
        });

        FILE = group(EXPRESSION, ofType(SEMICOLON), ofType(TokenType.END_OF_FILE));
        FILE.addTags(SuggestionTags.ENABLED);
    }

    static TokenItemMatch matchItem(TokenType type, String text) {
        return new TokenItemMatch(type, text).setName("ITEM_MATCH");
    }

    static TokenItemMatch brace(String brace) {
        return matchItem(BRACE, brace);
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

    static TokenStructureMatch choice(TokenType type, String... options) {
        if(options.length == 0) throw new IllegalArgumentException("Need one or more options for choice");
        TokenStructureMatch s = struct("CHOICE");
        for(String option : options) {
            s.add(stringMatch(type, option));
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

    static TokenStructureMatch choice(String... identifiers) {
        TokenStructureMatch s = struct("CHOICE");
        for(String identifier : identifiers) {
            s.add(stringMatch(IDENTIFIER, identifier));
        }
        return s;
    }

    public static TokenItemMatch stringMatch(TokenType type, String value) {
        return new TokenItemMatch(type, value).setCaseSensitive(false);
    }
}
