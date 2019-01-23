package com.energyxxer.trident.compiler.analyzers.constructs;

import com.energyxxer.trident.compiler.analyzers.constructs.selectors.SelectorArgumentParser;
import com.energyxxer.trident.extensions.EObject;
import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.score.PlayerName;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerManager;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.util.logger.Debug;

import java.util.Collection;

public class EntityParser {

    public static Selector parseSelector(TokenPattern<?> pattern, TridentFile file) {
        char header = pattern.find("SELECTOR_HEADER").flattenTokens().get(0).value.charAt(1);
        Selector selector = new Selector(Selector.BaseSelector.getForHeader(header + ""));

        TokenPattern<?> argList = pattern.find("..SELECTOR_ARGUMENT_LIST");
        if(argList instanceof TokenList) {
            TokenList list = (TokenList) argList;
            parseSelectorArguments(list, selector, pattern, file);
        }

        return selector;
    }

    private static void parseSelectorArguments(TokenList list, Selector selector, TokenPattern<?> pattern, TridentFile file) {
        for(TokenPattern<?> rawArg : list.getContents()) {
            if(rawArg.getName().equals("SELECTOR_ARGUMENT")) {
                SelectorArgumentParser parser = AnalyzerManager.getAnalyzer(SelectorArgumentParser.class, rawArg.flattenTokens().get(0).value);
                if(parser != null) {
                    Collection<SelectorArgument> arg = parser.parse(((TokenStructure)((TokenStructure) rawArg).getContents().find("SELECTOR_ARGUMENT_VALUE")).getContents(), file);
                    if(arg != null && !arg.isEmpty()) {
                        try {
                            selector.addArguments(arg);
                        } catch(IllegalArgumentException x) {
                            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, x.getMessage(), rawArg));
                        }
                    }
                } else {
                    throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown selector argument analyzer for '" + rawArg.flattenTokens().get(0).value + "'", rawArg, file);
                }
            }
        }
    }

    /**
     * EntityParser should not be instantiated.
     * */
    private EntityParser() {
    }

    public static Entity parseEntity(TokenPattern<?> pattern, TridentFile file) {
        if(pattern == null) return null;
        TokenPattern<?> inner = ((TokenStructure) pattern).getContents();
        switch(inner.getName()) {
            case "SELECTOR":
                return parseSelector(inner, file);
            case "PLAYER_NAME": return new PlayerName(inner.flatten(false));
            case "ENTITY_VARIABLE": {
                Entity symbol = InterpolationManager.parse(inner.find("INTERPOLATION_BLOCK"), file, Entity.class);
                EObject.assertNotNull(symbol, inner.find("INTERPOLATION_BLOCK"), file);
                if(inner.find("APPENDED_ARGUMENTS") != null) {
                    if(!(symbol instanceof Selector)) {
                        throw new TridentException(TridentException.Source.STRUCTURAL_ERROR, "The entity contained in this variable does not support selector arguments", inner, file);
                    }

                    Selector copy = ((Selector) symbol).clone();

                    TokenList argList = (TokenList) inner.find("APPENDED_ARGUMENTS.SELECTOR_ARGUMENT_LIST");
                    if(argList != null) parseSelectorArguments(argList, copy, pattern, file);

                    return copy;
                } else return symbol;
            }
        }
        Debug.log(pattern);
        return new PlayerName("sth");
    }
}
