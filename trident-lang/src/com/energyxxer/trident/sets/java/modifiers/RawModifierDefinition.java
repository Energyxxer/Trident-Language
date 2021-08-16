package com.energyxxer.trident.sets.java.modifiers;

import com.energyxxer.commodore.functionlogic.commands.execute.ExecuteModifier;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class RawModifierDefinition implements ExecuteModifierDefinition {
    @Override
    public String[] getSwitchKeys() {
        return new String[] {"raw"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions, PrismarineProjectWorker worker) {
        return group(
                TridentProductions.modifierHeader("raw"),
                wrapper(
                        choice(
                                TridentProductions.string(productions),
                                PrismarineTypeSystem.validatorGroup(productions.getOrCreateStructure("INTERPOLATION_BLOCK"), true, String.class, ListObject.class)
                        ),
                        d -> new Object[] {d[0]},
                        (v, p, d) -> {
                            ISymbolContext ctx = (ISymbolContext) d[0];
                            if (v == null) return Collections.emptyList();
                            if (v instanceof String) {
                                if (((String) v).isEmpty()) return Collections.emptyList();
                                return Collections.singletonList(new RawExecuteModifier(((String) v)));
                            }
                            ArrayList<ExecuteModifier> modifiers = new ArrayList<>();
                            ListObject list = ((ListObject) v);
                            for (Object elem : list) {
                                if (elem instanceof String) {
                                    if (!((String) elem).isEmpty())
                                        modifiers.add(new RawExecuteModifier(((String) elem)));
                                } else {
                                    throw new PrismarineException(PrismarineTypeSystem.TYPE_ERROR, "Cannot turn an object of type " + ctx.getTypeSystem().getTypeIdentifierForObject(elem) + " into a string for a raw execute modifier", p, ctx);
                                }
                            }
                            return modifiers;
                        }
                )
        ).setSimplificationFunctionContentIndex(1);
    }

    @Override
    public Collection<ExecuteModifier> parse(TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new UnsupportedOperationException(); //this step is optimized away
    }
}
