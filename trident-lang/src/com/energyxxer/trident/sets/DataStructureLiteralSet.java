package com.energyxxer.trident.sets;

import com.energyxxer.commodore.CommandUtils;
import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.trident.compiler.analyzers.type_handlers.DictionaryObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListObject;
import com.energyxxer.trident.compiler.lexer.summaries.TridentSummaryModule;
import com.energyxxer.trident.compiler.semantics.symbols.TridentSymbolVisibility;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenStructureMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenList;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.providers.PatternProviderSet;
import com.energyxxer.prismarine.summaries.SummarySymbol;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;

import static com.energyxxer.trident.compiler.lexer.TridentTokens.STRING_LITERAL;
import static com.energyxxer.prismarine.PrismarineProductions.*;

public class DataStructureLiteralSet extends PatternProviderSet { //dictionaries, lists

    public static Object nextThis = null;
    public static String nextFunctionName = null;

    public DataStructureLiteralSet() {
        super("ROOT_INTERPOLATION_VALUE");
    }

    public static void setNextFunctionName(String nextFunctionName) {
        DataStructureLiteralSet.nextFunctionName = nextFunctionName;
    }

    @Override
    protected void installUtilityProductions(PrismarineProductions productions, TokenStructureMatch providerStructure) {
        productions.getOrCreateStructure("DICTIONARY").add(group(
                TridentProductions.brace("{").addProcessor(startComplexValue),
                list(
                        group(
                                choice(TridentProductions.identifierX(), ofType(STRING_LITERAL))
                                        .setName("DICTIONARY_KEY")
                                        .addProcessor((p, l) -> {
                                            if(l.getSummaryModule() != null) {
                                                String key = p.flatten(false);
                                                if(key.startsWith("\"") || key.startsWith("'")) {
                                                    try {
                                                        key = CommandUtils.parseQuotedString(key);
                                                    } catch(CommodoreException ignore) {
                                                    }
                                                }
                                                SummarySymbol sym = new SummarySymbol((TridentSummaryModule) l.getSummaryModule(), key, TridentSymbolVisibility.MEMBER_ACCESS_ONLY, p.getStringLocation().index);
//                                                ((TridentSummaryModule) l.getSummaryModule()).addSymbolUsage(new TridentSummaryModule.SymbolUsage(p, key));
                                                sym.setDeclarationPattern(p);
                                                //sym.addUsage(p);
                                                ((TridentSummaryModule) l.getSummaryModule()).pushSubSymbol(sym);
                                            }
                                        }),
                                TridentProductions.colon(),
                                productions.getOrCreateStructure("INTERPOLATION_VALUE")
                        ).setName("DICTIONARY_ENTRY").addProcessor((p, l) -> {
                            if(l.getSummaryModule() != null) {
                                SummarySymbol sym = ((TridentSummaryModule) l.getSummaryModule()).popSubSymbol();
                                if(!sym.hasSubBlock()) {
                                    ((TridentSummaryModule) l.getSummaryModule()).addElement(sym);
                                }
                            }
                        }).addFailProcessor((ip, l) -> {if(ip != null && ip.getCharLength() > 0) endComplexValue.accept(null, l);}),
                        TridentProductions.comma()
                ).setOptional().setName("DICTIONARY_ENTRY_LIST"),
                TridentProductions.brace("}"))).addTags("primitive:dictionary").addProcessor(claimTopSymbol).addProcessor(endComplexValue).addFailProcessor((ip, l) -> {if(ip != null && ip.getCharLength() > 0) endComplexValue.accept(null, l);})
        .setEvaluator((p, d) -> {
            ISymbolContext ctx = (ISymbolContext) d[0];
            DictionaryObject dict = new DictionaryObject(ctx.getTypeSystem());

            TokenList entryList = (TokenList) p.find("DICTIONARY_ENTRY_LIST");

            if (entryList != null) {
                for (TokenPattern<?> entry : entryList.getContentsExcludingSeparators()) {
                    String key = entry.find("DICTIONARY_KEY").flatten(false);
                    if (key.startsWith("\"") || key.startsWith("'")) {
                        key = BasicLiteralSet.parseQuotedString(key, p, ctx);
                    }
                    nextThis = dict;
                    nextFunctionName = key;
                    Object value = entry.find("INTERPOLATION_VALUE").evaluate(ctx);
                    nextThis = null;
                    nextFunctionName = null;
                    dict.put(key, value);
                }
            }

            return dict;
        });

        productions.getOrCreateStructure("LIST").add(
                group(
                        TridentProductions.brace("[").addProcessor(startClosure),
                        list(
                                productions.getOrCreateStructure("INTERPOLATION_VALUE"), TridentProductions.comma()
                        ).setOptional().setName("LIST_ENTRIES"),
                        TridentProductions.brace("]")
                ).addProcessor(surroundBlock).addProcessor(endComplexValue).addFailProcessor((ip, l) -> {if(ip != null && ip.getCharLength() > 0) endComplexValue.accept(null, l);}))
                .addTags("primitive:list")
        .setEvaluator((p, d) -> {
            ISymbolContext ctx = (ISymbolContext) d[0];
            ListObject list = new ListObject(ctx.getTypeSystem());

            TokenList entryList = (TokenList) p.find("LIST_ENTRIES");

            if (entryList != null) {
                for (TokenPattern<?> entry : entryList.getContentsExcludingSeparators()) {
                    nextThis = list;
                    list.add(entry.evaluate(ctx));
                    nextThis = null;
                }
            }
            return list;
        });

        providerStructure.add(productions.getOrCreateStructure("DICTIONARY"));
        providerStructure.add(productions.getOrCreateStructure("LIST"));
    }
}
