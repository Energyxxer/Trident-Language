package com.energyxxer.trident.compiler.analyzers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerManager;
import com.energyxxer.trident.compiler.semantics.TridentException;
import com.energyxxer.trident.compiler.semantics.TridentFile;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface VariableMethod {
    Object call(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, TridentFile file);

    class HelperMethods {

        @SuppressWarnings("unchecked")
        public static <T> T assertOfType(Object param, TokenPattern<?> pattern, TridentFile file, Class<T> expected) {
            if(expected.isInstance(param)) return (T) param;
            VariableTypeHandler handler = param instanceof VariableTypeHandler ? (VariableTypeHandler) param : AnalyzerManager.getAnalyzer(VariableTypeHandler.class, VariableTypeHandler.Static.getIdentifierForClass(param.getClass()));
            if(handler != null) try {
                return (T) handler.coerce(param, expected, pattern, file);
            } catch(ClassCastException x) {
                //could not coerce
            } else {
                throw new TridentException(TridentException.Source.IMPOSSIBLE, "Unknown variable handler for '" + VariableTypeHandler.Static.getIdentifierForClass(param.getClass()) + "'", pattern, file);
            }
            throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Expected parameter of type " + expected.getSimpleName(), pattern, file);
        }

        @SuppressWarnings("unchecked")
        public static <T> T assertOfType(Object param, TokenPattern<?> pattern, TridentFile file, Class<? extends T>... expected) {
            for(Class cls : expected) {
                if(cls.isInstance(param)) return (T) param;
            }
            throw new TridentException(TridentException.Source.INTERNAL_EXCEPTION, "Expected parameter of one of the following types: " + Arrays.asList(expected).stream().map((Function<Class, String>) Class::getSimpleName).collect(Collectors.joining(", ")), pattern, file);
        }
    }
}
