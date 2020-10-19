package com.energyxxer.trident.compiler.semantics.custom.special;

import com.energyxxer.commodore.functionlogic.functions.Function;
import com.energyxxer.commodore.functionlogic.score.Objective;
import com.energyxxer.commodore.module.CommandModule;
import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.ItemEventFile;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.preparation.PrepareDroppedItemsFile;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.preparation.PrepareHeldItemsFile;
import com.energyxxer.trident.compiler.semantics.custom.special.item_events.preparation.SaveHeldItemsFile;
import com.energyxxer.trident.worker.tasks.SetupModuleTask;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.prismarine.PrismarineCompiler;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.util.Lazy;

import java.util.HashMap;

public class SpecialFileManager {
    private PrismarineCompiler compiler;
    private /*final but lazy lambda says no*/ PrismarineProjectWorker worker;
    private final HashMap<String, SpecialFile> files = new HashMap<>();

    private HashMap<Integer, TickingFunction> tickingFunctions = new HashMap<>();

    public SpecialFileManager(PrismarineProjectWorker worker) {
        this.worker = worker;

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

    private String defaultNamespace;
    private boolean givenDefaultNamespaceNotice = false;

    public String getDefaultNamespace() {
        return defaultNamespace;
    }

    public void setDefaultNamespace(String defaultNamespace) {
        this.defaultNamespace = defaultNamespace;
    }

    public Namespace getNamespace() {
        if(defaultNamespace == null || defaultNamespace.isEmpty()) {
            defaultNamespace = "trident_temp_please_specify_default_namespace";
            if(!givenDefaultNamespaceNotice) {
                compiler.getReport().addNotice(new Notice(NoticeType.WARNING, "Some language features used require a default namespace. Please specify a default namespace in the project settings"));
                givenDefaultNamespaceNotice = true;
            }
        }
        return worker.output.get(SetupModuleTask.INSTANCE).getNamespace(defaultNamespace);
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

    private Lazy<Objective> globalObjective = new Lazy<>(() -> worker.output.get(SetupModuleTask.INSTANCE).getObjectiveManager().getOrCreate("tdn_global"));

    public Objective getGlobalObjective() {
        return globalObjective.getValue();
    }

    public PrismarineProjectWorker getWorker() {
        return worker;
    }

    public CommandModule getModule() {
        return worker.output.get(SetupModuleTask.INSTANCE);
    }

    public void put(SpecialFile file) {
        files.put(file.getFunctionName(), file);
    }

    public SpecialFile get(String key) {
        return files.get(key);
    }

    public void setCompiler(PrismarineCompiler compiler) {
        this.compiler = compiler;
    }
}
