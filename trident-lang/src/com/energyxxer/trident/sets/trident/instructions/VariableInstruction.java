package com.energyxxer.trident.sets.trident.instructions;

import com.energyxxer.enxlex.lexical_analysis.inspections.SuggestionInspection;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenGroup;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.summaries.PrismarineSummaryModule;
import com.energyxxer.prismarine.summaries.SummarySymbol;
import com.energyxxer.prismarine.symbols.Symbol;
import com.energyxxer.prismarine.symbols.SymbolVisibility;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.TypeConstraints;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.lexer.TridentSuggestionTags;
import com.energyxxer.trident.compiler.lexer.summaries.TridentSummaryModule;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.trident.compiler.semantics.symbols.TridentSymbolVisibility;
import com.energyxxer.trident.compiler.util.TridentTempFindABetterHome;
import com.energyxxer.trident.sets.DataStructureLiteralSet;
import com.energyxxer.trident.sets.ValueAccessExpressionSet;
import com.energyxxer.util.StringBounds;

import java.util.function.Function;
import java.util.function.Supplier;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class VariableInstruction implements InstructionDefinition {

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {

        TokenPatternMatch VARIABLE_DECLARATION = group(choice("global", "local", "private").setName("SYMBOL_VISIBILITY").setOptional(), list(choice("final")).setOptional().setName("SYMBOL_MODIFIER_LIST"), TridentProductions.instructionKeyword("var"),
                TridentProductions.identifierX().setName("SYMBOL_NAME").addTags("cspn:Variable Name").addProcessor((p, l) -> {
                    if(l.getSummaryModule() != null) {
                        SummarySymbol sym = new SummarySymbol((TridentSummaryModule) l.getSummaryModule(), p.flatten(false), TridentSymbolVisibility.LOCAL, p.getStringLocation().index);
                        ((TridentSummaryModule) l.getSummaryModule()).addSymbolUsage(p);
                        sym.setDeclarationPattern(p);
                        //sym.addUsage(p);
                        ((TridentSummaryModule) l.getSummaryModule()).pushSubSymbol(sym);
                    }
                }),
                productions.getPatternMatch("INFERRABLE_TYPE_CONSTRAINTS"),
                optional(TridentProductions.equals(), choice(productions.getOrCreateStructure("INTERPOLATION_BLOCK"), productions.getOrCreateStructure("LINE_SAFE_INTERPOLATION_VALUE")).setName("INITIAL_VALUE")).setName("SYMBOL_INITIALIZATION")
        ).setName("VARIABLE_DECLARATION").addProcessor((p, l) -> {
            if(l.getSummaryModule() != null) {
                SummarySymbol sym = ((TridentSummaryModule) l.getSummaryModule()).popSubSymbol();

                ((PrismarineSummaryModule) l.getSummaryModule()).addFileAwareProcessor(s -> {
                    sym.setType(ValueAccessExpressionSet.getTypeSymbolFromConstraint(s, p.find("TYPE_CONSTRAINTS")));
                });

                sym.addTag(TridentSuggestionTags.TAG_VARIABLE);
                TokenPattern<?> root = p.find("SYMBOL_INITIALIZATION.INITIAL_VALUE.INTERPOLATION_VALUE.MID_INTERPOLATION_VALUE.ROOT_INTERPOLATION_VALUE");
                if(root instanceof TokenStructure) {
                    switch(((TokenStructure) root).getContents().getName()) {
                        case "WRAPPED_ENTITY": {
                            sym.addTag(TridentSuggestionTags.TAG_ENTITY);
                            break;
                        }
                        case "WRAPPED_ITEM": {
                            sym.addTag(TridentSuggestionTags.TAG_ITEM);
                            break;
                        }
                        case "WRAPPED_COORDINATE": {
                            sym.addTag(TridentSuggestionTags.TAG_COORDINATE);
                            break;
                        }
                        case "WRAPPED_COMMAND": {
                            sym.addTag(TridentSuggestionTags.TAG_COMMAND);
                            break;
                        }
                        case "NEW_FUNCTION": {
                            TokenPattern<?> dynamicFunctionPattern = ((TokenStructure)root.find("NEW_FUNCTION.NEW_FUNCTION_SPLIT")).getContents();
                            if(dynamicFunctionPattern != null && ((TokenGroup) dynamicFunctionPattern).getContents()[0].getName().equals("DYNAMIC_FUNCTION")) {
                                sym.addTag(TridentSuggestionTags.TAG_METHOD);

                                sym.setReturnType(ValueAccessExpressionSet.getTypeSymbolFromConstraint((PrismarineSummaryModule) l.getSummaryModule(), dynamicFunctionPattern.find("DYNAMIC_FUNCTION.PRE_CODE_BLOCK.TYPE_CONSTRAINTS")));
                            }
                            break;
                        }
                    }
                }
                sym.setVisibility(TridentProductions.parseVisibility(p.find("SYMBOL_VISIBILITY"), TridentSymbolVisibility.LOCAL));
                if(!sym.hasSubBlock()) {
                    ((TridentSummaryModule) l.getSummaryModule()).addElement(sym);
                }
            }

            if(l.getInspectionModule() != null) {
                if(p.find("SYMBOL_MODIFIER_LIST") == null) { //assume it's not final since there is only one option
                    int startIndex = p.find("INSTRUCTION_KEYWORD").getStringLocation().index;

                    StringBounds bounds = p.getStringBounds();

                    SuggestionInspection inspection = new SuggestionInspection("Make final")
                            .setStartIndex(bounds.start.index)
                            .setEndIndex(bounds.end.index)
                            .setReplacementStartIndex(startIndex)
                            .setReplacementEndIndex(startIndex)
                            .setReplacementText("final ");

                    l.getInspectionModule().addInspection(inspection);
                }

                if(p.find("TYPE_CONSTRAINTS.TYPE_CONSTRAINTS_WRAPPED.TYPE_CONSTRAINTS_INNER") == null && p.find("SYMBOL_INITIALIZATION.INITIAL_VALUE") != null) {
                    ((PrismarineSummaryModule) l.getSummaryModule()).addFileAwareProcessor(s -> {
                        TokenPattern<?> contents = p.find("SYMBOL_INITIALIZATION.INITIAL_VALUE.INTERPOLATION_VALUE.MID_INTERPOLATION_VALUE");
                        if (contents instanceof TokenGroup) {
                            SummarySymbol initSymbol = ValueAccessExpressionSet.getSymbolForChain(s, (TokenPattern<?>) contents.getContents());

                            if(initSymbol != null && initSymbol.getType() != null) {
                                int replacementStartIndex = p.find("SYMBOL_NAME").getStringBounds().end.index;
                                int replacementEndIndex = p.find("SYMBOL_INITIALIZATION").getStringLocation().index;

                                StringBounds bounds = p.getStringBounds();

                                SuggestionInspection inspection = new SuggestionInspection("Constrain variable to initialization type")
                                        .setStartIndex(bounds.start.index)
                                        .setEndIndex(bounds.end.index)
                                        .setReplacementStartIndex(replacementStartIndex)
                                        .setReplacementEndIndex(replacementEndIndex)
                                        .setReplacementText(" : " + initSymbol.getType().getName() + " ");

                                l.getInspectionModule().addInspection(inspection);
                            }
                        }
                    });
                }
            }
        });


        productions.getOrCreateStructure("VARIABLE_DECLARATION").add(VARIABLE_DECLARATION);

        return VARIABLE_DECLARATION;
    }

    @Override
    public void run(TokenPattern<?> pattern, ISymbolContext ctx) {
        SymbolDeclaration decl = parseSymbolDeclaration(pattern, ctx);
        ctx.putInContextForVisibility(decl.getVisibility(), decl.getSupplier().get());
    }

    public static SymbolDeclaration parseSymbolDeclaration(TokenPattern<?> pattern, ISymbolContext ctx) {
        String memberName = pattern.find("SYMBOL_NAME").flatten(false);
        SymbolVisibility memberVisibility = CommonParsers.parseVisibility(pattern.find("SYMBOL_VISIBILITY"), ctx, TridentSymbolVisibility.LOCAL);
        final TokenPattern<?> entryFinal = pattern;


        SymbolDeclaration response = new SymbolDeclaration(memberName);
        response.setName(memberName);
        response.setVisibility(memberVisibility);
        response.setConstraintSupplier(initialValue -> (TypeConstraints) entryFinal.find("TYPE_CONSTRAINTS").evaluate(ctx, initialValue));
        response.setSupplier(() -> {
            Object initialValue = null;
            boolean initialized = false;
            if(pattern.find("SYMBOL_INITIALIZATION") != null) {
                DataStructureLiteralSet.setNextFunctionName(memberName);
                initialValue = ((TokenPattern<?>) pattern.find("SYMBOL_INITIALIZATION.INITIAL_VALUE.INTERPOLATION_VALUE").getContents()).evaluate(ctx);
                DataStructureLiteralSet.setNextFunctionName(null);
                initialized = true;
            }
            Symbol sym = new Symbol(memberName, memberVisibility);
            sym.setTypeConstraints(response.getConstraint(initialValue));
            sym.setFinal(response.hasModifier(TridentTempFindABetterHome.SymbolModifier.FINAL));
            if(initialized) sym.safeSetValue(initialValue, entryFinal, ctx);
            return sym;
        });

        response.populate((TokenList) pattern.find("SYMBOL_MODIFIER_LIST"), ctx);

        return response;
    }

    public static class SymbolDeclaration extends SymbolModifierMap {
        private String symbolName;
        private SymbolVisibility visibility;
        private Supplier<Symbol> symbolSupplier;
        private TypeConstraints constraint;
        private boolean constraintForced = false;
        private Function<Object, TypeConstraints> constraintSupplier = null;

        public SymbolDeclaration(String symbolName) {
            this.symbolName = symbolName;
        }

        public void setName(String symbolName) {
            this.symbolName = symbolName;
        }

        public String getName() {
            return symbolName;
        }

        public SymbolVisibility getVisibility() {
            return visibility;
        }

        public void setVisibility(SymbolVisibility visibility) {
            this.visibility = visibility;
        }

        public void setSupplier(Supplier<Symbol> symbolSupplier) {
            this.symbolSupplier = symbolSupplier;
        }

        public Supplier<Symbol> getSupplier() {
            return symbolSupplier;
        }

        public void setConstraintSupplier(Function<Object, TypeConstraints> constraintSupplier) {
            this.constraintSupplier = constraintSupplier;
        }

        public TypeConstraints preParseConstraints() {
            constraint = getConstraint(TypeConstraints.SpecialInferInstruction.NO_INSTANCE_INFER);
            constraintForced = true;
            return constraint;
        }

        public TypeConstraints getConstraint(Object initialValue) {
            return constraintForced ? constraint : (constraintSupplier != null ? constraintSupplier.apply(initialValue) : null);
        }
    }

    public static class SymbolModifierMap {
        private int modifiers = 0;

        public boolean hasModifier(TridentTempFindABetterHome.SymbolModifier mod) {
            return (modifiers & mod.getBit()) > 0;
        }

        public SymbolModifierMap setModifier(TridentTempFindABetterHome.SymbolModifier mod) {
            setModifier(mod, null, null);
            return this;
        }

        public void setModifier(TridentTempFindABetterHome.SymbolModifier mod, TokenPattern<?> pattern, ISymbolContext ctx) {
            if(hasModifier(mod) && pattern != null) {
                throw new PrismarineException(TridentExceptionUtil.Source.STRUCTURAL_ERROR, "Duplicated modifier '" + mod.name().toLowerCase() + "'", pattern, ctx);
            }
            modifiers |= mod.getBit();
        }

        public static SymbolModifierMap createFromList(TokenList modifierList, ISymbolContext ctx) {
            return new SymbolModifierMap().populate(modifierList, ctx);
        }

        public SymbolModifierMap populate(TokenList modifierList, ISymbolContext ctx) {
            if(modifierList != null) {
                for(TokenPattern<?> rawModifier : modifierList.getContents()) {
                    String modifierName = rawModifier.flatten(false);
                    TridentTempFindABetterHome.SymbolModifier modifier = TridentTempFindABetterHome.SymbolModifier.valueOf(modifierName.toUpperCase());
                    setModifier(modifier, rawModifier, ctx);
                }
            }
            return this;
        }
    }
}
