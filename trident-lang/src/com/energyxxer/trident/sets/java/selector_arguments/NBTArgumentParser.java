package com.energyxxer.trident.sets.java.selector_arguments;

import com.energyxxer.commodore.functionlogic.nbt.TagCompound;
import com.energyxxer.commodore.functionlogic.selector.arguments.NBTArgument;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.constructs.NBTInspector;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.providers.PatternSwitchProviderUnit;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.enxlex.pattern_matching.matching.TokenPatternMatch;
import com.energyxxer.nbtmapper.PathContext;

import static com.energyxxer.prismarine.PrismarineProductions.*;

public class NBTArgumentParser implements PatternSwitchProviderUnit {
    @Override
    public String[] getSwitchKeys() {
        return new String[] {"nbt"};
    }

    @Override
    public TokenPatternMatch createPatternMatch(PrismarineProductions productions) {
        return group(
                literal("nbt").setName("SELECTOR_ARGUMENT_KEY"),
                TridentProductions.equals(),
                group(TridentProductions.not().setOptional(), productions.getOrCreateStructure("NBT_COMPOUND")).setEvaluator((p, d) -> {
                    ISymbolContext ctx = (ISymbolContext) d[0];
                    PathContext pathContext = (PathContext) d[1];
                    TagCompound nbt = (TagCompound) p.find("NBT_COMPOUND").evaluate(ctx);
                    NBTInspector.inspectTag(nbt, pathContext, p.find("NBT_COMPOUND"), ctx);
                    return new NBTArgument(nbt, p.find("NEGATED") != null);
                })
        ).setSimplificationFunctionContentIndex(2);
    }
}
