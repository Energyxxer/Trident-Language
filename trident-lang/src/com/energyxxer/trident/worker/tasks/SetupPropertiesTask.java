package com.energyxxer.trident.worker.tasks;

import com.energyxxer.enxlex.lexical_analysis.token.SourceFile;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.prismarine.worker.PrismarineProjectWorkerTask;
import com.energyxxer.trident.Trident;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import static com.energyxxer.trident.Trident.PROJECT_FILE_NAME;

public class SetupPropertiesTask extends PrismarineProjectWorkerTask<JsonObject> {
    public static final SetupPropertiesTask INSTANCE = new SetupPropertiesTask();

    private SetupPropertiesTask() {}

    @Override
    public JsonObject perform(PrismarineProjectWorker worker) throws IOException {
        File file = new File(worker.rootDir.getPath() + File.separator + PROJECT_FILE_NAME);
        try(InputStreamReader fr = new InputStreamReader(new FileInputStream(file), Trident.DEFAULT_CHARSET)) {
            return new Gson().fromJson(fr, JsonObject.class);
        } catch(IOException | JsonSyntaxException | JsonIOException x) {
            if(worker.setup.useReport) {
                Notice notice = new Notice(NoticeType.ERROR, x.getMessage());
                notice.setSourceLocation(new SourceFile(file), 0, 0);
                worker.report.addNotice(notice);
            }
            return new JsonObject();
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
