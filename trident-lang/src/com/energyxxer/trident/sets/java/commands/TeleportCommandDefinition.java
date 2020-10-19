package com.energyxxer.trident.sets.java.commands;

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
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.rotation.Rotation;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class TeleportCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"teleport", "tp"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        return group(
                choice(TridentProductions.commandHeader("teleport"), TridentProductions.commandHeader("tp")),
                choice(
                        group(
                                productions.getOrCreateStructure("ENTITY"),
                                choice(
                                        group(
                                                productions.getOrCreateStructure("COORDINATE_SET"),
                                                choice(
                                                        group(
                                                                literal("facing"),
                                                                choice(
                                                                        group(TridentProductions.noToken().addTags("cspn:Facing Coordinate"), productions.getOrCreateStructure("COORDINATE_SET")).setEvaluator((p, d) -> {
                                                                            CoordinateSet pos = (CoordinateSet) p.find("COORDINATE_SET").evaluate((ISymbolContext) d[0]);

                                                                            try {
                                                                                return new BlockFacing(pos);
                                                                            } catch (CommodoreException x) {
                                                                                TridentExceptionUtil.handleCommodoreException(x, p, (ISymbolContext) d[0])
                                                                                        .invokeThrow();
                                                                                return null;
                                                                            }
                                                                        }),
                                                                        group(TridentProductions.noToken().addTags("cspn:Facing Entity"), literal("entity"), productions.getOrCreateStructure("ENTITY"), wrapperOptional(productions.getOrCreateStructure("ANCHOR")).setName("ANCHOR")).setEvaluator((p, d) -> {
                                                                            ISymbolContext ctx = (ISymbolContext) d[0];
                                                                            Entity entity = (Entity) p.find("ENTITY").evaluate(ctx);
                                                                            EntityAnchor anchor = (EntityAnchor) p.findThenEvaluate("ANCHOR", EntityAnchor.FEET, ctx);
                                                                            try {
                                                                                return new EntityFacing(entity, anchor);
                                                                            } catch (CommodoreException x) {
                                                                                TridentExceptionUtil.handleCommodoreException(x, p, ctx)
                                                                                        .map(CommodoreException.Source.ENTITY_ERROR, p.tryFind("ENTITY"))
                                                                                        .invokeThrow();
                                                                                return null;
                                                                            }
                                                                        })
                                                                )
                                                        ).setSimplificationFunctionContentIndex(1),
                                                        wrapper(productions.getOrCreateStructure("ROTATION"), (v, p, d) -> {
                                                            try {
                                                                return new RotationFacing((Rotation) v);
                                                            } catch (CommodoreException x) {
                                                                TridentExceptionUtil.handleCommodoreException(x, p, (ISymbolContext) d[0])
                                                                        .invokeThrow();
                                                                return null;
                                                            }
                                                        })
                                                ).setOptional().setName("ROTATION_OPTION").addTags("cspn:Rotation")
                                        ).setEvaluator((p, d) -> {
                                            ISymbolContext ctx = (ISymbolContext) d[0];
                                            BlockDestination destination = new BlockDestination((CoordinateSet) p.find("COORDINATE_SET").evaluate(ctx));
                                            TeleportFacing facing = (TeleportFacing) p.findThenEvaluate("ROTATION_OPTION", null, ctx);
                                            return new Object[]{destination, facing};
                                        }),
                                        group(TridentProductions.sameLine(), productions.getOrCreateStructure("ENTITY")).setEvaluator((p, d) -> {
                                            Entity entity = (Entity) p.find("ENTITY").evaluate((ISymbolContext) d[0]);
                                            try {
                                                return new Object[]{new EntityDestination(entity)};
                                            } catch (CommodoreException x) {
                                                TridentExceptionUtil.handleCommodoreException(x, p, (ISymbolContext) d[0])
                                                        .map(CommodoreException.Source.ENTITY_ERROR, p.tryFind("ENTITY"))
                                                        .invokeThrow();
                                                return null;
                                            }
                                        }),
                                        PrismarineTypeSystem.validatorGroup(
                                                productions.getOrCreateStructure("INTERPOLATION_BLOCK"),
                                                d -> new Object[] {d[0]},
                                                (v, p, d) -> {
                                                    try {
                                                        if (v instanceof CoordinateSet)
                                                            return new Object[]{new BlockDestination((CoordinateSet) v)};
                                                        else return new Object[]{new EntityDestination((Entity) v)};
                                                    } catch (CommodoreException x) {
                                                        TridentExceptionUtil.handleCommodoreException(x, p, (ISymbolContext) d[0])
                                                                .invokeThrow();
                                                        return null;
                                                    }
                                                },
                                                false,
                                                CoordinateSet.class,
                                                Entity.class
                                        )
                                ).setOptional().addTags("cspn:Destination").setName("DESTINATION")
                        ).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            Entity entity = (Entity) p.find("ENTITY").evaluate(ctx);
                            Object[] destinationAndFacing = (Object[]) p.findThenEvaluate("DESTINATION", null, ctx);

                            try {
                                if (destinationAndFacing == null) {
                                    return new TeleportCommand(new EntityDestination(entity));
                                } else {
                                    TeleportDestination destination = (TeleportDestination) destinationAndFacing[0];
                                    TeleportFacing facing = destinationAndFacing.length > 1 ? (TeleportFacing) destinationAndFacing[1] : null;
                                    return new TeleportCommand(entity, destination, facing);
                                }
                            } catch (CommodoreException x) {
                                TridentExceptionUtil.handleCommodoreException(x, p, (ISymbolContext) d[0])
                                        .map(CommodoreException.Source.ENTITY_ERROR, p.tryFind("ENTITY"))
                                        .invokeThrow();
                                return null;
                            }
                        }),
                        group(productions.getOrCreateStructure("COORDINATE_SET")).setEvaluator((p, d) -> new TeleportCommand(new BlockDestination((CoordinateSet) p.find("COORDINATE_SET").evaluate((ISymbolContext) d[0]))))
                ).setName("INNER")
        ).setSimplificationFunctionFind("INNER");
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new UnsupportedOperationException(); //this step is optimized away
    }
}
