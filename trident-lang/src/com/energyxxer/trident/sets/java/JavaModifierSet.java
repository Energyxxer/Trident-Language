package com.energyxxer.trident.sets.java;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.trident.compiler.lexer.TridentSuggestionTags;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.providers.PatternSwitchProviderSet;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.trident.sets.java.modifiers.*;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenStructureMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.suggestions.SuggestionTags;

import java.util.ArrayList;
import java.util.Collection;

import static com.energyxxer.prismarine.PrismarineProductions.list;
import static com.energyxxer.trident.compiler.lexer.TridentTokens.MODIFIER_HEADER;

public class JavaModifierSet extends PatternSwitchProviderSet {
    public JavaModifierSet() {
        super("MODIFIER", MODIFIER_HEADER);
        importUnits(
                AlignModifierDefinition.class,
                AnchoredModifierDefinition.class,
                AsModifierDefinition.class,
                AtModifierDefinition.class,
                ConditionalModifierDefinition.class,
                FacingModifierDefinition.class,
                InModifierDefinition.class,
                PositionedModifierDefinition.class,
                RawModifierDefinition.class,
                RotatedModifierDefinition.class,
                StoreModifierDefinition.class
        );
    }

    @Override
    protected void installUtilityProductions(PrismarineProductions productions, TokenStructureMatch providerStructure) {
        super.installUtilityProductions(productions, providerStructure);

        productions.getOrCreateStructure("MODIFIER").addTags(SuggestionTags.ENABLED, TridentSuggestionTags.CONTEXT_MODIFIER);

        productions.getOrCreateStructure("MODIFIER_LIST").add(
                list(productions.getOrCreateStructure("MODIFIER")).setEvaluator((p, d) -> {
                    ISymbolContext ctx = (ISymbolContext) d[0];
                    ArrayList<ExecuteModifier> modifiers = new ArrayList<>();
                    for(TokenPattern<?> rawModifier : ((TokenList) p).getContents()) {
                        Object evaluationResult = null;
                        try {
                            evaluationResult = rawModifier.evaluate((ISymbolContext) d[0], null);
                        } catch(CommodoreException x) {
                            TridentExceptionUtil.handleCommodoreException(x, p, (ISymbolContext) d[0])
                                    .invokeThrow();
                        }
                        if(evaluationResult instanceof ExecuteModifier) {
                            modifiers.add((ExecuteModifier) evaluationResult);
                        } else {
                            Collection<ExecuteModifier> modifiersFromThisEntry = (Collection<ExecuteModifier>) evaluationResult;
                            if(modifiersFromThisEntry != null) {
                                modifiers.addAll(modifiersFromThisEntry);
                            }
                        }
                    }
                    return modifiers;
                })
        ).setOptional();
    }
}
