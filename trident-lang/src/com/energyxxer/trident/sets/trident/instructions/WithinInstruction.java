package com.energyxxer.trident.sets.trident.instructions;

import com.energyxxer.commodore.functionlogic.coordinates.Coordinate;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.Symbol;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.symbols.contexts.SymbolContext;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.lexer.TridentSuggestionTags;
import com.energyxxer.trident.compiler.lexer.summaries.TridentSummaryModule;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.trident.compiler.semantics.symbols.TridentSymbolVisibility;
import com.energyxxer.trident.worker.tasks.SetupWritingStackTask;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class WithinInstruction implements InstructionDefinition {
    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        return group(
                TridentProductions.instructionKeyword("within"),
                TridentProductions.identifierX().setName("VARIABLE_NAME").addProcessor((p, l) -> {
                    if(l.getSummaryModule() != null) {
                        ((TridentSummaryModule) l.getSummaryModule()).addPreBlockDeclaration(p.find("VARIABLE_NAME")).setTags(new String[] {TridentSuggestionTags.TAG_COORDINATE});
                    }
                }),
                wrapper(productions.getOrCreateStructure("COORDINATE_SET")).setName("FROM").addTags("cspn:From"),
                wrapper(productions.getOrCreateStructure("COORDINATE_SET")).setName("TO").addTags("cspn:To"),
                optional(
                        literal("step"),
                        TridentProductions.real(productions).addTags("cspn:Step")
                ).setName("STEP").setSimplificationFunctionContentIndex(1),
                wrapper(productions.getOrCreateStructure("ANONYMOUS_INNER_FUNCTION")).setName("WITHIN_BODY")
        );
    }

    @Override
    public void run(TokenPattern<?> pattern, ISymbolContext ctx) {
        SymbolContext innerFrame = new SymbolContext(ctx);

        Symbol symbol = new Symbol(pattern.find("VARIABLE_NAME").flatten(false), TridentSymbolVisibility.LOCAL);
        innerFrame.put(symbol);

        CoordinateSet from = (CoordinateSet) pattern.find("FROM").evaluate(ctx);
        CoordinateSet to = (CoordinateSet) pattern.find("TO").evaluate(ctx);

        double step = (double) pattern.findThenEvaluate("STEP", 1.0, ctx);
        if (step <= 0) {
            throw new PrismarineException(TridentExceptionUtil.Source.COMMAND_ERROR, "Within step must be positive", pattern.tryFind("STEP"), ctx);
        }

        if (
                from.getX().getType() != to.getX().getType() ||
                        from.getY().getType() != to.getY().getType() ||
                        from.getZ().getType() != to.getZ().getType()
        ) {
            throw new PrismarineException(TridentExceptionUtil.Source.COMMAND_ERROR, "'from' and 'to' coordinate sets must have matching coordinate types", pattern.tryFind("TO"), ctx);
        }

        double fromX = Math.min(from.getX().getCoord(), to.getX().getCoord());
        double fromY = Math.min(from.getY().getCoord(), to.getY().getCoord());
        double fromZ = Math.min(from.getZ().getCoord(), to.getZ().getCoord());
        double toX = Math.max(from.getX().getCoord(), to.getX().getCoord());
        double toY = Math.max(from.getY().getCoord(), to.getY().getCoord());
        double toZ = Math.max(from.getZ().getCoord(), to.getZ().getCoord());

        for (double x = fromX; x <= toX; x += step) {
            for (double y = fromY; y <= toY; y += step) {
                for (double z = fromZ; z <= toZ; z += step) {
                    symbol.setValue(new CoordinateSet(new Coordinate(from.getX().getType(), x), new Coordinate(from.getY().getType(), y), new Coordinate(from.getZ().getType(), z)));
                    TridentFile.resolveInnerFileIntoSection(pattern.find("WITHIN_BODY.ANONYMOUS_INNER_FUNCTION"), innerFrame, ctx.get(SetupWritingStackTask.INSTANCE).getWritingFile().getFunction());
                }
            }
        }
    }
}
