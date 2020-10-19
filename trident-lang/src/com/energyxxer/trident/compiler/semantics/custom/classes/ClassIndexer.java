package com.energyxxer.trident.compiler.semantics.custom.classes;

import com.energyxxer.trident.compiler.semantics.symbols.TridentSymbolVisibility;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.symbols.SymbolVisibility;
import com.energyxxer.prismarine.typesystem.functions.FormalParameter;
import com.energyxxer.prismarine.typesystem.functions.PrismarineFunction;
import com.energyxxer.trident.sets.trident.instructions.VariableInstruction;
import org.jetbrains.annotations.NotNull;

public class ClassIndexer {
    private final CustomClass definingClass;
    private final TokenPattern<?> definingPattern;
    private @NotNull SymbolVisibility getterVisibility = TridentSymbolVisibility.LOCAL;
    private @NotNull SymbolVisibility setterVisibility = TridentSymbolVisibility.LOCAL;
    private VariableInstruction.SymbolModifierMap modifiers;

    private final FormalParameter indexParameter;
    private final PrismarineFunction getterFunction;
    private final PrismarineFunction setterFunction;

    public ClassIndexer(CustomClass definingClass, TokenPattern<?> definingPattern, FormalParameter indexParameter, PrismarineFunction getterFunction, PrismarineFunction setterFunction) {
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

    public PrismarineFunction getGetterFunction() {
        return getterFunction;
    }

    public PrismarineFunction getSetterFunction() {
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
    public SymbolVisibility getGetterVisibility() {
        return getterVisibility;
    }

    @NotNull
    public SymbolVisibility getSetterVisibility() {
        return setterVisibility;
    }

    public void setGetterVisibility(@NotNull SymbolVisibility getterVisibility) {
        this.getterVisibility = getterVisibility;
    }

    public void setSetterVisibility(@NotNull SymbolVisibility setterVisibility) {
        this.setterVisibility = setterVisibility;
    }
}
