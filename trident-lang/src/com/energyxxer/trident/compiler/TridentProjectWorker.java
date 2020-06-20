package com.energyxxer.trident.compiler;

import com.energyxxer.commodore.defpacks.DefinitionPack;
import com.energyxxer.commodore.module.CommandModule;
import com.energyxxer.commodore.util.io.CompoundInput;
import com.energyxxer.commodore.util.io.DirectoryCompoundInput;
import com.energyxxer.commodore.util.io.ZipCompoundInput;
import com.energyxxer.commodore.versioning.JavaEditionVersion;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.lexer.TridentProductions;
import com.energyxxer.trident.compiler.plugin.TridentPlugin;
import com.energyxxer.trident.compiler.semantics.AliasType;
import com.energyxxer.trident.compiler.util.TridentProjectSummarizer;
import com.energyxxer.util.logger.Debug;
import com.google.gson.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.energyxxer.trident.compiler.TridentCompiler.PROJECT_FILE_NAME;

public class TridentProjectWorker {
    public final File rootDir;

    private TridentBuildConfiguration buildConfig;
    public final Setup setup;
    public final Output output;

    public CompilerReport report;

    private DependencyInfo dependencyInfo;

    public TridentProjectWorker(File rootDir) {
        this.rootDir = rootDir;
        this.setup = new Setup();
        this.output = new Output();
    }

    public void work() throws IOException {
        setup.resolveImplications();

        if(setup.useReport) {
            report = new CompilerReport();
        }

        if(setup.setupProperties) {
            try {
                output.properties = new Gson().fromJson(new FileReader(new File(rootDir.getPath() + File.separator + PROJECT_FILE_NAME)), JsonObject.class);
            } catch(JsonSyntaxException x) {
                logException(new IOException(x.getMessage()), "Error while reading project settings: ");
                return;
            } catch(IOException x) {
                logException(x, "Error while reading project settings: ");
                return;
            }
        }

        if(setup.setupModule) {
            try {
                setupModule();
            } catch(IOException x) {
                logException(x, "Error while importing vanilla definitions: ");
                return;
            }
        }

        if(setup.setupDependencies) {
            try {
                setupDependencies();
            } catch(IOException x) {
                logException(x, "Error while setting up dependencies: ");
                return;
            }
        }

        if(setup.setupProductions) {
            try {
                setupProductions();
            } catch(IOException x) {
                logException(x, "Error while importing vanilla definitions: ");
                return;
            }
        }

        if(setup.setupPlugins) {
            try {
                setupPlugins();
            } catch(IOException x) {
                logException(x, "Error while importing vanilla definitions: ");
                return;
            }
        }
    }

    private void setupModule() throws IOException {
        CommandModule module = new CommandModule(rootDir.getName(), "Command Module created with Trident");
        module.getSettingsManager().EXPORT_EMPTY_FUNCTIONS.setValue(true);

        JavaEditionVersion targetVersion = new JavaEditionVersion(1, 14, 0);
        if(output.properties.has("target-version") && output.properties.get("target-version").isJsonArray()) {
            JsonArray rawTargetVersion = output.properties.get("target-version").getAsJsonArray();
            try {
                int major = rawTargetVersion.get(0).getAsInt();
                int minor = rawTargetVersion.get(1).getAsInt();
                int patch = rawTargetVersion.get(2).getAsInt();

                targetVersion = new JavaEditionVersion(major, minor, patch);
            } catch(IndexOutOfBoundsException x) {
                throw new IOException("Invalid target version array: got less than 3 elements");
            } catch(UnsupportedOperationException x) {
                throw new IOException("Expected string in target version array");
            }
        }

        module.getSettingsManager().setTargetVersion(targetVersion);

        ArrayList<DefinitionPack> toImport = new ArrayList<>();

        if(output.properties.has("use-definitions") && output.properties.get("use-definitions").isJsonArray()) {
            for(JsonElement rawElement : output.properties.getAsJsonArray("use-definitions")) {
                if(rawElement.isJsonPrimitive() && rawElement.getAsJsonPrimitive().isString()) {
                    String element = rawElement.getAsString();
                    if(element.equals("DEFAULT")) {
                        toImport.addAll(Arrays.asList(buildConfig.defaultDefinitionPacks));
                    } else {
                        File pathToPack = rootDir.toPath().resolve("defpacks").resolve(element).toFile();
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

        if(output.properties.has("aliases") && output.properties.get("aliases").isJsonObject()) {
            for(Map.Entry<String, JsonElement> categoryEntry : output.properties.getAsJsonObject("aliases").entrySet()) {
                String category = categoryEntry.getKey();

                if(categoryEntry.getValue().isJsonObject()) {
                    for(Map.Entry<String, JsonElement> entry : categoryEntry.getValue().getAsJsonObject().entrySet()) {
                        TridentUtil.ResourceLocation alias = new TridentUtil.ResourceLocation(entry.getKey());
                        TridentUtil.ResourceLocation real = new TridentUtil.ResourceLocation(entry.getValue().getAsString());

                        module.getNamespace(alias.namespace).types.getDictionary(category).create((c, ns, n) -> new AliasType(c, ns, n, module.getNamespace(real.namespace), real.body), alias.body);
                        Debug.log("Created alias '" + alias + "' for '" + real + "'");
                    }
                }
            }
        }
        output.module = module;
    }

    private void setupDependencies() throws IOException {
        ArrayList<TridentProjectWorker> dependencies = new ArrayList<>();
        output.dependencies = dependencies;

        if(output.properties.has("dependencies") && output.properties.get("dependencies").isJsonArray()) {
            for(JsonElement rawElem : output.properties.get("dependencies").getAsJsonArray()) {
                if(rawElem.isJsonObject()) {
                    JsonObject obj = rawElem.getAsJsonObject();
                    if(obj.has("path") && obj.get("path").isJsonPrimitive() && obj.get("path").getAsJsonPrimitive().isString()) {
                        String dependencyPath = obj.get("path").getAsString();
                        TridentProjectWorker dependency = new TridentProjectWorker(newFileObject(dependencyPath));
                        dependency.dependencyInfo = new DependencyInfo();
                        dependency.setup.copyValuesFrom(this.setup);
                        dependency.buildConfig = buildConfig;
                        if(obj.has("export") && obj.get("export").isJsonPrimitive() && obj.get("export").getAsJsonPrimitive().isBoolean()) {
                            dependency.dependencyInfo.doExport = obj.get("export").getAsBoolean();
                        }
                        if(obj.has("mode") && obj.get("mode").isJsonPrimitive() && obj.get("mode").getAsJsonPrimitive().isString()) {
                            switch(obj.get("mode").getAsString()) {
                                case "precompile": {
                                    dependency.dependencyInfo.mode = TridentCompiler.Dependency.Mode.PRECOMPILE;
                                    break;
                                }
                                case "combine": {
                                    dependency.dependencyInfo.mode = TridentCompiler.Dependency.Mode.COMBINE;
                                    break;
                                }
                            }
                        }

                        dependencies.add(dependency);
                    }
                }
            }
        }

        for(TridentProjectWorker dependency : dependencies) {
            dependency.dependencyInfo.parentWorker = this;
            dependency.work();
            if(setup.useReport) report.addNotices(dependency.report.getAllNotices());
        }
    }

    private void setupProductions() throws IOException {
        output.productions = new TridentProductions(output.module);
    }

    private void setupPlugins() throws IOException {
        ArrayList<TridentPlugin> plugins = new ArrayList<>();
        output.plugins = plugins;

        if(output.properties.has("use-plugins") && output.properties.get("use-plugins").isJsonArray()) {
            for(JsonElement rawElement : output.properties.getAsJsonArray("use-plugins")) {
                if(rawElement.isJsonPrimitive() && rawElement.getAsJsonPrimitive().isString()) {
                    String element = rawElement.getAsString();
                    File pathToPack = rootDir.toPath().resolve("plugins").resolve(element).toFile();
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
                        plugins.add(new TridentPlugin(input, sourceFile));
                    } else if(buildConfig.pluginAliases != null && buildConfig.pluginAliases.containsKey(element)) {
                        plugins.add(buildConfig.pluginAliases.get(element));
                    } else {
                        throw new FileNotFoundException("Could not find folder nor zip at path '" + pathToPack + "'");
                    }
                }
            }
        }

        ArrayList<TridentPlugin> transitivePlugins = new ArrayList<>();
        output.transitivePlugins = transitivePlugins;
        transitivePlugins.addAll(plugins);

        if(setup.setupDependencies) {
            for(TridentProjectWorker dependency : output.dependencies) {
                transitivePlugins.addAll(dependency.output.transitivePlugins);
            }
        }

        if(setup.setupProductions) {
            for(TridentPlugin p : transitivePlugins) {
                p.populateProductions(output.productions);
            }
        }
    }

    private File newFileObject(String path) {
        return TridentCompiler.newFileObject(path, rootDir);
    }

    private void logException(IOException x, String prefix) throws IOException {
        if (setup.useReport) {
            this.report.addNotice(new Notice(NoticeType.ERROR, prefix + x.toString() + " ; See console for details"));
            x.printStackTrace();
        } else {
            throw x;
        }
    }

    public TridentBuildConfiguration getBuildConfig() {
        return buildConfig;
    }

    public void setBuildConfig(TridentBuildConfiguration buildConfig) {
        this.buildConfig = buildConfig;
    }

    public DependencyInfo getDependencyInfo() {
        return dependencyInfo;
    }

    public TridentCompiler createCompiler() {
        TridentCompiler compiler = new TridentCompiler(this);
        compiler.setBuildConfig(buildConfig);
        if(dependencyInfo != null) {
            compiler.setDependencyMode(dependencyInfo.mode);
        }
        return compiler;
    }

    public TridentProjectSummarizer createSummarizer() {
        TridentProjectSummarizer summarizer = new TridentProjectSummarizer(this);
        return summarizer;
    }

    public class Setup {
        public boolean setupProperties;
        public boolean setupModule;
        public boolean setupProductions;
        public boolean setupPlugins;
        public boolean setupDependencies;

        public boolean useReport;

        void resolveImplications() {
            if(setupProductions) setupModule = true;
            if(setupProductions) setupPlugins = true;
            if(setupPlugins) setupDependencies = true;
            if(setupModule) setupProperties = true;
            if(setupDependencies) setupProperties = true;
        }

        public void copyValuesFrom(Setup setup) {
            this.setupProperties = setup.setupProperties;
            this.setupModule = setup.setupModule;
            this.setupProductions = setup.setupProductions;
            this.setupPlugins = setup.setupPlugins;
            this.setupDependencies = setup.setupDependencies;
            this.useReport = setup.useReport;
        }
    }
    public class Output {
        public JsonObject properties;
        public CommandModule module;
        public TridentProductions productions;
        public List<TridentProjectWorker> dependencies;
        public ArrayList<TridentPlugin> plugins;
        public ArrayList<TridentPlugin> transitivePlugins;
    }

    public class DependencyInfo {
        public boolean doExport;
        public TridentCompiler.Dependency.Mode mode;
        public TridentProjectWorker parentWorker;
    }
}
