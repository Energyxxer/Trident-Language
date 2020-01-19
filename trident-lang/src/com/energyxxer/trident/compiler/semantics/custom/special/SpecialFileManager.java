package com.energyxxer.trident.compiler.semantics.custom.special;

import com.energyxxer.commodore.functionlogic.functions.Function;
import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.ItemEventFile;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.preparation.PrepareDroppedItemsFile;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.preparation.PrepareHeldItemsFile;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.preparation.SaveHeldItemsFile;

import java.util.HashMap;

public class SpecialFileManager {
    private final TridentCompiler compiler;
    private final HashMap<String, SpecialFile> files = new HashMap<>();

    private HashMap<Integer, TickingFunction> tickingFunctions = new HashMap<>();

    public SpecialFileManager(TridentCompiler compiler) {
        this.compiler = compiler;

        put(new ItemEventFile(this));
        put(new PrepareHeldItemsFile(this));
        put(new PrepareDroppedItemsFile(this));
        put(new SaveHeldItemsFile(this));

        put(new ObjectiveCreationFile(this));

        put(new GameLogFetcherFile(this));
    }

    public void compile() {
        for(SpecialFile file : files.values()) {
            if(file.shouldForceCompile()) file.startCompilation();
        }

        for(TickingFunction file : tickingFunctions.values()) {
            file.startCompilation();
        }
    }

    public Namespace getNamespace() {
        return compiler.getDefaultNamespace();
    }

    public TickingFunction getTickingFunction(int interval) {
        if(interval <= 0) throw new IllegalArgumentException("Ticking interval may not be zero or negative");
        TickingFunction function = tickingFunctions.get(interval);
        if(function == null) {
            tickingFunctions.put(interval, function = new TickingFunction(this, interval));
        }
        return function;
    }

    public Function getTickFunction() {
        return getTickingFunction(1).getFunction();
    }

    public Objective getGlobalObjective() {
        return compiler.getGlobalObjective();
    }

    public TridentCompiler getCompiler() {
        return compiler.getRootCompiler();
    }

    public void put(SpecialFile file) {
        files.put(file.getFunctionName(), file);
    }

    public SpecialFile get(String key) {
        return files.get(key);
    }
}
