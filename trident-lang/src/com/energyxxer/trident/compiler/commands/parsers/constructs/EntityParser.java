package com.energyxxer.trident.compiler.commands.parsers.constructs;

import com.energyxxer.commodore.functionlogic.entity.Entity;
import com.energyxxer.commodore.functionlogic.entity.GenericEntity;
import com.energyxxer.commodore.functionlogic.score.PlayerName;
import com.energyxxer.commodore.functionlogic.selector.Selector;
import com.energyxxer.commodore.functionlogic.selector.arguments.SelectorArgument;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.pattern_matching.structures.TokenStructure;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.parsers.constructs.selectors.SelectorArgumentParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserManager;
import com.energyxxer.util.logger.Debug;

public class EntityParser {

    public static Selector parseSelector(TokenPattern<?> pattern, TridentCompiler compiler) {
        char header = pattern.find("SELECTOR_HEADER").flattenTokens().get(0).value.charAt(1);
        Selector selector = new Selector(Selector.BaseSelector.getForHeader(header + ""));

        TokenPattern<?> argList = pattern.find("..SELECTOR_ARGUMENT_LIST");
        if(argList instanceof TokenList) {
            TokenList list = (TokenList) argList;
            for(TokenPattern<?> rawArg : list.getContents()) {
                if(rawArg.getName().equals("SELECTOR_ARGUMENT")) {
                    SelectorArgumentParser parser = ParserManager.getParser(SelectorArgumentParser.class, rawArg.flattenTokens().get(0).value);
                    if(parser != null) {
                        SelectorArgument arg = parser.parse(((TokenStructure)((TokenStructure) rawArg).getContents().find("SELECTOR_ARGUMENT_VALUE")).getContents(), compiler);
                        if(arg != null) {
                            try {
                                selector.addArgument(arg);
                            } catch(IllegalArgumentException x) {
                                compiler.getReport().addNotice(new Notice(NoticeType.ERROR, x.getMessage(), rawArg));
                            }
                        }
                    }
                    //selector.addArgument(parseArgument(((TokenStructure)rawArg).getContents()));
                }
            }
        }

        return selector;
    }

    /**
     * EntityParser should not be instantiated.
     * */
    private EntityParser() {
    }

    public static Entity parseEntity(TokenPattern<?> pattern, TridentCompiler compiler) {
        if(pattern == null) return null;
        TokenPattern<?> inner = ((TokenStructure) pattern).getContents();
        switch(inner.getName()) {
            case "SELECTOR": return new GenericEntity(parseSelector(inner, compiler));
            case "PLAYER_NAME": return new PlayerName(inner.flatten(false));
            case "VARIABLE_MARKER": return new PlayerName("\$var:" + inner.find("VARIABLE_NAME").flatten(false));
        }
        Debug.log(pattern);
        return new PlayerName("sth");
    }
}
