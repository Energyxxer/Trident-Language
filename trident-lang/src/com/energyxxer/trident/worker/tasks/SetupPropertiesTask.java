package com.energyxxer.trident.worker.tasks;

import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.prismarine.worker.PrismarineProjectWorkerTask;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static com.energyxxer.trident.Trident.PROJECT_FILE_NAME;

public class SetupPropertiesTask extends PrismarineProjectWorkerTask<JsonObject> {
    public static final SetupPropertiesTask INSTANCE = new SetupPropertiesTask();

    private SetupPropertiesTask() {}

    @Override
    public JsonObject perform(PrismarineProjectWorker worker) throws IOException {
        try(FileReader fr = new FileReader(new File(worker.rootDir.getPath() + File.separator + PROJECT_FILE_NAME))) {
            return new Gson().fromJson(fr, JsonObject.class);
        }
    }

    @Override
    public String getProgressMessage() {
        return "Reading project settings";
    }

    @Override
    public PrismarineProjectWorkerTask[] getImplications() {
        return new PrismarineProjectWorkerTask[0];
    }
}
