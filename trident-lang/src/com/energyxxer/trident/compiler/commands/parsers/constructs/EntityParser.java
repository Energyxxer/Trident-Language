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
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.energyxxer.trident.compiler.commands.parsers.constructs.selectors.SelectorArgumentParser;
import com.energyxxer.trident.compiler.commands.parsers.general.ParserManager;
import com.energyxxer.util.logger.Debug;

import java.util.Collection;

public class EntityParser {

    public static Selector parseSelector(TokenPattern<?> pattern, TridentCompiler compiler) {
        char header = pattern.find("SELECTOR_HEADER").flattenTokens().get(0).value.charAt(1);
        Selector selector = new Selector(Selector.BaseSelector.getForHeader(header + ""));

        TokenPattern<?> argList = pattern.find("..SELECTOR_ARGUMENT_LIST");
        if(argList instanceof TokenList) {
            TokenList list = (TokenList) argList;
            parseSelectorArguments(list, selector, pattern, compiler);
        }

        return selector;
    }

    private static void parseSelectorArguments(TokenList list, Selector selector, TokenPattern<?> pattern, TridentCompiler compiler) {
        for(TokenPattern<?> rawArg : list.getContents()) {
            if(rawArg.getName().equals("SELECTOR_ARGUMENT")) {
                SelectorArgumentParser parser = ParserManager.getParser(SelectorArgumentParser.class, rawArg.flattenTokens().get(0).value);
                if(parser != null) {
                    Collection<SelectorArgument> arg = parser.parse(((TokenStructure)((TokenStructure) rawArg).getContents().find("SELECTOR_ARGUMENT_VALUE")).getContents(), compiler);
                    if(arg != null && !arg.isEmpty()) {
                        try {
                            selector.addArguments(arg);
                        } catch(IllegalArgumentException x) {
                            compiler.getReport().addNotice(new Notice(NoticeType.ERROR, x.getMessage(), rawArg));
                        }
                    }
                }
            }
        }
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
            case "ENTITY_VARIABLE": {
                Entity symbol = CommonParsers.retrieveSymbol(inner.find("VARIABLE_MARKER"), compiler, Entity.class);
                if(inner.find("APPENDED_ARGUMENTS") != null) {
                    if(!(symbol instanceof GenericEntity)) {
                        compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "The entity contained in this variable does not support selector arguments", inner));
                        throw new EntryParsingException();
                    }

                    Selector copy = ((GenericEntity) symbol).getSelector().clone();

                    TokenList argList = (TokenList) inner.find("APPENDED_ARGUMENTS.SELECTOR_ARGUMENT_LIST");
                    if(argList != null) parseSelectorArguments(argList, copy, pattern, compiler);

                    return new GenericEntity(copy);
                } else return symbol;
            }
        }
        Debug.log(pattern);
        return new PlayerName("sth");
    }
}
