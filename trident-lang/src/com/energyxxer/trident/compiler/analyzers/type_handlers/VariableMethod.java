package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerManager;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.energyxxer.trident.extensions.EObject.assertNotNull;

public interface VariableMethod {
    Object call(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext ctx);

    default Object safeCall(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, ISymbolContext ctx) {
        try {
            return call(params, patterns, pattern, ctx);
        } catch(TridentException | TridentException.Grouped x) {
            throw x;
        } catch (Exception x) {
            throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, x.toString(), pattern, ctx);
        }
    }

    class HelperMethods {

        @SuppressWarnings("unchecked")
        public static <T> T assertOfType(Object param, TokenPattern<?> pattern, ISymbolContext ctx, Class<T> expected) {
            if(expected.isInstance(param)) return (T) param;
            assertNotNull(param, pattern, ctx);
            VariableTypeHandler handler = param instanceof VariableTypeHandler ? (VariableTypeHandler) param : AnalyzerManager.getAnalyzer(VariableTypeHandler.class, VariableTypeHandler.Static.getIdentifierForClass(param.getClass()));
            if(handler != null) try {
                return (T) handler.coerce(param, expected, pattern, ctx);
            } catch(ClassCastException x) {
                //could not coerce
            } else {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown variable handler for '" + VariableTypeHandler.Static.getIdentifierForClass(param.getClass()) + "'", pattern, ctx);
            }
            throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Expected parameter of type " + expected.getSimpleName(), pattern, ctx);
        }

        @SuppressWarnings("unchecked")
        public static <T> T assertOfType(Object param, TokenPattern<?> pattern, ISymbolContext file, Class<? extends T>... expected) {
            for(Class cls : expected) {
                if(cls.isInstance(param)) return (T) param;
            }
            throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Expected parameter of one of the following types: " + Arrays.asList(expected).stream().map((Function<Class, String>) Class::getSimpleName).collect(Collectors.joining(", ")), pattern, file);
        }
    }
}