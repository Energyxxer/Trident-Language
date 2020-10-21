package com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection;

import com.energyxxer.enxlex.lexical_analysis.token.TokenSource;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.enxlex.report.StackTrace;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.prismarine.typesystem.functions.natives.NativeFunctionAnnotations;

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
            TokenSource source = frame.getPattern().getSource();
            notice.pointToSource(source);
        }

        ctx.getCompiler().getReport().addNotice(notice);
    }
}
