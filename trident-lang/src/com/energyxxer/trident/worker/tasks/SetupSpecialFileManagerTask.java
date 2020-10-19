package com.energyxxer.trident.worker.tasks;

import com.energyxxer.trident.compiler.semantics.custom.special.SpecialFileManager;
import com.energyxxer.prismarine.util.JsonTraverser;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.prismarine.worker.PrismarineProjectWorkerTask;

public class SetupSpecialFileManagerTask extends PrismarineProjectWorkerTask<SpecialFileManager> {

    public static final SetupSpecialFileManagerTask INSTANCE = new SetupSpecialFileManagerTask();

    private SetupSpecialFileManagerTask() {}

    @Override
    public SpecialFileManager perform(PrismarineProjectWorker worker) throws Exception {
        SpecialFileManager specialFileManager = new SpecialFileManager(worker);

        specialFileManager.setDefaultNamespace(
                JsonTraverser.INSTANCE.reset(worker.output.get(SetupPropertiesTask.INSTANCE))
                        .get("default-namespace")
                        .asNonEmptyString()
        );

        return specialFileManager;
    }

    @Override
    public String getProgressMessage() {
        return "Setting up special files";
    }

    @Override
    public PrismarineProjectWorkerTask[] getImplications() {
        return new PrismarineProjectWorkerTask[] {SetupPropertiesTask.INSTANCE};
    }
}
