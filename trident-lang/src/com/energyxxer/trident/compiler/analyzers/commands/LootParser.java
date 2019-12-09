package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.loot.*;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.types.defaults.ItemSlot;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.custom.items.NBTMode;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "loot")
public class LootParser implements SimpleCommandParser {
    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        LootCommand.LootDestination destination = parseDestination(pattern.find("LOOT_DESTINATION"), ctx);
        LootCommand.LootSource source = parseSource(pattern.find("LOOT_SOURCE"), ctx);
        return new LootCommand(destination, source);
    }

    private LootCommand.LootDestination parseDestination(TokenPattern<?> pattern, ISymbolContext ctx) {
        pattern = ((TokenStructure) pattern).getContents();
        switch(pattern.getName()) {
            case "GIVE": {
                try {
                    return new LootGive(EntityParser.parseEntity(pattern.find("ENTITY"), ctx));
                } catch(CommodoreException x) {
                    TridentException.handleCommodoreException(x, pattern, ctx)
                            .map(CommodoreException.Source.ENTITY_ERROR, pattern.find("ENTITY"))
                            .invokeThrow();
                }
            }
            case "INSERT": {
                return new LootInsertBlock(CoordinateParser.parse(pattern.find("COORDINATE_SET"), ctx));
            }
            case "REPLACE": {
                Type slot = CommonParsers.parseType(pattern.find("SLOT_ID"), ctx, ItemSlot.CATEGORY);
                int count = -1;
                if(pattern.find("COUNT") != null) {
                    count = CommonParsers.parseInt(pattern.find("COUNT"), ctx);
                }

                TokenPattern<?> rawCoord = pattern.find("CHOICE.COORDINATE_SET");
                try {
                    if(rawCoord != null) return new LootReplaceBlock(CoordinateParser.parse(rawCoord, ctx), slot, count);
                    else return new LootReplaceEntity(EntityParser.parseEntity(pattern.find("CHOICE.ENTITY"), ctx), slot, count);
                } catch(CommodoreException x) {
                    TridentException.handleCommodoreException(x, pattern, ctx)
                            .map(CommodoreException.Source.NUMBER_LIMIT_ERROR, pattern.find("COUNT"))
                            .invokeThrow();
                }
            }
            case "SPAWN": {
                return new LootSpawn(CoordinateParser.parse(pattern.find("COORDINATE_SET"), ctx));
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + pattern.getName() + "'", pattern, ctx);
            }
        }
    }

    private LootCommand.LootSource parseSource(TokenPattern<?> pattern, ISymbolContext ctx) {
        pattern = ((TokenStructure) pattern).getContents();
        switch(pattern.getName()) {
            case "FISH": {
                TridentUtil.ResourceLocation table = CommonParsers.parseResourceLocation(pattern.find("RESOURCE_LOCATION"), ctx);
                table.assertStandalone(pattern.find("RESOURCE_LOCATION"), ctx);
                CoordinateSet pos = CoordinateParser.parse(pattern.find("COORDINATE_SET"), ctx);
                ToolOrHand tool = parseTool(pattern.find("TOOL"), ctx);
                return new LootFromFish(table.toString(), pos, tool);
            }
            case "KILL": {
                try {
                    return new LootFromKill(EntityParser.parseEntity(pattern.find("ENTITY"), ctx));
                } catch(CommodoreException x) {
                    TridentException.handleCommodoreException(x, pattern, ctx)
                            .map(CommodoreException.Source.ENTITY_ERROR, pattern.find("ENTITY"))
                            .invokeThrow();
                }
            }
            case "LOOT": {
                TridentUtil.ResourceLocation table = CommonParsers.parseResourceLocation(pattern.find("RESOURCE_LOCATION"), ctx);
                table.assertStandalone(pattern.find("RESOURCE_LOCATION"), ctx);
                return new LootFromLoot(table.toString());
            }
            case "MINE": {
                CoordinateSet pos = CoordinateParser.parse(pattern.find("COORDINATE_SET"), ctx);
                ToolOrHand tool = parseTool(pattern.find("TOOL"), ctx);
                return new LootFromMine(pos, tool);
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + pattern.getName() + "'", pattern, ctx);
            }
        }
    }

    private ToolOrHand parseTool(TokenPattern<?> pattern, ISymbolContext ctx) {
        if(pattern == null) return null;
        pattern = ((TokenStructure) pattern).getContents();
        switch(pattern.getName()) {
            case "LITERAL_MAINHAND": return ToolOrHand.MAINHAND;
            case "LITERAL_OFFHAND": return ToolOrHand.OFFHAND;
            default: return new ToolOrHand(CommonParsers.parseItem(pattern, ctx, NBTMode.TESTING));
        }
    }
}
