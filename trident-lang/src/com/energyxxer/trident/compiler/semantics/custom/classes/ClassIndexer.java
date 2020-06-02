package com.energyxxer.trident.compiler.semantics.custom.classes;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.FormalParameter;
import com.energyxxer.trident.compiler.analyzers.instructions.VariableInstruction;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentUserFunction;
import com.energyxxer.trident.compiler.semantics.Symbol;
import org.jetbrains.annotations.NotNull;

public class ClassIndexer {
    private final CustomClass definingClass;
    private final TokenPattern<?> definingPattern;
    private @NotNull Symbol.SymbolVisibility getterVisibility = Symbol.SymbolVisibility.LOCAL;
    private @NotNull Symbol.SymbolVisibility setterVisibility = Symbol.SymbolVisibility.LOCAL;
    private VariableInstruction.SymbolModifierMap modifiers;

    private final FormalParameter indexParameter;
    private final TridentUserFunction getterFunction;
    private final TridentUserFunction setterFunction;

    public ClassIndexer(CustomClass definingClass, TokenPattern<?> definingPattern, FormalParameter indexParameter, TridentUserFunction getterFunction, TridentUserFunction setterFunction) {
        this.definingClass = definingClass;
        this.definingPattern = definingPattern;
        this.indexParameter = indexParameter;
        this.getterFunction = getterFunction;
        this.setterFunction = setterFunction;
    }

    public CustomClass getDefiningClass() {
        return definingClass;
    }

    public FormalParameter getIndexParameter() {
        return indexParameter;
    }

    public VariableInstruction.SymbolModifierMap getModifiers() {
        return modifiers;
    }

    public ClassIndexer setModifiers(VariableInstruction.SymbolModifierMap modifiers) {
        this.modifiers = modifiers;
        return this;
    }

    public TokenPattern<?> getDefiningPattern() {
        return definingPattern;
    }

    public TridentUserFunction getGetterFunction() {
        return getterFunction;
    }

    public TridentUserFunction getSetterFunction() {
        return setterFunction;
    }

    @Override
    public String toString() {
        return "this[" + indexParameter + "]";
    }

    public ClassIndexerSymbol createSymbol(CustomClassObject thisObject, Object index) {
        return new ClassIndexerSymbol(this, thisObject, index);
    }

    @NotNull
    public Symbol.SymbolVisibility getGetterVisibility() {
        return getterVisibility;
    }

    @NotNull
    public Symbol.SymbolVisibility getSetterVisibility() {
        return setterVisibility;
    }

    public void setGetterVisibility(@NotNull Symbol.SymbolVisibility getterVisibility) {
        this.getterVisibility = getterVisibility;
    }

    public void setSetterVisibility(@NotNull Symbol.SymbolVisibility setterVisibility) {
        this.setterVisibility = setterVisibility;
    }
}
