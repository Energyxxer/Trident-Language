package com.energyxxer.trident.compiler.analyzers.instructions;

import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.BreakException;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.symbols.SymbolContext;

import java.util.Objects;

import static com.energyxxer.trident.compiler.analyzers.instructions.LoopInstruction.getLabel;

@AnalyzerMember(key = "switch")
public class SwitchInstruction implements Instruction {
    @Override
    public void run(TokenPattern<?> pattern, ISymbolContext ctx) {
        String label = getLabel(pattern);
        try {
            SymbolContext innerFrame = new SymbolContext(ctx);
            Object switchValue = InterpolationManager.parse(pattern.find("SWITCH_VALUE.INTERPOLATION_VALUE"), innerFrame);

            TokenList switchCases = (TokenList) pattern.find("SWITCH_CASES");
            if (switchCases != null) {

                boolean passed = false;
                for (TokenPattern<?> rawCase : switchCases.getContents()) {
                    TokenPattern<?> branch = ((TokenStructure) rawCase.find("CASE_BRANCH")).getContents();
                    if (!passed) {
                        if (branch.find("INTERPOLATION_VALUE") != null) {
                            if (Objects.equals(switchValue, InterpolationManager.parse(branch.find("INTERPOLATION_VALUE"), innerFrame))) {
                                passed = true;
                            }
                        } else {
                            passed = true;
                        }
                    }
                    if (passed) {
                        TokenPattern<?> executionBlock = rawCase.find("CASE_BLOCK");
                        if (executionBlock != null) {
                            executionBlock = ((TokenStructure) executionBlock).getContents();
                            switch (executionBlock.getName()) {
                                /*case "BRACELESS_BLOCK": {
                                    TridentFile.resolveEntryListIntoSection(((TokenList) executionBlock), innerFrame, innerFrame.getWritingFile().getFunction());
                                    break;
                                }*/
                                case "ANONYMOUS_INNER_FUNCTION": {
                                    TridentFile.resolveInnerFileIntoSection(executionBlock, innerFrame, innerFrame.getWritingFile().getFunction());
                                    break;
                                }
                                default: {
                                    throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + executionBlock.getName() + "'", executionBlock, innerFrame);
                                }
                            }
                        }
                    }
                }
            }
        } catch(BreakException x) {
            if (x.getLabel() != null && !x.getLabel().equals(label)) {
                throw x;
            }
        }
    }
}
