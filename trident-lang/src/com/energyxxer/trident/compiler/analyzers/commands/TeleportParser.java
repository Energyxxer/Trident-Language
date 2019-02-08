package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.CommodoreException;
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
import com.energyxxer.trident.compiler.analyzers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.TridentException;

@AnalyzerMember(key = "teleport")
public class TeleportParser implements SimpleCommandParser {
    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        Entity victim;
        TeleportDestination destination;
        TeleportFacing facing = null;

        TokenPattern<?> rawDestination = pattern.find("SUBCOMMAND.COORDINATE_SET");
        if(rawDestination != null) {
            victim = null;
            destination = new BlockDestination(CoordinateParser.parse(rawDestination, ctx));
        } else {
            Entity first = EntityParser.parseEntity(pattern.find("SUBCOMMAND..ENTITY"), ctx);
            rawDestination = pattern.find("SUBCOMMAND..CHOICE");
            if(rawDestination != null) {
                victim = first;
                TokenPattern<?> rawDestinationEntity = rawDestination.find("ENTITY");
                if(rawDestinationEntity != null) {
                    try {
                        destination = new EntityDestination(EntityParser.parseEntity(rawDestinationEntity, ctx));
                    } catch(CommodoreException x) {
                        TridentException.handleCommodoreException(x, pattern, ctx)
                                .map(CommodoreException.Source.ENTITY_ERROR, rawDestinationEntity)
                                .invokeThrow();
                        throw new TridentException(TridentException.Source.IMPOSSIBLE, "Impossible code reached", rawDestinationEntity, ctx);
                    }
                } else {
                    destination = new BlockDestination(CoordinateParser.parse(rawDestination.find(".COORDINATE_SET"), ctx));
                    TokenPattern<?> rotationOption = rawDestination.find("ROTATION_OPTION");
                    if(rotationOption != null) rotationOption = ((TokenStructure)rotationOption).getContents();

                    if(rotationOption != null) {
                        switch(rotationOption.getName()) {
                            case "FACING_CLAUSE": {
                                TokenPattern<?> facingBlock = rotationOption.find("CHOICE.COORDINATE_SET");
                                if(facingBlock != null) {
                                    facing = new BlockFacing(CoordinateParser.parse(facingBlock, ctx));
                                } else {
                                    Entity facingEntity = EntityParser.parseEntity(rotationOption.find("CHOICE..ENTITY"), ctx);
                                    TokenPattern<?> rawAnchor = rotationOption.find("CHOICE..ANCHOR");
                                    try {
                                        if(rawAnchor != null) {
                                            facing = new EntityFacing(facingEntity, EntityAnchor.valueOf(rawAnchor.flatten(false).toUpperCase()));
                                        } else {
                                            facing = new EntityFacing(facingEntity);
                                        }
                                    } catch(CommodoreException x) {
                                        TridentException.handleCommodoreException(x, pattern, ctx)
                                                .map(CommodoreException.Source.ENTITY_ERROR, rotationOption.find("CHOICE..ENTITY"))
                                                .invokeThrow();
                                    }
                                }
                                break;
                            }
                            case "TWO_COORDINATE_SET": {
                                facing = new RotationFacing(CoordinateParser.parseRotation(rotationOption, ctx));
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

    @AnalyzerMember(key = "tp")
    public static class TeleportParserAlias extends TeleportParser implements SimpleCommandParser {}
}
