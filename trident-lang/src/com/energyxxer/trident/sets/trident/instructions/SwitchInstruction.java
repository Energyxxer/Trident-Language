package com.energyxxer.trident.sets.trident.instructions;

import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenGroupMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.symbols.contexts.SymbolContext;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.semantics.BreakException;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.worker.tasks.SetupWritingStackTask;

import java.util.Objects;

import static com.energyxxer.prismarine.PrismarineProductions.*;
import static com.energyxxer.trident.sets.trident.instructions.LoopInstruction.getLabel;

public class SwitchInstruction implements InstructionDefinition {
    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        TokenGroupMatch blockLabel = optional(TridentProductions.identifierX().setName("LABEL").setRecessive(), TridentProductions.colon()).setName("BLOCK_LABEL");

        return group(blockLabel, TridentProductions.instructionKeyword("switch"), TridentProductions.brace("("), group(productions.getOrCreateStructure("INTERPOLATION_VALUE")).setName("SWITCH_VALUE").addTags("cspn:Switch Value"), TridentProductions.brace(")"),
                TridentProductions.brace("{"),
                list(
                        group(
                                choice(TridentProductions.instructionKeyword("default", false), group(TridentProductions.instructionKeyword("case", false), productions.getOrCreateStructure("INTERPOLATION_VALUE"))).setName("CASE_BRANCH"), TridentProductions.colon(),
                                choice(productions.getOrCreateStructure("ANONYMOUS_INNER_FUNCTION")).setOptional().setName("CASE_BLOCK")
                        )
                ).setOptional().setName("SWITCH_CASES"),
                TridentProductions.brace("}")
        ).setName("SWITCH_STATEMENT");
    }

    @Override
    public void run(TokenPattern<?> pattern, ISymbolContext ctx) {
        String label = getLabel(pattern);
        try {
            SymbolContext innerFrame = new SymbolContext(ctx);
            Object switchValue = pattern.find("SWITCH_VALUE.INTERPOLATION_VALUE").evaluate(innerFrame);

            TokenList switchCases = (TokenList) pattern.find("SWITCH_CASES");
            if (switchCases != null) {

                boolean passed = false;
                for (TokenPattern<?> rawCase : switchCases.getContents()) {
                    TokenPattern<?> branch = ((TokenStructure) rawCase.find("CASE_BRANCH")).getContents();
                    if (!passed) {
                        if (branch.find("INTERPOLATION_VALUE") != null) {
                            if (Objects.equals(switchValue, branch.find("INTERPOLATION_VALUE").evaluate(innerFrame))) {
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
                                case "BRACELESS_BLOCK": {
                                    TridentFile.resolveEntryListIntoSection(((TokenList) executionBlock), innerFrame, innerFrame.get(SetupWritingStackTask.INSTANCE).getWritingFile().getFunction());
                                    break;
                                }
                                case "ANONYMOUS_INNER_FUNCTION": {
                                    TridentFile.resolveInnerFileIntoSection(executionBlock, innerFrame, innerFrame.get(SetupWritingStackTask.INSTANCE).getWritingFile().getFunction());
                                    break;
                                }
                                default: {
                                    throw new PrismarineException(PrismarineException.Type.IMPOSSIBLE, "Unknown grammar branch name '" + executionBlock.getName() + "'", executionBlock, innerFrame);
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
