package com.energyxxer.trident.sets;

import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.prismarine.typesystem.functions.ActualParameterList;
import com.energyxxer.prismarine.typesystem.functions.PrimitivePrismarineFunction;
import com.energyxxer.prismarine.controlflow.MemberNotFoundException;
import com.energyxxer.trident.compiler.lexer.TridentSuggestionTags;
import com.energyxxer.trident.compiler.lexer.summaries.TridentSummaryModule;
import com.energyxxer.trident.compiler.semantics.custom.classes.ClassMethodFamily;
import com.energyxxer.trident.compiler.semantics.custom.classes.ParameterizedMemberHolder;
import com.energyxxer.trident.compiler.semantics.symbols.TridentSymbolVisibility;
import com.energyxxer.trident.extensions.EObject;
import com.energyxxer.trident.worker.tasks.SetupOperatorManagerTask;
import com.energyxxer.enxlex.lexical_analysis.Lexer;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenStructureMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenGroup;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.suggestions.LiteralSuggestion;
import com.energyxxer.enxlex.suggestions.SuggestionTags;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.expressions.TokenExpression;
import com.energyxxer.prismarine.expressions.TokenExpressionMatch;
import com.energyxxer.prismarine.providers.PatternProviderSet;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.summaries.SummarySymbol;
import com.energyxxer.prismarine.symbols.Symbol;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.util.StringBounds;
import com.energyxxer.util.StringLocation;
import com.energyxxer.util.logger.Debug;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static com.energyxxer.trident.compiler.lexer.TridentTokens.COMPILER_OPERATOR;
import static com.energyxxer.trident.compiler.lexer.TridentTokens.PRIMITIVE_TYPE;
import static com.energyxxer.prismarine.PrismarineProductions.*;

public class ValueAccessExpressionSet extends PatternProviderSet {

    public ValueAccessExpressionSet() {
        super(null);
    }

    private static final ArrayList<PreBlockDeclaration> preBlockDeclarations = new ArrayList<>();
    public static final BiConsumer<TokenPattern<?>, Lexer> clearPreBlockDeclarations = (p, l) -> preBlockDeclarations.clear();

    public static final BiConsumer<TokenPattern<?>, Lexer> capturePreBlockDeclarations = (p, l) -> {
        if(l.getSummaryModule() != null) {
            for(PreBlockDeclaration declaration : preBlockDeclarations) {
                SummarySymbol sym = new SummarySymbol((TridentSummaryModule) l.getSummaryModule(), declaration.declarationPattern.flatten(false), TridentSymbolVisibility.LOCAL, p.getStringLocation().index + 1);
                ((TridentSummaryModule) l.getSummaryModule()).addSymbolUsage(declaration.declarationPattern);
                sym.setDeclarationPattern(declaration.declarationPattern);
                sym.setType(getTypeSymbolFromConstraint(l, declaration.constraintsPattern));
                if(declaration.tags != null) {
                    for(String tag : declaration.tags) {
                        sym.addTag(tag);
                    }
                }
                sym.addTag(TridentSuggestionTags.TAG_VARIABLE);
                ((TridentSummaryModule) l.getSummaryModule()).peek().putElement(sym);
            }
        }
        preBlockDeclarations.clear();
    };

    private final Stack<ArrayList<TokenPattern<?>>> memberAccessStack = new Stack<>();

    private BiConsumer<TokenPattern<?>, Lexer> resetMemberAccessStack = (ip, l) -> {
        memberAccessStack.push(new ArrayList<>());
    };

    @Override
    protected void installUtilityProductions(PrismarineProductions productions, TokenStructureMatch providerStructure) {

        productions.getOrCreateStructure("ROOT_INTERPOLATION_VALUE")
                .add(
                        choice(TridentProductions.identifierX(), literal("this"))
                                .setName("VARIABLE_NAME")
                                .addTags(SuggestionTags.ENABLED_INDEX, TridentSuggestionTags.IDENTIFIER, TridentSuggestionTags.IDENTIFIER_EXISTING, TridentSuggestionTags.TAG_VARIABLE)
                                .addProcessor((p, l) -> {
                                    if(l.getSummaryModule() != null) {
                                        ((TridentSummaryModule) l.getSummaryModule()).addSymbolUsage(p);
                                    }
                                }).setEvaluator(ValueAccessExpressionSet::parseVariable)
                )
                .add(
                        group(
                                literal("new").setName("VALUE_WRAPPER_KEY"),
                                productions.getOrCreateStructure("INTERPOLATION_TYPE"),
                                TridentProductions.brace("("),
                                list(
                                        productions.getOrCreateStructure("INTERPOLATION_VALUE"),
                                        TridentProductions.comma()
                                ).setOptional().setName("PARAMETERS"),
                                TridentProductions.brace(")")
                        ).setName("CONSTRUCTOR_CALL")
                        .setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            TypeHandler<?> handler = (TypeHandler<?>) p.find("INTERPOLATION_TYPE").evaluate(ctx);

                            PrimitivePrismarineFunction constructor = handler.getConstructor(p, ctx);

                            if (constructor == null) {
                                throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "There is no constructor for type '" + ctx.getTypeSystem().getTypeIdentifierForType(handler) + "'", p.find("INTERPOLATION_TYPE"), ctx);
                            }
                            ArrayList<Object> params = new ArrayList<>();
                            ArrayList<TokenPattern<?>> patterns = new ArrayList<>();

                            TokenList paramList = (TokenList) p.find("PARAMETERS");

                            if (paramList != null) {
                                for (TokenPattern<?> rawParam : paramList.getContentsExcludingSeparators()) {
                                    params.add(rawParam.evaluate(ctx, false));
                                    patterns.add(rawParam);
                                }
                            }

                            return ctx.getTypeSystem().sanitizeObject(constructor.safeCall(params.toArray(), patterns.toArray(new TokenPattern<?>[0]), p, ctx, null));
                        })
                ).addTags(SuggestionTags.ENABLED, SuggestionTags.DISABLED_INDEX, TridentSuggestionTags.CONTEXT_INTERPOLATION_VALUE);


        productions.getOrCreateStructure("ROOT_INTERPOLATION_TYPE")
                .add(
                        TridentProductions.identifierX()
                                .setName("VARIABLE_NAME")
                                .addTags(SuggestionTags.ENABLED_INDEX, TridentSuggestionTags.IDENTIFIER, TridentSuggestionTags.IDENTIFIER_EXISTING, TridentSuggestionTags.TAG_VARIABLE)
                                .addProcessor((p, l) -> {
                                    if(l.getSummaryModule() != null) {
                                        ((TridentSummaryModule) l.getSummaryModule()).addSymbolUsage(p);
                                    }
                                })
                                .setEvaluator(ValueAccessExpressionSet::parseVariable)
                );






        TokenStructureMatch MID_INTERPOLATION_VALUE = struct("MID_INTERPOLATION_VALUE");
        MID_INTERPOLATION_VALUE.add(createChainForRoot(productions.getOrCreateStructure("ROOT_INTERPOLATION_VALUE"), productions, NORMAL_VALUE_CHAIN_CONFIG));
        MID_INTERPOLATION_VALUE.add(
                group(TridentProductions.brace("("), productions.getOrCreateStructure("INTERPOLATION_TYPE"), TridentProductions.brace(")"), productions.getOrCreateStructure("INTERPOLATION_VALUE")).setName("CAST").setEvaluator((p, d) -> {
                    ISymbolContext ctx = (ISymbolContext) d[0];
                    Object parent = p.find("INTERPOLATION_VALUE").evaluate(ctx);
                    TypeHandler targetType = (TypeHandler) p.find("INTERPOLATION_TYPE").evaluate(ctx);
                    return ctx.getTypeSystem().cast(parent, targetType, p, ctx);
                })
        );


        productions.getOrCreateStructure("INTERPOLATION_VALUE").add(new TokenExpressionMatch(MID_INTERPOLATION_VALUE, productions.unitConfig.getOperatorPool(), ofType(COMPILER_OPERATOR)).setName("EXPRESSION").setEvaluator((p, d) -> ((ISymbolContext) d[0]).get(SetupOperatorManagerTask.INSTANCE).evaluate((TokenExpression) p, (ISymbolContext) d[0])));
        productions.getOrCreateStructure("LINE_SAFE_INTERPOLATION_VALUE").setName("INTERPOLATION_VALUE").add(new TokenExpressionMatch(MID_INTERPOLATION_VALUE, productions.unitConfig.getOperatorPool(), group(TridentProductions.sameLine(), ofType(COMPILER_OPERATOR))).setName("EXPRESSION").setEvaluator((p, d) -> ((ISymbolContext) d[0]).get(SetupOperatorManagerTask.INSTANCE).evaluate((TokenExpression) p, (ISymbolContext) d[0])));

        productions.getOrCreateStructure("INTERPOLATION_TYPE")
                .add(PrismarineTypeSystem.validatorGroup(createChainForRoot(productions.getOrCreateStructure("ROOT_INTERPOLATION_TYPE"), productions, TYPE_CHAIN_CONFIG), false, TypeHandler.class).setName("INTERPOLATION_TYPE_CHAIN_VALIDATION"))
                .add(ofType(PRIMITIVE_TYPE).setName("PRIMITIVE_ROOT_TYPE").addTags(TridentSuggestionTags.PRIMITIVE_TYPE).setEvaluator((p, d) -> ((ISymbolContext) d[0]).getTypeSystem().getPrimitiveHandlerForShorthand(p.flatten(false))));

    }

    private TokenPatternMatch createChainForRoot(TokenPatternMatch rootMatch, PrismarineProductions productions, ValueChainConfiguration config) {

        TokenStructureMatch MEMBER_ACCESS;
        MEMBER_ACCESS = choice(
                config.memberAccess ? group(TridentProductions.dot(), TridentProductions.identifierX()
                        .setName("SYMBOL_NAME")
                        .addTags(SuggestionTags.ENABLED)
                ).setName("MEMBER_KEY") : null,
                config.indexAccess ? group(TridentProductions.brace("["), group(productions.getOrCreateStructure("INTERPOLATION_VALUE")).setName("INDEX"), TridentProductions.brace("]")).setName("MEMBER_INDEX") : null,
                config.callAccess ? group(TridentProductions.brace("(").setName("__member_access_call").addProcessor(startClosure), list(productions.getOrCreateStructure("INTERPOLATION_VALUE"), TridentProductions.comma()).setOptional().setName("PARAMETERS"), TridentProductions.brace(")")).setName("METHOD_CALL").addProcessor(endComplexValue).addFailProcessor((ip, l) -> {
                    if(ip.find("__member_access_call") != null) {
                        endComplexValue.accept(null, l);
                    }
                }) : null
        ).setName("MEMBER_ACCESS");
        MEMBER_ACCESS.addFailProcessor((ip, l) -> memberAccessStack.peek().add(ip));

        return group(
                TridentProductions.noToken().addFailProcessor(resetMemberAccessStack),
                rootMatch,
                list(MEMBER_ACCESS).setOptional().setName("MEMBER_ACCESSES").addFailProcessor((ip, l) -> memberAccessStack.peek().add(0, ip)),
                config.tail ? wrapperOptional(productions.getOrCreateStructure("INTERPOLATION_CHAIN_TAIL")).setName("INTERPOLATION_CHAIN_TAIL") : null
        ).setName("INTERPOLATION_CHAIN")
                .setSimplificationFunction(d -> {
                    if(d.pattern.find("MEMBER_ACCESSES") == null && d.pattern.find("INTERPOLATION_CHAIN_TAIL") == null) {
                        d.pattern = ((TokenGroup) d.pattern).getContents()[0];
                    }
                })
                .addProcessor((p, l) -> {
                    memberAccessStack.pop(); //all succeeded, so no use
                    processMemberAccessData(l, ((TokenGroup) p).getContents()[0], ((TokenList) p.find("MEMBER_ACCESSES")), null);
                })
                .addFailProcessor((ip, l) -> {
                    ArrayList<TokenPattern<?>> memberAccessData = memberAccessStack.pop();
                    if(memberAccessData.size() <= 1) return; //Meaning it failed, not in the member accesses, but rather in the tail.
                    TokenPattern<?>[] ipContents = ((TokenGroup) ip).getContents();
                    TokenPattern<?> rootPattern = ipContents.length > 0 ? ipContents[0] : null;
                    if(rootPattern == null) return;

                    processMemberAccessData(l, rootPattern, memberAccessData.size() > 0 ? ((TokenList) memberAccessData.get(0)) : null, memberAccessData.size() > 1 ? memberAccessData.get(1) : null);
                })
                .setEvaluator(
                        (p, d) -> parseAccessorChain(p, ((ISymbolContext) d[0]), (d.length > 1 && (boolean) d[1]))
                );
    }

    public static SummarySymbol getTypeSymbolFromConstraint(Lexer l, TokenPattern<?> pattern) {
        if(pattern == null) return null;
        return getTypeSymbolFromTypePattern(l, pattern.find("TYPE_CONSTRAINTS_WRAPPED.TYPE_CONSTRAINTS_INNER.TYPE_CONSTRAINTS_EXPLICIT.INTERPOLATION_TYPE"));
    }

    private static SummarySymbol getTypeSymbolFromTypePattern(Lexer l, TokenPattern<?> pattern) {
        if(pattern == null || !pattern.getName().equals("INTERPOLATION_TYPE") || !(pattern instanceof TokenStructure)) return null;
        TokenPattern<?> inner = ((TokenStructure) pattern).getContents();

        if(inner != null) {
            if("INTERPOLATION_TYPE_CHAIN_VALIDATION".equals(inner.getName())) inner = ((TokenGroup) inner).getContents()[0];

            switch(inner.getName()) {
                case "PRIMITIVE_ROOT_TYPE": {
                    //todo
                    break;
                }
                case "INTERPOLATION_CHAIN": {
                    if(inner.find("MEMBER_ACCESSES") != null) {
                        //not dealing with this
                        return null;
                    }
                    String typeName = inner.find("ROOT_INTERPOLATION_TYPE").flatten(false);
                    return ((TridentSummaryModule) l.getSummaryModule()).getSymbolForName(typeName, inner.getStringLocation().index);
                }
            }
        }
        return null;
    }

    private static void processMemberAccessData(Lexer l, TokenPattern<?> root, TokenList memberAccesses, TokenPattern<?> failingAccess) {
        if(memberAccesses == null || (memberAccesses.getContents().length == 0 && failingAccess == null)) return;
        if(l.getSuggestionModule() == null || l.getSummaryModule() == null) return;

        StringLocation start = root.getStringLocation();
        StringLocation end = failingAccess != null ? failingAccess.getStringBounds().end : memberAccesses.getStringBounds().end;
        int suggestionIndex = l.getSuggestionModule().getSuggestionIndex();

        if(suggestionIndex >= start.index && suggestionIndex <= end.index) {
            Debug.log("IN CHAIN, READY TO SUGGEST");

            TokenPattern<?>[] memberAccessesArr = memberAccesses.getContents();

            SummarySymbol symbol = null;

            Path filePath = root.getFile().toPath();

            for(int i = 0; i < memberAccessesArr.length + 2; i++) {
                TokenPattern<?> memberAccess = i == 0 ? root : (i == memberAccessesArr.length+1 ? failingAccess : memberAccessesArr[i-1]);
                if(memberAccess == null) continue;
                if(memberAccess.getStringBounds().start.index > suggestionIndex) break;
                boolean isLastAccess = memberAccess.getStringBounds().end.index >= suggestionIndex;

                Debug.log(memberAccess.flatten(false));
                Debug.log("isLastAccess: " + isLastAccess);

                if(i == 0) {
                    switch(((TokenStructure) memberAccess).getContents().getName()) {
                        case "VARIABLE_NAME": {
                            symbol = ((TridentSummaryModule) l.getSummaryModule()).getSymbolForName(memberAccess.flatten(false), start.index);
                            Debug.log(symbol);
                            if(symbol == null) return;
                            break;
                        }
                        case "CONSTRUCTOR_CALL": {
                            SummarySymbol typeSymbol = getTypeSymbolFromTypePattern(l, memberAccess.find("INTERPOLATION_TYPE"));
                            if(typeSymbol == null) {
                                return;
                            }
                            symbol = new SummarySymbol(((TridentSummaryModule) l.getSummaryModule()), "", TridentSymbolVisibility.LOCAL, memberAccess.getStringLocation().index);
                            symbol.setType(typeSymbol);
                            break;
                        }
                        default: {
                            return;
                        }
                    }
                } else {
                    if(i <= memberAccessesArr.length) {
                        memberAccess = ((TokenStructure) memberAccess).getContents();
                    }
                    switch(memberAccess.getName()) {
                        case "MEMBER_KEY": {
                            if(isLastAccess) {
                                Debug.log("symbols to suggest: ");
                                for(SummarySymbol subSymbol : symbol.getSubSymbols(filePath, start.index)) {
                                    Debug.log(subSymbol);
                                    l.getSuggestionModule().addSuggestion(new LiteralSuggestion(subSymbol.getName()));
                                }
                                Debug.log("end of symbols");
                            } else {
                                TokenPattern<?> memberName = memberAccess.find("SYMBOL_NAME");
                                boolean looped = false;
                                if(memberName != null) {
                                    for(SummarySymbol subSymbol : symbol.getSubSymbolsByName(memberName.flatten(false), filePath, start.index)) {
                                        symbol = subSymbol;
                                        looped = true;
                                    }
                                }
                                if(!looped) {
                                    Debug.log("Abandoning, found no sub symbols: " + (memberName != null ? memberName.flatten(false) : "null"));
                                    return;
                                }
                            }
                            break;
                        }
                        case "METHOD_CALL": {
                            StringBounds parameterBounds = memberAccess.getStringBounds();
                            if(suggestionIndex >= parameterBounds.start.index && suggestionIndex <= parameterBounds.end.index) {
                                Debug.log("Abandoning, inside method call parameters");
                                return;
                            }
                            SummarySymbol returnType = symbol.getReturnType();
                            if(returnType == null) {
                                Debug.log("Abandoning, no known return type");
                                return;
                            }
                            symbol = new SummarySymbol(((TridentSummaryModule) l.getSummaryModule()), "", TridentSymbolVisibility.LOCAL, parameterBounds.start.index);
                            symbol.setType(returnType);
                            break;
                        }
                        case "MEMBER_INDEX": {
                            StringBounds parameterBounds = memberAccess.getStringBounds();
                            if(suggestionIndex >= parameterBounds.start.index && suggestionIndex <= parameterBounds.end.index) {
                                Debug.log("Abandoning, inside indexer");
                                return;
                            }
                            break;
                        }
                        default: {
                            break;
                        }
                    }
                }
            }
            /*if(l.getSummaryModule() != null) {
                ArrayList<String> memberPath = ((TridentSummaryModule) l.getSummaryModule()).getTempMemberAccessList();
                l.getSuggestionModule().setLookingAtMemberPath(memberPath.toArray(new String[0]));
                l.getSuggestionModule().addSuggestion(new ComplexSuggestion(TridentSuggestionTags.IDENTIFIER_MEMBER));
            }*/
            Debug.log(symbol);
            Debug.log("end");
        }
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

                if(parent instanceof ClassMethodFamily.ClassMethodSymbol) {
                    parent = ctx.getTypeSystem().sanitizeObject(((ClassMethodFamily.ClassMethodSymbol) parent).safeCall(firstAccessorParameters[0].getValues().toArray(), firstAccessorParameters[0].getPatterns().toArray(new TokenPattern<?>[0]), accessors[0], ctx));
                    toBlame = accessors[0];
                } else if (parent instanceof PrimitivePrismarineFunction) {
                    parent = ctx.getTypeSystem().sanitizeObject(((PrimitivePrismarineFunction) parent).safeCall(firstAccessorParameters[0].getValues().toArray(), firstAccessorParameters[0].getPatterns().toArray(new TokenPattern<?>[0]), accessors[0], ctx, null));
                    toBlame = accessors[0];
                } else {
                    throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "This is not a function", toBlame, ctx);
                }
            }

            for (int i = firstAccessorParameters[0] == null ? 0 : 1; i < accessors.length; i++) {
                TokenPattern<?> accessor = accessors[i];
                EObject.assertNotNull(parent, toBlame, ctx);

                if(parent instanceof ParameterizedMemberHolder && i+1 < accessors.length && accessor.getName().equals("MEMBER_KEY") && accessors[i+1].getName().equals("METHOD_CALL")) {
                    ActualParameterList paramList = parseActualParameterList(accessors[i+1], ctx);

                    Object member = ((ParameterizedMemberHolder) parent).getMemberForParameters(accessor.find("SYMBOL_NAME").flatten(false), accessor, paramList, ctx, false);

                    EObject.assertNotNull(member, toBlame, ctx);

                    if(member instanceof ClassMethodFamily.ClassMethodSymbol) {
                        parent = ctx.getTypeSystem().sanitizeObject(((ClassMethodFamily.ClassMethodSymbol) member).safeCall(paramList.getValues().toArray(), paramList.getPatterns().toArray(new TokenPattern<?>[0]), accessors[i+1], ctx));
                        toBlame = accessor;
                    } else if (member instanceof PrimitivePrismarineFunction) {
                        parent = ctx.getTypeSystem().sanitizeObject(((PrimitivePrismarineFunction) member).safeCall(paramList.getValues().toArray(), paramList.getPatterns().toArray(new TokenPattern<?>[0]), accessors[i+1], ctx, null));
                        toBlame = accessor;
                    } else {
                        throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "This is not a function", toBlame, ctx);
                    }

                    i++;
                    continue;
                }

                parent = parseAccessor(parent, toBlame, accessor, ctx, keepSymbol && (i == accessors.length-1));
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

                    return ctx.getTypeSystem().sanitizeObject(((PrimitivePrismarineFunction) parent).safeCall(paramList.getValues().toArray(), paramList.getPatterns().toArray(new TokenPattern<?>[0]), accessorPattern, ctx, null));
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
        return keepSymbol || symbol instanceof ClassMethodFamily.ClassMethodSymbol ? symbol : ctx.getTypeSystem().sanitizeObject(symbol.getValue(pattern, ctx));
    }

    private static ActualParameterList parseActualParameterList(TokenPattern<?> pattern, ISymbolContext ctx) {
        TokenList paramList = (TokenList) pattern.find("PARAMETERS");
        if(paramList == null) return new ActualParameterList(Collections.emptyList(), Collections.emptyList(), pattern);
        ArrayList<Object> params = new ArrayList<>();
        ArrayList<TokenPattern<?>> patterns = new ArrayList<>();
        for (TokenPattern<?> rawParam : paramList.getContents()) {
            if (rawParam.getName().equals("INTERPOLATION_VALUE")) {
                params.add(rawParam.evaluate(ctx));
                patterns.add(rawParam);
            }
        }
        return new ActualParameterList(params, patterns, pattern);
    }


    public static PreBlockDeclaration addPreBlockDeclaration(TokenPattern<?> declarationPattern) {
        PreBlockDeclaration declaration = new PreBlockDeclaration(declarationPattern);
        preBlockDeclarations.add(declaration);
        return declaration;
    }

    public static PreBlockDeclaration addPreBlockDeclaration(TokenPattern<?> declarationPattern, TokenPattern<?> constraintsPattern) {
        PreBlockDeclaration declaration = new PreBlockDeclaration(declarationPattern, constraintsPattern);
        preBlockDeclarations.add(declaration);
        return declaration;
    }

    public static class PreBlockDeclaration {
        public TokenPattern<?> declarationPattern;
        public TokenPattern<?> constraintsPattern;
        public String[] tags = null;

        public PreBlockDeclaration(TokenPattern<?> declarationPattern) {
            this(declarationPattern, null);
        }

        public PreBlockDeclaration(TokenPattern<?> declarationPattern, TokenPattern<?> constraintsPattern) {
            this.declarationPattern = declarationPattern;
            this.constraintsPattern = constraintsPattern;
        }

        public PreBlockDeclaration setTags(String[] tags) {
            this.tags = tags;
            return this;
        }
    }

    private static class ValueChainConfiguration {
        boolean memberAccess = true;
        boolean indexAccess = true;
        boolean callAccess = true;

        boolean tail = true;
    }

    private static final ValueChainConfiguration NORMAL_VALUE_CHAIN_CONFIG = new ValueChainConfiguration();
    private static final ValueChainConfiguration TYPE_CHAIN_CONFIG = new ValueChainConfiguration() {{indexAccess = callAccess = tail = false;}};
}
