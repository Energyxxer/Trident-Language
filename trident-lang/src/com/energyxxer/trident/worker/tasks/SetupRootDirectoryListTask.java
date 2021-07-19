package com.energyxxer.trident.worker.tasks;

import com.energyxxer.prismarine.PrismarineCompiler;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.prismarine.worker.PrismarineProjectWorkerTask;

import java.io.File;
import java.util.ArrayList;

public class SetupRootDirectoryListTask extends PrismarineProjectWorkerTask<ArrayList<File>> {

    public static final SetupRootDirectoryListTask INSTANCE = new SetupRootDirectoryListTask();

    private SetupRootDirectoryListTask() {}

    @Override
    public ArrayList<File> perform(PrismarineProjectWorker worker) throws Exception {
        ArrayList<File> rootDirectories = new ArrayList<>();
        rootDirectories.add(worker.rootDir);
        for(PrismarineProjectWorker subWorker : worker.output.get(SetupDependenciesTask.INSTANCE)) {
            if(subWorker.getDependencyInfo().mode == PrismarineCompiler.Dependency.Mode.COMBINE && !rootDirectories.contains(subWorker.rootDir)) {
                rootDirectories.addAll(subWorker.output.get(INSTANCE));
            }
        }
        return rootDirectories;
    }

    @Override
    public String getProgressMessage() {
        return "Collecting root directories";
    }

    @Override
    public PrismarineProjectWorkerTask[] getImplications() {
        return new PrismarineProjectWorkerTask[] {SetupDependenciesTask.INSTANCE};
    }
}
