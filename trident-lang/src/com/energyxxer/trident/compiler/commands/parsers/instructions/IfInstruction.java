package com.energyxxer.trident.compiler.commands.parsers.instructions;

import Trident.extensions.java.lang.Object.EObject;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.commands.parsers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.SymbolTable;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "if")
public class IfInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, TridentFile file) {
        Object condition = InterpolationManager.parse(pattern.find("CONDITION.INTERPOLATION_VALUE"), file);
        EObject.assertNotNull(condition, pattern, file);

        if(condition.getClass() != Boolean.class) {
            throw new TridentException(TridentException.Source.TYPE_ERROR, "Required boolean in 'if' condition", pattern.find("CONDITION"), file);
        }

        if((boolean)condition) {
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
