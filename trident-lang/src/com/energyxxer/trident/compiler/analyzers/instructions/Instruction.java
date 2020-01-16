package com.energyxxer.trident.compiler.analyzers.instructions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerGroup;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerGroup(
        classes="BreakInstruction,ContinueInstruction,DefineInstruction,EvalInstruction,IfInstruction,LogInstruction,LoopInstruction,ReturnInstruction,SwitchInstruction,ThrowInstruction,TryInstruction,UsingInstruction,VariableInstruction,WithinInstruction"
)
public interface Instruction {
    void run(TokenPattern<?> pattern, ISymbolContext ctx);
}
