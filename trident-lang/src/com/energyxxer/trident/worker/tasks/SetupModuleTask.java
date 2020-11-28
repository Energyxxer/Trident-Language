package com.energyxxer.trident.worker.tasks;

import com.energyxxer.commodore.defpacks.DefinitionPack;
import com.energyxxer.commodore.module.CommandModule;
import com.energyxxer.commodore.util.io.CompoundInput;
import com.energyxxer.commodore.util.io.DirectoryCompoundInput;
import com.energyxxer.commodore.util.io.ZipCompoundInput;
import com.energyxxer.commodore.versioning.JavaEditionVersion;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.prismarine.worker.PrismarineProjectWorkerTask;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.TridentBuildConfiguration;
import com.energyxxer.trident.compiler.semantics.AliasType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class SetupModuleTask extends PrismarineProjectWorkerTask<CommandModule> {

    public static final SetupModuleTask INSTANCE = new SetupModuleTask();

    private SetupModuleTask() {}

    @Override
    public CommandModule perform(PrismarineProjectWorker worker) throws IOException {
        CommandModule module = new CommandModule(worker.rootDir.getName(), "Command Module created with Trident");
        module.getSettingsManager().EXPORT_EMPTY_FUNCTIONS.setValue(true);

        JavaEditionVersion targetVersion = new JavaEditionVersion(1, 14, 0);

        JsonObject properties = worker.output.get(SetupPropertiesTask.INSTANCE);
        TridentBuildConfiguration buildConfig = worker.output.get(SetupBuildConfigTask.INSTANCE);

        if(properties.has("target-version") && properties.get("target-version").isJsonArray()) {
            JsonArray rawTargetVersion = properties.get("target-version").getAsJsonArray();
            try {
                int major = rawTargetVersion.get(0).getAsInt();
                int minor = rawTargetVersion.get(1).getAsInt();
                int patch = rawTargetVersion.get(2).getAsInt();

                targetVersion = new JavaEditionVersion(major, minor, patch);
            } catch(IndexOutOfBoundsException x) {
                throw new IOException("Invalid target version array: got less than 3 elements");
            } catch(UnsupportedOperationException x) {
                throw new IOException("Expected int in target version array");
            }
        } else {
            if(worker.report != null) {
                worker.report.addNotice(new Notice(NoticeType.ERROR, "Missing target-version property in project properties.\nPlease set it to an array of 3 integers."));
            } else {
                throw new IOException("Missing target-version property");
            }
        }

        module.getSettingsManager().setTargetVersion(targetVersion);

        ArrayList<DefinitionPack> toImport = new ArrayList<>();

        if(properties.has("use-definitions") && properties.get("use-definitions").isJsonArray()) {
            for(JsonElement rawElement : properties.getAsJsonArray("use-definitions")) {
                if(rawElement.isJsonPrimitive() && rawElement.getAsJsonPrimitive().isString()) {
                    String element = rawElement.getAsString();
                    if(element.equals("DEFAULT")) {
                        toImport.addAll(Arrays.asList(buildConfig.defaultDefinitionPacks));
                    } else {
                        File pathToPack = worker.rootDir.toPath().resolve("defpacks").resolve(element).toFile();
                        File pathToZip = new File(pathToPack.getPath() + ".zip");

                        CompoundInput input = null;

                        if(pathToZip.exists() && pathToZip.isFile()) {
                            input = new ZipCompoundInput(pathToZip);
                        } else if(pathToPack.exists() && pathToPack.isDirectory()) {
                            input = new DirectoryCompoundInput(pathToPack);
                        }

                        if(input != null) {
                            toImport.add(new DefinitionPack(input));
                        } else if(buildConfig.definitionPackAliases != null && buildConfig.definitionPackAliases.containsKey(element)) {
                            toImport.add(buildConfig.definitionPackAliases.get(element));
                        } else {
                            throw new FileNotFoundException("Could not find folder nor zip at path '" + pathToPack + "'");
                        }
                    }
                }
            }
        } else {
            toImport.addAll(Arrays.asList(buildConfig.defaultDefinitionPacks));
        }

        for(DefinitionPack defpack : toImport) {
            module.importDefinitions(defpack);
        }

        if(properties.has("aliases") && properties.get("aliases").isJsonObject()) {
            for(Map.Entry<String, JsonElement> categoryEntry : properties.getAsJsonObject("aliases").entrySet()) {
                String category = categoryEntry.getKey();

                if(categoryEntry.getValue().isJsonObject()) {
                    for(Map.Entry<String, JsonElement> entry : categoryEntry.getValue().getAsJsonObject().entrySet()) {
                        ResourceLocation alias = new ResourceLocation(entry.getKey());
                        ResourceLocation real = new ResourceLocation(entry.getValue().getAsString());

                        module.getNamespace(alias.namespace).types.getDictionary(category).create((c, ns, n) -> new AliasType(c, ns, n, module.getNamespace(real.namespace), real.body), alias.body);
//                        Debug.log("Created alias '" + alias + "' for '" + real + "'");
                    }
                }
            }
        }
        return module;
    }

    @Override
    public String getProgressMessage() {
        return "Importing vanilla definitions";
    }

    @Override
    public PrismarineProjectWorkerTask[] getImplications() {
        return new PrismarineProjectWorkerTask[] {
                SetupBuildConfigTask.INSTANCE,
                SetupPropertiesTask.INSTANCE
        };
    }
}
