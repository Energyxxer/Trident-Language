package com.energyxxer.trident.compiler.semantics;

import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.commands.parsers.type_handlers.MemberNotFoundException;
import com.energyxxer.trident.compiler.commands.parsers.type_handlers.VariableTypeHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;

public class TridentException extends RuntimeException implements VariableTypeHandler<TridentException> {

    public enum Source {
        PARSING_ERROR,
        COMMAND_ERROR,
        USER_EXCEPTION,
        IMPOSSIBLE
    }

    private Source source;
    private Notice notice;
    private TokenPattern<?> cause;

    public TridentException(Source source, String message, TokenPattern<?> cause) {
        this(source, message, cause, (CallStack.StackTrace) null);
    }

    public TridentException(Source source, String message, TokenPattern<?> cause, TridentFile file) {
        this(source, message, cause, file.getCompiler().getCallStack().getView());
    }

    public TridentException(Source source, String message, TokenPattern<?> cause, CallStack.StackTrace stackTrace) {
        this.source = source;
        this.notice = new Notice(NoticeType.ERROR, message, message + (stackTrace != null ? ("\n" + stackTrace.toString()) : ""), cause);
        this.cause = cause;
    }

    public Source getSource() {
        return source;
    }

    public Notice getNotice() {
        return notice;
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

    @Override
    public Object getMember(TridentException object, String member, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
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
    public Object getIndexer(TridentException object, Object index, TokenPattern<?> pattern, TridentFile file, boolean keepSymbol) {
        throw new MemberNotFoundException();
    }

    @Override
    public <F> F cast(TridentException object, Class<F> targetType, TokenPattern<?> pattern, TridentFile file) {
        throw new ClassCastException();
    }

    @Override
    public String toString() {
        return notice.getMessage();
    }
}
