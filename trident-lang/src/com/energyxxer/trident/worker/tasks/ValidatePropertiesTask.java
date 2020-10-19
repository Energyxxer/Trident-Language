package com.energyxxer.trident.worker.tasks;

import com.energyxxer.trident.compiler.TridentProjectProperties;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.prismarine.util.JsonTraverser;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.prismarine.worker.PrismarineProjectWorkerTask;
import com.google.gson.JsonObject;

public class ValidatePropertiesTask extends PrismarineProjectWorkerTask<TridentProjectProperties> {

    public static final ValidatePropertiesTask INSTANCE = new ValidatePropertiesTask();

    private ValidatePropertiesTask() {}

    @Override
    public TridentProjectProperties perform(PrismarineProjectWorker worker) throws Exception {
        JsonObject properties = worker.output.get(SetupPropertiesTask.INSTANCE);

        int languageLevel = JsonTraverser.INSTANCE.reset(properties).get("language-level").asInt(1);
        if(languageLevel < 1 || languageLevel > 3) {
            worker.report.addNotice(new Notice(NoticeType.ERROR, "Invalid language level: " + languageLevel));
            languageLevel = 1;
        }

        String anonymousFunctionTemplate = JsonTraverser.INSTANCE.reset(properties).get("anonymous-function-name").asNonEmptyString("_anonymous*");

        TridentProjectProperties propertiesObject = new TridentProjectProperties();
        propertiesObject.languageLevel = languageLevel;
        propertiesObject.anonymousFunctionTemplate = anonymousFunctionTemplate;

        return propertiesObject;
    }

    @Override
    public String getProgressMessage() {
        return "Validating project properties";
    }

    @Override
    public PrismarineProjectWorkerTask[] getImplications() {
        return new PrismarineProjectWorkerTask[] {SetupPropertiesTask.INSTANCE};
    }
}
