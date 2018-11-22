package com.energyxxer.trident.compiler.commands.parsers.constructs;

import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.TridentCompiler;

public class EntityParser {

    public static Selector parseSelector(TokenPattern<?> pattern, TridentCompiler compiler) {
        char header = pattern.find("SELECTOR_HEADER").flattenTokens().get(0).value.charAt(1);
        Selector selector = new Selector(Selector.BaseSelector.getForHeader(header + ""));
        return selector;
    }

    /**
     * EntityParser should not be instantiated.
     * */
    private EntityParser() {
    }
}
