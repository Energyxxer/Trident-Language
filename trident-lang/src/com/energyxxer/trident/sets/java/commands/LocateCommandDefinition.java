package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.locate.LocateCommand;
import com.energyxxer.commodore.functionlogic.commands.locate.LocatePOICommand;
import com.energyxxer.commodore.functionlogic.commands.locatebiome.LocateBiomeCommand;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.types.defaults.StructureType;
import com.energyxxer.enxlex.lexical_analysis.inspections.CodeChainAction;
import com.energyxxer.enxlex.lexical_analysis.inspections.CodeReplacementAction;
import com.energyxxer.enxlex.lexical_analysis.inspections.Inspection;
import com.energyxxer.enxlex.lexical_analysis.inspections.InspectionSeverity;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.util.StringBounds;

import static com.energyxxer.prismarine.PrismarineProductions.*;
import static com.energyxxer.trident.compiler.TridentProductions.checkVersionFeature;
import static com.energyxxer.trident.compiler.TridentProductions.commandHeader;

public class LocateCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"locate"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                commandHeader("locate"),
                choice(
                        checkVersionFeature(worker, "command.locate.merge", false) ? choice(
                                group(literal("structure"), productions.getOrCreateStructure("STRUCTURE_ID_TAGGED"))
                                        .setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                                            Type structure = (Type) p.find("STRUCTURE_ID_TAGGED").evaluate(ctx, null);
                                            return new LocateCommand(structure);
                                        }),
                                group(literal("poi"), productions.getOrCreateStructure("POINT_OF_INTEREST_TYPE_ID_TAGGED"))
                                        .setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                                            Type poi = (Type) p.find("POINT_OF_INTEREST_TYPE_ID_TAGGED").evaluate(ctx, null);
                                            return new LocatePOICommand(poi);
                                        }),
                                group(literal("biome"), productions.getOrCreateStructure("BIOME_ID_TAGGED"))
                                        .setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                                            Type biome = (Type) p.find("BIOME_ID_TAGGED").evaluate(ctx, null);
                                            return new LocateBiomeCommand(biome);
                                        })
                        ) : null,
                        checkVersionFeature(worker, "type_tags.universal", false) ?
                                group(
                                        productions.getOrCreateStructure("LEGACY_STRUCTURE_ID")
                                ).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                                    Type legacyType = (Type) p.find("LEGACY_STRUCTURE_ID").evaluate(ctx, null);
                                    Type structure = legacyType.getNamespace().tags.getGroup(StructureType.CATEGORY).get(legacyType.getName());

                                    return new LocateCommand(structure);
                                }).addProcessor((p, l) -> {
                                    if(l.getInspectionModule() != null) {

                                        StringBounds bounds = p.getStringBounds();

                                        String flattened = p.flatten(false);
                                        if(flattened.startsWith("$")) return;

                                        Inspection inspection = new Inspection("Convert to locate structure command")
                                                .setStartIndex(bounds.start.index)
                                                .setEndIndex(bounds.end.index)
                                                .addAction(
                                                        new CodeChainAction(
                                                                "Convert to locate structure command",

                                                                new CodeReplacementAction()
                                                                        .setReplacementStartIndex(bounds.start.index)
                                                                        .setReplacementEndIndex(bounds.end.index)
                                                                        .setReplacementText("structure #minecraft:" + flattened)
                                                        )
                                                ).setSeverity(InspectionSeverity.SUGGESTION);

                                        l.getInspectionModule().addInspection(inspection);
                                    }
                                }) :
                                group(
                                        productions.getOrCreateStructure("STRUCTURE_ID")
                                ).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> {
                                    Type structure = (Type) p.find("STRUCTURE_ID").evaluate(ctx, null);
                                    return new LocateCommand(structure);
                                })
                )
        ).setSimplificationFunctionContentIndex(1);
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new UnsupportedOperationException(); //this step is optimized away
    }
}
