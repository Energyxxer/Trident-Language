package com.energyxxer.trident.compiler;

import com.energyxxer.commodore.defpacks.DefinitionPack;
import com.energyxxer.commodore.util.io.CompoundInput;
import com.energyxxer.commodore.util.io.DirectoryCompoundInput;
import com.energyxxer.commodore.util.io.ZipCompoundInput;
import com.energyxxer.commodore.versioning.compatibility.VersionFeatureManager;
import com.energyxxer.commodore.versioning.compatibility.VersionFeatures;
import com.energyxxer.enxlex.lexical_analysis.token.SourceFile;
import com.energyxxer.enxlex.lexical_analysis.token.ZipSource;
import com.energyxxer.nbtmapper.packs.NBTTypeMapPack;
import com.energyxxer.prismarine.plugins.PrismarinePlugin;
import com.energyxxer.prismarine.util.JsonTraverser;
import com.energyxxer.trident.Trident;
import com.energyxxer.trident.TridentSuiteConfiguration;
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

import static com.energyxxer.prismarine.PrismarineCompiler.newFileObject;

public class TridentBuildConfiguration {
    public DefinitionPack[] defaultDefinitionPacks;
    public Map<String, DefinitionPack> definitionPackAliases;
    public VersionFeatures featureMap;
    public Map<String, PrismarinePlugin> pluginAliases;
    public NBTTypeMapPack[] typeMapPacks;

    public File dataPackOutput;
    public File resourcePackOutput;
    public boolean cleanDataPackOutput = false;
    public boolean cleanResourcePackOutput = false;
    public boolean exportComments = false;
    public boolean exportGameLog = true;

    public JsonObject populateFromProjectRoot(File projectRootFile) throws IOException {
        return populateFromJson(projectRootFile.toPath().resolve(Trident.PROJECT_BUILD_FILE_NAME).toFile(), projectRootFile);
    }

    public JsonObject populateFromJson(File buildJsonFile) throws IOException {
        return populateFromJson(buildJsonFile, buildJsonFile.getParentFile());
    }

    public JsonObject populateFromJson(File buildJsonFile, File rootDir) throws IOException {
        try(JsonReader jsonReader = new JsonReader(new FileReader(buildJsonFile))) {
            return populateFromJson((JsonObject) new Gson().fromJson(jsonReader, JsonObject.class), rootDir);
        }
    }

    public JsonObject populateFromJson(JsonObject root, File rootDir) throws IOException {
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
                try(FileReader fr = new FileReader(newFileObject(featureMapPath, rootDir))) {
                    this.featureMap = VersionFeatureManager.parseFeatureMap(fr);
                }
            }
        }

        //Plugin Aliases
        if(this.pluginAliases == null) {
            HashMap<String, PrismarinePlugin> pluginAliases = new HashMap<>();
            for(Map.Entry<String, JsonElement> aliasEntry : traverser.reset().get("input-resources").get("plugin-aliases").iterateAsObject()) {
                String key = aliasEntry.getKey();
                JsonElement rawValue = aliasEntry.getValue();
                if(!rawValue.isJsonPrimitive() || !rawValue.getAsJsonPrimitive().isString()) continue;
                String value = rawValue.getAsString();

                File file = newFileObject(value, rootDir);
                String pluginName = file.getName();
                if(pluginName.endsWith(".zip")) {
                    pluginName = pluginName.substring(0, pluginName.length()- ".zip".length());
                }
                pluginAliases.put(key, new PrismarinePlugin(pluginName, retrieveCompoundInputForFile(file), file, TridentSuiteConfiguration.INSTANCE));
            }
            this.pluginAliases = pluginAliases;
        }

        //Type Maps
        if(this.typeMapPacks == null) {
            ArrayList<NBTTypeMapPack> typeMapPacks = new ArrayList<>();
            for(JsonElement rawTypeMapPack : traverser.reset().get("input-resources").get("type-map-packs").iterateAsArray()) {
                if(rawTypeMapPack.isJsonPrimitive() && rawTypeMapPack.getAsJsonPrimitive().isString()) {
                    typeMapPacks.add(retrieveNBTTypeMapPackForFile(newFileObject(rawTypeMapPack.getAsString(), rootDir)));
                }
            }
            this.typeMapPacks = typeMapPacks.toArray(new NBTTypeMapPack[0]);
        }


        //Output

        //Data Pack Output
        if(this.dataPackOutput == null) {
            String rawDataPackOutput = traverser.reset().get("output").get("directories").get("data-pack").asString();
            if(rawDataPackOutput != null) {
                this.dataPackOutput = newFileObject(rawDataPackOutput, rootDir);
            }
        }

        //Resource Pack Output
        if(this.resourcePackOutput == null) {
            String rawResourcePackOutputPath = traverser.reset().get("output").get("directories").get("resource-pack").asString();
            if(rawResourcePackOutputPath != null) {
                this.resourcePackOutput = newFileObject(rawResourcePackOutputPath, rootDir);
            }
        }

        //Clean Output
        this.cleanDataPackOutput = traverser.reset().get("output").get("clean-directories").get("data-pack").asBoolean(this.cleanDataPackOutput);
        this.cleanResourcePackOutput = traverser.reset().get("output").get("clean-directories").get("resource-pack").asBoolean(this.cleanResourcePackOutput);

        //Export
        this.exportComments = traverser.reset().get("output").get("export-comments").asBoolean(this.exportComments);
        this.exportGameLog = traverser.reset().get("output").get("export-gamelog").asBoolean(this.exportGameLog);

        return root;
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

    private static NBTTypeMapPack retrieveNBTTypeMapPackForFile(File file) throws IOException {
        if(file.isDirectory()) {
            return NBTTypeMapPack.fromCompound(new DirectoryCompoundInput(file), p -> new SourceFile(file.toPath().resolve(p).toFile()));
        } else if(file.isFile() && file.getName().endsWith(".zip")) {
            return NBTTypeMapPack.fromCompound(new ZipCompoundInput(file), p -> new ZipSource(file, p));
        }
        throw new FileNotFoundException("Could not find folder nor zip at path '" + file + "'");
    }
}
