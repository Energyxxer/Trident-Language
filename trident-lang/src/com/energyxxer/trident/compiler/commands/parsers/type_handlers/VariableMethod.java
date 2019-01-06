package com.energyxxer.trident.compiler.commands.parsers.type_handlers;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.commands.EntryParsingException;
import com.energyxxer.trident.compiler.semantics.TridentFile;

import java.util.Arrays;

public interface VariableMethod {
    Object call(Object[] params, TokenPattern<?>[] patterns, TokenPattern<?> pattern, TridentFile file);

    class HelperMethods {

        @SuppressWarnings("unchecked")
        public static <T> T assertOfType(Object param, TokenPattern<?> pattern, TridentFile file, Class<T> expected) {
            if(expected.isInstance(param)) return (T) param;
            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Expected parameter of type " + expected.getSimpleName(), pattern));
            throw new EntryParsingException();
        }

        @SuppressWarnings("unchecked")
        public static <T> T assertOfType(Object param, TokenPattern<?> pattern, TridentFile file, Class<? extends T>... expected) {
            for(Class cls : expected) {
                if(cls.isInstance(param)) return (T) param;
            }
            file.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Expected parameter of one of the following types: " + Arrays.asList(expected).map((Class c) -> c.getSimpleName()).toSet().join(", "), pattern));
            throw new EntryParsingException();
        }
    }
}
