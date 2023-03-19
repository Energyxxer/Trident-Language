package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.datapack.DataPackDisableCommand;
import com.energyxxer.commodore.functionlogic.commands.datapack.DataPackEnableCommand;
import com.energyxxer.commodore.functionlogic.commands.datapack.DataPackListCommand;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class DatapackCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"datapack"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                TridentProductions.commandHeader("datapack"),
                choice(
                        group(literal("list"), enumChoice(DataPackListCommand.Filter.class).setOptional().setName("DATAPACK_FILTER")).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new DataPackListCommand((DataPackListCommand.Filter) p.findThenEvaluate("DATAPACK_FILTER", null, ctx, null))),
                        group(
                                literal("enable"),
                                TridentProductions.noToken().addTags("cspn:Data Pack"),
                                wrapper(productions.getOrCreateStructure("STRING_LITERAL_OR_IDENTIFIER_A")).setName("DATAPACK_NAME"),
                                choice(
                                        group(literal("first")).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> DataPackEnableCommand.Order.FIRST()),
                                        group(literal("last")).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> DataPackEnableCommand.Order.LAST()),
                                        group(literal("before"), TridentProductions.noToken().addTags("cspn:Before Data Pack"), productions.getOrCreateStructure("STRING_LITERAL_OR_IDENTIFIER_A")).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> DataPackEnableCommand.Order.BEFORE((String) p.find("STRING_LITERAL_OR_IDENTIFIER_A").evaluate(ctx, null))),
                                        group(literal("after"), TridentProductions.noToken().addTags("cspn:After Data Pack"), productions.getOrCreateStructure("STRING_LITERAL_OR_IDENTIFIER_A")).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> DataPackEnableCommand.Order.AFTER((String) p.find("STRING_LITERAL_OR_IDENTIFIER_A").evaluate(ctx, null)))
                                ).setOptional().setName("DATAPACK_ORDER")
                        ).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                            String datapackName = (String) p.find("DATAPACK_NAME").evaluate(ctx, null);
                            DataPackEnableCommand.Order order = (DataPackEnableCommand.Order) p.findThenEvaluate("DATAPACK_ORDER", DataPackEnableCommand.Order.LAST(), ctx, null);
                            return new DataPackEnableCommand(datapackName, order);
                        }),
                        group(
                                literal("disable"),
                                TridentProductions.noToken().addTags("cspn:Data Pack"),
                                wrapper(productions.getOrCreateStructure("STRING_LITERAL_OR_IDENTIFIER_A")).setName("DATAPACK_NAME")
                        ).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> new DataPackDisableCommand((String) p.find("DATAPACK_NAME").evaluate(ctx, null)))
                ).setName("INNER")
        ).setSimplificationFunctionFind("INNER");
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new UnsupportedOperationException(); //this step is optimized away
    }
}
