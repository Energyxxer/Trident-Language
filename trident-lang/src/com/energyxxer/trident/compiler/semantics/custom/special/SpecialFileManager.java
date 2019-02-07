package com.energyxxer.trident.compiler.semantics.custom.special;

import com.energyxxer.commodore.functionlogic.functions.Function;
import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.commodore.types.defaults.FunctionReference;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.ItemEventFile;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.preparation.PrepareDroppedItemsFile;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.preparation.PrepareHeldItemsFile;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.preparation.SaveHeldItemsFile;
import com.energyxxer.util.Lazy;

import java.util.HashMap;

public class SpecialFileManager {
    private final TridentCompiler compiler;
    private final HashMap<String, SpecialFile> files = new HashMap<>();

    private Lazy<Function> tickFunction;
    private Lazy<Objective> globalObjective;

    public SpecialFileManager(TridentCompiler compiler) {
        this.compiler = compiler;

        put(new ItemEventFile(this));
        put(new PrepareHeldItemsFile(this));
        put(new PrepareDroppedItemsFile(this));
        put(new SaveHeldItemsFile(this));

        put(new ObjectiveCreationFile(this));

        tickFunction = new Lazy<>(() -> {
            Function function = getNamespace().functions.create("trident/tick");
            compiler.getModule().minecraft.tags.functionTags.create("tick").addValue(new FunctionReference(function));
            return function;
        });
        globalObjective = new Lazy<>(() -> compiler.getModule().getObjectiveManager().create("trident_global", true));
    }

    public void compile() {
        for(SpecialFile file : files.values()) {
            if(file.shouldForceCompile()) file.startCompilation();
        }
    }

    public Namespace getNamespace() {
        return compiler.getDefaultNamespace();
    }

    public Function getTickFunction() {
        return tickFunction.getValue();
    }

    public Objective getGlobalObjective() {
        return globalObjective.getValue();
    }

    public TridentCompiler getCompiler() {
        return compiler;
    }

    public void put(SpecialFile file) {
        files.put(file.getFunctionName(), file);
    }

    public SpecialFile get(String key) {
        return files.get(key);
    }
}
