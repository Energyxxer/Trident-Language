package com.energyxxer.trident.compiler.commands.parsers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.execute.EntityAnchor;
import com.energyxxer.commodore.functionlogic.commands.teleport.TeleportCommand;
import com.energyxxer.commodore.functionlogic.commands.teleport.destination.BlockDestination;
import com.energyxxer.commodore.functionlogic.commands.teleport.destination.EntityDestination;
import com.energyxxer.commodore.functionlogic.commands.teleport.destination.TeleportDestination;
import com.energyxxer.commodore.functionlogic.commands.teleport.facing.BlockFacing;
import com.energyxxer.commodore.functionlogic.commands.teleport.facing.EntityFacing;
import com.energyxxer.commodore.functionlogic.commands.teleport.facing.RotationFacing;
import com.energyxxer.commodore.functionlogic.commands.teleport.facing.TeleportFacing;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.commands.parsers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.commands.parsers.constructs.EntityParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserMember;
import com.energyxxer.trident.compiler.semantics.TridentFile;

@ParserMember(key = "teleport")
public class TeleportParser implements CommandParser {
    @Override
    public Command parse(TokenPattern<?> pattern, TridentFile file) {
        Entity victim;
        TeleportDestination destination;
        TeleportFacing facing = null;

        TokenPattern<?> rawDestination = pattern.find("SUBCOMMAND.COORDINATE_SET");
        if(rawDestination != null) {
            victim = null;
            destination = new BlockDestination(CoordinateParser.parse(rawDestination, file.getCompiler()));
        } else {
            Entity first = EntityParser.parseEntity(pattern.find("SUBCOMMAND..ENTITY"), file.getCompiler());
            rawDestination = pattern.find("SUBCOMMAND..CHOICE");
            if(rawDestination != null) {
                victim = first;
                TokenPattern<?> rawDestinationEntity = rawDestination.find("ENTITY");
                if(rawDestinationEntity != null) {
                    destination = new EntityDestination(EntityParser.parseEntity(rawDestinationEntity, file.getCompiler()));
                } else {
                    destination = new BlockDestination(CoordinateParser.parse(rawDestination.find(".COORDINATE_SET"), file.getCompiler()));
                    TokenPattern<?> rotationOption = rawDestination.find("ROTATION_OPTION");
                    if(rotationOption != null) rotationOption = ((TokenStructure)rotationOption).getContents();

                    if(rotationOption != null) {
                        switch(rotationOption.getName()) {
                            case "FACING_CLAUSE": {
                                TokenPattern<?> facingBlock = rotationOption.find("CHOICE.COORDINATE_SET");
                                if(facingBlock != null) {
                                    facing = new BlockFacing(CoordinateParser.parse(facingBlock, file.getCompiler()));
                                } else {
                                    Entity facingEntity = EntityParser.parseEntity(rotationOption.find("CHOICE..ENTITY"), file.getCompiler());
                                    TokenPattern<?> rawAnchor = rotationOption.find("CHOICE..ANCHOR");
                                    if(rawAnchor != null) {
                                        facing = new EntityFacing(facingEntity, EntityAnchor.valueOf(rawAnchor.flatten(false).toUpperCase()));
                                    } else {
                                        facing = new EntityFacing(facingEntity);
                                    }
                                }
                                break;
                            }
                            case "TWO_COORDINATE_SET": {
                                facing = new RotationFacing(CoordinateParser.parseRotation(rotationOption));
                                break;
                            }
                        }
                    }
                }
            } else {
                victim = null;
                destination = new EntityDestination(first);
            }
        }
        return new TeleportCommand(victim, destination, facing);
    }

    @ParserMember(key = "tp")
    public static class TeleportParserAlias extends TeleportParser implements CommandParser {}
}
