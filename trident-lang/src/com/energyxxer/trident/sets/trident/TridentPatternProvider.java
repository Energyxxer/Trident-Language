package com.energyxxer.trident.sets.trident;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteCommand;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.commodore.functionlogic.functions.FunctionSection;
import com.energyxxer.enxlex.lexical_analysis.Lexer;
import com.energyxxer.enxlex.lexical_analysis.token.Token;
import com.energyxxer.enxlex.lexical_analysis.token.TokenSection;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenGroupMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenItemMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenStructureMatch;
import com.energyxxer.enxlex.pattern_matching.structures.*;
import com.energyxxer.enxlex.suggestions.SuggestionTags;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.providers.PatternProviderSet;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.summaries.PrismarineSummaryModule;
import com.energyxxer.prismarine.summaries.SummarySymbol;
import com.energyxxer.prismarine.symbols.SymbolVisibility;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.typesystem.TypeConstraints;
import com.energyxxer.prismarine.typesystem.TypeHandler;
import com.energyxxer.trident.TridentFileUnitConfiguration;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.lexer.TridentSuggestionTags;
import com.energyxxer.trident.compiler.lexer.summaries.TridentProjectSummary;
import com.energyxxer.trident.compiler.lexer.summaries.TridentSummaryModule;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.sets.ValueAccessExpressionSet;
import com.energyxxer.trident.worker.tasks.SetupWritingStackTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;

import static com.energyxxer.prismarine.PrismarineProductions.*;
import static com.energyxxer.trident.compiler.TridentProductions.*;
import static com.energyxxer.trident.compiler.lexer.TridentTokens.*;

public class TridentPatternProvider extends PatternProviderSet {

    public TridentPatternProvider() {
        super(null);
    }

    @Override
    protected void installUtilityProductions(PrismarineProductions productions, TokenStructureMatch providerStructure) {
        ValueAccessExpressionSet vae = productions.getProviderSet(ValueAccessExpressionSet.class);
        
        productions.getOrCreateStructure("COMMENT").add(new TokenItemMatch(COMMENT).setName("COMMENT").addProcessor(
                (p, l) -> {
                    if (l.getSummaryModule() != null) {
                        Token token = ((TokenItem) p).getContents();
                        if(token.getSubSections() != null) {
                            for (Map.Entry<TokenSection, String> entry : token.getSubSections().entrySet()) {
                                if(entry.getValue().equals("comment.todo")) {
                                    ((TridentSummaryModule) l.getSummaryModule()).addTodo(token, token.value.substring(entry.getKey().start, entry.getKey().start + entry.getKey().length));
                                }
                            }
                        }
                    }
                }
        ).setEvaluator((p, d) -> p.flatten(false).substring(1)));

        productions.getOrCreateStructure("ENTRY")
                .add(
                        productions.getOrCreateStructure("COMMENT")
                )
                .add(
                        productions.getOrCreateStructure("INSTRUCTION")
                )
                .add(
                        group(productions.getOrCreateStructure("MODIFIER_LIST"), literal("run").setOptional(), productions.getOrCreateStructure("COMMAND")).setName("COMMAND_WRAPPER")
                        .setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            TridentFile tridentFile = ((TridentFile) ctx.getStaticParentUnit());
                            TridentFile writingFile = ctx.get(SetupWritingStackTask.INSTANCE).getWritingFile();

                            FunctionSection appendTo = ((FunctionSection) d[1]);
                            if(appendTo != null) {
                                ArrayList<ExecuteModifier> modifiers = (ArrayList<ExecuteModifier>) p.findThenEvaluateLazyDefault("MODIFIER_LIST", ArrayList::new, ctx);
                                Object commands = p.find("COMMAND").evaluate((ISymbolContext) ctx, modifiers);

                                ArrayList<ExecuteModifier> modifiersForCommand = modifiers;
                                if(!writingFile.getWritingModifiers().isEmpty()) {
                                    modifiersForCommand = new ArrayList<>(writingFile.getWritingModifiers());
                                    modifiersForCommand.addAll(modifiers);
                                }

                                if(commands instanceof Command) {
                                    if (modifiersForCommand.isEmpty()) appendTo.append(((Command) commands));
                                    else appendTo.append(new ExecuteCommand(((Command) commands), modifiersForCommand));
                                } else {
                                    for(Command command : ((Collection<Command>) commands)) {
                                        if (modifiersForCommand.isEmpty()) appendTo.append(command);
                                        else appendTo.append(new ExecuteCommand(command, modifiersForCommand));
                                    }
                                }
                            } else if(!tridentFile.reportedNoCommands) {
                                tridentFile.reportedNoCommands = true;
                                throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "A compile-only function may not have commands", p, ctx);
                            }
                            return null;
                        })
                )
                .addTags(SuggestionTags.ENABLED)
                .addTags(SuggestionTags.DISABLED_INDEX)
                .addTags(TridentSuggestionTags.CONTEXT_ENTRY);

        TokenPatternMatch TYPE_CONSTRAINTS_EXPLICIT = group(
                productions.getOrCreateStructure("INTERPOLATION_TYPE"),
                TridentProductions.symbol("?").setOptional().setName("VARIABLE_NULLABLE")
        ).setName("TYPE_CONSTRAINTS_EXPLICIT").setEvaluator((p, d) -> {
            ISymbolContext ctx = (ISymbolContext) d[0];
            TypeHandler type = (TypeHandler) p.find("INTERPOLATION_TYPE").evaluate(ctx);
            boolean nullable = p.find("VARIABLE_NULLABLE") != null;
            return new TypeConstraints(ctx.getTypeSystem(), type, nullable);
        });

        TokenPatternMatch TYPE_CONSTRAINTS = group(
                optional(
                        TridentProductions.colon(),
                        wrapper(TYPE_CONSTRAINTS_EXPLICIT).setName("TYPE_CONSTRAINTS_INNER")
                ).setName("TYPE_CONSTRAINTS_WRAPPED").setSimplificationFunctionContentIndex(1)
        ).setName("TYPE_CONSTRAINTS")
                .setSimplificationFunction(d -> d.pattern = d.pattern.tryFind("TYPE_CONSTRAINTS_WRAPPED"))
                .setEvaluator((p, d) -> new TypeConstraints(((ISymbolContext) d[0]).getTypeSystem(), (TypeHandler<?>) null, true));

        TokenPatternMatch INFERRABLE_TYPE_CONSTRAINTS = group(
                optional(
                        TridentProductions.colon(),
                        wrapperOptional(TYPE_CONSTRAINTS_EXPLICIT).setName("TYPE_CONSTRAINTS_INNER")
                ).setName("TYPE_CONSTRAINTS_WRAPPED")
                        .setSimplificationFunction(d -> d.pattern = d.pattern.tryFind("TYPE_CONSTRAINTS_INNER"))
                        .setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            Object value = d[1];
                            if(value == TypeConstraints.SpecialInferInstruction.NO_INSTANCE_INFER) {
                                throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Cannot infer type constraints for instance fields", p, ctx);
                            } else if(value != null) {
                                return new TypeConstraints(ctx.getTypeSystem(), ctx.getTypeSystem().getStaticHandlerForObject(value), true);
                            } else {
                                throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Cannot infer type constraints for null", p, ctx);
                            }
                        })
        ).setName("TYPE_CONSTRAINTS")
                .setSimplificationFunction(d -> d.pattern = d.pattern.tryFind("TYPE_CONSTRAINTS_WRAPPED"))
                .setEvaluator((p, d) -> new TypeConstraints(((ISymbolContext) d[0]).getTypeSystem(), (TypeHandler<?>) null, true));

        productions.putPatternMatch("TYPE_CONSTRAINTS", TYPE_CONSTRAINTS);
        productions.putPatternMatch("INFERRABLE_TYPE_CONSTRAINTS", INFERRABLE_TYPE_CONSTRAINTS);

        TokenPatternMatch FORMAL_PARAMETER = group(identifierX().setName("FORMAL_PARAMETER_NAME"), TYPE_CONSTRAINTS).setName("FORMAL_PARAMETER");

        TokenPatternMatch FORMAL_PARAMETERS = group(
                brace("("),
                list(FORMAL_PARAMETER, comma()).setOptional().setName("FORMAL_PARAMETER_LIST"),
                brace(")")
        ).setName("FORMAL_PARAMETERS");

        productions.putPatternMatch("FORMAL_PARAMETER", FORMAL_PARAMETER);
        productions.putPatternMatch("FORMAL_PARAMETERS", FORMAL_PARAMETERS);

        productions.getOrCreateStructure("FORMAL_TYPE_PARAMETERS").add(
                group(
                        brace("<"),
                        list(
                                group(identifierX().addProcessor((p, l) -> {
                                    if(l.getSummaryModule() != null) {
                                        ((TridentSummaryModule) l.getSummaryModule()).addPreBlockDeclaration(p);
                                    }
                                })),
                                comma()
                        ),
                        brace(">")
                ).setEvaluator((p, d) -> {
                    ISymbolContext ctx = (ISymbolContext) d[0];
                    TokenPattern<?>[] rawGenericTypes = ((TokenList) ((TokenGroup) p).getContents()[1]).getContentsExcludingSeparators();
                    String[] typeParamNames = new String[rawGenericTypes.length];
                    for(int i = 0; i < rawGenericTypes.length; i++) {
                        typeParamNames[i] = rawGenericTypes[i].flatten(false);
                    }
                    return typeParamNames;
                })
        );
        productions.getOrCreateStructure("ACTUAL_TYPE_PARAMETERS").add(
                group(
                        brace("<"),
                        list(
                                productions.getOrCreateStructure("INTERPOLATION_TYPE"),
                                comma()
                        ),
                        brace(">")
                ).setEvaluator((p, d) -> {
                    ISymbolContext ctx = (ISymbolContext) d[0];
                    TokenPattern<?>[] rawGenericTypes = ((TokenList) ((TokenGroup) p).getContents()[1]).getContentsExcludingSeparators();
                    TypeHandler<?>[] handlers = new TypeHandler<?>[rawGenericTypes.length];
                    for(int i = 0; i < rawGenericTypes.length; i++) {
                        handlers[i] = (TypeHandler<?>) rawGenericTypes[i].evaluate(ctx);
                    }
                    return handlers;
                })
        );

        TokenPatternMatch DYNAMIC_FUNCTION = group(
                noToken().addFailProcessor((a, l) -> startClosure.accept(null, l)),
                group(FORMAL_PARAMETERS, TYPE_CONSTRAINTS).setName("PRE_CODE_BLOCK").addProcessor((p, l) -> {
                        if(l.getSummaryModule() != null) {
                            TokenList paramList = (TokenList) p.find("FORMAL_PARAMETERS.FORMAL_PARAMETER_LIST");
                            if(paramList != null) {
                                for(TokenPattern<?> paramPattern : paramList.getContentsExcludingSeparators()) {
                                    ((TridentSummaryModule) l.getSummaryModule()).addPreBlockDeclaration(paramPattern.find("FORMAL_PARAMETER_NAME"), paramPattern.find("TYPE_CONSTRAINTS"));
                                }
                            }
                        }
                }),
                productions.getOrCreateStructure("ANONYMOUS_INNER_FUNCTION")
        ).setName("DYNAMIC_FUNCTION").addProcessor(endComplexValue).addFailProcessor((n, l) -> endComplexValue.accept(null, l));

        productions.getOrCreateStructure("DYNAMIC_FUNCTION").add(DYNAMIC_FUNCTION);



        productions.getOrCreateStructure("INTERPOLATION_CHAIN_TAIL")
                .add(
                        group(TridentProductions.sameLine(), TridentProductions.keyword("is"), productions.getOrCreateStructure("INTERPOLATION_TYPE")).setName("INTERPOLATION_CHAIN_TAIL_IS")
                        .setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            Object value = d[1];
                            TypeHandler type = (TypeHandler<?>) p.find("INTERPOLATION_TYPE").evaluate(ctx);
                            return type.isInstance(value);
                        })
                )
                .add(
                        group(TridentProductions.sameLine(), TridentProductions.keyword("as"), productions.getOrCreateStructure("INTERPOLATION_TYPE")).setName("INTERPOLATION_CHAIN_TAIL_AS")
                        .setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            Object value = d[1];
                            TypeHandler type = (TypeHandler<?>) p.find("INTERPOLATION_TYPE").evaluate(ctx);
                            return ctx.getTypeSystem().castOrCoerce(value, type, p, ctx, false);
                        })
                );




        TokenPatternMatch DIRECTIVE = group(ofType(DIRECTIVE_HEADER), choice(
                group(literal("on").setName("DIRECTIVE_LABEL"), literal("compile")
                        .addProcessor((p, l) -> {
                            if(l.getSummaryModule() != null) {
                                ((TridentSummaryModule) l.getSummaryModule()).setCompileOnly();
                            }
                        })).setName("ON_DIRECTIVE"),
                group(literal("tag").setName("DIRECTIVE_LABEL"), TridentProductions.resourceLocationFixer, ofType(RESOURCE_LOCATION).addTags(TridentSuggestionTags.RESOURCE)
                        .addProcessor((p, l) -> {
                            if(l.getSummaryModule() != null) {
                                ((TridentSummaryModule) l.getSummaryModule()).addFunctionTag(new ResourceLocation(p.flatten(false)));
                            }
                        })).setName("TAG_DIRECTIVE"),
                group(literal("require").setName("DIRECTIVE_LABEL"), TridentProductions.resourceLocationFixer, ofType(RESOURCE_LOCATION).addTags(TridentSuggestionTags.RESOURCE, TridentSuggestionTags.TRIDENT_FUNCTION)
                        .addProcessor((p, l) -> {
                            if(l.getSummaryModule() != null) {
                                ((TridentSummaryModule) l.getSummaryModule()).addRequires(TridentFileUnitConfiguration.resourceLocationToFunctionPath(new ResourceLocation(p.flatten(false))));
                            }
                        })).setName("REQUIRE_DIRECTIVE"),
                group(literal("meta_tag").setName("DIRECTIVE_LABEL"), ofType(RESOURCE_LOCATION).addTags(TridentSuggestionTags.RESOURCE)).setName("META_TAG_DIRECTIVE"),
                group(literal("priority").setName("DIRECTIVE_LABEL"), TridentProductions.real(productions)).setName("PRIORITY_DIRECTIVE"),
                group(literal("breaking").setName("DIRECTIVE_LABEL")).setName("BREAKING_DIRECTIVE"),
                group(literal("language_level").setName("DIRECTIVE_LABEL"), TridentProductions.integer(productions)).setName("LANGUAGE_LEVEL_DIRECTIVE"),
                group(literal("metadata").setName("DIRECTIVE_LABEL"), productions.getOrCreateStructure("DICTIONARY")).setName("METADATA_DIRECTIVE"),
                group(literal("using_plugin").setName("DIRECTIVE_LABEL"), group(productions.getOrCreateStructure("PLUGIN_NAME")).addProcessor((p, lx) -> {
                    TridentProductions.importPlugin(productions, p.flatten(false), p, lx);
                })).setName("USING_PLUGIN_DIRECTIVE")
        )).setSimplificationFunctionContentIndex(1).addTags(SuggestionTags.ENABLED);





        productions.getOrCreateStructure("INNER_FUNCTION").add(
                group(
                        group(
                                productions.getOrCreateStructure("RESOURCE_LOCATION")
                        ).setName("INNER_FUNCTION_NAME"),
                        brace("{").addProcessor(TridentSummaryModule.CAPTURE_PRE_BLOCK_DECLARATIONS),
                        productions.getOrCreateStructure("FILE_INNER"),
                        brace("}")
                ).setGreedy(true).addProcessor(surroundBlock)
        );
        productions.getOrCreateStructure("ANONYMOUS_INNER_FUNCTION").add(
                group(
                        brace("{").addProcessor(TridentSummaryModule.CAPTURE_PRE_BLOCK_DECLARATIONS),
                        productions.getOrCreateStructure("FILE_INNER"),
                        brace("}")
                ).setGreedy(true).addProcessor(surroundBlock)
        );
        productions.getOrCreateStructure("OPTIONAL_NAME_INNER_FUNCTION").add(
                group(
                        group(
                                productions.getOrCreateStructure("RESOURCE_LOCATION")
                        ).setOptional().setName("INNER_FUNCTION_NAME"),
                        brace("{").addProcessor(TridentSummaryModule.CAPTURE_PRE_BLOCK_DECLARATIONS),
                        productions.getOrCreateStructure("FILE_INNER"),
                        brace("}")
                ).setGreedy(true).addProcessor(surroundBlock)
        );







        productions.getOrCreateStructure("FILE_INNER").add(
                group(
                        list(DIRECTIVE).setOptional().setName("DIRECTIVES"),
                        list(productions.getOrCreateStructure("ENTRY")).setOptional().setName("ENTRIES")
                ).setGreedy(true)
        );

        productions.getOrCreateStructure("COMMAND").setEvaluator((p, d) -> {
            TokenPattern<?> inner = ((TokenStructure) p).getContents();
            try {
                return inner.evaluate(d);
            } catch(CommodoreException x) {
                if(x.getSource() == CommodoreException.Source.VERSION_ERROR) {
                    throw new PrismarineException(TridentExceptionUtil.Source.COMMAND_ERROR, x.getSource() + ": " + x.getMessage(), inner, (ISymbolContext) d[0]);
                } else {
                    throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Commodore Exception of type " + x.getSource() + ": " + x.getMessage(), inner, (ISymbolContext) d[0]);
                }
            }
        });



        BiConsumer<TokenPattern<?>, Lexer> uninstallCommands = (p, lx) -> TridentProductions.uninstallCommands(productions);
        productions.addFileGroup(
                (TokenGroupMatch) group(
                        list(DIRECTIVE).setOptional().setName("DIRECTIVES").addProcessor((p, lx) -> {
                            if(lx.getSummaryModule() != null) {
                                ((TridentSummaryModule) lx.getSummaryModule()).lockDirectives();
                            }
                        }),
                        list(productions.getOrCreateStructure("ENTRY")).setOptional().setName("ENTRIES")
                ).setGreedy(true).addProcessor(uninstallCommands).addFailProcessor(uninstallCommands).addProcessor(TridentPatternProvider::addFileSymbols).addFailProcessor(TridentPatternProvider::addFileSymbols)
        );
    }

    private static void addFileSymbols(TokenPattern<?> p, Lexer l) {
        if (l.getSummaryModule() != null) {
            ((PrismarineSummaryModule) l.getSummaryModule()).addFileAwareProcessor(TridentProjectSummary.PASS_FILE_SYMBOLS, f -> {
                if (f.getFileLocation() == null) {
                    SummarySymbol argsSym = new SummarySymbol(
                            f, "args", SymbolVisibility.PUBLIC, 0
                    );
                    if (f.getParentSummary() != null) {
                        argsSym.setType(((TridentProjectSummary) f.getParentSummary()).getPrimitiveSymbol("dictionary"));
                    }
                    f.addElement(argsSym);
                }
            });
        }
    }
}
