package com.energyxxer.trident.compiler.semantics;

import com.energyxxer.commodore.CommodoreException;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MemberNotFoundException;
import com.energyxxer.trident.compiler.analyzers.type_handlers.extensions.VariableTypeHandler;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Supplier;

public class TridentException extends RuntimeException implements VariableTypeHandler<TridentException> {

    public enum Source {
        TYPE_ERROR("Type Error"),
        ARITHMETIC_ERROR("Arithmetic Error"),
        COMMAND_ERROR("Command Error"),
        INTERNAL_EXCEPTION("Internal Exception"),
        USER_EXCEPTION("User Exception"),
        IMPOSSIBLE("Impossible Exception"),
        STRUCTURAL_ERROR("Structural Error"),
        LANGUAGE_LEVEL_ERROR("Language Level Error"),
        DUPLICATION_ERROR("Duplication Error");

        private final String humanReadableName;

        Source(String humanReadableName) {
            this.humanReadableName = humanReadableName;
        }

        public String getHumanReadableName() {
            return humanReadableName;
        }
    }

    private Source source;
    private Notice notice;
    private TokenPattern<?> cause;
    private boolean breaking = false;

    public TridentException(Source source, String message, @NotNull TokenPattern<?> cause, ISymbolContext ctx) {
        this(source, message, cause, ctx.getCompiler().getCallStack().getView().setLeaf(cause));
    }

    public TridentException(Source source, String message, @NotNull TokenPattern<?> cause, CallStack.StackTrace stackTrace) {
        this.source = source;

        if(source == Source.IMPOSSIBLE) {
            StackTraceElement[] javaStackTrace = Thread.currentThread().getStackTrace();
            for(int i = 1; i < javaStackTrace.length; i++) {
                if(!javaStackTrace[i].getClassName().equals(getClass().getName())) {

                    message += " (" + javaStackTrace[i].getFileName() + ":" + javaStackTrace[i].getLineNumber() + ") Please report as soon as possible";
                    break;
                }
            }
        }

        this.notice = new Notice(NoticeType.ERROR, message, message + (stackTrace != null ? ("\n" + stackTrace.toString()) : ""), cause);
        this.cause = cause;
    }

    public static ExceptionMapper handleCommodoreException(CommodoreException x, TokenPattern<?> defaultPattern, ISymbolContext ctx) {
        return new ExceptionMapper(x, defaultPattern, ctx);
    }

    public Source getSource() {
        return source;
    }

    public Notice getNotice() {
        return notice;
    }

    public void expandToUncaught() {
        notice.setExtendedMessage("Uncaught " + source.getHumanReadableName() + ": " + notice.getExtendedMessage());
    }

    public boolean isBreaking() {
        return breaking;
    }

    public TridentException setBreaking(boolean breaking) {
        this.breaking = breaking;
        return this;
    }

    public TokenPattern<?> getCausePattern() {
        return cause;
    }

    @Override
    public Object getMember(TridentException object, String member, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        switch(member) {
            case "message": return notice.getMessage();
            case "extendedMessage": return notice.getExtendedMessage();
            case "line": return cause.getStringLocation().line;
            case "column": return cause.getStringLocation().column;
            case "index": return cause.getStringLocation().index;
            case "type": return source.toString();
        }
        throw new MemberNotFoundException();
    }

    @Override
    public Object getIndexer(TridentException object, Object index, TokenPattern<?> pattern, ISymbolContext ctx, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public <F> F cast(TridentException object, Class<F> targetType, TokenPattern<?> pattern, ISymbolContext ctx) {
        throw new ClassCastException();
    }

    @Override
    public String toString() {
        return source.getHumanReadableName() + ": " + notice.getExtendedMessage();
    }

    public static class Grouped extends RuntimeException implements Iterable<TridentException> {
        private ArrayList<TridentException> exceptions;

        public Grouped(ArrayList<TridentException> exceptions) {
            this.exceptions = exceptions;
        }

        public ArrayList<TridentException> getExceptions() {
            return exceptions;
        }

        @NotNull
        @Override
        public Iterator<TridentException> iterator() {
            return exceptions.iterator();
        }
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

        public void invokeThrow() throws TridentException {
            for(Map.Entry<String, Supplier<TokenPattern<?>>> entry : causeMap.entrySet()) {
                if(entry.getKey().equals(ex.getCauseKey())) {
                    throw new TridentException(Source.COMMAND_ERROR, ex.getMessage(), entry.getValue().get(), ctx);
                }
            }
            for(Map.Entry<CommodoreException.Source, TokenPattern<?>> entry : sourceMap.entrySet()) {
                if(ex.getSource() == entry.getKey()) {
                    throw new TridentException(Source.COMMAND_ERROR, ex.getMessage(), entry.getValue(), ctx);
                }
            }
            throw new TridentException(Source.COMMAND_ERROR, ex.getMessage(), defaultPattern, ctx);
        }
    }
}
