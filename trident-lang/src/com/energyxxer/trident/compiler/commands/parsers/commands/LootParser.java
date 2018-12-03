package com.energyxxer.trident.compiler.commands.parsers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.loot.*;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.commands.parsers.constructs.EntityParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "loot")
public class LootParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        LootCommand.LootDestination destination = parseDestination(pattern.find("LOOT_DESTINATION"), file.getCompiler());
        LootCommand.LootSource source = parseSource(pattern.find("LOOT_SOURCE"), file.getCompiler());
        return new LootCommand(destination, source);
    }

    private LootCommand.LootDestination parseDestination(TokenPattern<?> pattern, TridentCompiler compiler) {
        pattern = ((TokenStructure) pattern).getContents();
        switch(pattern.getName()) {
            case "GIVE": {
                return new LootGive(EntityParser.parseEntity(pattern.find("ENTITY"), compiler));
            }
            case "INSERT": {
                return new LootInsertBlock(CoordinateParser.parse(pattern.find("COORDINATE_SET")));
            }
            case "REPLACE": {
                Type slot = compiler.getModule().minecraft.types.slot.get(pattern.find("SLOT_ID").flatten(false));

                TokenPattern<?> rawCoord = pattern.find("CHOICE.COORDINATE_SET");
                if(rawCoord != null) return new LootReplaceBlock(CoordinateParser.parse(rawCoord), slot);
                else return new LootReplaceEntity(EntityParser.parseEntity(pattern.find("CHOICE.ENTITY"), compiler), slot);
            }
            case "SPAWN": {
                return new LootSpawn(CoordinateParser.parse(pattern.find("COORDINATE_SET")));
            }
            default: {
                compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + pattern.getName() + "'", pattern));
                return null;
            }
        }
    }

    private LootCommand.LootSource parseSource(TokenPattern<?> pattern, TridentCompiler compiler) {
        pattern = ((TokenStructure) pattern).getContents();
        switch(pattern.getName()) {
            case "FISH": {
                TridentUtil.ResourceLocation table = new TridentUtil.ResourceLocation(pattern.find("RESOURCE_LOCATION").flatten(false));
                CoordinateSet pos = CoordinateParser.parse(pattern.find("COORDINATE_SET"));
                ToolOrHand tool = parseTool(pattern.find("TOOL"), compiler);
                return new LootFromFish(table.toString(), pos, tool);
            }
            case "KILL": {
                return new LootFromKill(EntityParser.parseEntity(pattern.find("ENTITY"), compiler));
            }
            case "LOOT": {
                TridentUtil.ResourceLocation table = new TridentUtil.ResourceLocation(pattern.find("RESOURCE_LOCATION").flatten(false));
                return new LootFromLoot(table.toString());
            }
            case "MINE": {
                CoordinateSet pos = CoordinateParser.parse(pattern.find("COORDINATE_SET"));
                ToolOrHand tool = parseTool(pattern.find("TOOL"), compiler);
                return new LootFromMine(pos, tool);
            }
            default: {
                compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Unknown grammar branch name '" + pattern.getName() + "'", pattern));
                return null;
            }
        }
    }

    private ToolOrHand parseTool(TokenPattern<?> pattern, TridentCompiler compiler) {
        if(pattern == null) return null;
        pattern = ((TokenStructure) pattern).getContents();
        switch(pattern.getName()) {
            case "LITERAL_MAINHAND": return ToolOrHand.MAINHAND;
            case "LITERAL_OFFHAND": return ToolOrHand.OFFHAND;
            default: return new ToolOrHand(CommonParsers.parseItem(pattern, compiler));
        }
    }
}
