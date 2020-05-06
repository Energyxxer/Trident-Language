package com.energyxxer.trident.compiler.analyzers.instructions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.TypeConstraints;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.function.Function;
import java.util.function.Supplier;

@AnalyzerMember(key = "var")
public class VariableInstruction implements Instruction {

    @Override
    public void run(TokenPattern<?> pattern, ISymbolContext ctx) {
        SymbolDeclaration decl = parseSymbolDeclaration(pattern, ctx);
        ctx.putInContextForVisibility(decl.getVisibility(), decl.getSupplier().get());
    }

    public static SymbolDeclaration parseSymbolDeclaration(TokenPattern<?> pattern, ISymbolContext ctx) {
        String memberName = pattern.find("SYMBOL_NAME").flatten(false);
        Symbol.SymbolVisibility memberVisibility = CommonParsers.parseVisibility(pattern.find("SYMBOL_VISIBILITY"), ctx, Symbol.SymbolVisibility.LOCAL);
        final TokenPattern<?> entryFinal = pattern;


        SymbolDeclaration response = new SymbolDeclaration(memberName);
        response.setName(memberName);
        response.setVisibility(memberVisibility);
        response.setConstraintSupplier(initialValue -> {
            return TypeConstraints.parseConstraintsInfer(entryFinal.find("TYPE_CONSTRAINTS"), ctx, initialValue);
        });
        response.setSupplier(() -> {
            Object initialValue = null;
            boolean initialized = false;
            if(pattern.find("SYMBOL_INITIALIZATION") != null) {
                InterpolationManager.setNextFunctionName(memberName);
                initialValue = InterpolationManager.parse((TokenPattern<?>) ((pattern.find("SYMBOL_INITIALIZATION.INITIAL_VALUE")).getContents()), ctx);
                InterpolationManager.setNextFunctionName(null);
                initialized = true;
            }
            Symbol sym = new Symbol(memberName, memberVisibility);
            sym.setTypeConstraints(response.getConstraint(initialValue));
            sym.setFinal(response.hasModifier(Symbol.SymbolModifier.FINAL));
            if(initialized) sym.safeSetValue(initialValue, entryFinal, ctx);
            return sym;
        });

        TokenList modifierList = ((TokenList) pattern.find("SYMBOL_MODIFIER_LIST"));
        if(modifierList != null) {
            for(TokenPattern<?> rawModifier : modifierList.getContents()) {
                String modifierName = rawModifier.flatten(false);
                Symbol.SymbolModifier modifier = Symbol.SymbolModifier.valueOf(modifierName.toUpperCase());
                response.setModifier(modifier, rawModifier, ctx);
            }
        }

        return response;
    }

    public static class SymbolDeclaration {
        private String symbolName;
        private Symbol.SymbolVisibility visibility;
        private Supplier<Symbol> symbolSupplier;
        private TypeConstraints constraint;
        private boolean constraintForced = false;
        private Function<Object, TypeConstraints> constraintSupplier = null;
        private int modifiers = 0;

        public SymbolDeclaration(String symbolName) {
            this.symbolName = symbolName;
        }

        public void setName(String symbolName) {
            this.symbolName = symbolName;
        }

        public String getName() {
            return symbolName;
        }

        public Symbol.SymbolVisibility getVisibility() {
            return visibility;
        }

        public void setVisibility(Symbol.SymbolVisibility visibility) {
            this.visibility = visibility;
        }

        public void setSupplier(Supplier<Symbol> symbolSupplier) {
            this.symbolSupplier = symbolSupplier;
        }

        public Supplier<Symbol> getSupplier() {
            return symbolSupplier;
        }

        public boolean hasModifier(Symbol.SymbolModifier mod) {
            return (modifiers & mod.getBit()) > 0;
        }

        public void setModifier(Symbol.SymbolModifier mod) {
            setModifier(mod, null, null);
        }

        public void setModifier(Symbol.SymbolModifier mod, TokenPattern<?> pattern, ISymbolContext ctx) {
            if(hasModifier(mod) && pattern != null) {
                throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "Duplicated modifier '" + mod.name().toLowerCase() + "'", pattern, ctx);
            }
            modifiers |= mod.getBit();
        }

        public void setConstraintSupplier(Function<Object, TypeConstraints> constraintSupplier) {
            this.constraintSupplier = constraintSupplier;
        }

        public TypeConstraints preparseConstraints() {
            constraint = getConstraint(TypeConstraints.SpecialInferInstruction.NO_INSTANCE_INFER);
            constraintForced = true;
            return constraint;
        }

        public TypeConstraints getConstraint(Object initialValue) {
            return constraintForced ? constraint : (constraintSupplier != null ? constraintSupplier.apply(initialValue) : null);
        }
    }
}
