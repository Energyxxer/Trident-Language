package com.energyxxer.trident.sets;

import com.energyxxer.enxlex.lexical_analysis.Lexer;
import com.energyxxer.enxlex.lexical_analysis.summary.SummaryModule;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenStructureMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenGroup;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.enxlex.suggestions.SuggestionModule;
import com.energyxxer.enxlex.suggestions.SuggestionTags;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.prismarine.expressions.TokenExpression;
import com.energyxxer.prismarine.expressions.TokenExpressionMatch;
import com.energyxxer.prismarine.operators.UnaryOperator;
import com.energyxxer.prismarine.providers.PatternProviderSet;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.summaries.PrismarineSummaryModule;
import com.energyxxer.prismarine.summaries.SummarySymbol;
import com.energyxxer.prismarine.summaries.SymbolSuggestion;
import com.energyxxer.prismarine.symbols.Symbol;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.prismarine.typesystem.functions.ActualParameterList;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;
import com.energyxxer.prismarine.typesystem.functions.PrismarineFunction;
import com.energyxxer.prismarine.typesystem.generics.GenericWrapperType;
import com.energyxxer.trident.TridentSuiteConfiguration;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.lexer.TridentSuggestionTags;
import com.energyxxer.trident.compiler.lexer.summaries.TridentProjectSummary;
import com.energyxxer.trident.compiler.semantics.custom.classes.ParameterizedMemberHolder;
import com.energyxxer.trident.compiler.semantics.symbols.TridentSymbolVisibility;
import com.energyxxer.trident.extensions.EObject;
import com.energyxxer.util.StringBounds;
import com.energyxxer.util.StringLocation;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

import static com.energyxxer.prismarine.PrismarineProductions.*;
import static com.energyxxer.trident.compiler.lexer.TridentTokens.COMPILER_OPERATOR;
import static com.energyxxer.trident.compiler.lexer.TridentTokens.PRIMITIVE_TYPE;
import static com.energyxxer.trident.compiler.lexer.summaries.TridentProjectSummary.PASS_VALIDATION;

public class ValueAccessExpressionSet extends PatternProviderSet {

    public ValueAccessExpressionSet() {
        super(null);
    }

    private final ThreadLocal<Stack<ArrayList<TokenPattern<?>>>> memberAccessStack = ThreadLocal.withInitial(Stack::new);

    private BiConsumer<TokenPattern<?>, Lexer> resetMemberAccessStack = (ip, l) -> {
        memberAccessStack.get().push(new ArrayList<>());
    };

    @Override
    protected void installUtilityProductions(PrismarineProductions productions, TokenStructureMatch providerStructure) {

        TokenPatternMatch ACTUAL_PARAMETERS = list(
                group(
                        optional(
                                TridentProductions.identifierX(),
                                TridentProductions.colon()
                        ).setName("ACTUAL_PARAMETER_LABEL").setEvaluator((p, d) -> ((TokenGroup) p).getContents()[0].flatten(false)),
                        productions.getOrCreateStructure("INTERPOLATION_VALUE")
                ),
                TridentProductions.comma()
        ).setOptional().setName("ACTUAL_PARAMETERS");
        productions.putPatternMatch("ACTUAL_PARAMETERS", ACTUAL_PARAMETERS);

        TokenPatternMatch VARIABLE_NAME = choice(
                TridentProductions.identifierX().addProcessor((p, l) -> {
                    if(l.getSummaryModule() != null) {
                        ((PrismarineSummaryModule) l.getSummaryModule()).addFileAwareProcessor(TridentProjectSummary.PASS_HIGHLIGHT_TYPE_ERRORS, s -> {
                            if(p.isValidated() && s.getParentSummary() != null) {
                                String varName = p.flatten(false);
                                SummarySymbol relatedSym = s.getSymbolForName(varName, p.getStringLocation().index);
                                if(relatedSym == null) {
                                    l.getNotices().add(new Notice(NoticeType.ERROR, "Cannot resolve symbol '" + varName + "'", p));
                                }
                            }
                        });
                    }
                }),
                literal("this").addProcessor((p, l) -> {
                    if(l.getSummaryModule() != null) {
                        for(SummarySymbol sym : ((PrismarineSummaryModule) l.getSummaryModule()).getSubSymbolStack()) {
                            if(sym != null && sym.getSuggestionTags().contains(TridentSuggestionTags.TAG_CLASS) && sym.getParentFileSummary() != null) {
                                p.addTag(TridentSuggestionTags.__THIS_TYPE + sym.getName());
                            }
                        }
                    }
                })
        )
        .setName("VARIABLE_NAME")
        .addTags(SuggestionTags.ENABLED_INDEX, TridentSuggestionTags.IDENTIFIER, TridentSuggestionTags.IDENTIFIER_EXISTING, TridentSuggestionTags.TAG_VARIABLE)
        .addProcessor((p, l) -> {
            if(l.getSummaryModule() != null) {
                ((PrismarineSummaryModule) l.getSummaryModule()).addFileAwareProcessor(PASS_VALIDATION, f -> {
                    if(p.isValidated()) f.addSymbolUsage(p);
                });
            }
        }).setEvaluator(ValueAccessExpressionSet::parseVariable);

        productions.getOrCreateStructure("ROOT_INTERPOLATION_VALUE")
                .add(
                        VARIABLE_NAME
                )
                .add(
                        group(
                                literal("new").setName("VALUE_WRAPPER_KEY"),
                                productions.getOrCreateStructure("INTERPOLATION_TYPE"),
                                TridentProductions.brace("("),
                                productions.getPatternMatch("ACTUAL_PARAMETERS"),
                                TridentProductions.brace(")")
                        ).setName("CONSTRUCTOR_CALL")
                        .setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            TypeHandler<?> handler = (TypeHandler<?>) p.find("INTERPOLATION_TYPE").evaluate(ctx);

                            PrimitivePrismarineFunction constructor = handler.getConstructor(p, ctx, null);

                            if (constructor == null) {
                                throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "There is no constructor for type '" + ctx.getTypeSystem().getTypeIdentifierForType(handler) + "'", p.find("INTERPOLATION_TYPE"), ctx);
                            }

                            ActualParameterList actualParams = parseActualParameterList(p, ctx);

                            return ctx.getTypeSystem().sanitizeObject(constructor.safeCall(actualParams, ctx, null));
                        })
                ).addTags(SuggestionTags.ENABLED, SuggestionTags.DISABLED_INDEX, TridentSuggestionTags.CONTEXT_INTERPOLATION_VALUE);


        productions.getOrCreateStructure("ROOT_INTERPOLATION_TYPE")
                .add(
                        TridentProductions.identifierX()
                                .setName("VARIABLE_NAME")
                                .addTags(SuggestionTags.ENABLED_INDEX, TridentSuggestionTags.IDENTIFIER, TridentSuggestionTags.IDENTIFIER_EXISTING, TridentSuggestionTags.TAG_VARIABLE)
                                .addProcessor((p, l) -> {
                                    if(l.getSummaryModule() != null) {
                                        ((PrismarineSummaryModule) l.getSummaryModule()).addFileAwareProcessor(PASS_VALIDATION, f -> {
                                            if(p.isValidated()) f.addSymbolUsage(p);
                                        });
                                    }
                                })
                                .setEvaluator(ValueAccessExpressionSet::parseVariable)
                );






        TokenStructureMatch MID_INTERPOLATION_VALUE = struct("MID_INTERPOLATION_VALUE");
        MID_INTERPOLATION_VALUE.add(createChainForRoot(productions.getOrCreateStructure("ROOT_INTERPOLATION_VALUE"), productions, NORMAL_VALUE_CHAIN_CONFIG));
        MID_INTERPOLATION_VALUE.add(
                group(TridentProductions.brace("("),
                        wrapper(productions.getOrCreateStructure("INTERPOLATION_TYPE")).setName("CAST_TYPE"),
                        TridentProductions.brace(")"),
                        wrapper(
                                new TokenExpressionMatch(
                                        MID_INTERPOLATION_VALUE,
                                        productions.unitConfig.getOperatorPool(),
                                        ofType(COMPILER_OPERATOR)
                                ).setOperatorFilter(
                                        op -> op instanceof UnaryOperator && productions.unitConfig.getOperatorPool().getBinaryOrTernaryOperatorForSymbol(op.getSymbol()) == null
                                ).setName(
                                        "EXPRESSION"
                                ).setEvaluator(
                                        (p, d) -> ((ISymbolContext) d[0]).getTypeSystem().getOperatorManager().evaluate((TokenExpression) p, (ISymbolContext) d[0])
                                )).setName("CAST_VALUE")
                ).setName("CAST").setEvaluator((p, d) -> {
                    ISymbolContext ctx = (ISymbolContext) d[0];
                    Object parent = p.find("CAST_VALUE").evaluate(ctx);
                    TypeHandler targetType = (TypeHandler) p.find("CAST_TYPE").evaluate(ctx);
                    return ctx.getTypeSystem().cast(parent, targetType, p, ctx);
                })
        );


        productions.getOrCreateStructure("INTERPOLATION_VALUE").add(new TokenExpressionMatch(MID_INTERPOLATION_VALUE, productions.unitConfig.getOperatorPool(), ofType(COMPILER_OPERATOR)).setName("EXPRESSION").setEvaluator((p, d) -> ((ISymbolContext) d[0]).getTypeSystem().getOperatorManager().evaluate((TokenExpression) p, (ISymbolContext) d[0])));
        productions.getOrCreateStructure("LINE_SAFE_INTERPOLATION_VALUE").setName("INTERPOLATION_VALUE").add(new TokenExpressionMatch(MID_INTERPOLATION_VALUE, productions.unitConfig.getOperatorPool(), group(TridentProductions.sameLine(), ofType(COMPILER_OPERATOR))).setName("EXPRESSION").setEvaluator((p, d) -> ((ISymbolContext) d[0]).getTypeSystem().getOperatorManager().evaluate((TokenExpression) p, (ISymbolContext) d[0])));

        TokenPatternMatch NON_PRIMITIVE_INTERPOLATION_TYPE = group(
                PrismarineTypeSystem.validatorGroup(createChainForRoot(productions.getOrCreateStructure("ROOT_INTERPOLATION_TYPE"), productions, TYPE_CHAIN_CONFIG), false, TypeHandler.class).setName("INTERPOLATION_TYPE_CHAIN_VALIDATION"),
                wrapperOptional(productions.getOrCreateStructure("ACTUAL_TYPE_PARAMETERS")).setName("ACTUAL_TYPE_PARAMETERS")
        ).setName("NON_PRIMITIVE_INTERPOLATION_TYPE").setSimplificationFunction(d -> {
            if(d.pattern.find("ACTUAL_TYPE_PARAMETERS") == null) {
                d.pattern = ((TokenGroup) d.pattern).getContents()[0];
            }
        }).setEvaluator((p, d) -> {
            ISymbolContext ctx = (ISymbolContext) d[0];
            TypeHandler<?> sourceType = (TypeHandler<?>) ((TokenGroup) p).getContents()[0].evaluate(ctx);

            TypeHandler<?>[] genericTypes = (TypeHandler<?>[]) ((TokenGroup) p).getContents()[1].evaluate(ctx);

            GenericWrapperType<?> wrapperType = new GenericWrapperType<>(sourceType);
            wrapperType.putGenericInfo(sourceType, genericTypes);
            return wrapperType;
        });
        productions.getOrCreateStructure("NON_PRIMITIVE_INTERPOLATION_TYPE").setName("INTERPOLATION_TYPE").add(NON_PRIMITIVE_INTERPOLATION_TYPE);

        productions.getOrCreateStructure("INTERPOLATION_TYPE")
                .add(NON_PRIMITIVE_INTERPOLATION_TYPE)
                .add(ofType(PRIMITIVE_TYPE).setName("PRIMITIVE_ROOT_TYPE").addTags(TridentSuggestionTags.PRIMITIVE_TYPE).setEvaluator((p, d) -> ((ISymbolContext) d[0]).getTypeSystem().getPrimitiveHandlerForShorthand(p.flatten(false))));

    }

    private TokenPatternMatch createChainForRoot(TokenPatternMatch rootMatch, PrismarineProductions productions, ValueChainConfiguration config) {

        TokenStructureMatch MEMBER_ACCESS;
        MEMBER_ACCESS = choice(
                config.memberAccess ? group(TridentProductions.nullPropagation(), TridentProductions.dot(), TridentProductions.identifierX()
                        .setName("SYMBOL_NAME")
                        .addTags(SuggestionTags.ENABLED)
                ).setName("MEMBER_KEY") : null,
                config.indexAccess ? group(TridentProductions.nullPropagation(), TridentProductions.brace("["), group(productions.getOrCreateStructure("INTERPOLATION_VALUE")).setName("INDEX"), TridentProductions.brace("]")).setName("MEMBER_INDEX") : null,
                config.callAccess ? group(
                        TridentProductions.nullPropagation(),
                        TridentProductions.brace("(").setName("__member_access_call").addProcessor(startClosure),
                        productions.getPatternMatch("ACTUAL_PARAMETERS"),
                        TridentProductions.brace(")")
                ).setName("METHOD_CALL").addProcessor(endComplexValue).addFailProcessor((ip, l) -> {
                    if(ip.find("__member_access_call") != null) {
                        endComplexValue.accept(null, l);
                    }
                }) : null
        ).setName("MEMBER_ACCESS");
        MEMBER_ACCESS.addFailProcessor((ip, l) -> memberAccessStack.get().peek().add(ip));

        return group(
                TridentProductions.noToken().addFailProcessor(resetMemberAccessStack),
                rootMatch,
                list(MEMBER_ACCESS).setOptional().setName("MEMBER_ACCESSES").addFailProcessor((ip, l) -> memberAccessStack.get().peek().add(0, ip)),
                config.tail ? wrapperOptional(productions.getOrCreateStructure("INTERPOLATION_CHAIN_TAIL")).setName("INTERPOLATION_CHAIN_TAIL") : null
        ).setName("INTERPOLATION_CHAIN")
                .setSimplificationFunction(d -> {
                    if(d.pattern.find("MEMBER_ACCESSES") == null && d.pattern.find("INTERPOLATION_CHAIN_TAIL") == null) {
                        d.pattern = ((TokenGroup) d.pattern).getContents()[0];
                    }
                })
                .addProcessor((p, l) -> {
                    memberAccessStack.get().pop(); //all succeeded, so no use
                    suggestMemberAccessData(l, ((TokenGroup) p).getContents()[0], ((TokenList) p.find("MEMBER_ACCESSES")), null);
                })
                .addFailProcessor((ip, l) -> {
                    ArrayList<TokenPattern<?>> memberAccessData = memberAccessStack.get().pop();
                    if(memberAccessData.size() <= 1) return; //Meaning it failed, not in the member accesses, but rather in the tail.
                    TokenPattern<?>[] ipContents = ((TokenGroup) ip).getContents();
                    TokenPattern<?> rootPattern = ipContents.length > 0 ? ipContents[0] : null;
                    if(rootPattern == null) return;

                    suggestMemberAccessData(l, rootPattern, memberAccessData.size() > 0 ? ((TokenList) memberAccessData.get(0)) : null, memberAccessData.size() > 1 ? memberAccessData.get(1) : null);
                })
                .setEvaluator(
                        (p, d) -> parseAccessorChain(p, ((ISymbolContext) d[0]), (d.length > 1 && (boolean) d[1]))
                ).addProcessor((p, l) -> {
                    if(l.getSummaryModule() != null) {
                        TokenList memberAccesses = (TokenList) p.find("MEMBER_ACCESSES");
                        if(memberAccesses != null) {
                            ((PrismarineSummaryModule) l.getSummaryModule()).addFileAwareProcessor(PASS_VALIDATION, f -> {
                                if(p.isValidated()) {
                                    for(TokenPattern<?> memberAccess : memberAccesses.getContentsExcludingSeparators()) {
                                        switch(((TokenStructure) memberAccess).getContents().getName()) {
                                            case "MEMBER_KEY": {
                                                TokenPattern<?> symbolNamePattern = memberAccess.find("SYMBOL_NAME");
                                                PrismarineSummaryModule.SymbolUsage usage = new PrismarineSummaryModule.SymbolUsage(symbolNamePattern, symbolNamePattern.flatten(false));
                                                usage.symbolGetter = (f2, u) -> getSymbolForChain(
                                                        p.find(rootMatch.name),
                                                        memberAccesses,
                                                        f2,
                                                        (p2, symbol) -> p2.getStringLocation().index > memberAccess.getStringLocation().index
                                                );
                                                f.addSymbolUsage(usage);
                                                break;
                                            }
                                        }
                                    }
                                }
                            });
                        }
                    }
                });
    }

    public static SummarySymbol getTypeSymbolFromConstraint(PrismarineSummaryModule fileSummary, TokenPattern<?> pattern) {
        if(pattern == null) return null;
        return getTypeSymbolFromTypePattern(fileSummary, pattern.find("TYPE_CONSTRAINTS_WRAPPED.TYPE_CONSTRAINTS_INNER.TYPE_CONSTRAINTS_EXPLICIT.INTERPOLATION_TYPE"));
    }

    public static SummarySymbol getTypeSymbolFromTypePattern(PrismarineSummaryModule fileSummary, TokenPattern<?> pattern) {
        if(pattern == null || !pattern.getName().equals("INTERPOLATION_TYPE") || !(pattern instanceof TokenStructure)) return null;
        TokenPattern<?> inner = ((TokenStructure) pattern).getContents();

        if(inner != null) {
            if("NON_PRIMITIVE_INTERPOLATION_TYPE".equals(inner.getName())) inner = ((TokenGroup) inner.find("INTERPOLATION_TYPE_CHAIN_VALIDATION")).getContents()[0];

            switch(inner.getName()) {
                case "PRIMITIVE_ROOT_TYPE": {
                    String identifier = inner.flatten(false);

                    TridentProjectSummary parentSummary = (TridentProjectSummary) fileSummary.getParentSummary();
                    if(parentSummary != null) {
                        return parentSummary.getPrimitiveSymbol(identifier);
                    } else if(TridentSuiteConfiguration.PRIMITIVES_SUMMARY_PATH.equals(fileSummary.getFileLocation())) {
                        return fileSummary.getSymbolForName(identifier, inner.getStringLocation().index);
                    }
                    break;
                }
                case "INTERPOLATION_CHAIN": {
                    if(inner.find("MEMBER_ACCESSES") != null) {
                        //not dealing with this
                        return null;
                    }
                    String typeName = inner.find("ROOT_INTERPOLATION_TYPE").flatten(false);
                    return fileSummary.getSymbolForName(typeName, inner.getStringLocation().index);
                }
            }
        }
        return null;
    }

    private static SummarySymbol createSymbolForPrimitiveValue(String identifier, SummaryModule summaryModule) {
        TridentProjectSummary parentSummary = (TridentProjectSummary) summaryModule.getParentSummary();
        if(parentSummary != null) {
            SummarySymbol primitiveType = parentSummary.getPrimitiveSymbol(identifier);
            if(primitiveType == null) return null;
            SummarySymbol symbol = new SummarySymbol(((PrismarineSummaryModule) summaryModule), "", 0);
            symbol.setType(primitiveType);
            return symbol;
        }
        return null;
    }

    public static SummarySymbol getSymbolForChain(PrismarineSummaryModule fileSummary, TokenPattern<?> p) {
        TokenGroup chainGroup = (TokenGroup) p;
        if(chainGroup == null) return null;
        TokenPattern<?>[] chainGroupContents = chainGroup.getContents();

        if(chainGroupContents.length == 1) {
            return processChainRootSymbol(fileSummary, chainGroupContents[0]);
        }

        if(!(chainGroupContents[1] instanceof TokenList)) {
            return null;
        }

        return getSymbolForChain(chainGroupContents[0], (TokenList) chainGroupContents[1], fileSummary, (a, b) -> false);
    }

    private static SummarySymbol processChainRootSymbol(PrismarineSummaryModule fileSummary, TokenPattern<?> root) {
        SummarySymbol symbol = null;
        switch(((TokenStructure) root).getContents().getName()) {
            case "VARIABLE_NAME": {
                String varName = root.flatten(false);
                symbol = fileSummary.getSymbolForName(varName, root.getStringLocation().index);
                if(symbol == null) {
                    if("this".equals(varName)) {
                        TokenPattern<?> literalPattern = ((TokenStructure)((TokenStructure) root).getContents()).getContents();
                        for(String tag : literalPattern.getTags()) {
                            if(tag.startsWith(TridentSuggestionTags.__THIS_TYPE)) {
                                String className = tag.substring(TridentSuggestionTags.__THIS_TYPE.length());

                                SummarySymbol typeSym = fileSummary.getSymbolForName(className, root.getStringLocation().index);

                                if(typeSym != null) {
                                    symbol = new SummarySymbol(fileSummary, "", 0);
                                    symbol.setType(typeSym);
                                }
                            }
                        }
                    }
                }
                if(symbol == null) return null;
                break;
            }
            case "CONSTRUCTOR_CALL": {
                SummarySymbol typeSymbol = getTypeSymbolFromTypePattern(fileSummary, root.find("INTERPOLATION_TYPE"));
                if(typeSymbol == null) {
                    return null;
                }
                symbol = new SummarySymbol(fileSummary, "", TridentSymbolVisibility.LOCAL, root.getStringLocation().index);
                symbol.setType(typeSymbol);
                break;
            }
            default: {
                boolean primitiveFound = false;
                for(String tag : ((TokenStructure) root).getContents().getTags()) {
                    if(tag.startsWith("primitive:")) {
                        symbol = createSymbolForPrimitiveValue(tag.substring("primitive:".length()), fileSummary);
                        if(symbol == null) return null;
                        primitiveFound = true;
                        break;
                    }
                }
                if(!primitiveFound) return null;
            }
        }
        return symbol;
    }

    private static void suggestMemberAccessData(Lexer l, TokenPattern<?> root, TokenList memberAccesses, TokenPattern<?> failingAccess) {
        PrismarineSummaryModule fileSummary = (PrismarineSummaryModule) l.getSummaryModule();
        SuggestionModule suggestionModule = l.getSuggestionModule();
        if(fileSummary == null || suggestionModule == null) return;
        if(memberAccesses == null || (memberAccesses.getContents().length == 0 && failingAccess == null) || (failingAccess != null && failingAccess.getCharLength() == 0)) return;

        StringLocation start = root.getStringLocation();
        StringLocation end = failingAccess != null ? failingAccess.getStringBounds().end : memberAccesses.getStringBounds().end;
        int suggestionIndex = suggestionModule.getSuggestionIndex();

        if(suggestionIndex >= start.index && suggestionIndex <= end.index) {

            Path filePath = fileSummary.getFileLocation();

            fileSummary.addFileAwareProcessor(TridentProjectSummary.PASS_MEMBER_SUGGESTION, fs -> {
                boolean[] broken = new boolean[] {false};

                SummarySymbol obtainedSymbol = getSymbolForChain(root, memberAccesses, fs, (m, s) -> {
                    StringBounds bounds = m.getStringBounds();
                    boolean shouldBreak = bounds.start.index > suggestionIndex;
                    if(bounds.start.index < suggestionIndex && suggestionIndex <= bounds.end.index) {
                        if(m.getName().equals("MEMBER_KEY")) {
                            shouldBreak = true;
                            if(s != null) {
                                for(SummarySymbol subSymbol : s.getSubSymbols(filePath, start.index)) {
                                    SymbolSuggestion suggestion = new SymbolSuggestion(subSymbol);
                                    suggestionModule.addSuggestion(suggestion);
                                }
                            }
                        }
                    }
                    if(!shouldBreak && (m.getName().equals("METHOD_CALL") || m.getName().equals("MEMBER_INDEX"))) {
                        StringBounds parameterBounds = m.getStringBounds();
                        if(suggestionIndex >= parameterBounds.start.index && suggestionIndex <= parameterBounds.end.index) {
                            shouldBreak = true;
                        }
                    }
                    if(shouldBreak) broken[0] = true;
                    return shouldBreak;
                });

                if(obtainedSymbol != null && !broken[0] && failingAccess != null) {
                    if(failingAccess.getName().equals("MEMBER_KEY")) {
                        for(SummarySymbol subSymbol : obtainedSymbol.getSubSymbols(filePath, start.index)) {
                            SymbolSuggestion suggestion = new SymbolSuggestion(subSymbol);
                            suggestionModule.addSuggestion(suggestion);
                        }
                    }
                }
            });
        }
    }

    private static SummarySymbol getSymbolForChain(TokenPattern<?> root, TokenList memberAccesses, PrismarineSummaryModule fileSummary, BiPredicate<TokenPattern<?>, SummarySymbol> shouldBreak) {
        TokenPattern<?>[] memberAccessesArr = memberAccesses.getContents();

        int rootIndex = root.getStringLocation().index;

        SummarySymbol symbol = null;

        Path filePath = fileSummary.getFileLocation();

        for(int i = 0; i < memberAccessesArr.length + 1; i++) {
            TokenPattern<?> memberAccess = i == 0 ? root : memberAccessesArr[i-1];
            if(memberAccess == null) continue;
            memberAccess = sanitizeMemberAccessPattern(memberAccess);
            if(shouldBreak.test(memberAccess, symbol)) return symbol;

            if(i == 0) {
                symbol = processChainRootSymbol(fileSummary, memberAccess);
                if(symbol == null) return null;
            } else {
                switch(memberAccess.getName()) {
                    case "MEMBER_KEY": {
                        TokenPattern<?> memberName = memberAccess.find("SYMBOL_NAME");
                        boolean looped = false;
                        if(memberName != null) {
                            for(SummarySymbol subSymbol : symbol.getSubSymbolsByName(memberName.flatten(false), filePath, rootIndex)) {
                                symbol = subSymbol;
                                looped = true;
                            }
                        }
                        if(!looped) {
                            return null;
                        }
                        break;
                    }
                    case "METHOD_CALL": {
                        SummarySymbol returnType = symbol.getReturnType();
                        if(returnType == null) {
                            return null;
                        }
                        symbol = new SummarySymbol(fileSummary, "", TridentSymbolVisibility.LOCAL, rootIndex);
                        symbol.setType(returnType);
                        break;
                    }
                    case "MEMBER_INDEX": {
                        //Can't do much about this atm
                        return null;
                    }
                    default: {
                        break;
                    }
                }
            }
        }
        return symbol;
    }

    private static TokenPattern<?> sanitizeMemberAccessPattern(@NotNull TokenPattern<?> pattern) {
        while(pattern.getName().equals("MEMBER_ACCESS")) {
            pattern = ((TokenStructure) pattern).getContents();
        }
        return pattern;
    }

    public static Object parseAccessorChain(TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        TokenPattern<?> toBlame = ((TokenGroup) pattern).getContents()[0];
        TokenList accessorList = (TokenList) pattern.find("MEMBER_ACCESSES");

        Object parent;
        if (accessorList != null) {
            TokenPattern<?>[] accessors = accessorList.getContents();
            for(int i = 0; i < accessors.length; i++) {
                accessors[i] = sanitizeMemberAccessPattern(accessors[i]);
            }

            ActualParameterList[] firstAccessorParameters = new ActualParameterList[] {null};

            parent = toBlame.evaluate(ctx, false, (Supplier<ActualParameterList>) () -> {
                TokenPattern<?> firstAccessor = accessors[0];
                if(firstAccessor.getName().equals("METHOD_CALL")) {
                    ActualParameterList parameterList = parseActualParameterList(firstAccessor, ctx);
                    firstAccessorParameters[0] = parameterList;
                    return parameterList;
                }
                return null;
            });

            if(firstAccessorParameters[0] != null) {
                //Evaluated first accessor, gotta be a function

                if(parent instanceof PrismarineFunction.FixedThisFunctionSymbol) {
                    parent = ctx.getTypeSystem().sanitizeObject(((PrismarineFunction.FixedThisFunctionSymbol) parent).safeCall(firstAccessorParameters[0], ctx));
                    toBlame = accessors[0];
                } else if (parent instanceof PrimitivePrismarineFunction) {
                    parent = ctx.getTypeSystem().sanitizeObject(((PrimitivePrismarineFunction) parent).safeCall(firstAccessorParameters[0], ctx, null));
                    toBlame = accessors[0];
                } else {
                    throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "This is not a function", toBlame, ctx);
                }
            }

            for (int i = firstAccessorParameters[0] == null ? 0 : 1; i < accessors.length; i++) {
                TokenPattern<?> accessor = accessors[i];

                if(parent instanceof ParameterizedMemberHolder && i+1 < accessors.length && accessor.getName().equals("MEMBER_KEY") && accessors[i+1].getName().equals("METHOD_CALL")) {
                    ActualParameterList paramList = parseActualParameterList(accessors[i+1], ctx);

                    Object member = ((ParameterizedMemberHolder) parent).getMemberForParameters(accessor.find("SYMBOL_NAME").flatten(false), accessor, paramList, ctx, false);

                    EObject.assertNotNull(member, toBlame, ctx);

                    if(member instanceof PrismarineFunction.FixedThisFunctionSymbol) {
                        parent = ctx.getTypeSystem().sanitizeObject(((PrismarineFunction.FixedThisFunctionSymbol) member).safeCall(paramList, ctx));
                        toBlame = accessor;
                    } else if (member instanceof PrimitivePrismarineFunction) {
                        parent = ctx.getTypeSystem().sanitizeObject(((PrimitivePrismarineFunction) member).safeCall(paramList, ctx, null));
                        toBlame = accessor;
                    } else {
                        throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "This is not a function", toBlame, ctx);
                    }

                    i++;
                    continue;
                }

                parent = parseAccessor(parent, toBlame, accessor, ctx, keepSymbol && (i == accessors.length-1));
                if(parent == NULL_PROPAGATION.class) {
                    parent = null;
                    break;
                }
                toBlame = accessor;
            }

        } else {
            parent = toBlame.evaluate(ctx, keepSymbol);
        }

        TokenPattern<?> rawTail = pattern.find("INTERPOLATION_CHAIN_TAIL");

        if(rawTail != null) {
            parent = rawTail.evaluate(ctx, parent);
        }
        return parent;
    }

    private static Object parseAccessor(Object parent, TokenPattern<?> parentPattern, TokenPattern<?> accessorPattern, ISymbolContext ctx, boolean keepSymbol) {
        //expect sanitized accessor pattern
        boolean propagateNull = accessorPattern.find("NULL_PROPAGATION") != null;
        if(propagateNull && parent == null) {
            return NULL_PROPAGATION.class;
        }
        EObject.assertNotNull(parent, parentPattern, ctx);
        switch (accessorPattern.getName()) {
            case "MEMBER_KEY": {
                String memberName = accessorPattern.find("SYMBOL_NAME").flatten(false);
                TypeHandler handler = ctx.getTypeSystem().getHandlerForObject(parent, parentPattern, ctx);
                while (true) {
                    try {
                        return ctx.getTypeSystem().sanitizeObject(handler.getMember(parent, memberName, accessorPattern, ctx, keepSymbol));
                    } catch (MemberNotFoundException x) {
                        if ((handler = handler.getSuperType()) == null) {
                            throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Cannot resolve member '" + memberName + "' of " + ctx.getTypeSystem().getTypeIdentifierForObject(parent), accessorPattern, ctx);
                        }
                    }
                }
            }
            case "MEMBER_INDEX": {
                Object index = accessorPattern.find("INDEX.INTERPOLATION_VALUE").evaluate(ctx);
                TypeHandler handler = ctx.getTypeSystem().getHandlerForObject(parent, parentPattern, ctx);
                while (true) {
                    try {
                        return ctx.getTypeSystem().sanitizeObject(handler.getIndexer(parent, index, accessorPattern, ctx, keepSymbol));
                    } catch (MemberNotFoundException x) {
                        if ((handler = handler.getSuperType()) == null) {
                            throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Cannot resolve member for index " + index + " of " + ctx.getTypeSystem().getTypeIdentifierForObject(parent), accessorPattern, ctx);
                        }
                    }
                }
            }
            case "METHOD_CALL": {
                if (parent instanceof PrimitivePrismarineFunction) {
                    ActualParameterList paramList = parseActualParameterList(accessorPattern, ctx);

                    return ctx.getTypeSystem().sanitizeObject(((PrimitivePrismarineFunction) parent).safeCall(paramList, ctx, null));
                } else {
                    throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "This is not a function", parentPattern, ctx);
                }
            }
            default: {
                throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Unknown grammar branch name '" + accessorPattern.getName() + "'", accessorPattern, ctx);
            }
        }
    }

    public static Object parseVariable(TokenPattern<?> pattern, Object... data) {
        ISymbolContext ctx = (ISymbolContext) data[0];
        boolean keepSymbol = data.length > 1 && (boolean) data[1];
        Supplier<ActualParameterList> followingFunctionParams = data.length > 2 ? (Supplier<ActualParameterList>) data[2] : null;
        Symbol symbol = ctx.search(pattern.flatten(false), ctx, followingFunctionParams != null ? followingFunctionParams.get() : null);
        if (symbol == null) {
            throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Symbol '" + pattern.flatten(false) + "' is not defined", pattern, ctx);
        }
        return keepSymbol || symbol instanceof PrismarineFunction.FixedThisFunctionSymbol ? symbol : ctx.getTypeSystem().sanitizeObject(symbol.getValue(pattern, ctx));
    }

    private static ActualParameterList parseActualParameterList(TokenPattern<?> pattern, ISymbolContext ctx) {
        TokenList paramList = (TokenList) pattern.find("ACTUAL_PARAMETERS");
        if(paramList == null) return new ActualParameterList(pattern);
        ArrayList<String> names = null;
        ArrayList<Object> params = new ArrayList<>();
        ArrayList<TokenPattern<?>> patterns = new ArrayList<>();
        int i = 0;
        for (TokenPattern<?> rawParam : paramList.getContentsExcludingSeparators()) {
            Object value = rawParam.find("INTERPOLATION_VALUE").evaluate(ctx);
            params.add(value);
            patterns.add(rawParam);

            String name = (String) rawParam.findThenEvaluate("ACTUAL_PARAMETER_LABEL", null);
            if(name != null || names != null) {
                if(names == null) {
                    names = new ArrayList<>();
                    for(int j = 0; j < i; j++) {
                        names.add(null);
                    }
                }
                names.add(name);
            }

            i++;
        }
        return new ActualParameterList(params.toArray(), names != null ? names.toArray(new String[0]) : null, patterns.toArray(new TokenPattern<?>[0]), pattern);
    }

    private static class ValueChainConfiguration {
        boolean memberAccess = true;
        boolean indexAccess = true;
        boolean callAccess = true;

        boolean tail = true;
    }

    private static final ValueChainConfiguration NORMAL_VALUE_CHAIN_CONFIG = new ValueChainConfiguration();
    private static final ValueChainConfiguration TYPE_CHAIN_CONFIG = new ValueChainConfiguration() {{indexAccess = callAccess = tail = false;}};

    private static class NULL_PROPAGATION {}
}
