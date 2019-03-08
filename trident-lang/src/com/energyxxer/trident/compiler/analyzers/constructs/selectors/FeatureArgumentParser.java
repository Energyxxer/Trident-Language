package com.energyxxer.trident.compiler.analyzers.constructs.selectors;

import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.TagArgument;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.constructs.InterpolationManager;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.custom.entities.CustomEntity;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "feature")
public class FeatureArgumentParser implements SimpleSelectorArgumentParser {
    @Override
    public SelectorArgument parseSingle(TokenPattern<?> pattern, ISymbolContext ctx) {
        CustomEntity feature = InterpolationManager.parse(pattern.find("INTERPOLATION_VALUE"), ctx, CustomEntity.class);
        if(!feature.isFeature()) {
            throw new TridentException(TridentException.Source.TYPE_ERROR, "Expected entity feature, instead got custom entity", pattern, ctx);
        }
        return new TagArgument(feature.getIdTag(), pattern.find("NEGATED") != null);
    }
}
