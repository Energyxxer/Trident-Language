package com.energyxxer.trident.worker.tasks;

import com.energyxxer.prismarine.plugins.PrismarinePlugin;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.prismarine.worker.PrismarineProjectWorkerTask;

import java.util.ArrayList;

public class SetupPluginsTransitivelyTask extends PrismarineProjectWorkerTask<ArrayList<PrismarinePlugin>> {

    public static final SetupPluginsTransitivelyTask INSTANCE = new SetupPluginsTransitivelyTask();

    private SetupPluginsTransitivelyTask() {}

    @Override
    public ArrayList<PrismarinePlugin> perform(PrismarineProjectWorker worker) throws Exception {
        ArrayList<PrismarinePlugin> transitivePlugins = new ArrayList<>(worker.output.get(SetupPluginsTask.INSTANCE));
        for(PrismarineProjectWorker dependency : worker.output.getDependencies()) {
            transitivePlugins.addAll(dependency.output.get(SetupPluginsTransitivelyTask.INSTANCE));
            //TODO please check if this works
            // can't trust it lol
        }
        return transitivePlugins;
    }

    @Override
    public String getProgressMessage() {
        return "Setting up plugin dependency tree";
    }

    @Override
    public PrismarineProjectWorkerTask[] getImplications() {
        return new PrismarineProjectWorkerTask[] {SetupDependenciesTask.INSTANCE, SetupPluginsTask.INSTANCE};
    }
}
