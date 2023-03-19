package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.place.PlaceFeatureCommand;
import com.energyxxer.commodore.functionlogic.commands.place.PlaceJigsawCommand;
import com.energyxxer.commodore.functionlogic.commands.place.PlaceStructureCommand;
import com.energyxxer.commodore.functionlogic.commands.place.PlaceTemplateCommand;
import com.energyxxer.commodore.functionlogic.coordinates.CoordinateSet;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;

import static com.energyxxer.prismarine.PrismarineProductions.*;
import static com.energyxxer.trident.compiler.TridentProductions.checkVersionFeature;
import static com.energyxxer.trident.compiler.TridentProductions.commandHeader;

public class PlaceCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"place"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        if(!checkVersionFeature(worker, "command.place.feature", false)) return null;
        return group(
                commandHeader("place"),
                choice(
                        group(literal("feature"), productions.getOrCreateStructure("WORLDGEN/CONFIGURED_FEATURE_ID"), wrapperOptional(productions.getOrCreateStructure("COORDINATE_SET")).setName("POS"))
                                .setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                                    Type type = (Type) p.find("WORLDGEN/CONFIGURED_FEATURE_ID").evaluate(ctx, null);
                                    CoordinateSet pos = (CoordinateSet) p.findThenEvaluate("POS", null, ctx, null);
                                    return new PlaceFeatureCommand(type, pos);
                                }),
                        group(literal("structure"), productions.getOrCreateStructure("STRUCTURE_ID"), wrapperOptional(productions.getOrCreateStructure("COORDINATE_SET")).setName("POS"))
                                .setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                                    Type type = (Type) p.find("STRUCTURE_ID").evaluate(ctx, null);
                                    CoordinateSet pos = (CoordinateSet) p.findThenEvaluate("POS", null, ctx, null);
                                    return new PlaceStructureCommand(type, pos);
                                }),
                        group(
                                literal("jigsaw"),
                                productions.getOrCreateStructure("WORLDGEN/TEMPLATE_POOL_ID"),
                                productions.getOrCreateStructure("JIGSAW_TARGET_ID"),
                                TridentProductions.integer(productions).setName("MAX_DEPTH"),
                                wrapperOptional(productions.getOrCreateStructure("COORDINATE_SET")).setName("POS")
                        )
                                .setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                                    Type type = (Type) p.find("WORLDGEN/TEMPLATE_POOL_ID").evaluate(ctx, null);
                                    Type target = (Type) p.find("JIGSAW_TARGET_ID").evaluate(ctx, null);
                                    int maxDepth = (int) p.find("MAX_DEPTH").evaluate(ctx, null);
                                    CoordinateSet pos = (CoordinateSet) p.findThenEvaluate("POS", null, ctx, null);
                                    return new PlaceJigsawCommand(type, target, maxDepth, pos);
                                }),
                        group(
                                literal("template"),
                                productions.getOrCreateStructure("TEMPLATE_ID"),
                                wrapperOptional(productions.getOrCreateStructure("COORDINATE_SET")).setName("POS"),
                                choice(enumChoice(PlaceTemplateCommand.Rotation.class), wrapper(TridentProductions.integer(productions), (Object v, TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                                    switch(Math.floorMod((int)v, 360)) {
                                        case 0: return PlaceTemplateCommand.Rotation.NONE;
                                        case 90: return PlaceTemplateCommand.Rotation.CLOCKWISE_90;
                                        case 180: return PlaceTemplateCommand.Rotation._180;
                                        case 270: return PlaceTemplateCommand.Rotation.COUNTERCLOCKWISE_90;
                                        default: throw new PrismarineException(TridentExceptionUtil.Source.COMMAND_ERROR, "Invalid rotation: " + v, p, ctx);
                                    }
                                })).setOptional().setName("ROTATION").addTags("cspn:Rotation"),
                                wrapperOptional(enumChoice(PlaceTemplateCommand.Mirror.class)).setName("MIRROR").addTags("cspn:Mirror")
                        )
                                .setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                                    Type type = (Type) p.find("TEMPLATE_ID").evaluate(ctx, null);
                                    CoordinateSet pos = (CoordinateSet) p.findThenEvaluate("POS", null, ctx, null);
                                    PlaceTemplateCommand.Rotation rotation = (PlaceTemplateCommand.Rotation) p.findThenEvaluate("ROTATION", PlaceTemplateCommand.Rotation.NONE, ctx, null);
                                    PlaceTemplateCommand.Mirror mirror = (PlaceTemplateCommand.Mirror) p.findThenEvaluate("MIRROR", PlaceTemplateCommand.Mirror.NONE, ctx, null);
                                    return new PlaceTemplateCommand(type, pos, rotation, mirror);
                                })
                )
        ).setSimplificationFunctionContentIndex(1);
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new UnsupportedOperationException(); //this step is optimized away
    }
}
