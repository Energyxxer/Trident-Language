package com.energyxxer.trident.compiler.semantics.custom.special;

import com.energyxxer.commodore.functionlogic.functions.Function;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;

public abstract class SpecialFile {
    protected final PrismarineProjectWorker worker;
    protected final SpecialFileManager parent;
    private final String functionName;
    protected Function function;

    private boolean compiled = false;

    public SpecialFile(SpecialFileManager parent, String functionName) {
        this.worker = parent.getWorker();
        this.parent = parent;
        this.functionName = functionName;
    }

    public abstract boolean shouldForceCompile();

    public void startCompilation() {
        if(compiled) return;
        this.function = parent.getNamespace().functions.getOrCreate("trident/" + functionName);
        this.compile();
        this.compiled = true;
    }

    protected abstract void compile();

    public String getFunctionName() {
        return functionName;
    }

    public Function getFunction() {
        if(function == null) throw new IllegalStateException();
        return function;
    }

    public SpecialFileManager getParent() {
        return parent;
    }
}
