package com.energyxxer.trident.worker.tasks;

import com.energyxxer.trident.compiler.analyzers.type_handlers.operators.OperatorManager;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.prismarine.worker.PrismarineProjectWorkerTask;

public class SetupOperatorManagerTask extends PrismarineProjectWorkerTask<OperatorManager> {

    public static final SetupOperatorManagerTask INSTANCE = new SetupOperatorManagerTask();

    private SetupOperatorManagerTask() {}

    @Override
    public OperatorManager perform(PrismarineProjectWorker worker) {
        return new OperatorManager();
    }

    @Override
    public String getProgressMessage() {
        return "Setting up default operators";
    }

    @Override
    public PrismarineProjectWorkerTask[] getImplications() {
        return new PrismarineProjectWorkerTask[0];
    }
}
