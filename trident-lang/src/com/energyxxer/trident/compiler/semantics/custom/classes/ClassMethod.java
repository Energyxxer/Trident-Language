package com.energyxxer.trident.compiler.semantics.custom.classes;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.symbols.SymbolVisibility;
import com.energyxxer.prismarine.typesystem.functions.FormalParameter;
import com.energyxxer.prismarine.typesystem.functions.PrismarineFunction;
import com.energyxxer.prismarine.typesystem.functions.typed.TypedFunction;
import com.energyxxer.trident.sets.trident.instructions.VariableInstruction;

import java.util.List;

public class ClassMethod extends TypedFunction {
    private final CustomClass definingClass;
    private VariableInstruction.SymbolModifierMap modifiers;

    public ClassMethod(String name, CustomClass definingClass) {
        super(name);
        this.definingClass = definingClass;
    }

    public ClassMethod(CustomClass definingClass, TokenPattern<?> definingPattern, PrismarineFunction function) {
        super(function.getFunctionName(), definingPattern, function);
        this.definingClass = definingClass;
    }

    public CustomClass getDefiningClass() {
        return definingClass;
    }

    public VariableInstruction.SymbolModifierMap getModifiers() {
        return modifiers;
    }

    public ClassMethod setModifiers(VariableInstruction.SymbolModifierMap modifiers) {
        this.modifiers = modifiers;
        return this;
    }



    //overrides, self-returning

    @Override
    public ClassMethod setFormalParameters(List<FormalParameter> formalParameters) {
        super.setFormalParameters(formalParameters);
        return this;
    }

    @Override
    public ClassMethod setVisibility(SymbolVisibility visibility) {
        super.setVisibility(visibility);
        return this;
    }
}
