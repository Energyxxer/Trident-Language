package com.energyxxer.trident.compiler.analyzers.constructs.selectors;

import com.energyxxer.commodore.functionlogic.selector.arguments.PredicateArgument;
import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.commodore.types.defaults.PredicateReference;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.nbtmapper.PathContext;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

@AnalyzerMember(key = "predicate")
public class PredicateArgumentParser implements SimpleSelectorArgumentParser {
    @Override
    public SelectorArgument parseSingle(TokenPattern<?> pattern, ISymbolContext ctx, PathContext pathContext) {
        boolean negated = pattern.find("NEGATED") != null;

        TridentUtil.ResourceLocation reference = CommonParsers.parseResourceLocation(pattern.find("RESOURCE_LOCATION"), ctx);
        reference.assertStandalone(pattern.find("RESOURCE_LOCATION"), ctx);

        return new PredicateArgument(new PredicateReference(ctx.getCompiler().getModule().getNamespace(reference.namespace), reference.body), negated);
    }
}
