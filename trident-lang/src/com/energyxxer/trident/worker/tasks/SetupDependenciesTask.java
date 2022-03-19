package com.energyxxer.trident.worker.tasks;

import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.prismarine.PrismarineCompiler;
import com.energyxxer.prismarine.util.JsonTraverser;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.prismarine.worker.PrismarineProjectWorkerTask;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
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
                    File file = newFileObject(dependencyPath, worker.rootDir);
                    if(!file.exists()) {
                        if(worker.setup.useReport) worker.report.addNotice(new Notice(NoticeType.ERROR, "Missing dependency: " + dependencyPath));
                        continue;
                    }
                    if(worker.output.get(ValidateCircularDependenciesTask.INSTANCE).contains(file)) {
                        if(worker.setup.useReport) worker.report.addNotice(new Notice(NoticeType.ERROR, "Circular dependencies: " + dependencyPath));
                        continue;
                    }
                    boolean doCompile = !worker.output.get(SetupRootDirectoryListTask.INSTANCE).contains(file);
                    worker.output.get(SetupRootDirectoryListTask.INSTANCE).add(file);
                    worker.output.get(ValidateCircularDependenciesTask.INSTANCE).add(file);
                    try {
                        PrismarineProjectWorker dependency = new PrismarineProjectWorker(worker.suiteConfig, file);
                        dependency.setDependencyInfo(new PrismarineProjectWorker.DependencyInfo());
                        if(!doCompile) dependency.getDependencyInfo().doCompile = false;
                        dependency.setup.copyFrom(worker.setup);
                        dependency.output.put(SetupBuildConfigTask.INSTANCE, worker.output.get(SetupBuildConfigTask.INSTANCE));
                        dependency.output.put(SetupRootDirectoryListTask.INSTANCE, worker.output.get(SetupRootDirectoryListTask.INSTANCE));
                        if (obj.has("export") && obj.get("export").isJsonPrimitive() && obj.get("export").getAsJsonPrimitive().isBoolean()) {
                            dependency.getDependencyInfo().doExport = obj.get("export").getAsBoolean();
                        }
                        if (obj.has("mode") && obj.get("mode").isJsonPrimitive() && obj.get("mode").getAsJsonPrimitive().isString()) {
                            switch (obj.get("mode").getAsString()) {
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
                    } finally {
                        worker.output.get(ValidateCircularDependenciesTask.INSTANCE).remove(file);
                    }
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
        return new PrismarineProjectWorkerTask[] {SetupPropertiesTask.INSTANCE, SetupRootDirectoryListTask.INSTANCE, ValidateCircularDependenciesTask.INSTANCE};
    }
}
