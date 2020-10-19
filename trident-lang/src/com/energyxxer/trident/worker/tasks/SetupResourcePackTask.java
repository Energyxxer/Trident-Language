package com.energyxxer.trident.worker.tasks;

import com.energyxxer.trident.compiler.TridentBuildConfiguration;
import com.energyxxer.trident.compiler.resourcepack.ResourcePackGenerator;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.prismarine.worker.PrismarineProjectWorkerTask;

public class SetupResourcePackTask extends PrismarineProjectWorkerTask<ResourcePackGenerator> {

    public static final SetupResourcePackTask INSTANCE = new SetupResourcePackTask();

    private SetupResourcePackTask() {}

    @Override
    public ResourcePackGenerator perform(PrismarineProjectWorker worker) throws Exception {
        TridentBuildConfiguration buildConfig = worker.output.get(SetupBuildConfigTask.INSTANCE);
        if(buildConfig.resourcePackOutput != null) {
            return new ResourcePackGenerator(null, buildConfig.resourcePackOutput);
        }
        return null;
    }

    @Override
    public String getProgressMessage() {
        return "Setting up resource pack";
    }

    @Override
    public PrismarineProjectWorkerTask[] getImplications() {
        return new PrismarineProjectWorkerTask[] {SetupBuildConfigTask.INSTANCE};
    }
}
