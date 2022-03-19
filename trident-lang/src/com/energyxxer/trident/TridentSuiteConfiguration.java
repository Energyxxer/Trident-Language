package com.energyxxer.trident;

import com.energyxxer.commodore.module.CommandModule;
import com.energyxxer.commodore.module.Exportable;
import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.commodore.tags.Tag;
import com.energyxxer.commodore.tags.TagGroup;
import com.energyxxer.commodore.types.Type;
import com.energyxxer.commodore.types.TypeDictionary;
import com.energyxxer.commodore.versioning.compatibility.VersionFeatureManager;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.prismarine.PrismarineCompiler;
import com.energyxxer.prismarine.PrismarineSuiteConfiguration;
import com.energyxxer.prismarine.libraries.PrismarineLibrary;
import com.energyxxer.prismarine.libraries.PrismarineLibraryUnit;
import com.energyxxer.prismarine.plugins.PrismarinePlugin;
import com.energyxxer.prismarine.summaries.PrismarineProjectSummarizer;
import com.energyxxer.prismarine.summaries.PrismarineProjectSummary;
import com.energyxxer.prismarine.symbols.contexts.GlobalSymbolContext;
import com.energyxxer.prismarine.typesystem.PrismarineTypeSystem;
import com.energyxxer.prismarine.walker.FileWalker;
import com.energyxxer.prismarine.worker.PrismarineProjectWorker;
import com.energyxxer.prismarine.worker.tasks.SetupProductionsTask;
import com.energyxxer.trident.compiler.ResourceLocation;
import com.energyxxer.trident.compiler.TridentBuildConfiguration;
import com.energyxxer.trident.compiler.analyzers.default_libs.DefaultLibraryProvider;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerManager;
import com.energyxxer.trident.compiler.analyzers.type_handlers.TridentTypeSystem;
import com.energyxxer.trident.compiler.analyzers.type_handlers.operators.DefaultOperators;
import com.energyxxer.trident.compiler.lexer.summaries.TridentProjectSummary;
import com.energyxxer.trident.compiler.plugin.TridentPluginUnitConfiguration;
import com.energyxxer.trident.compiler.resourcepack.ResourcePackGenerator;
import com.energyxxer.trident.compiler.semantics.AliasType;
import com.energyxxer.trident.compiler.semantics.custom.special.SpecialFileManager;
import com.energyxxer.trident.filewalkers.CompilerWalkers;
import com.energyxxer.trident.filewalkers.SummaryWalkers;
import com.energyxxer.trident.worker.tasks.*;
import com.energyxxer.util.FileUtil;
import com.energyxxer.util.logger.Debug;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;

public class TridentSuiteConfiguration extends PrismarineSuiteConfiguration {
    public static final Path PRIMITIVES_SUMMARY_PATH = Paths.get("datapack/data/trident-util/functions/primitives.tdn");

    public static final TridentSuiteConfiguration INSTANCE = new TridentSuiteConfiguration();

    private final PrismarineLibrary standardLibrary;

    public TridentSuiteConfiguration() {
        this.putLanguageUnitConfiguration(TridentFileUnitConfiguration.INSTANCE);
        this.putPluginUnitConfiguration(TridentPluginUnitConfiguration.INSTANCE);

        standardLibrary = new PrismarineLibrary("Trident Standard Library", this);

        try {
            standardLibrary
                    .registerUnit(
                            PRIMITIVES_SUMMARY_PATH,
                            TridentFileUnitConfiguration.INSTANCE,
                            read("/trident_utils/datapack/data/trident-util/functions/primitives.tdn"),
                            PrismarineLibraryUnit.Availability.SUMMARY_ONLY
                    )
                    .registerUnit(
                            Paths.get("datapack/data/trident-util/functions/native.tdn"),
                            TridentFileUnitConfiguration.INSTANCE,
                            read("/trident_utils/datapack/data/trident-util/functions/native_compiler.tdn"),
                            PrismarineLibraryUnit.Availability.COMPILER_ONLY
                    )
                    .registerUnit(
                            Paths.get("datapack/data/trident-util/functions/native.tdn"),
                            TridentFileUnitConfiguration.INSTANCE,
                            read("/trident_utils/datapack/data/trident-util/functions/native_summary.tdn"),
                            PrismarineLibraryUnit.Availability.SUMMARY_ONLY
                    )
                    .registerUnit(
                            Paths.get("datapack/data/trident-util/functions/type_checking.tdn"),
                            TridentFileUnitConfiguration.INSTANCE,
                            read("/trident_utils/datapack/data/trident-util/functions/type_checking.tdn")
                    )
                    .registerUnit(
                            Paths.get("datapack/data/trident-util/functions/generators.tdn"),
                            TridentFileUnitConfiguration.INSTANCE,
                            read("/trident_utils/datapack/data/trident-util/functions/generators.tdn")
                    )
                    .registerUnit(
                            Paths.get("datapack/data/trident-util/functions/shared.tdn"),
                            TridentFileUnitConfiguration.INSTANCE,
                            read("/trident_utils/datapack/data/trident-util/functions/shared.tdn")
                    )
                    .registerUnit(
                            Paths.get("datapack/data/trident-util/functions/predicate.tdn"),
                            TridentFileUnitConfiguration.INSTANCE,
                            read("/trident_utils/datapack/data/trident-util/functions/predicate.tdn")
                    )
                    .registerUnit(
                            Paths.get("datapack/data/trident-util/functions/advancement_triggers.tdn"),
                            TridentFileUnitConfiguration.INSTANCE,
                            read("/trident_utils/datapack/data/trident-util/functions/advancement_triggers.tdn")
                    )
                    .start();
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    @Override
    public UnitPassStrategy getUnitPassStrategy() {
        return UnitPassStrategy.GROUP_BY_UNIT_TYPE;
    }

    @Override
    public TridentProjectSummary createSummary() {
        return new TridentProjectSummary();
    }

    @Override
    public PrismarineTypeSystem createTypeSystem(PrismarineCompiler compiler, GlobalSymbolContext globalCtx) {
        return new TridentTypeSystem(compiler, globalCtx);
    }

    @Override
    public void populateGlobalContext(PrismarineCompiler compiler, GlobalSymbolContext global) {
        DefaultLibraryProvider.populateViaReflection(global, compiler.getTypeSystem());
    }

    @Override
    public void setupWorkerForCompilation(PrismarineProjectWorker worker) {
        worker.setup.addTasks(
                SetupPropertiesTask.INSTANCE,
                ValidatePropertiesTask.INSTANCE,
                SetupBuildConfigTask.INSTANCE,
                SetupModuleTask.INSTANCE,
                SetupProductionsTask.INSTANCE,
                SetupPluginsTask.INSTANCE,
                ValidateCircularDependenciesTask.INSTANCE,
                SetupRootDirectoryListTask.INSTANCE,
                SetupDependenciesTask.INSTANCE,
                SetupPluginsTransitivelyTask.INSTANCE,
                SetupPluginProductionsTask.INSTANCE,
                SetupResourcePackTask.INSTANCE,
                SetupSpecialFileManagerTask.INSTANCE,
                SetupTypeMapTask.INSTANCE,
                SetupWritingStackTask.INSTANCE
        );
    }

    @Override
    public void setupWorkerForSummary(PrismarineProjectWorker worker) {
        worker.setup.addTasks(
                SetupPropertiesTask.INSTANCE,
                SetupBuildConfigTask.INSTANCE,
                SetupModuleTask.INSTANCE,
                SetupProductionsTask.INSTANCE,
                SetupPluginsTask.INSTANCE,
                ValidateCircularDependenciesTask.INSTANCE,
                SetupRootDirectoryListTask.INSTANCE,
                SetupDependenciesTask.INSTANCE,
                SetupPluginsTransitivelyTask.INSTANCE,
                SetupPluginProductionsTask.INSTANCE
        );
    }

    @Override
    public void setupWorkerForLibrary(PrismarineProjectWorker worker) {
        worker.setup.addTasks(
                SetupLibraryModuleTask.INSTANCE,
                SetupProductionsTask.INSTANCE
        );
    }

    @Override
    public void setupWorkerForPlugin(PrismarineProjectWorker worker) {

    }

    @Override
    public void setupWalkerForCompilation(FileWalker<PrismarineCompiler> walker) {
        walker.addStops(
                CompilerWalkers.FUNCTION_STOP,
                CompilerWalkers.TAG_STOP,
                CompilerWalkers.DATA_EXPORTABLE_STOP,
                CompilerWalkers.RESOURCE_EXPORTABLE_STOP,
                CompilerWalkers.TYPE_MAP_STOP
        );
    }

    @Override
    public void setupWalkerForSummary(FileWalker<PrismarineProjectSummary> walker) {
        walker.addStops(
                SummaryWalkers.FUNCTION_STOP,
                SummaryWalkers.TAG_STOP,
                SummaryWalkers.SOUNDS_JSON_STOP
        );
    }

    @Override
    public void setupWalkerForPlugin(FileWalker<PrismarinePlugin> walker) {

    }

    @Override
    public void onCompilationStarted(PrismarineCompiler compiler) {
        AnalyzerManager.initialize();
    }

    @Override
    public void onAllCompilationWorkerTasksFinished(PrismarineProjectWorker worker, PrismarineCompiler compiler) {
        ResourcePackGenerator resourcePack = worker.output.get(SetupResourcePackTask.INSTANCE);
        if(resourcePack != null) {
            resourcePack.setCompiler(compiler);
        }

        DefaultOperators.populateOperatorManager(compiler.getTypeSystem().getOperatorManager(), compiler.getTypeSystem());
    }

    @Override
    public void onDependenciesResolved(PrismarineCompiler compiler) {
        TridentBuildConfiguration buildConfig = compiler.getWorker().output.get(SetupBuildConfigTask.INSTANCE);
        CommandModule module = compiler.getWorker().output.get(SetupModuleTask.INSTANCE);

        VersionFeatureManager.setActiveFeatureMap(buildConfig.featureMap);
        module.setSettingsActive();
    }

    @Override
    public void runSummaryPreFileTree(PrismarineProjectSummarizer<?> prismarineProjectSummarizer) {

    }

    @Override
    public void runSummaryPostFileTree(PrismarineProjectSummarizer<?> summarizer) {
        CommandModule module = summarizer.get(SetupModuleTask.INSTANCE);
        TridentProjectSummary summary = (TridentProjectSummary) summarizer.getSummary();

        // Add default minecraft types and tags:
        for(Namespace ns : module.getAllNamespaces()) {
            for(TypeDictionary typeDict : ns.types.getAllDictionaries()) {
                String category = typeDict.getCategory();
                for(Type type : typeDict.list()) {
                    if(type instanceof AliasType) {
                        summary.addType(category, new ResourceLocation(((AliasType) type).getAliasNamespace().getName() + ":" + ((AliasType) type).getAliasName()));
                    } else {
                        summary.addType(category, new ResourceLocation(type.toString()));
                    }
                }
            }
            for(TagGroup<?> group : ns.tags.getGroups()) {
                String category = group.getCategory();
                for(Tag tag : group.getAll()) {
                    summary.addTag(category, new ResourceLocation(tag.toString()));
                }
            }
        }
        JsonObject defaultSounds = ((JsonObject) module.getResource("sounds.json"));
        if(defaultSounds != null) {
            for(String key : defaultSounds.keySet()) {
                summary.addSoundEvent(new ResourceLocation("minecraft:" + key));
            }
        }
    }

    @Override
    public void incorporateDependency(PrismarineCompiler thisCompiler, PrismarineCompiler subCompiler) {
        CommandModule module = thisCompiler.getWorker().output.get(SetupModuleTask.INSTANCE);
        CommandModule subModule = subCompiler.getWorker().output.get(SetupModuleTask.INSTANCE);

        ResourcePackGenerator resourcePack = thisCompiler.getWorker().output.get(SetupResourcePackTask.INSTANCE);
        ResourcePackGenerator subResourcePack = subCompiler.getWorker().output.get(SetupResourcePackTask.INSTANCE);

        if(!subCompiler.getWorker().getDependencyInfo().doExport) {
            subModule.propagateExport(false);

            if(subResourcePack != null) {
                for(Exportable exportable : subResourcePack.exportables) {
                    exportable.setExport(false);
                }
            }
        }
        module.join(subModule);

        if(resourcePack != null && subResourcePack != null) {
            resourcePack.exportables.addAll(0, subResourcePack.exportables);
        }
    }

    @Override
    public void onAllPassesFinished(PrismarineCompiler compiler) {
        SpecialFileManager specialFileManager = compiler.getWorker().output.get(SetupSpecialFileManagerTask.INSTANCE);
        specialFileManager.setCompiler(compiler);
        specialFileManager.compile();
    }

    @Override
    public void generateOutput(PrismarineCompiler compiler) {
        compiler.updateProgress(0);
        compiler.setProgress("Generating data pack");

        TridentBuildConfiguration buildConfig = compiler.getWorker().output.get(SetupBuildConfigTask.INSTANCE);
        CommandModule module = compiler.getWorker().output.get(SetupModuleTask.INSTANCE);

        HashSet<String> dataPackSubFolderNames = new HashSet<>();
        HashSet<String> resourcePackSubFolderNames = new HashSet<>();

        if(buildConfig.dataPackOutput != null) {
            try {
                File bpOut = buildConfig.dataPackOutput;
                for(Exportable exportable : module.getAllExportables()) {
                    if(exportable.shouldExport() && exportable.getExportPath() != null) {
                        String firstName = exportable.getExportPath();
                        if(firstName.contains("/")) firstName = firstName.substring(0, firstName.indexOf("/"));
                        dataPackSubFolderNames.add(firstName);
                    }
                }
                if(buildConfig.cleanDataPackOutput) {
                    for(String subFolder : dataPackSubFolderNames) {
                        Debug.log("Clearing folder: " + subFolder);
                        FileUtil.recursivelyDelete(bpOut.toPath().resolve(subFolder).toFile());
                    }
                }
                module.compile(bpOut);
            } catch(IOException x) {
                compiler.logException(x, "Error while generating output data pack: ");
            }
        } else {
            compiler.getReport().addNotice(new Notice(NoticeType.ERROR, "Data pack output directory not specified"));
        }

        ResourcePackGenerator resourcePack = compiler.getWorker().output.get(SetupResourcePackTask.INSTANCE);

        compiler.updateProgress(0);
        compiler.setProgress("Generating resource pack");
        try {
            if(resourcePack != null) {
                if(buildConfig.cleanResourcePackOutput) {
                    File resourceOut = buildConfig.resourcePackOutput;
                    resourcePackSubFolderNames.add("assets");
                    for(Exportable exportable : resourcePack.getAllExportables()) {
                        if(exportable.shouldExport() && exportable.getExportPath() != null) {
                            String firstName = exportable.getExportPath();
                            if(firstName.contains("/")) firstName = firstName.substring(0, firstName.indexOf("/"));
                            resourcePackSubFolderNames.add(firstName);
                        }
                    }
                    for(String subFolder : resourcePackSubFolderNames) {
                        FileUtil.recursivelyDelete(resourceOut.toPath().resolve(subFolder).toFile());
                    }
                }
                resourcePack.generate();
            }
        } catch(IOException x) {
            compiler.logException(x, "Error while generating output resource pack: ");
        }
    }

    @Override
    public PrismarineCompiler createCompiler(PrismarineProjectWorker worker) {
        return new PrismarineCompiler(worker);
    }

    @Override
    public PrismarineProjectSummarizer createSummarizer(PrismarineProjectWorker worker) {
        return new PrismarineProjectSummarizer(worker);
    }

    @Override
    public PrismarineLibrary getStandardLibrary() {
        return standardLibrary;
    }







    private static String read(String file) {
        try(BufferedReader br = new BufferedReader(new InputStreamReader(Trident.class.getResourceAsStream(file)))) {
            StringBuilder sb = new StringBuilder();
            String line;
            for (; (line = br.readLine()) != null; ) {
                sb.append(line);
                sb.append("\n");
            }
            return sb.toString();
        } catch(NullPointerException x) {
            Debug.log("File not found: " + file, Debug.MessageType.ERROR);
            x.printStackTrace();
        } catch(IOException x) {
            Debug.log("Unable to access file: " + file, Debug.MessageType.ERROR);
        }
        return "";
    }
}
