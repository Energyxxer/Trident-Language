package com.energyxxer.trident.compiler.analyzers.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.locatebiome.LocateBiomeCommand;
import com.energyxxer.commodore.types.defaults.BiomeType;
import com.energyxxer.commodore.versioning.compatibility.VersionFeatureManager;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "locatebiome")
public class LocateBiomeParser implements SimpleCommandParser {
    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        return new LocateBiomeCommand(CommonParsers.parseType(((TokenStructure) pattern.find("BIOME_ID")).getContents(), ctx, BiomeType.CATEGORY, VersionFeatureManager.getBoolean("custom_biomes")));
    }
}
