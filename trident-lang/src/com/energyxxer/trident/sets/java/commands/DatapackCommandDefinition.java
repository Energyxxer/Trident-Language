package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.datapack.DataPackDisableCommand;
import com.energyxxer.commodore.functionlogic.commands.datapack.DataPackEnableCommand;
import com.energyxxer.commodore.functionlogic.commands.datapack.DataPackListCommand;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class DatapackCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"datapack"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        return group(
                TridentProductions.commandHeader("datapack"),
                choice(
                        group(literal("list"), enumChoice(DataPackListCommand.Filter.class).setOptional().setName("DATAPACK_FILTER")).setEvaluator((p, d) -> new DataPackListCommand((DataPackListCommand.Filter) p.findThenEvaluate("DATAPACK_FILTER", null))),
                        group(
                                literal("enable"),
                                TridentProductions.noToken().addTags("cspn:Data Pack"),
                                wrapper(productions.getOrCreateStructure("STRING_LITERAL_OR_IDENTIFIER_A")).setName("DATAPACK_NAME"),
                                choice(
                                        group(literal("first")).setEvaluator((p, d) -> DataPackEnableCommand.Order.FIRST()),
                                        group(literal("last")).setEvaluator((p, d) -> DataPackEnableCommand.Order.LAST()),
                                        group(literal("before"), TridentProductions.noToken().addTags("cspn:Before Data Pack"), productions.getOrCreateStructure("STRING_LITERAL_OR_IDENTIFIER_A")).setEvaluator((p, d) -> DataPackEnableCommand.Order.BEFORE((String) p.find("STRING_LITERAL_OR_IDENTIFIER_A").evaluate((ISymbolContext) d[0]))),
                                        group(literal("after"), TridentProductions.noToken().addTags("cspn:After Data Pack"), productions.getOrCreateStructure("STRING_LITERAL_OR_IDENTIFIER_A")).setEvaluator((p, d) -> DataPackEnableCommand.Order.AFTER((String) p.find("STRING_LITERAL_OR_IDENTIFIER_A").evaluate((ISymbolContext) d[0])))
                                ).setOptional().setName("DATAPACK_ORDER")
                        ).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            String datapackName = (String) p.find("DATAPACK_NAME").evaluate(ctx);
                            DataPackEnableCommand.Order order = (DataPackEnableCommand.Order) p.findThenEvaluate("DATAPACK_ORDER", DataPackEnableCommand.Order.LAST(), ctx);
                            return new DataPackEnableCommand(datapackName, order);
                        }),
                        group(
                                literal("disable"),
                                TridentProductions.noToken().addTags("cspn:Data Pack"),
                                wrapper(productions.getOrCreateStructure("STRING_LITERAL_OR_IDENTIFIER_A")).setName("DATAPACK_NAME")
                        ).setEvaluator((p, d) -> new DataPackDisableCommand((String) p.find("DATAPACK_NAME").evaluate((ISymbolContext) d[0])))
                ).setName("INNER")
        ).setSimplificationFunctionFind("INNER");
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new UnsupportedOperationException(); //this step is optimized away
    }
}
