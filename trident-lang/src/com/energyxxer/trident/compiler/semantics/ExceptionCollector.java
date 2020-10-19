package com.energyxxer.trident.compiler.semantics;

import com.energyxxer.prismarine.reporting.PrismarineException;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;

import java.util.ArrayList;

public class ExceptionCollector {
    private final ISymbolContext file;
    private ArrayList<PrismarineException> exceptions = new ArrayList<>();
    private boolean wasEmpty;
    private boolean wasBreaking;

    public ExceptionCollector(ISymbolContext ctx) {
        this.file = ctx;
    }

    public void begin() {

        wasEmpty = file.getCompiler().getTryStack().isEmpty();

        if(wasEmpty || file.getCompiler().getTryStack().isRecovering()) {
            file.getCompiler().getTryStack().pushRecovering();
        } else if(file.getCompiler().getTryStack().isBreaking()) {
            file.getCompiler().getTryStack().pushBreaking();
        }
        wasBreaking = file.getCompiler().getTryStack().isBreaking();
    }

    public void log(RuntimeException x) {
        if(x instanceof PrismarineException) log(((PrismarineException) x));
        if(x instanceof PrismarineException.Grouped) log(((PrismarineException.Grouped) x));
        else throw x;
    }

    public void log(PrismarineException x) {
        if(wasEmpty) {
            x.expandToUncaught();
            file.getCompiler().getReport().addNotice(x.getNotice());
        } else if(file.getCompiler().getTryStack().isRecovering()) {
            exceptions.add(x);
        } else {
            throw x;
        }
    }

    public void log(PrismarineException.Grouped gx) {
        if(wasEmpty) {
            for(PrismarineException ex : gx.getExceptions()) {
                file.getCompiler().getReport().addNotice(ex.getNotice());
            }
        } else if(file.getCompiler().getTryStack().isRecovering()) {
            exceptions.addAll(gx.getExceptions());
        } else {
            throw gx;
        }
    }

    public void end() {
        file.getCompiler().getTryStack().pop();
        if(!exceptions.isEmpty()) {
            if(!file.getCompiler().getTryStack().isEmpty()) {
                throw new PrismarineException.Grouped(exceptions);
            }
        }
    }
}
