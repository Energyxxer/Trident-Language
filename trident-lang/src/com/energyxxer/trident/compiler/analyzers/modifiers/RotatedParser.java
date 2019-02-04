package com.energyxxer.trident.compiler.analyzers.modifiers;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecutePositionedAsEntity;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.trident.compiler.analyzers.constructs.CoordinateParser;
import com.energyxxer.trident.compiler.analyzers.constructs.EntityParser;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.TridentException;

@AnalyzerMember(key = "rotated")
public class RotatedParser implements SimpleModifierParser {
    @Override
    public ExecuteModifier parseSingle(TokenPattern<?> pattern, ISymbolContext ctx) {
        TokenPattern<?> branch = ((TokenStructure) pattern.find("CHOICE")).getContents();
        switch(branch.getName()) {
            case "ENTITY_BRANCH": {
                return new ExecutePositionedAsEntity(EntityParser.parseEntity(branch.find("ENTITY"), ctx));
            }
            case "TWO_COORDINATE_SET": {
                return CoordinateParser.parseRotation(branch, ctx);
            }
            default: {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown grammar branch name '" + branch.getName() + "'", branch, ctx);
            }
        }
    }
}