package com.energyxxer.trident.worker.tasks;

import com.energyxxer.commodore.util.io.CompoundInput;
import com.energyxxer.commodore.util.io.DirectoryCompoundInput;
import com.energyxxer.commodore.util.io.ZipCompoundInput;
import com.energyxxer.trident.TridentSuiteConfiguration;
import com.energyxxer.trident.compiler.TridentBuildConfiguration;
import com.energyxxer.prismarine.plugins.PrismarinePlugin;
import com.energyxxer.prismarine.util.JsonTraverser;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.prismarine.worker.PrismarineProjectWorkerTask;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

public class SetupPluginsTask extends PrismarineProjectWorkerTask<ArrayList<PrismarinePlugin>> {

    public static final SetupPluginsTask INSTANCE = new SetupPluginsTask();

    private SetupPluginsTask() {}

    @Override
    public ArrayList<PrismarinePlugin> perform(PrismarineProjectWorker worker) throws Exception {

        ArrayList<PrismarinePlugin> plugins = new ArrayList<>();

        JsonObject properties = worker.output.get(SetupPropertiesTask.INSTANCE);
        TridentBuildConfiguration buildConfig = worker.output.get(SetupBuildConfigTask.INSTANCE);

        for(JsonElement rawElement : JsonTraverser.getThreadInstance().reset(properties).get("use-plugins").iterateAsArray()) {
            if(rawElement.isJsonPrimitive() && rawElement.getAsJsonPrimitive().isString()) {
                String element = rawElement.getAsString();
                File pathToPack = worker.rootDir.toPath().resolve("plugins").resolve(element).toFile();
                File pathToZip = new File(pathToPack.getPath() + ".zip");

                File sourceFile = null;

                CompoundInput input = null;

                if(pathToZip.exists() && pathToZip.isFile()) {
                    input = new ZipCompoundInput(pathToZip);
                    sourceFile = pathToZip;
                } else if(pathToPack.exists() && pathToPack.isDirectory()) {
                    input = new DirectoryCompoundInput(pathToPack);
                    sourceFile = pathToPack;
                }

                if(input != null) {
                    String pluginName = sourceFile.getName();
                    if(pluginName.endsWith(".zip")) {
                        pluginName = pluginName.substring(0, pluginName.length()- ".zip".length());
                    }
                    plugins.add(new PrismarinePlugin(pluginName, input, sourceFile, TridentSuiteConfiguration.INSTANCE));
                } else if(buildConfig.pluginAliases != null && buildConfig.pluginAliases.containsKey(element)) {
                    plugins.add(buildConfig.pluginAliases.get(element));
                } else {
                    throw new FileNotFoundException("Could not find folder nor zip at path '" + pathToPack + "'");
                }
            }
        }

        return plugins;
    }

    @Override
    public String getProgressMessage() {
        return "Setting up plugins";
    }

    @Override
    public PrismarineProjectWorkerTask[] getImplications() {
        return new PrismarineProjectWorkerTask[] {SetupPropertiesTask.INSTANCE, SetupBuildConfigTask.INSTANCE};
    }
}
