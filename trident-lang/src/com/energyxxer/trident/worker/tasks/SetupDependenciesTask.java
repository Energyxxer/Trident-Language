package com.energyxxer.trident.worker.tasks;

import com.energyxxer.prismarine.PrismarineCompiler;
import com.energyxxer.prismarine.util.JsonTraverser;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.prismarine.worker.PrismarineProjectWorkerTask;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import static com.energyxxer.prismarine.PrismarineCompiler.newFileObject;

public class SetupDependenciesTask extends PrismarineProjectWorkerTask<List<PrismarineProjectWorker>> {

    public static final SetupDependenciesTask INSTANCE = new SetupDependenciesTask();

    private SetupDependenciesTask() {}

    @Override
    public List<PrismarineProjectWorker> perform(PrismarineProjectWorker worker) throws Exception {
        ArrayList<PrismarineProjectWorker> dependencies = new ArrayList<>();

        JsonObject properties = worker.output.get(SetupPropertiesTask.INSTANCE);

        for(JsonElement rawElem : JsonTraverser.getThreadInstance().reset(properties).get("dependencies").iterateAsArray()) {
            if(rawElem.isJsonObject()) {
                JsonObject obj = rawElem.getAsJsonObject();
                if(obj.has("path") && obj.get("path").isJsonPrimitive() && obj.get("path").getAsJsonPrimitive().isString()) {
                    String dependencyPath = obj.get("path").getAsString();
                    PrismarineProjectWorker dependency = new PrismarineProjectWorker(worker.suiteConfig, newFileObject(dependencyPath, worker.rootDir));
                    dependency.setDependencyInfo(new PrismarineProjectWorker.DependencyInfo());
                    dependency.setup.copyFrom(worker.setup);
                    dependency.output.put(SetupBuildConfigTask.INSTANCE, worker.output.get(SetupBuildConfigTask.INSTANCE));
                    if(obj.has("export") && obj.get("export").isJsonPrimitive() && obj.get("export").getAsJsonPrimitive().isBoolean()) {
                        dependency.getDependencyInfo().doExport = obj.get("export").getAsBoolean();
                    }
                    if(obj.has("mode") && obj.get("mode").isJsonPrimitive() && obj.get("mode").getAsJsonPrimitive().isString()) {
                        switch(obj.get("mode").getAsString()) {
                            case "precompile": {
                                dependency.getDependencyInfo().mode = PrismarineCompiler.Dependency.Mode.PRECOMPILE;
                                break;
                            }
                            case "combine": {
                                dependency.getDependencyInfo().mode = PrismarineCompiler.Dependency.Mode.COMBINE;
                                break;
                            }
                        }
                    }

                    worker.output.addDependency(dependency);
                    dependencies.add(dependency);
                }
            }
        }

        for(PrismarineProjectWorker dependency : dependencies) {
            dependency.getDependencyInfo().parentWorker = worker;
            dependency.work();
            if(worker.setup.useReport) worker.report.addNotices(dependency.report.getAllNotices());
        }

        return dependencies;
    }

    @Override
    public String getProgressMessage() {
        return "Resolving dependencies";
    }

    @Override
    public PrismarineProjectWorkerTask[] getImplications() {
        return new PrismarineProjectWorkerTask[] {SetupPropertiesTask.INSTANCE};
    }
}
