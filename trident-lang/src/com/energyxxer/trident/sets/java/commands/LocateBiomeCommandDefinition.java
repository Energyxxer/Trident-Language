package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.locatebiome.LocateBiomeCommand;
import com.energyxxer.commodore.types.Type;
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

import static com.energyxxer.prismarine.PrismarineProductions.group;
import static com.energyxxer.prismarine.PrismarineProductions.wrapper;
import static com.energyxxer.trident.compiler.TridentProductions.checkVersionFeature;
import static com.energyxxer.trident.compiler.TridentProductions.commandHeader;

public class LocateBiomeCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"locatebiome"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                commandHeader("locatebiome"),
                wrapper(productions.getOrCreateStructure(checkVersionFeature(worker, "type_tags.universal", false) ? "BIOME_ID_TAGGED" : "BIOME_ID")).setName("BIOME")
        ).addProcessor((p, l) -> {
            if(l.getInspectionModule() != null && p.find("BIOME.BIOME_ID_TAGGED") != null) {

                StringBounds bounds = p.getStringBounds();

                Inspection inspection = new Inspection("Convert to locate biome command")
                        .setStartIndex(bounds.start.index)
                        .setEndIndex(bounds.end.index)
                        .addAction(
                                new CodeChainAction(
                                        "Convert to locate biome command",

                                        new CodeReplacementAction()
                                                .setReplacementStartIndex(bounds.start.index)
                                                .setReplacementEndIndex(bounds.start.index + "locatebiome".length())
                                                .setReplacementText("locate biome")
                                )
                        ).setSeverity(InspectionSeverity.SUGGESTION);

                l.getInspectionModule().addInspection(inspection);
            }
        });
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        return new LocateBiomeCommand((Type) pattern.find("BIOME").evaluate(ctx));
    }
}
