package com.energyxxer.trident.compiler;

import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.trident.compiler.semantics.ExceptionCollector;
import com.energyxxer.trident.compiler.semantics.TridentExceptionUtil;
import com.energyxxer.trident.compiler.semantics.TridentFile;

public class TridentUtil {
    /**
     * TridentUtil should not be instantiated.
     * */
    private TridentUtil() {
    }

    public static Entity getTopLevelEntity(Entity entity) {
        if(entity instanceof Selector) return new Selector(((Selector) entity).getBase());
        else return new Selector(Selector.BaseSelector.ALL_PLAYERS);
    }

    public static void assertLanguageLevel(ISymbolContext ctx, int minLevel, String featureDesc, TokenPattern<?> pattern) {
        assertLanguageLevel(ctx, minLevel, featureDesc, pattern, null);
    }

    public static void assertLanguageLevel(ISymbolContext ctx, int minLevel, String featureDesc, TokenPattern<?> pattern, ExceptionCollector collector) {
        if(((TridentFile) ctx.getStaticParentUnit()).getLanguageLevel() < minLevel) {
            PrismarineException x = new PrismarineException(TridentExceptionUtil.Source.LANGUAGE_LEVEL_ERROR, featureDesc + " only supported in language level " + minLevel + (minLevel < 3 ? " and above": ""), pattern, ctx);
            if(collector != null) {
                collector.log(x);
            } else {
                throw x;
            }
        }
    }

    public static String backCompatCategoryName(String name) {
        switch(name) {
            case "entity_type":
                return "entity";
            case "block_entity_type":
                return "block_entity";
            case "particle_type":
                return "particle";
            case "dimension_type":
                return "dimension";
            case "worldgen/structure":
                return "structure";
            case "worldgen/biome":
                return "biome";
            case "mob_effect":
                return "effect";
            case "painting_variant":
                return "motive";
            default:
                return name;
        }
    }
}
