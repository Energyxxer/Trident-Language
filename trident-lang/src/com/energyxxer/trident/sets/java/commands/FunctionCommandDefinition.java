package com.energyxxer.trident.sets.java.commands;

import com.energyxxer.commodore.functionlogic.commands.Command;
import com.energyxxer.commodore.functionlogic.commands.function.FunctionCommand;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenGroup;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.suggestions.SuggestionTags;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.commands.SimpleCommandDefinition;
import com.energyxxer.trident.compiler.analyzers.constructs.CommonParsers;
import com.energyxxer.trident.compiler.lexer.TridentSuggestionTags;
import com.energyxxer.trident.compiler.semantics.TridentFile;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class FunctionCommandDefinition implements SimpleCommandDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[]{"function"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                TridentProductions.commandHeader("function"),
                choice(
                        group(
                                TridentProductions.resourceLocationFixer,
                                wrapper(productions.getOrCreateStructure("RESOURCE_LOCATION_TAGGED")).setName("FUNCTION_REFERENCE").addTags(TridentSuggestionTags.RESOURCE, TridentSuggestionTags.FUNCTION)
                        ).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];

                            return new FunctionCommand(CommonParsers.parseFunctionTag((TokenStructure) p.find("FUNCTION_REFERENCE.RESOURCE_LOCATION_TAGGED"), ctx));
                        }),
                        group(productions.getOrCreateStructure("OPTIONAL_NAME_INNER_FUNCTION")).setEvaluator((p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];

                            TridentFile inner = TridentFile.createInnerFile(((TokenGroup) p).getContents()[0], ctx);
                            return new FunctionCommand(inner.getFunction());
                        })
                ).setGreedy(true).addTags(SuggestionTags.ENABLED).setName("INNER")
        ).setSimplificationFunctionFind("INNER");
    }

    @Override
    public Command parseSimple(TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new UnsupportedOperationException(); //this step is optimized away
    }
}
