package com.energyxxer.trident.sets;

import com.energyxxer.commodore.CommandUtils;
import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.enxlex.pattern_matching.matching.lazy.TokenStructureMatch;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.suggestions.SuggestionTags;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.providers.PatternProviderSet;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import org.jetbrains.annotations.NotNull;

import static com.energyxxer.trident.compiler.lexer.TridentTokens.*;
import static com.energyxxer.prismarine.PrismarineProductions.group;
import static com.energyxxer.prismarine.PrismarineProductions.ofType;

public class BasicLiteralSet extends PatternProviderSet {

    public BasicLiteralSet() {
        super("ROOT_INTERPOLATION_VALUE");
    }

    @Override
    protected void installUtilityProductions(PrismarineProductions productions, TokenStructureMatch providerStructure) {

        productions.getOrCreateStructure("ROOT_INTERPOLATION_VALUE")
                .add(
                        productions.putPatternMatch("REAL_NUMBER", ofType(REAL_NUMBER).setName("RAW_REAL").setEvaluator((p, d) -> Double.parseDouble(p.flatten(false))))
                )
                .add(
                        productions.putPatternMatch("INTEGER_NUMBER", ofType(INTEGER_NUMBER).setName("RAW_INTEGER").setEvaluator(BasicLiteralSet::evaluateIntegerPattern))
                )
                .add(
                        productions.putPatternMatch("BOOLEAN", ofType(BOOLEAN).addTags(SuggestionTags.ENABLED).setEvaluator((p, d) -> "true".equals(p.flatten(false))))
                )
                .add(
                        productions.putPatternMatch("STRING_LITERAL", ofType(STRING_LITERAL).setName("STRING_LITERAL").setEvaluator((p, d) -> parseQuotedString(p.flatten(false), p, (ISymbolContext) d[0])))
                )
                .add(
                        productions.putPatternMatch("NULL", ofType(NULL).setName("NULL_VALUE").setEvaluator((p, d) -> null))
                )
                .add(
                        group(TridentProductions.brace("("), productions.getOrCreateStructure("INTERPOLATION_VALUE"), TridentProductions.brace(")")).setName("PARENTHESIZED_VALUE").setSimplificationFunctionContentIndex(1)
                );
    }

    public static int evaluateIntegerPattern(TokenPattern<?> p, Object... d) {
        try {
            String raw = p.flatten(false);
            if(raw.toLowerCase().startsWith("0x")) {
                long asLong = Long.parseLong(raw.substring(2), 16);
                if(Long.highestOneBit(asLong) <= (long)Integer.MAX_VALUE+1) {
                    return (int) asLong;
                }
                throw new PrismarineException(PrismarineException.Type.INTERNAL_EXCEPTION, "Integer out of range", p, (ISymbolContext) d[0]);
            } else if(raw.toLowerCase().startsWith("0b")) {
                long asLong = Long.parseLong(raw.substring(2), 2);
                if(Long.highestOneBit(asLong) <= (long)Integer.MAX_VALUE+1) {
                    return (int) asLong;
                }
                throw new PrismarineException(PrismarineException.Type.INTERNAL_EXCEPTION, "Integer out of range", p, (ISymbolContext) d[0]);
            } else {
                return Integer.parseInt(raw);
            }
        } catch(NumberFormatException x) {
            throw new PrismarineException(PrismarineException.Type.INTERNAL_EXCEPTION, "Integer out of range", p, (ISymbolContext) d[0]);
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
