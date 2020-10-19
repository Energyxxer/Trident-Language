package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.loot.*;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.item.Item;
import com.energyxxer.commodore.types.defaults.ItemSlot;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.trident.compiler.semantics.custom.items.NBTMode;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenStructureMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class LootCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"loot"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        TokenPatternMatch toolMatch = choice(
                literal("mainhand").setEvaluator((p, d) -> ToolOrHand.MAINHAND),
                literal("offhand").setEvaluator((p, d) -> ToolOrHand.OFFHAND),
                group(productions.getOrCreateStructure("ITEM")).setEvaluator((p, d) -> {
                    Item item = (Item) p.find("ITEM").evaluate((ISymbolContext) d[0], NBTMode.TESTING, false);
                    return new ToolOrHand(item);
                })
        ).setOptional().setName("TOOL").addTags("cspn:Tool");

        TokenStructureMatch destination = choice(
                group(literal("give"), productions.getOrCreateStructure("ENTITY")).setEvaluator((p, d) -> {
                    ISymbolContext ctx = (ISymbolContext) d[0];
                    Entity entity = (Entity) p.find("ENTITY").evaluate(ctx);

                    try {
                        return new LootGive(entity);
                    } catch (CommodoreException x) {
                        TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                .map(CommodoreException.Source.ENTITY_ERROR, p.tryFind("ENTITY"))
                                .invokeThrow();
                        return null;
                    }
                }),
                group(literal("insert"), productions.getOrCreateStructure("COORDINATE_SET")).setEvaluator((p, d) -> {
                    ISymbolContext ctx = (ISymbolContext) d[0];
                    CoordinateSet pos = (CoordinateSet) p.find("COORDINATE_SET").evaluate(ctx);
                    return new LootInsertBlock(pos);
                }),
                group(literal("replace"),
                        choice(
                                group(literal("block"), productions.getOrCreateStructure("COORDINATE_SET")).setEvaluator((p, d) -> {
                                    ISymbolContext ctx = (ISymbolContext) d[0];
                                    ItemSlot slot = (ItemSlot) d[1];
                                    int count = (int) d[2];

                                    CoordinateSet pos = (CoordinateSet) p.find("COORDINATE_SET").evaluate(ctx);

                                    try {
                                        return new LootReplaceBlock(pos, slot, count);
                                    } catch (CommodoreException x) {
                                        TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                                .map(CommodoreException.Source.NUMBER_LIMIT_ERROR, (TokenPattern) d[3])
                                                .invokeThrow();
                                        return null;
                                    }
                                }),
                                group(literal("entity"), productions.getOrCreateStructure("ENTITY")).setEvaluator((p, d) -> {
                                    ISymbolContext ctx = (ISymbolContext) d[0];
                                    ItemSlot slot = (ItemSlot) d[1];
                                    int count = (int) d[2];

                                    Entity entity = (Entity) p.find("ENTITY").evaluate(ctx);

                                    try {
                                        return new LootReplaceEntity(entity, slot, count);
                                    } catch (CommodoreException x) {
                                        TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                                .map(CommodoreException.Source.NUMBER_LIMIT_ERROR, (TokenPattern) d[3])
                                                .invokeThrow();
                                        return null;
                                    }
                                })
                        ).setName("INNER"),
                        productions.getOrCreateStructure("SLOT_ID"),
                        TridentProductions.integer(productions).setOptional().setName("COUNT")
                ).setSimplificationFunction(d -> {
                    ISymbolContext ctx = (ISymbolContext) d.data[0];
                    ItemSlot slot = (ItemSlot) d.pattern.find("SLOT_ID").evaluate(ctx);
                    int count = (int) d.pattern.findThenEvaluate("COUNT", -1, ctx);
                    d.pattern = d.pattern.find("INNER");
                    d.data = new Object[]{ctx, slot, count, d.pattern.tryFind("COUNT")};
                }),
                group(literal("spawn"), productions.getOrCreateStructure("COORDINATE_SET")).setEvaluator((p, d) -> {
                    ISymbolContext ctx = (ISymbolContext) d[0];
                    CoordinateSet pos = (CoordinateSet) p.find("COORDINATE_SET").evaluate(ctx);
                    return new LootSpawn(pos);
                })
        ).setName("LOOT_DESTINATION");
        destination.addTags("cspn:Loot Destination");

        TokenStructureMatch source = choice(
                group(
                        literal("fish"),
                        TridentProductions.noToken().addTags("cspn:Loot Table"),
                        productions.getOrCreateStructure("RESOURCE_LOCATION"),
                        productions.getOrCreateStructure("COORDINATE_SET"),
                        toolMatch
                ).setEvaluator((p, d) -> {
                    ISymbolContext ctx = (ISymbolContext) d[0];
                    ResourceLocation lootTable = (ResourceLocation) p.find("RESOURCE_LOCATION").evaluate(ctx);
                    CoordinateSet pos = (CoordinateSet) p.find("COORDINATE_SET").evaluate(ctx);
                    ToolOrHand tool = (ToolOrHand) p.findThenEvaluate("TOOL", null, ctx);
                    return new LootFromFish(lootTable.toString(), pos, tool);
                }),
                group(literal("kill"), productions.getOrCreateStructure("ENTITY")).setEvaluator((p, d) -> {
                    ISymbolContext ctx = (ISymbolContext) d[0];
                    Entity entity = (Entity) p.find("ENTITY").evaluate(ctx);

                    try {
                        return new LootFromKill(entity);
                    } catch (CommodoreException x) {
                        TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                .map(CommodoreException.Source.ENTITY_ERROR, p.tryFind("ENTITY"))
                                .invokeThrow();
                        return null;
                    }
                }),
                group(literal("loot"), TridentProductions.noToken().addTags("cspn:Loot Table"), productions.getOrCreateStructure("RESOURCE_LOCATION")).setEvaluator((p, d) -> {
                    ISymbolContext ctx = (ISymbolContext) d[0];
                    ResourceLocation lootTable = (ResourceLocation) p.find("RESOURCE_LOCATION").evaluate(ctx);
                    return new LootFromLoot(lootTable.toString());
                }),
                group(literal("mine"), productions.getOrCreateStructure("COORDINATE_SET"), toolMatch).setEvaluator((p, d) -> {
                    ISymbolContext ctx = (ISymbolContext) d[0];
                    CoordinateSet pos = (CoordinateSet) p.find("COORDINATE_SET").evaluate(ctx);
                    ToolOrHand tool = (ToolOrHand) p.findThenEvaluate("TOOL", null, ctx);
                    return new LootFromMine(pos, tool);
                })
        ).setName("LOOT_SOURCE");
        source.addTags("cspn:Loot Source");

        return group(
                TridentProductions.commandHeader("loot"),
                destination, source
        );
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        LootCommand.LootDestination destination = (LootCommand.LootDestination) pattern.find("LOOT_DESTINATION").evaluate(ctx);
        LootCommand.LootSource source = (LootCommand.LootSource) pattern.find("LOOT_SOURCE").evaluate(ctx);
        return new LootCommand(destination, source);
    }
}
