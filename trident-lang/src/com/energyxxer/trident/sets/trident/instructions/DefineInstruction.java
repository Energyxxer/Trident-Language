package com.energyxxer.trident.sets.trident.instructions;

import com.energyxxer.commodore.textcomponents.TextComponent;
import com.energyxxer.enxlex.lexical_analysis.inspections.ReplacementInspectionAction;
import com.energyxxer.enxlex.lexical_analysis.inspections.SuggestionInspection;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenStructureMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenGroup;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.operators.Operator;
import com.energyxxer.prismarine.operators.OperatorPool;
import com.energyxxer.prismarine.operators.TernaryOperator;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.summaries.PrismarineSummaryModule;
import com.energyxxer.prismarine.summaries.SummarySymbol;
import com.energyxxer.prismarine.symbols.SymbolVisibility;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.lexer.TridentSuggestionTags;
import com.energyxxer.trident.compiler.lexer.summaries.TridentSummaryModule;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClass;
import com.energyxxer.trident.compiler.semantics.custom.entities.CustomEntity;
import com.energyxxer.trident.compiler.semantics.custom.entities.EntityEvent;
import com.energyxxer.trident.compiler.semantics.custom.items.CustomItem;
import com.energyxxer.trident.compiler.semantics.symbols.TridentSymbolVisibility;
import com.energyxxer.trident.sets.ValueAccessExpressionSet;
import com.energyxxer.trident.worker.tasks.SetupModuleTask;
import com.energyxxer.util.StringBounds;
import com.energyxxer.util.logger.Debug;

import java.util.ArrayList;

import static com.energyxxer.prismarine.PrismarineProductions.*;
import static com.energyxxer.trident.compiler.TridentProductions.*;
import static com.energyxxer.trident.compiler.lexer.TridentTokens.COMPILER_OPERATOR;

public class DefineInstruction implements InstructionDefinition {

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        ValueAccessExpressionSet vae = productions.getProviderSet(ValueAccessExpressionSet.class);

        OperatorPool operatorPool = productions.unitConfig.getOperatorPool();

        TokenPatternMatch SYMBOL_MODIFIER_LIST = list(choice("static", "final")).setOptional().setName("SYMBOL_MODIFIER_LIST").addProcessor(
                (p, lx) -> checkDuplicates(((TokenList) p), "Duplicate modifier", lx)
        );

        TokenStructureMatch entityBodyEntry = choice(
                group(literal("default"), literal("nbt"), productions.getOrCreateStructure("NBT_COMPOUND")).setName("DEFAULT_NBT"),
                group(literal("default"), literal("passengers"), brace("["), list(productions.getOrCreateStructure("NEW_ENTITY_LITERAL"), comma()).setName("PASSENGER_LIST"), brace("]")).setName("DEFAULT_PASSENGERS"),
                group(literal("default"), literal("health"), real(productions).setName("HEALTH").addTags("cspn:Health")).setName("DEFAULT_HEALTH"),
                group(literal("default"), literal("name"), productions.getOrCreateStructure("TEXT_COMPONENT")).setName("DEFAULT_NAME"),
                group(literal("var"), SYMBOL_MODIFIER_LIST, identifierX().setName("SYMBOL_NAME").addTags("cspn:Field Name"), productions.getPatternMatch("INFERRABLE_TYPE_CONSTRAINTS"), optional(TridentProductions.equals(), choice(productions.getOrCreateStructure("LINE_SAFE_INTERPOLATION_VALUE"), productions.getOrCreateStructure("INTERPOLATION_BLOCK")).setName("INITIAL_VALUE")).setName("SYMBOL_INITIALIZATION")).setName("ENTITY_FIELD"),
                group(literal("eval"), productions.getOrCreateStructure("LINE_SAFE_INTERPOLATION_VALUE")).setName("ENTITY_EVAL"),
                group(literal("on"), PrismarineTypeSystem.validatorGroup(productions.getOrCreateStructure("INTERPOLATION_VALUE"), false, EntityEvent.class).setName("EVENT_NAME"), group(productions.getOrCreateStructure("MODIFIER_LIST")).setName("EVENT_MODIFIERS").setEvaluator((p, d) -> p.findThenEvaluateLazyDefault("MODIFIER_LIST", ArrayList::new, d)), literal("function"),
                        productions.getOrCreateStructure("OPTIONAL_NAME_INNER_FUNCTION")).setName("ENTITY_EVENT_IMPLEMENTATION"),
                productions.getOrCreateStructure("COMMENT"),
                group(choice(group(literal("ticking"), wrapperOptional(productions.getOrCreateStructure("TIME")).setName("TICKING_INTERVAL"), wrapperOptional(productions.getOrCreateStructure("MODIFIER_LIST")).setName("TICKING_MODIFIERS")).setName("TICKING_ENTITY_FUNCTION")).setOptional().setName("ENTITY_FUNCTION_MODIFIER"), literal("function"), productions.getOrCreateStructure("OPTIONAL_NAME_INNER_FUNCTION")).setName("ENTITY_INNER_FUNCTION")
        );
        entityBodyEntry.addTags(TridentSuggestionTags.CONTEXT_ENTITY_BODY);

        TokenPatternMatch entityBody = group(
                brace("{"),
                list(entityBodyEntry).setOptional().setName("ENTITY_BODY_ENTRIES"),
                brace("}")
        ).setOptional().setName("ENTITY_DECLARATION_BODY");


        TokenStructureMatch itemBodyEntry = choice(
                group(literal("default"), literal("nbt"), productions.getOrCreateStructure("NBT_COMPOUND")).setName("DEFAULT_NBT"),
                group(
                        choice(
                                group(literal("on"), choice(
                                        group(choice("used", "broken", "crafted", "dropped", "picked_up").setName("ITEM_CRITERIA_KEY")).setName("ITEM_CRITERIA")
                                ).setName("FUNCTION_ON_INNER"), literal("pure").setOptional(), group(productions.getOrCreateStructure("MODIFIER_LIST")).setEvaluator((p, d) -> p.findThenEvaluateLazyDefault("MODIFIER_LIST", ArrayList::new, d)).setName("EVENT_MODIFIERS")).setName("FUNCTION_ON")
                        ).setOptional().setName("INNER_FUNCTION_MODIFIERS"),
                        literal("function"),
                        productions.getOrCreateStructure("OPTIONAL_NAME_INNER_FUNCTION")).setName("ITEM_INNER_FUNCTION"),
                group(literal("default"), literal("name"), productions.getOrCreateStructure("TEXT_COMPONENT")).setName("DEFAULT_NAME"),
                group(literal("default"), literal("lore"), brace("["), list(productions.getOrCreateStructure("TEXT_COMPONENT"), comma()).setOptional().setName("LORE_LIST"), brace("]")).setName("DEFAULT_LORE"),
                productions.getOrCreateStructure("COMMENT"),
                group(literal("var"), SYMBOL_MODIFIER_LIST, identifierX().setName("SYMBOL_NAME").addTags("cspn:Field Name"), productions.getPatternMatch("INFERRABLE_TYPE_CONSTRAINTS"), optional(TridentProductions.equals(), choice(productions.getOrCreateStructure("LINE_SAFE_INTERPOLATION_VALUE"), productions.getOrCreateStructure("INTERPOLATION_BLOCK")).setName("INITIAL_VALUE")).setName("SYMBOL_INITIALIZATION")).setName("ITEM_FIELD"),
                group(literal("eval"), productions.getOrCreateStructure("LINE_SAFE_INTERPOLATION_VALUE")).setName("ITEM_EVAL")
        );
        itemBodyEntry.addTags(TridentSuggestionTags.CONTEXT_ITEM_BODY);

        TokenPatternMatch itemBody = group(
                brace("{"),
                list(itemBodyEntry).setOptional().setName("ITEM_BODY_ENTRIES"),
                brace("}")
        ).setOptional().setName("ITEM_DECLARATION_BODY");

        TokenPatternMatch classGetter = group(choice("public", "local", "private").setName("SYMBOL_VISIBILITY").setOptional(), literal("get"), productions.getPatternMatch("TYPE_CONSTRAINTS"), productions.getOrCreateStructure("ANONYMOUS_INNER_FUNCTION")).setName("CLASS_GETTER");
        TokenPatternMatch classSetter = group(choice("public", "local", "private").setName("SYMBOL_VISIBILITY").setOptional(), literal("set"), brace("("), productions.getPatternMatch("FORMAL_PARAMETER"), brace(")"), productions.getOrCreateStructure("ANONYMOUS_INNER_FUNCTION")).setName("CLASS_SETTER").setOptional();

        TokenStructureMatch classBodyEntry = choice(
                group(
                        choice("public", "local", "private").setName("SYMBOL_VISIBILITY").setOptional(),
                        SYMBOL_MODIFIER_LIST, literal("override").setOptional().setName("MEMBER_PARENT_MODE"),
                        literal("var"), identifierX().setName("SYMBOL_NAME").addProcessor((p, l) -> {
                            if(l.getSummaryModule() != null) {
                                SummarySymbol sym = new SummarySymbol((TridentSummaryModule) l.getSummaryModule(), p.flatten(false), TridentSymbolVisibility.LOCAL, p.getStringLocation().index);
                                ((TridentSummaryModule) l.getSummaryModule()).addSymbolUsage(p);
                                sym.setDeclarationPattern(p);
                                //sym.addUsage(p);
                                ((TridentSummaryModule) l.getSummaryModule()).pushSubSymbol(sym);
                            }
                        }),
                        productions.getPatternMatch("INFERRABLE_TYPE_CONSTRAINTS"),
                        optional(TridentProductions.equals(), wrapper(productions.getOrCreateStructure("INTERPOLATION_VALUE")).setName("INITIAL_VALUE")).setName("SYMBOL_INITIALIZATION")
                ).setName("CLASS_MEMBER")
                        .addFailProcessor((ip, l) -> {
                            if(l.getSummaryModule() != null && ip.find("SYMBOL_NAME") != null) {
                                //Remove the symbol that was pushed by the name
                                String symbolName = ip.find("SYMBOL_NAME").flatten(false);
                                SummarySymbol peekingSymbol = ((TridentSummaryModule) l.getSummaryModule()).peekSubSymbol();
                                if(peekingSymbol == null || !peekingSymbol.getName().equals(symbolName)) {
                                    Debug.log("POPPING WRONG SYMBOL");
                                }
                                ((TridentSummaryModule) l.getSummaryModule()).popSubSymbol();
                            }
                        })
                        .addProcessor(
                                (p, lx) -> {
                                    if(p.find("MEMBER_PARENT_MODE") != null && p.find("SYMBOL_MODIFIER_LIST") != null && p.find("SYMBOL_MODIFIER_LIST").flatten(false).contains("static")) {
                                        lx.getNotices().add(new Notice(NoticeType.ERROR, "Cannot override a static member", p.find("MEMBER_PARENT_MODE")));
                                    }
                                }
                        ).addProcessor((p, l) -> {
                    if(l.getSummaryModule() != null) {
                        SummarySymbol sym = ((TridentSummaryModule) l.getSummaryModule()).popSubSymbol();
                        sym.setVisibility(parseVisibility(p.find("SYMBOL_VISIBILITY"), TridentSymbolVisibility.LOCAL));
                        sym.addTag(TridentSuggestionTags.TAG_FIELD);

                        ((PrismarineSummaryModule) l.getSummaryModule()).addFileAwareProcessor(s -> {
                            sym.setType(ValueAccessExpressionSet.getTypeSymbolFromConstraint(s, p.find("TYPE_CONSTRAINTS")));
                        });

                        if (p.find("SYMBOL_MODIFIER_LIST") == null || !p.find("SYMBOL_MODIFIER_LIST").flatten(false).contains("static")) {
                            sym.setInstanceField(true);
                        }
                        if(!sym.hasSubBlock()) {
                            ((TridentSummaryModule) l.getSummaryModule()).addElement(sym);
                        }
                        if(l.getInspectionModule() != null) {
                            if(p.find("TYPE_CONSTRAINTS.TYPE_CONSTRAINTS_WRAPPED.TYPE_CONSTRAINTS_INNER") == null && p.find("SYMBOL_INITIALIZATION.INITIAL_VALUE") != null) {
                                ((PrismarineSummaryModule) l.getSummaryModule()).addFileAwareProcessor(s -> {
                                    TokenPattern<?> contents = p.find("SYMBOL_INITIALIZATION.INITIAL_VALUE.INTERPOLATION_VALUE.MID_INTERPOLATION_VALUE");
                                    if(contents instanceof TokenGroup || contents instanceof TokenStructure) {
                                        SummarySymbol initSymbol = ValueAccessExpressionSet.getSymbolForChain(s, (TokenPattern<?>) contents.getContents());
                                        if (initSymbol != null && initSymbol.getType() != null) {
                                            int replacementStartIndex = p.find("SYMBOL_NAME").getStringBounds().end.index;
                                            int replacementEndIndex = p.find("SYMBOL_INITIALIZATION").getStringLocation().index;

                                            StringBounds bounds = p.getStringBounds();

                                            SuggestionInspection inspection = new SuggestionInspection("Constrain field to initialization type")
                                                    .setStartIndex(bounds.start.index)
                                                    .setEndIndex(bounds.end.index)
                                                    .addAction(
                                                            new ReplacementInspectionAction()
                                                            .setReplacementStartIndex(replacementStartIndex)
                                                            .setReplacementEndIndex(replacementEndIndex)
                                                            .setReplacementText(" : " + initSymbol.getType().getName() + " ")
                                                    );

                                            l.getInspectionModule().addInspection(inspection);
                                        }
                                    }
                                });
                            }
                        }
                    }
                }),
                group(
                        choice("public", "local", "private").setName("SYMBOL_VISIBILITY").setOptional(),
                        SYMBOL_MODIFIER_LIST, literal("override").setOptional().setName("MEMBER_PARENT_MODE"),
                        choice(
                                literal("new").setName("CONSTRUCTOR_LABEL"),
                                identifierX()
                        ).setName("SYMBOL_NAME"),
                        productions.getOrCreateStructure("DYNAMIC_FUNCTION")
                ).setName("CLASS_FUNCTION").addProcessor(
                        (p, lx) -> {
                            boolean overriding = p.find("MEMBER_PARENT_MODE") != null;
                            boolean _static = p.find("SYMBOL_MODIFIER_LIST") != null && p.find("SYMBOL_MODIFIER_LIST").flatten(false).contains("static");
                            if(overriding && _static) {
                                lx.getNotices().add(new Notice(NoticeType.ERROR, "Cannot override a static member", p.find("MEMBER_PARENT_MODE")));
                            }
                            if(p.find("SYMBOL_NAME").flatten(false).equals("new")) {
                                if(overriding) {
                                    lx.getNotices().add(new Notice(NoticeType.ERROR, "Cannot override a constructor", p.find("MEMBER_PARENT_MODE")));
                                }
                                if(_static) {
                                    lx.getNotices().add(new Notice(NoticeType.ERROR, "'static' modifier not allowed here", p.find("SYMBOL_MODIFIER_LIST")));
                                }
                            }
                        }
                ).addProcessor((p, l) -> {
                    if(l.getSummaryModule() != null) {
                        String methodName = p.find("SYMBOL_NAME").flatten(false);
                        if("new".equals(methodName)) return;
                        SummarySymbol sym = new SummarySymbol((TridentSummaryModule) l.getSummaryModule(), methodName, TridentSymbolVisibility.LOCAL, p.find("SYMBOL_NAME").getStringLocation().index);
                        ((TridentSummaryModule) l.getSummaryModule()).addSymbolUsage(p.find("SYMBOL_NAME"));
                        sym.setDeclarationPattern(p.find("SYMBOL_NAME"));
                        //sym.addUsage(p.find("SYMBOL_NAME"));
                        sym.setVisibility(parseVisibility(p.find("SYMBOL_VISIBILITY"), TridentSymbolVisibility.LOCAL));
                        sym.addTag(TridentSuggestionTags.TAG_METHOD);
                        if (p.find("SYMBOL_MODIFIER_LIST") == null || !p.find("SYMBOL_MODIFIER_LIST").flatten(false).contains("static")) {
                            sym.setInstanceField(true);
                        }
                        sym.setReturnType(ValueAccessExpressionSet.getTypeSymbolFromConstraint((PrismarineSummaryModule) l.getSummaryModule(), p.find("DYNAMIC_FUNCTION.PRE_CODE_BLOCK.TYPE_CONSTRAINTS")));
                        if(!sym.hasSubBlock()) {
                            ((TridentSummaryModule) l.getSummaryModule()).addElement(sym);
                        }
                    }
                }),
                group(
                        choice("public", "local", "private").setName("SYMBOL_VISIBILITY").setOptional(),
                        list(choice("final")).setOptional().setName("SYMBOL_MODIFIER_LIST"),
                        literal("override").setOptional().setName("MEMBER_PARENT_MODE"),
                        literal("this"),
                        brace("["),
                        productions.getPatternMatch("FORMAL_PARAMETER"),
                        brace("]"),
                        brace("{"),
                        classGetter,
                        classSetter,
                        brace("}")
                ).setName("CLASS_INDEXER"),
                group(
                        choice("public", "local", "private").setName("SYMBOL_VISIBILITY").setOptional(),
                        SYMBOL_MODIFIER_LIST, literal("override").setOptional().setName("MEMBER_PARENT_MODE"),
                        TridentProductions.identifierX().setName("SYMBOL_NAME").addProcessor((p, l) -> {
                            if(l.getSummaryModule() != null) {
                                SummarySymbol sym = new SummarySymbol((TridentSummaryModule) l.getSummaryModule(), p.flatten(false), TridentSymbolVisibility.LOCAL, p.getStringLocation().index);
                                ((TridentSummaryModule) l.getSummaryModule()).addSymbolUsage(p);
                                sym.setDeclarationPattern(p);
                                //sym.addUsage(p);
                                ((TridentSummaryModule) l.getSummaryModule()).pushSubSymbol(sym);
                            }
                        }),
                        brace("{"),
                        classGetter,
                        classSetter,
                        brace("}")
                ).setName("CLASS_PROPERTY"),
                group(literal("operator"), ofType(COMPILER_OPERATOR).setName("OPERATOR_SYMBOL"), productions.getOrCreateStructure("DYNAMIC_FUNCTION")).setName("CLASS_OPERATOR")
                        .addProcessor((p, l) -> {
                            String operatorSymbol = p.find("OPERATOR_SYMBOL").flatten(false);
                            TokenList formalParamList = ((TokenList) p.find("DYNAMIC_FUNCTION.PRE_CODE_BLOCK.FORMAL_PARAMETERS.FORMAL_PARAMETER_LIST"));
                            int operandCount = formalParamList != null ? (formalParamList.size() + 1) / 2 : 0;
                            Operator associatedOperator = null;
                            if(operandCount == 1) {
                                associatedOperator = operatorPool.getUnaryLeftOperatorForSymbol(operatorSymbol);
                                if(associatedOperator == null) {
                                    associatedOperator = operatorPool.getUnaryLeftOperatorForSymbol(operatorSymbol);
                                }
                            } else if(operandCount >= 2 && operandCount <= 3) {
                                associatedOperator = operatorPool.getBinaryOrTernaryOperatorForSymbol(operatorSymbol);
                                if((associatedOperator instanceof TernaryOperator) != (operandCount == 3)) {
                                    associatedOperator = null;
                                }
                            }
                            if(associatedOperator == null) {
                                l.getNotices().add(new Notice(NoticeType.ERROR, "There is no operator " + operatorSymbol + " for " + operandCount + " operands", p.find("DYNAMIC_FUNCTION.PRE_CODE_BLOCK")));
                            }
                        }),
                group(literal("override").setOptional(), choice("explicit", "implicit").setName("CLASS_TRANSFORM_TYPE"), brace("<"), productions.getOrCreateStructure("INTERPOLATION_TYPE"), brace(">"), productions.getOrCreateStructure("ANONYMOUS_INNER_FUNCTION")).setName("CLASS_OVERRIDE"),
                productions.getOrCreateStructure("COMMENT")
        ).setName("CLASS_BODY_ENTRY");
        classBodyEntry.addTags(TridentSuggestionTags.CONTEXT_CLASS_BODY);

        TokenPatternMatch classBody = group(
                brace("{").addProcessor(vae.capturePreBlockDeclarations).addProcessor(startComplexValue),
                list(classBodyEntry).setOptional().setName("CLASS_BODY_ENTRIES"),
                brace("}")
        ).setOptional().setName("CLASS_DECLARATION_BODY").addProcessor(claimTopSymbol).addProcessor(endComplexValue).addFailProcessor((ip, l) -> {if(ip != null && ip.getCharLength() > 0) endComplexValue.accept(null, l);});

        return group(instructionKeyword("define"),
                choice(
                        group(literal("objective"), group(identifierA(productions)).setName("OBJECTIVE_NAME").addTags("cspn:Objective Name"), optional(sameLine(), group(identifierB(productions)).setName("CRITERIA"), optional(productions.getOrCreateStructure("TEXT_COMPONENT")))).setName("DEFINE_OBJECTIVE")
                                .addProcessor((p, l) -> {
                                    if(l.getSummaryModule() != null) {
                                        ((TridentSummaryModule) l.getSummaryModule()).addObjective(new SummarySymbol((TridentSummaryModule) l.getSummaryModule(), p.find("OBJECTIVE_NAME").flatten(false), TridentSymbolVisibility.LOCAL, p.getStringLocation().index).addTag(TridentSuggestionTags.TAG_OBJECTIVE));
                                    }
                                }),
                        group(choice("global", "local", "private").setName("SYMBOL_VISIBILITY").setOptional(), literal("entity"), choice(
                                group(choice(identifierA(productions), identifierX().addTags("cspn:Entity Name"), literal("default")).setName("ENTITY_NAME").addTags("cspn:Entity Type Name"), choice(symbol("*"), productions.getOrCreateStructure("TRIDENT_ENTITY_ID_TAGGED")).setName("ENTITY_BASE").addTags("cspn:Base Type")).setName("CONCRETE_ENTITY_DECLARATION").addProcessor(
                                        (p, lx) -> {
                                            if("*".equals(p.find("ENTITY_BASE").flatten(false)) && !"default".equals(p.find("ENTITY_NAME").flatten(false))) {
                                                lx.getNotices().add(new Notice(NoticeType.ERROR, "The wildcard entity base may only be used on default entities", p.find("ENTITY_BASE")));
                                            }
                                        }
                                ),
                                group(literal("component"), choice(identifierA(productions), identifierX()).setName("ENTITY_NAME").addTags("cspn:Component Name")).setName("ABSTRACT_ENTITY_DECLARATION")
                        ).setName("ENTITY_DECLARATION_HEADER"), optional(keyword("implements"), wrapper(productions.getOrCreateStructure("COMPONENT_LIST_BRACELESS")).setName("COMPONENT_LIST").addTags("cspn:Implemented Components")).setName("IMPLEMENTED_COMPONENTS"), entityBody).setName("DEFINE_ENTITY")
                                .addProcessor((p, l) -> {
                                    if(l.getSummaryModule() != null) {
                                        TokenPattern<?> namePattern = p.find("ENTITY_DECLARATION_HEADER.ENTITY_NAME");
                                        if(namePattern.find("IDENTIFIER_A") != null) return;
                                        String name = namePattern.flatten(false);
                                        if(!name.equals("default")) {
                                            SummarySymbol sym = new SummarySymbol((TridentSummaryModule) l.getSummaryModule(), name, TridentSymbolVisibility.LOCAL, p.getStringLocation().index);
                                            ((TridentSummaryModule) l.getSummaryModule()).addSymbolUsage(p);
                                            sym.setDeclarationPattern(p);
                                            //sym.addUsage(p);
                                            sym.addTag(TridentSuggestionTags.TAG_VARIABLE);
                                            sym.addTag(TridentSuggestionTags.TAG_CUSTOM_ENTITY);
                                            sym.setVisibility(parseVisibility(p.find("SYMBOL_VISIBILITY"), SymbolVisibility.GLOBAL));
                                            if(p.find("ENTITY_DECLARATION_HEADER.LITERAL_COMPONENT") != null) sym.addTag(TridentSuggestionTags.TAG_ENTITY_COMPONENT);
                                            ((TridentSummaryModule) l.getSummaryModule()).addElement(sym);
                                        } else {
                                            if(p.find("IMPLEMENTED_COMPONENTS") != null) {
                                                l.getNotices().add(new Notice(NoticeType.ERROR, "Default entities may not implement components", p.find("IMPLEMENTED_COMPONENTS")));
                                            }

                                            TokenList body = (TokenList) p.find("ENTITY_DECLARATION_BODY.ENTITY_BODY_ENTRIES");
                                            if(body != null) {
                                                for(TokenPattern<?> entry : body.getContents()) {
                                                    if(((TokenStructure) entry).getContents().getName().startsWith("DEFAULT_")) {
                                                        l.getNotices().add(new Notice(NoticeType.ERROR, "Default properties are not allowed for default entities", entry));
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }),
                        group(choice("global", "local", "private").setName("SYMBOL_VISIBILITY").setOptional(), literal("event"), group(identifierX().addTags("cspn:Event Name")).setName("EVENT_NAME"), optional(productions.getOrCreateStructure("ANONYMOUS_INNER_FUNCTION")).setName("EVENT_INITIALIZATION")).setName("DEFINE_EVENT")
                                .addProcessor((p, l) -> {
                                    if(l.getSummaryModule() != null) {
                                        TokenPattern<?> namePattern = p.find("EVENT_NAME");
                                        String name = namePattern.flatten(false);
                                        SummarySymbol sym = new SummarySymbol((TridentSummaryModule) l.getSummaryModule(), name, TridentSymbolVisibility.LOCAL, p.getStringLocation().index).addTag(TridentSuggestionTags.TAG_VARIABLE);
                                        ((TridentSummaryModule) l.getSummaryModule()).addSymbolUsage(p);
                                        sym.setDeclarationPattern(p);
                                        //sym.addUsage(p);
                                        sym.addTag(TridentSuggestionTags.TAG_ENTITY_EVENT);
                                        sym.setVisibility(parseVisibility(p.find("SYMBOL_VISIBILITY"), TridentSymbolVisibility.LOCAL));
                                        ((TridentSummaryModule) l.getSummaryModule()).addElement(sym);
                                    }
                                }),
                        group(choice("global", "local", "private").setName("SYMBOL_VISIBILITY").setOptional(), literal("item"), choice(identifierA(productions), identifierX().addTags("cspn:Item Type Name"), literal("default")).setName("ITEM_NAME"), noToken().addTags("cspn:Base Type"), resourceLocationFixer, productions.getOrCreateStructure("ITEM_ID"), optional(hash(), integer(productions).addTags("cspn:Model Index")).setName("CUSTOM_MODEL_DATA"), itemBody).setName("DEFINE_ITEM")
                                .addProcessor((p, l) -> {
                                    if(l.getSummaryModule() != null) {
                                        TokenPattern<?> namePattern = p.find("ITEM_NAME");
                                        if(namePattern.find("IDENTIFIER_A") != null) return;
                                        String name = namePattern.flatten(false);
                                        if(!name.equals("default")) {
                                            SummarySymbol sym = new SummarySymbol((TridentSummaryModule) l.getSummaryModule(), name, TridentSymbolVisibility.LOCAL, p.getStringLocation().index).addTag(TridentSuggestionTags.TAG_VARIABLE);
                                            ((TridentSummaryModule) l.getSummaryModule()).addSymbolUsage(p);
                                            sym.setDeclarationPattern(p);
                                            //sym.addUsage(p);
                                            sym.addTag(TridentSuggestionTags.TAG_CUSTOM_ITEM);
                                            sym.setVisibility(parseVisibility(p.find("SYMBOL_VISIBILITY"), SymbolVisibility.GLOBAL));
                                            ((TridentSummaryModule) l.getSummaryModule()).addElement(sym);
                                        } else {
                                            if(p.find("CUSTOM_MODEL_DATA") != null) {
                                                l.getNotices().add(new Notice(NoticeType.ERROR, "Default items don't support custom model data specifiers", p.find("CUSTOM_MODEL_DATA")));
                                            }

                                            TokenList body = (TokenList) p.find("ITEM_DECLARATION_BODY.ITEM_BODY_ENTRIES");
                                            if(body != null) {
                                                for(TokenPattern<?> entry : body.getContents()) {
                                                    if(((TokenStructure) entry).getContents().getName().startsWith("DEFAULT_")) {
                                                        l.getNotices().add(new Notice(NoticeType.ERROR, "Default properties are not allowed for default items", entry));
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }),
                        group(choice("global", "local", "private").setName("SYMBOL_VISIBILITY").setOptional(), SYMBOL_MODIFIER_LIST, literal("class"),
                                group(identifierX().addTags("cspn:Class Name")).setName("CLASS_NAME")
                                        .addProcessor((p, l) -> {
                                            if(l.getSummaryModule() != null) {
                                                SummarySymbol sym = new SummarySymbol((TridentSummaryModule) l.getSummaryModule(), p.flatten(false), TridentSymbolVisibility.LOCAL, p.getStringLocation().index);
                                                sym.setDeclarationPattern(p);
                                                ((TridentSummaryModule) l.getSummaryModule()).addSymbolUsage(p);
                                                //sym.addUsage(p);
                                                sym.addTag(TridentSuggestionTags.TAG_CLASS);
                                                sym.addTag(TridentSuggestionTags.TAG_VARIABLE);
                                                ((TridentSummaryModule) l.getSummaryModule()).pushSubSymbol(sym);
                                            }
                                        }),
                                wrapperOptional(productions.getOrCreateStructure("FORMAL_TYPE_PARAMETERS")).setName("FORMAL_TYPE_PARAMETERS"), optional(colon(), list(productions.getOrCreateStructure("INTERPOLATION_TYPE"), comma()).addTags("cspn:Superclasses").setName("SUPERCLASS_LIST").addProcessor((p, l) -> checkDuplicates(((TokenList) p), "Duplicate superclass", l))).setName("CLASS_INHERITS"), classBody).setName("DEFINE_CLASS")
                                .addProcessor((p, l) -> {
                                    if(l.getSummaryModule() != null) {
                                        SummarySymbol sym = ((TridentSummaryModule) l.getSummaryModule()).popSubSymbol();
                                        sym.setVisibility(parseVisibility(p.find("SYMBOL_VISIBILITY"), TridentSymbolVisibility.LOCAL));
                                        if(!sym.hasSubBlock()) {
                                            ((TridentSummaryModule) l.getSummaryModule()).addElement(sym);
                                        }
                                    }
                                }),
                        group(literal("function"), productions.getOrCreateStructure("INNER_FUNCTION")).setName("DEFINE_FUNCTION")
                )
        );
    }


    @Override
    public void run(TokenPattern<?> pattern, ISymbolContext ctx) {
        TokenPattern<?> inner = ((TokenStructure)pattern.find("CHOICE")).getContents();
        switch(inner.getName()) {
            case "DEFINE_OBJECTIVE":
                defineObjective(inner, ctx);
                break;
            case "DEFINE_ENTITY":
                CustomEntity.defineEntity(inner, ctx);
                break;
            case "DEFINE_EVENT":
                EntityEvent.defineEvent(inner, ctx);
                break;
            case "DEFINE_ITEM":
                CustomItem.defineItem(inner, ctx);
                break;
            case "DEFINE_FUNCTION":
                TridentFile.createInnerFile(inner.find("INNER_FUNCTION"), ctx);
                break;
            case "DEFINE_CLASS":
                CustomClass.defineClass(inner, ctx);
                break;
            default: {
                throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Unknown grammar branch name '" + inner.getName() + "'", inner, ctx);
            }
        }
    }

    private void defineObjective(TokenPattern<?> pattern, ISymbolContext ctx) {
        String objectiveName = (String) pattern.find("OBJECTIVE_NAME.IDENTIFIER_A").evaluate(ctx);
        String criteria = "dummy";
        TextComponent displayName = null;

        TokenPattern<?> sub = pattern.find("");
        if(sub != null) {
            criteria = (String) sub.find("CRITERIA.IDENTIFIER_B").evaluate(ctx);
            displayName = (TextComponent) sub.findThenEvaluate(".TEXT_COMPONENT", null, ctx);
        }

        if(ctx.get(SetupModuleTask.INSTANCE).getObjectiveManager().exists(objectiveName)) {
            if(!ctx.get(SetupModuleTask.INSTANCE).getObjectiveManager().get(objectiveName).getType().equals(criteria)) {
                throw new PrismarineException(TridentExceptionUtil.Source.DUPLICATION_ERROR, "An objective with the name '" + objectiveName + "' of a different type has already been defined", pattern, ctx);
            } else {
                ctx.getCompiler().getReport().addNotice(new Notice(NoticeType.WARNING, "An objective with the name '" + objectiveName + "' has already been defined", pattern));
            }
        } else {
            ctx.get(SetupModuleTask.INSTANCE).getObjectiveManager().create(objectiveName, criteria, displayName);
        }
    }
}
