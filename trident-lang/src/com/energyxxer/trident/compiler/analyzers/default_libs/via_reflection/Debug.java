package com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.enxlex.report.StackTrace;
import com.energyxxer.prismarine.Prismarine;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.functions.natives.NativeFunctionAnnotations;

import java.io.File;

public class Debug {
    public static void log(Object obj, TokenPattern<?> pattern, ISymbolContext ctx) {
        String message = ctx.getTypeSystem().castToString(obj, pattern, ctx);
        ctx.getCompiler().getReport().addNotice(new Notice(NoticeType.INFO, message, pattern));
    }

    public static void warn(Object obj, TokenPattern<?> pattern, ISymbolContext ctx) {
        String message = ctx.getTypeSystem().castToString(obj, pattern, ctx);
        ctx.getCompiler().getReport().addNotice(new Notice(NoticeType.WARNING, message, pattern));
    }

    public static void err(Object obj, TokenPattern<?> pattern, ISymbolContext ctx) {
        String message = ctx.getTypeSystem().castToString(obj, pattern, ctx);
        ctx.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, message, pattern));
    }

    public static void stopAtBreakpoint(@NativeFunctionAnnotations.NullableArg Object obj, TokenPattern<?> pattern, ISymbolContext ctx) {
        long start = System.currentTimeMillis();
        String message = ctx.getTypeSystem().castToString(obj, pattern, ctx);
        ctx.getCompiler().getReport().addNotice(new Notice(NoticeType.DEBUG, message, pattern));

        if(System.currentTimeMillis() < start + 10) {
            ctx.getCompiler().getReport().addNotice(new Notice(NoticeType.ERROR, "Debugger not attached", pattern));
        }
    }

    public static void printStackTrace(TokenPattern<?> pattern, ISymbolContext ctx) {
        StackTrace stackTrace = ctx.getCompiler().getCallStack().getView(pattern);

        Notice notice = new Notice(NoticeType.DEBUG, "Stack Trace:", "Stack Trace:", pattern).setStackTrace(stackTrace);

        for(StackTrace.StackTraceElement frame : stackTrace.getElements()) {
            File file = frame.getPattern().getFile();
            if(!file.equals(Prismarine.NULL_FILE)) {
                notice.pointToFile(file);
                break;
            }
        }

        ctx.getCompiler().getReport().addNotice(notice);
    }
}
