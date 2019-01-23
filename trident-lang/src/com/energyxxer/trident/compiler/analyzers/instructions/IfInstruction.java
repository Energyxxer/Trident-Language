package com.energyxxer.trident.compiler.analyzers.instructions;

import com.energyxxer.trident.extensions.EObject;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.SymbolTable;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@AnalyzerMember(key = "if")
public class IfInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, TridentFile file) {
        boolean condition = InterpolationManager.parse(pattern.find("CONDITION.INTERPOLATION_VALUE"), file, Boolean.class);
        EObject.assertNotNull(condition, pattern, file);

        if(condition) {
            resolveBlock(pattern.find("EXECUTION_BLOCK"), file);
        } else if(pattern.find("ELSE_CLAUSE") != null) {
            resolveBlock(pattern.find("ELSE_CLAUSE.EXECUTION_BLOCK"), file);
        }
    }

    public static void resolveBlock(TokenPattern<?> pattern, TridentFile file) {
        if(pattern.getName().equals("EXECUTION_BLOCK")) {
            resolveBlock(((TokenStructure) pattern).getContents(), file);
            return;
        }
        if(pattern.getName().equals("ANONYMOUS_INNER_FUNCTION")) {
            TridentFile.resolveInnerFileIntoSection(pattern, file, file.getFunction());
        } else {
            file.getCompiler().getSymbolStack().push(new SymbolTable(file));
            try {
                TridentFile.resolveEntry(((TokenStructure) pattern).getContents(), file, file.getFunction(), false);
            } finally {
                file.getCompiler().getSymbolStack().pop();
            }
        }
    }
}
