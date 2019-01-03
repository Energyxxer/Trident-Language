package com.energyxxer.trident.compiler.commands.parsers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.commands.EntryParsingException;

import java.util.Arrays;

public interface VariableMethod {
    Object call(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, TridentCompiler compiler);

    class HelperMethods {

        @SuppressWarnings("unchecked")
        public static <T> T assertOfType(Object param, TokenPattern<?> pattern, TridentCompiler compiler, Class<T> expected) {
            if(expected.isInstance(param)) return (T) param;
            compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Expected parameter of type " + expected.getSimpleName(), pattern));
            throw new EntryParsingException();
        }

        public static Object assertOfType(Object param, TokenPattern<?> pattern, TridentCompiler compiler, Class<?>... expected) {
            for(Class cls : expected) {
                if(cls.isInstance(param)) return param;
            }
            compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Expected parameter of one of the following types: " + Arrays.asList(expected).map((Class c) -> c.getSimpleName()).toSet().join(", "), pattern));
            throw new EntryParsingException();
        }
    }
}
