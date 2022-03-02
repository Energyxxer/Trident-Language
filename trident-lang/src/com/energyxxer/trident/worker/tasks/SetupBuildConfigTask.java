package com.energyxxer.trident.worker.tasks;

import com.energyxxer.trident.compiler.TridentBuildConfiguration;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.prismarine.worker.PrismarineProjectWorkerTask;

public class SetupBuildConfigTask extends PrismarineProjectWorkerTask<TridentBuildConfiguration> {

    public static final SetupBuildConfigTask INSTANCE = new SetupBuildConfigTask();

    private SetupBuildConfigTask() {}

    @Override
    public TridentBuildConfiguration perform(PrismarineProjectWorker worker) throws Exception {
        throw new IllegalStateException("Build configuration was not set by IDE or CLI");
    }

    @Override
    public String getProgressMessage() {
        return "Reading build configuration";
    }

    @Override
    public PrismarineProjectWorkerTask[] getImplications() {
        return new PrismarineProjectWorkerTask[0];
    }
}
