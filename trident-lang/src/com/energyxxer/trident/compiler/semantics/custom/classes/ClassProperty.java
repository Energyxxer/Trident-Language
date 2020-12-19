package com.energyxxer.trident.compiler.semantics.custom.classes;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.symbols.SymbolVisibility;
import com.energyxxer.prismarine.typesystem.functions.PrismarineFunction;
import com.energyxxer.trident.compiler.semantics.symbols.TridentSymbolVisibility;
import com.energyxxer.trident.sets.trident.instructions.VariableInstruction;
import org.jetbrains.annotations.NotNull;

public class ClassProperty {
    private final CustomClass definingClass;
    private final TokenPattern<?> definingPattern;
    private @NotNull SymbolVisibility getterVisibility = TridentSymbolVisibility.LOCAL;
    private @NotNull SymbolVisibility setterVisibility = TridentSymbolVisibility.LOCAL;
    private VariableInstruction.SymbolModifierMap modifiers;

    private final String name;
    private final PrismarineFunction getterFunction;
    private final PrismarineFunction setterFunction;

    public ClassProperty(CustomClass definingClass, TokenPattern<?> definingPattern, String name, PrismarineFunction getterFunction, PrismarineFunction setterFunction) {
        this.definingClass = definingClass;
        this.definingPattern = definingPattern;
        this.name = name;
        this.getterFunction = getterFunction;
        this.setterFunction = setterFunction;
    }

    public CustomClass getDefiningClass() {
        return definingClass;
    }

    public VariableInstruction.SymbolModifierMap getModifiers() {
        return modifiers;
    }

    public ClassProperty setModifiers(VariableInstruction.SymbolModifierMap modifiers) {
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
        return name;
    }

    public ClassPropertySymbol createSymbol(CustomClassObject thisObject) {
        return new ClassPropertySymbol(this, thisObject);
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

    public String getName() {
        return name;
    }
}
