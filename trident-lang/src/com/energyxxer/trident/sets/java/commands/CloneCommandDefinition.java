package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.block.Block;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.clone.CloneCommand;
import com.energyxxer.commodore.functionlogic.commands.clone.CloneFilteredCommand;
import com.energyxxer.commodore.functionlogic.commands.clone.CloneMaskedCommand;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class CloneCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"clone"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        TokenPatternMatch modeMatch = enumChoice(CloneCommand.SourceMode.class).setOptional().setName("CLONE_MODE").addTags("cspn:Clone Mode");

        return group(
                TridentProductions.commandHeader("clone"),
                wrapper(productions.getOrCreateStructure("COORDINATE_SET")).setName("FROM").addTags("cspn:Source From"),
                wrapper(productions.getOrCreateStructure("COORDINATE_SET")).setName("TO").addTags("cspn:To").addTags("cspn:Source To"),
                wrapper(productions.getOrCreateStructure("COORDINATE_SET")).setName("DESTINATION").addTags("cspn:Destination"),
                choice(
                        group(
                                literal("filtered"),
                                TridentProductions.sameLine(),
                                productions.getOrCreateStructure("BLOCK_TAGGED"),
                                modeMatch
                        ).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                            CoordinateSet from = (CoordinateSet) d[0];
                            CoordinateSet to = (CoordinateSet) d[1];
                            CoordinateSet destination = (CoordinateSet) d[2];

                            Block filterBlock = (Block) p.find("BLOCK_TAGGED").evaluate(ctx, null);
                            CloneCommand.SourceMode mode = (CloneCommand.SourceMode) p.findThenEvaluate("CLONE_MODE", CloneCommand.SourceMode.DEFAULT, ctx, null);

                            return new CloneFilteredCommand(from, to, destination, filterBlock, mode);
                        }),
                        group(
                                literal("masked"),
                                modeMatch
                        ).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                            CoordinateSet from = (CoordinateSet) d[0];
                            CoordinateSet to = (CoordinateSet) d[1];
                            CoordinateSet destination = (CoordinateSet) d[2];

                            CloneCommand.SourceMode mode = (CloneCommand.SourceMode) p.findThenEvaluate("CLONE_MODE", CloneCommand.SourceMode.DEFAULT, ctx, null);

                            return new CloneMaskedCommand(from, to, destination, mode);
                        }),
                        group(
                                literal("replace"),
                                modeMatch
                        ).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                            CoordinateSet from = (CoordinateSet) d[0];
                            CoordinateSet to = (CoordinateSet) d[1];
                            CoordinateSet destination = (CoordinateSet) d[2];

                            CloneCommand.SourceMode mode = (CloneCommand.SourceMode) p.findThenEvaluate("CLONE_MODE", CloneCommand.SourceMode.DEFAULT, ctx, null);

                            return new CloneCommand(from, to, destination, mode);
                        })
                ).setOptional().setName("INNER")
        );
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        CoordinateSet from = (CoordinateSet) pattern.findThenEvaluate("FROM.COORDINATE_SET", null, ctx, null);
        CoordinateSet to = (CoordinateSet) pattern.findThenEvaluate("TO.COORDINATE_SET", null, ctx, null);
        CoordinateSet destination = (CoordinateSet) pattern.findThenEvaluate("DESTINATION.COORDINATE_SET", null, ctx, null);

        TokenPattern<?> inner = pattern.find("INNER");
        if (inner != null) {
            return (Command) inner.evaluate(ctx, new Object[] {from, to, destination});
        }
        return new CloneCommand(from, to, destination);
    }
}
