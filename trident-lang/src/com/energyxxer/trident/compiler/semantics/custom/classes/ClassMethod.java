package com.energyxxer.trident.compiler.semantics.custom.classes;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.FormalParameter;
import com.energyxxer.trident.compiler.analyzers.instructions.VariableInstruction;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentUserFunction;
import com.energyxxer.trident.compiler.semantics.Symbol;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class ClassMethod {
    private final String name;
    private final CustomClass definingClass;
    private final TokenPattern<?> definingPattern;
    private final TridentUserFunction function;
    private List<FormalParameter> formalParameters;
    private @NotNull Symbol.SymbolVisibility visibility = Symbol.SymbolVisibility.LOCAL;
    private VariableInstruction.SymbolModifierMap modifiers;

    public ClassMethod(String name, CustomClass definingClass) {
        this.name = name;
        this.definingClass = definingClass;
        this.definingPattern = null;
        this.function = null;
        this.formalParameters = Collections.emptyList();
    }

    public ClassMethod(CustomClass definingClass, TokenPattern<?> definingPattern, TridentUserFunction function) {
        this.name = function.getFunctionName();
        this.definingClass = definingClass;
        this.definingPattern = definingPattern;
        this.function = function;

        this.formalParameters = function.getBranch().getFormalParameters();
    }

    public CustomClass getDefiningClass() {
        return definingClass;
    }

    public List<FormalParameter> getFormalParameters() {
        return formalParameters;
    }

    public void setFormalParameters(List<FormalParameter> formalParameters) {
        this.formalParameters = formalParameters;
    }

    public TridentUserFunction getFunction() {
        return function;
    }

    public Symbol.SymbolVisibility getVisibility() {
        return visibility;
    }

    public ClassMethod setVisibility(Symbol.SymbolVisibility visibility) {
        this.visibility = visibility;
        return this;
    }

    public VariableInstruction.SymbolModifierMap getModifiers() {
        return modifiers;
    }

    public ClassMethod setModifiers(VariableInstruction.SymbolModifierMap modifiers) {
        this.modifiers = modifiers;
        return this;
    }

    public TokenPattern<?> getDefiningPattern() {
        return definingPattern;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        String formalParams = formalParameters.toString();
        return name + "(" + formalParams.substring(1, formalParams.length()-1) + ")";
    }
}
