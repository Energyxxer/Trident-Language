package com.energyxxer.trident.compiler.morelang;

import com.energyxxer.trident.compiler.molang.MoLang;
import com.energyxxer.enxlex.lexical_analysis.token.TokenType;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenGroupMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenItemMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenListMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenStructureMatch;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.enxlex.suggestions.SuggestionTags;
import com.energyxxer.prismarine.expressions.TokenExpressionMatch;
import com.energyxxer.prismarine.operators.OperationOrder;
import com.energyxxer.prismarine.operators.OperatorPool;
import com.energyxxer.util.logger.Debug;

import static com.energyxxer.trident.compiler.morelang.MoreLangTokens.*;

public class MoreLangProductions {
    public static TokenGroupMatch FILE;

    private static TokenStructureMatch ROOT_VALUE;
    private static TokenStructureMatch CONTEXT_VALUE;
    private static TokenStructureMatch STATEMENT;
    private static TokenExpressionMatch EXPRESSION;

    static {
        ROOT_VALUE = struct("ROOT_VALUE");
        CONTEXT_VALUE = struct("CONTEXT_VALUE");
        STATEMENT = struct("STATEMENT");

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

        ROOT_VALUE.add(ofType(NUMBER));
        ROOT_VALUE.add(ofType(STRING_LITERAL));

        ROOT_VALUE.add(CONTEXT_VALUE);

        TokenGroupMatch furtherContext = optional(ofType(ARROW), CONTEXT_VALUE);


        TokenStructureMatch MID_VALUE = struct("MID_VALUE");
        MID_VALUE.add(ROOT_VALUE);

        EXPRESSION = new TokenExpressionMatch(MID_VALUE, operatorPool, ofType(OPERATOR));
        MID_VALUE.add(group(brace("(").addTags(SuggestionTags.DISABLED), EXPRESSION, brace(")")));


        TokenGroupMatch functionCall = optional(brace("(").addTags(SuggestionTags.DISABLED), list(EXPRESSION, comma()).setOptional(), brace(")"));

        ROOT_VALUE.add(group(
                stringMatch(IDENTIFIER, "Math").addTags(SuggestionTags.DISABLED_INDEX, SuggestionTags.ENABLED),
                ofType(MoreLangTokens.DOT),
                ofType(MoreLangTokens.IDENTIFIER)
                        .addTags(SuggestionTags.ENABLED, MoreLangSuggestionTags.MATH_MEMBER)
                        .addProcessor((p, l) -> {
                            String thisMemberName = p.flatten(false);
                            for(String queryName : MoLang.mathMembers) {
                                if(queryName.equalsIgnoreCase(thisMemberName)) return;
                            }
                            l.getNotices().add(new Notice(NoticeType.WARNING, "No such Math property or function '" + thisMemberName + "'", p));
                        }),
                functionCall
        ));

        CONTEXT_VALUE.add(group(choice("variable", "v").addTags(SuggestionTags.DISABLED_INDEX, SuggestionTags.ENABLED), ofType(MoreLangTokens.DOT), ofType(MoreLangTokens.IDENTIFIER), furtherContext).setName("VARIABLE"));
        CONTEXT_VALUE.add(group(
                choice("query", "q").addTags(SuggestionTags.DISABLED_INDEX, SuggestionTags.ENABLED),
                ofType(MoreLangTokens.DOT),
                ofType(MoreLangTokens.IDENTIFIER)
                        .addTags(SuggestionTags.ENABLED, MoreLangSuggestionTags.QUERY_NAME)
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



        STATEMENT.add(group(EXPRESSION, ofType(SEMICOLON)).setName("EXPRESSION_STATEMENT"));
        STATEMENT.add(group(stringMatch(IDENTIFIER, "break"), ofType(SEMICOLON)).setName("BREAK_STATEMENT"));
        STATEMENT.add(group(stringMatch(IDENTIFIER, "continue"), ofType(SEMICOLON)).setName("CONTINUE_STATEMENT"));
        STATEMENT.add(group(stringMatch(IDENTIFIER, "return"), EXPRESSION, ofType(SEMICOLON)).setName("RETURN_STATEMENT"));

        FILE = group(list(STATEMENT), ofType(TokenType.END_OF_FILE));
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
