package com.energyxxer.trident.sets;

import com.energyxxer.commodore.CommandUtils;
import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenStructureMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.suggestions.SuggestionTags;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.providers.PatternProviderSet;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.trident.compiler.TridentProductions;
import org.jetbrains.annotations.NotNull;

import static com.energyxxer.prismarine.PrismarineProductions.group;
import static com.energyxxer.prismarine.PrismarineProductions.ofType;
import static com.energyxxer.trident.compiler.lexer.TridentTokens.*;

public class BasicLiteralSet extends PatternProviderSet {

    public BasicLiteralSet() {
        super("ROOT_INTERPOLATION_VALUE");
    }

    @Override
    protected void installUtilityProductions(PrismarineProductions productions, TokenStructureMatch providerStructure, PrismarineProjectWorker worker) {

        productions.getOrCreateStructure("ROOT_INTERPOLATION_VALUE")
                .add(
                        productions.putPatternMatch("REAL_NUMBER", ofType(REAL_NUMBER).setName("RAW_REAL").addTags("primitive:real").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> Double.parseDouble(p.flatten(false))))
                )
                .add(
                        productions.putPatternMatch("INTEGER_NUMBER", ofType(INTEGER_NUMBER).setName("RAW_INTEGER").addTags("primitive:int").setEvaluator(BasicLiteralSet::evaluateIntegerPattern))
                )
                .add(
                        productions.putPatternMatch("BOOLEAN", ofType(BOOLEAN).addTags("primitive:boolean").addTags(SuggestionTags.ENABLED).setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> "true".equals(p.flatten(false))))
                )
                .add(
                        productions.putPatternMatch("STRING_LITERAL", ofType(STRING_LITERAL).setName("STRING_LITERAL").addTags("primitive:string").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> parseQuotedString(p.flatten(false), p, ctx)))
                )
                .add(
                        productions.putPatternMatch("NULL", ofType(NULL).setName("NULL_VALUE").setEvaluator((TokenPattern<?> p, ISymbolContext ctx, Object[] d) -> null))
                )
                .add(
                        group(TridentProductions.brace("("), productions.getOrCreateStructure("INTERPOLATION_VALUE"), TridentProductions.brace(")")).setName("PARENTHESIZED_VALUE").setSimplificationFunctionContentIndex(1)
                );
    }

    public static int evaluateIntegerPattern(TokenPattern<?> p, ISymbolContext ctx, Object[] data) {
        try {
            String raw = p.flatten(false);
            if(raw.toLowerCase().startsWith("0x")) {
                long asLong = Long.parseLong(raw.substring(2), 16);
                if(Long.highestOneBit(asLong) <= (long)Integer.MAX_VALUE+1) {
                    return (int) asLong;
                }
                throw new PrismarineException(PrismarineException.Type.INTERNAL_EXCEPTION, "Integer out of range", p, ctx);
            } else if(raw.toLowerCase().startsWith("0b")) {
                long asLong = Long.parseLong(raw.substring(2), 2);
                if(Long.highestOneBit(asLong) <= (long)Integer.MAX_VALUE+1) {
                    return (int) asLong;
                }
                throw new PrismarineException(PrismarineException.Type.INTERNAL_EXCEPTION, "Integer out of range", p, ctx);
            } else {
                return Integer.parseInt(raw);
            }
        } catch(NumberFormatException x) {
            throw new PrismarineException(PrismarineException.Type.INTERNAL_EXCEPTION, "Integer out of range", p, ctx);
        }
    }

    @NotNull
    public static String parseQuotedString(String str, TokenPattern<?> pattern, ISymbolContext ctx) {
        try {
            return CommandUtils.parseQuotedString(str);
        } catch(CommodoreException x) {
            throw new PrismarineException(PrismarineException.Type.INTERNAL_EXCEPTION, x.getMessage(), pattern, ctx);
        }
    }
}
