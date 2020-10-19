package com.energyxxer.trident.worker.tasks;

import com.energyxxer.trident.TridentFileUnitConfiguration;
import com.energyxxer.trident.compiler.TridentProductions;
import com.energyxxer.prismarine.PrismarineProductions;
import com.energyxxer.prismarine.plugins.PrismarinePlugin;
import com.energyxxer.prismarine.util.JsonTraverser;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.prismarine.worker.PrismarineProjectWorkerTask;
import com.energyxxer.prismarine.worker.tasks.SetupProductionsTask;

public class SetupPluginProductionsTask extends PrismarineProjectWorkerTask {

    public static final SetupPluginProductionsTask INSTANCE = new SetupPluginProductionsTask();

    private SetupPluginProductionsTask() {}

    @Override
    public Object perform(PrismarineProjectWorker worker) throws Exception {
        PrismarineProductions productions = worker.output.get(SetupProductionsTask.INSTANCE).get(TridentFileUnitConfiguration.INSTANCE);

        for(PrismarinePlugin p : worker.output.get(SetupPluginsTransitivelyTask.INSTANCE)) {
            TridentProductions.registerPlugin(productions, p);

            p.attachToProjectWorker(worker);

            if(JsonTraverser.INSTANCE.reset(worker.output.get(SetupPropertiesTask.INSTANCE)).get("using-all-plugins").asBoolean(true)) {
                TridentProductions.importPlugin(productions, p.getName(), null, null);
            }
        }
        return null;
    }

    @Override
    public String getProgressMessage() {
        return "Setting up plugin syntax";
    }

    @Override
    public PrismarineProjectWorkerTask[] getImplications() {
        return new PrismarineProjectWorkerTask[] {SetupPropertiesTask.INSTANCE, SetupProductionsTask.INSTANCE, SetupPluginsTransitivelyTask.INSTANCE};
    }
}
