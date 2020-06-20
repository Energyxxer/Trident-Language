package com.energyxxer.trident.compiler;

import com.energyxxer.commodore.defpacks.DefinitionPack;
import com.energyxxer.commodore.util.io.CompoundInput;
import com.energyxxer.commodore.util.io.DirectoryCompoundInput;
import com.energyxxer.commodore.util.io.ZipCompoundInput;
import com.energyxxer.commodore.versioning.compatibility.VersionFeatureManager;
import com.energyxxer.commodore.versioning.compatibility.VersionFeatures;
import com.energyxxer.nbtmapper.NBTTypeMapPack;
import com.energyxxer.trident.compiler.plugin.TridentPlugin;
import com.energyxxer.trident.compiler.util.JsonTraverser;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.energyxxer.trident.compiler.TridentCompiler.newFileObject;

public class TridentBuildConfiguration {
    public DefinitionPack[] defaultDefinitionPacks;
    public Map<String, DefinitionPack> definitionPackAliases;
    public VersionFeatures featureMap;
    public Map<String, TridentPlugin> pluginAliases;
    public NBTTypeMapPack[] typeMapPacks; //TODO Replace with a Type Map Pack class

    public File dataPackOutput;
    public File resourcePackOutput;
    public boolean cleanDataPackOutput = false;
    public boolean cleanResourcePackOutput = false;
    public boolean exportComments = false;
    public boolean exportGameLog = true;

    public String[] preActions;
    public String[] postActions;

    //NO NEED FOR CLONE ANYMORE
    /*public TridentBuildConfiguration shallowClone() {
        TridentBuildConfiguration copy = new TridentBuildConfiguration();
        copy.defaultDefinitionPacks = this.defaultDefinitionPacks;
        copy.definitionPackAliases = this.definitionPackAliases;
        copy.featureMap = this.featureMap;
        copy.pluginAliases = this.pluginAliases;
        copy.rawTypeMaps = this.rawTypeMaps;

        copy.dataPackOutput = this.dataPackOutput;
        copy.resourcePackOutput = this.resourcePackOutput;
        copy.cleanDataPackOutput = this.cleanDataPackOutput;
        copy.cleanResourcePackOutput = this.cleanResourcePackOutput;
        copy.exportComments = this.exportComments;
        copy.exportGameLog = this.exportGameLog;

        copy.preActions = this.preActions;
        copy.postActions = this.postActions;
        return copy;

        //Arrays and maps copied as references
        //No Trident classes should
    }*/

    public void populateFromProjectRoot(File projectRootFile) throws IOException {
        populateFromJson(projectRootFile.toPath().resolve(TridentCompiler.PROJECT_BUILD_FILE_NAME).toFile(), projectRootFile);
    }

    public void populateFromJson(File buildJsonFile) throws IOException {
        populateFromJson(buildJsonFile, buildJsonFile.getParentFile());
    }

    public void populateFromJson(File buildJsonFile, File rootDir) throws IOException {
        try(JsonReader jsonReader = new JsonReader(new FileReader(buildJsonFile))) {
            populateFromJson((JsonObject) new Gson().fromJson(jsonReader, JsonObject.class), rootDir);
        }
    }

    public void populateFromJson(JsonObject root, File rootDir) throws IOException {
        JsonTraverser traverser = new JsonTraverser(root);

        //Input Resources
        //Definition Pack Aliases
        if(this.definitionPackAliases == null) {
            HashMap<String, DefinitionPack> definitionPackAliases = new HashMap<>();
            for(Map.Entry<String, JsonElement> aliasEntry : traverser.reset().get("input-resources").get("definition-pack-aliases").iterateAsObject()) {
                String key = aliasEntry.getKey();
                JsonElement rawValue = aliasEntry.getValue();
                if(!rawValue.isJsonPrimitive() || !rawValue.getAsJsonPrimitive().isString()) continue;
                String value = rawValue.getAsString();

                definitionPackAliases.put(key, retrieveDefinitionPackForFile(value, rootDir, null));
            }
            this.definitionPackAliases = definitionPackAliases;
        }

        //Default definition packs
        if(this.defaultDefinitionPacks == null) {
            ArrayList<DefinitionPack> defaultDefinitionPacks = new ArrayList<>();
            for(JsonElement rawDefPack : traverser.reset().get("input-resources").get("default-definition-packs").iterateAsArray()) {
                if(rawDefPack.isJsonPrimitive() && rawDefPack.getAsJsonPrimitive().isString()) {
                    defaultDefinitionPacks.add(retrieveDefinitionPackForFile(rawDefPack.getAsString(), rootDir, definitionPackAliases));
                }
            }
            this.defaultDefinitionPacks = defaultDefinitionPacks.toArray(new DefinitionPack[0]);
        }

        //Feature Map
        if(this.featureMap == null) {
            String featureMapPath = traverser.reset().get("input-resources").get("feature-map").asString();
            if(featureMapPath != null) {
                this.featureMap = VersionFeatureManager.parseFeatureMap(new FileReader(newFileObject(featureMapPath, rootDir)));
            }
        }

        //Plugin Aliases
        if(this.pluginAliases == null) {
            HashMap<String, TridentPlugin> pluginAliases = new HashMap<>();
            for(Map.Entry<String, JsonElement> aliasEntry : traverser.reset().get("input-resources").get("plugin-aliases").iterateAsObject()) {
                String key = aliasEntry.getKey();
                JsonElement rawValue = aliasEntry.getValue();
                if(!rawValue.isJsonPrimitive() || !rawValue.getAsJsonPrimitive().isString()) continue;
                String value = rawValue.getAsString();

                File file = newFileObject(value, rootDir);
                pluginAliases.put(key, new TridentPlugin(retrieveCompoundInputForFile(file), file));
            }
            this.pluginAliases = pluginAliases;
        }

        //Type Maps
        if(this.typeMapPacks == null) {
            ArrayList<NBTTypeMapPack> typeMapPacks = new ArrayList<>();
            for(JsonElement rawTypeMapPack : traverser.reset().get("input-resources").get("type-map-packs").iterateAsArray()) {
                if(rawTypeMapPack.isJsonPrimitive() && rawTypeMapPack.getAsJsonPrimitive().isString()) {
                    typeMapPacks.add(NBTTypeMapPack.fromCompound(retrieveCompoundInputForFile(newFileObject(rawTypeMapPack.getAsString(), rootDir))));
                }
            }
            this.typeMapPacks = typeMapPacks.toArray(new NBTTypeMapPack[0]);
        }


        //Output

        //Data Pack Output
        if(this.dataPackOutput == null) {
            String rawDataOutputPath = traverser.reset().get("output").get("directories").get("data-pack").asString();
            if(rawDataOutputPath != null) {
                this.dataPackOutput = newFileObject(rawDataOutputPath, rootDir);
            }
        }

        //Resource Pack Output
        if(this.resourcePackOutput == null) {
            String rawResourcesOutputPath = traverser.reset().get("output").get("directories").get("resource-pack").asString();
            if(rawResourcesOutputPath != null) {
                this.resourcePackOutput = newFileObject(rawResourcesOutputPath, rootDir);
            }
        }

        //Clean Output
        this.cleanDataPackOutput = traverser.reset().get("output").get("clean-directories").get("data-pack").asBoolean(this.cleanDataPackOutput);
        this.cleanResourcePackOutput = traverser.reset().get("output").get("clean-directories").get("resource-pack").asBoolean(this.cleanResourcePackOutput);

        //Export
        this.exportComments = traverser.reset().get("output").get("export-comments").asBoolean(this.exportComments);
        this.exportGameLog = traverser.reset().get("output").get("export-gamelog").asBoolean(this.exportGameLog);


        //Actions

        if(this.preActions == null) {
            ArrayList<String> preActions = new ArrayList<>();
            for(JsonElement rawCommand : traverser.reset().get("actions").get("pre").iterateAsArray()) {
                if(rawCommand.isJsonPrimitive() && rawCommand.getAsJsonPrimitive().isString()) {
                    String command = rawCommand.getAsString();
                    if(!command.isEmpty()) {
                        preActions.add(command);
                    }
                }
            }
            this.preActions = preActions.toArray(new String[0]);
        }

        if(this.postActions == null) {
            ArrayList<String> postActions = new ArrayList<>();
            for(JsonElement rawCommand : traverser.reset().get("actions").get("post").iterateAsArray()) {
                if(rawCommand.isJsonPrimitive() && rawCommand.getAsJsonPrimitive().isString()) {
                    String command = rawCommand.getAsString();
                    if(!command.isEmpty()) {
                        postActions.add(command);
                    }
                }
            }
            this.postActions = postActions.toArray(new String[0]);
        }
    }

    private static DefinitionPack retrieveDefinitionPackForFile(String pathToPack, File rootDir, Map<String, DefinitionPack> aliases) throws IOException {
        if(aliases != null && aliases.containsKey(pathToPack)) {
            return aliases.get(pathToPack);
        }
        File file = newFileObject(pathToPack, rootDir);
        if(file.isDirectory()) {
            return new DefinitionPack(new DirectoryCompoundInput(file));
        } else if(file.isFile() && file.getName().endsWith(".zip")) {
            return new DefinitionPack(new ZipCompoundInput(file));
        }
        throw new FileNotFoundException("Could not find folder nor zip at path '" + file + "'");
    }

    private static CompoundInput retrieveCompoundInputForFile(File file) throws IOException {
        if(file.isDirectory()) {
            return new DirectoryCompoundInput(file);
        } else if(file.isFile() && file.getName().endsWith(".zip")) {
            return new ZipCompoundInput(file);
        }
        throw new FileNotFoundException("Could not find folder nor zip at path '" + file + "'");
    }
}
