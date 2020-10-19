package com.energyxxer.trident.compiler.semantics;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class TridentExceptionUtil {
    public static class Source {
        public static final PrismarineException.Type ARITHMETIC_ERROR = new PrismarineException.Type("Arithmetic Error");
        public static final PrismarineException.Type COMMAND_ERROR = new PrismarineException.Type("Command Error");
        public static final PrismarineException.Type USER_EXCEPTION = new PrismarineException.Type("User Exception");
        public static final PrismarineException.Type STRUCTURAL_ERROR = new PrismarineException.Type("Structural Error");
        public static final PrismarineException.Type LANGUAGE_LEVEL_ERROR = new PrismarineException.Type("Language Level Error");
        public static final PrismarineException.Type DUPLICATION_ERROR = new PrismarineException.Type("Duplication Error");
    }

    private TridentExceptionUtil() {}

    public static ExceptionMapper handleCommodoreException(CommodoreException x, TokenPattern<?> defaultPattern, ISymbolContext ctx) {
        return new ExceptionMapper(x, defaultPattern, ctx);
    }

    public static class ExceptionMapper {
        private CommodoreException ex;
        private TokenPattern<?> defaultPattern;
        private ISymbolContext ctx;
        private HashMap<CommodoreException.Source, TokenPattern<?>> sourceMap = new HashMap<>();
        private HashMap<String, Supplier<TokenPattern<?>>> causeMap = new HashMap<>();

        public ExceptionMapper(CommodoreException ex, TokenPattern<?> defaultPattern, ISymbolContext ctx) {
            this.ex = ex;
            this.defaultPattern = defaultPattern;
            this.ctx = ctx;
        }

        public ExceptionMapper map(CommodoreException.Source source, TokenPattern<?> pattern) {
            sourceMap.put(source, pattern);
            return this;
        }

        public ExceptionMapper map(String cause, TokenPattern<?> pattern) {
            causeMap.put(cause, () -> pattern);
            return this;
        }

        public ExceptionMapper map(String cause, Supplier<TokenPattern<?>> pattern) {
            causeMap.put(cause, pattern);
            return this;
        }

        public void invokeThrow() throws PrismarineException {
            for(Map.Entry<String, Supplier<TokenPattern<?>>> entry : causeMap.entrySet()) {
                if(entry.getKey().equals(ex.getCauseKey())) {
                    throw new PrismarineException(Source.COMMAND_ERROR, ex.getMessage(), entry.getValue().get(), ctx);
                }
            }
            for(Map.Entry<CommodoreException.Source, TokenPattern<?>> entry : sourceMap.entrySet()) {
                if(ex.getSource() == entry.getKey()) {
                    throw new PrismarineException(Source.COMMAND_ERROR, ex.getMessage(), entry.getValue(), ctx);
                }
            }
            throw new PrismarineException(Source.COMMAND_ERROR, ex.getMessage(), defaultPattern, ctx);
        }
    }
}
