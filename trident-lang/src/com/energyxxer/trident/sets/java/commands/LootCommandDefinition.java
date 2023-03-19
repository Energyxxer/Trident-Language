package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.loot.*;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.item.Item;
import com.energyxxer.commodore.types.defaults.ItemSlot;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenStructureMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.trident.compiler.semantics.custom.items.NBTMode;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class LootCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"loot"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        TokenPatternMatch toolMatch = choice(
                literal("mainhand").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> ToolOrHand.MAINHAND),
                literal("offhand").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> ToolOrHand.OFFHAND),
                group(productions.getOrCreateStructure("ITEM")).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                    Item item = (Item) p.find("ITEM").evaluate(ctx, new Object[] {NBTMode.TESTING, false});
                    return new ToolOrHand(item);
                })
        ).setOptional().setName("TOOL").addTags("cspn:Tool");

        TokenStructureMatch destination = choice(
                group(literal("give"), productions.getOrCreateStructure("ENTITY")).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                    Entity entity = (Entity) p.find("ENTITY").evaluate(ctx, null);

                    try {
                        return new LootGive(entity);
                    } catch (CommodoreException x) {
                        TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                .map(CommodoreException.Source.ENTITY_ERROR, p.tryFind("ENTITY"))
                                .invokeThrow();
                        return null;
                    }
                }),
                group(literal("insert"), productions.getOrCreateStructure("COORDINATE_SET")).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                    CoordinateSet pos = (CoordinateSet) p.find("COORDINATE_SET").evaluate(ctx, null);
                    return new LootInsertBlock(pos);
                }),
                group(literal("replace"),
                        choice(
                                group(literal("block"), productions.getOrCreateStructure("COORDINATE_SET")).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                                    ItemSlot slot = (ItemSlot) d[0];
                                    int count = (int) d[1];

                                    CoordinateSet pos = (CoordinateSet) p.find("COORDINATE_SET").evaluate(ctx, null);

                                    try {
                                        return new LootReplaceBlock(pos, slot, count);
                                    } catch (CommodoreException x) {
                                        TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                                .map(CommodoreException.Source.NUMBER_LIMIT_ERROR, (TokenPattern) d[2])
                                                .invokeThrow();
                                        return null;
                                    }
                                }),
                                group(literal("entity"), productions.getOrCreateStructure("ENTITY")).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                                    ItemSlot slot = (ItemSlot) d[0];
                                    int count = (int) d[1];

                                    Entity entity = (Entity) p.find("ENTITY").evaluate(ctx, null);

                                    try {
                                        return new LootReplaceEntity(entity, slot, count);
                                    } catch (CommodoreException x) {
                                        TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                                .map(CommodoreException.Source.NUMBER_LIMIT_ERROR, (TokenPattern) d[2])
                                                .invokeThrow();
                                        return null;
                                    }
                                })
                        ).setName("INNER"),
                        productions.getOrCreateStructure("SLOT_ID"),
                        TridentProductions.integer(productions).setOptional().setName("COUNT")
                ).setSimplificationFunction(d -> {
                    TokenPattern<?> pattern = d.pattern;
                    ISymbolContext ctx = (ISymbolContext) d.ctx;

                    d.unlock(); d = null;
                    ItemSlot slot = (ItemSlot) pattern.find("SLOT_ID").evaluate(ctx, null);
                    int count = (int) pattern.findThenEvaluate("COUNT", -1, ctx, null);

                    TokenPattern.SimplificationDomain.get(pattern.find("INNER"), ctx, new Object[] {slot, count, pattern.tryFind("COUNT")});
                }),
                group(literal("spawn"), productions.getOrCreateStructure("COORDINATE_SET")).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                    CoordinateSet pos = (CoordinateSet) p.find("COORDINATE_SET").evaluate(ctx, null);
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
                ).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                    ResourceLocation lootTable = (ResourceLocation) p.find("RESOURCE_LOCATION").evaluate(ctx, null);
                    CoordinateSet pos = (CoordinateSet) p.find("COORDINATE_SET").evaluate(ctx, null);
                    ToolOrHand tool = (ToolOrHand) p.findThenEvaluate("TOOL", null, ctx, null);
                    return new LootFromFish(lootTable.toString(), pos, tool);
                }),
                group(literal("kill"), productions.getOrCreateStructure("ENTITY")).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                    Entity entity = (Entity) p.find("ENTITY").evaluate(ctx, null);

                    try {
                        return new LootFromKill(entity);
                    } catch (CommodoreException x) {
                        TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                .map(CommodoreException.Source.ENTITY_ERROR, p.tryFind("ENTITY"))
                                .invokeThrow();
                        return null;
                    }
                }),
                group(literal("loot"), TridentProductions.noToken().addTags("cspn:Loot Table"), productions.getOrCreateStructure("RESOURCE_LOCATION")).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                    ResourceLocation lootTable = (ResourceLocation) p.find("RESOURCE_LOCATION").evaluate(ctx, null);
                    return new LootFromLoot(lootTable.toString());
                }),
                group(literal("mine"), productions.getOrCreateStructure("COORDINATE_SET"), toolMatch).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                    CoordinateSet pos = (CoordinateSet) p.find("COORDINATE_SET").evaluate(ctx, null);
                    ToolOrHand tool = (ToolOrHand) p.findThenEvaluate("TOOL", null, ctx, null);
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
        LootCommand.LootDestination destination = (LootCommand.LootDestination) pattern.find("LOOT_DESTINATION").evaluate(ctx, null);
        LootCommand.LootSource source = (LootCommand.LootSource) pattern.find("LOOT_SOURCE").evaluate(ctx, null);
        return new LootCommand(destination, source);
    }
}
