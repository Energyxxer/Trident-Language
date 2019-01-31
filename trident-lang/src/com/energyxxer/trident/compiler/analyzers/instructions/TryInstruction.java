package com.energyxxer.trident.compiler.analyzers.instructions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListType;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.SymbolTable;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@AnalyzerMember(key = "try")
public class TryInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, TridentFile file) {
        if(pattern.find("LITERAL_RECOVERING") != null) file.getCompiler().getTryStack().pushRecovering();
        else file.getCompiler().getTryStack().pushBreaking();

        Object variable = null;

        try {
            IfInstruction.resolveBlock(pattern.find("EXECUTION_BLOCK"), file);
        } catch(TridentException x) { //blocking
            variable = x;
        } catch(TridentException.Grouped gx) {
            variable = new ListType(gx.getExceptions());
        } finally {
            file.getCompiler().getTryStack().pop();
        }

        try {
            SymbolTable symbolTable = new SymbolTable(file);
            String variableName = pattern.find("CATCH_CLAUSE.EXCEPTION_VARIABLE").flatten(false);
            symbolTable.put(new Symbol(variableName, Symbol.SymbolVisibility.LOCAL, variable));
            file.getCompiler().getSymbolStack().push(symbolTable);

            IfInstruction.resolveBlock(pattern.find("CATCH_CLAUSE.EXECUTION_BLOCK"), file);
        } finally {
            file.getCompiler().getSymbolStack().pop();
        }
    }
}
