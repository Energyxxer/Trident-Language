package com.energyxxer.trident.worker.tasks;

import com.energyxxer.prismarine.plugins.PrismarinePlugin;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.prismarine.worker.PrismarineProjectWorkerTask;

import java.util.ArrayList;
import java.util.HashSet;

public class SetupPluginsTransitivelyTask extends PrismarineProjectWorkerTask<ArrayList<PrismarinePlugin>> {

    public static final SetupPluginsTransitivelyTask INSTANCE = new SetupPluginsTransitivelyTask();

    private SetupPluginsTransitivelyTask() {}

    @Override
    public ArrayList<PrismarinePlugin> perform(PrismarineProjectWorker worker) throws Exception {
        ArrayList<PrismarinePlugin> transitivePlugins = new ArrayList<>(worker.output.get(SetupPluginsTask.INSTANCE));
        HashSet<String> pluginsFound = new HashSet<>();
        for(PrismarineProjectWorker dependency : worker.output.getDependencies()) {
            for(PrismarinePlugin plugin : dependency.output.get(SetupPluginsTransitivelyTask.INSTANCE)) {
                if(pluginsFound.add(plugin.getName())) {
                    transitivePlugins.add(plugin);
                }
            }
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
