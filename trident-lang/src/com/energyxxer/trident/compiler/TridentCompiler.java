package com.energyxxer.trident.compiler;

import com.energyxxer.commodore.module.CommandModule;
import com.energyxxer.commodore.module.ModulePackGenerator;
import com.energyxxer.commodore.standard.StandardDefinitionPacks;
import com.energyxxer.enxlex.lexical_analysis.LazyLexer;
import com.energyxxer.enxlex.lexical_analysis.token.TokenStream;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.commands.parsers.CommandParser;
import com.energyxxer.trident.compiler.interfaces.ProgressListener;
import com.energyxxer.trident.compiler.lexer.TridentLexerProfile;
import com.energyxxer.trident.compiler.lexer.TridentProductions;
import com.energyxxer.trident.compiler.semantics.TridentFile;
import com.energyxxer.util.logger.Debug;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TridentCompiler {

    public static final String PROJECT_FILE_NAME = ".tdnproj";

    protected final File rootDir;
    protected final CommandModule module;

    private JsonObject properties = null;

    private ArrayList<ProgressListener> progressListeners = new ArrayList<>();
    private ArrayList<Runnable> completionListeners = new ArrayList<>();
    private CompilerReport report = null;

    private Thread thread;

    private HashMap<File, TokenPattern<?>> filePatterns = new HashMap<>();
    private HashMap<File, TridentFile> files = new HashMap<>();

    public TridentCompiler(File rootDir) {
        this.rootDir = rootDir;
        module = new CommandModule(rootDir.getName());


    }

    public void compile() {
        this.thread = new Thread(this::runCompilation,"Trident-Compiler[" + rootDir.getName() + "]");
        report = new CompilerReport();
        thread.start();
    }

    private void runCompilation() {

        this.setProgress("Initializing default command parsers");

        CommandParser.Static.initialize();

        this.setProgress("Reading project settings file");
        try {
            properties = new Gson().fromJson(new FileReader(new File(rootDir.getPath() + File.separator + PROJECT_FILE_NAME)), JsonObject.class);
        } catch(IOException x) {
            logException(x);
            return;
        }


        this.setProgress("Importing vanilla definitions");
        try {
            module.importDefinitions(StandardDefinitionPacks.MINECRAFT_JAVA_LATEST_SNAPSHOT);
            Debug.log(module.minecraft.tags.itemTags.getAll());
        } catch(IOException x) {
            logException(x);
            return;
        }


        this.setProgress("Scanning files");
        TokenStream ts = new TokenStream();
        LazyLexer lex = new LazyLexer(ts, TridentProductions.FILE);
        recursivelyParse(lex, rootDir);

        this.getReport().addNotices(lex.getNotices());
        if(lex.getNotices().size() > 0) {
            finalizeCompilation();
            return;
        }

        Path dataRoot = new File(rootDir, "datapack" + File.separator + "data").toPath();

        for(Map.Entry<File, TokenPattern<?>> entry : filePatterns.entrySet()) {
            Path relativePath = dataRoot.relativize(entry.getKey().toPath());
            if("functions".equals(relativePath.getName(1).toString())) {
                files.put(entry.getKey(), new TridentFile(this, relativePath, entry.getValue()));
            }
            else {
                report.addNotice(new Notice(NoticeType.WARNING, "Found .tdn file outside of a function folder, ignoring: " + relativePath, entry.getValue()));
            }
        }

        files.values().forEach(TridentFile::checkCircularRequires);

        ArrayList<TridentFile> sortedFiles = new ArrayList<>(files.values());

        sortedFiles.sort((a,b) ->
                (a.isCompileOnly() != b.isCompileOnly()) ?
                        a.isCompileOnly() ? -1 : 1 :
                (a.getRequires().contains(b.getResourceLocation())) ?
                        1 :
                        (b.getRequires().contains(a.getResourceLocation())) ?
                            -1 :
                            0
        );

        for(TridentFile file : sortedFiles) {
            file.resolveEntries();
        }

        if(report.hasErrors()) {
            finalizeCompilation();
            return;
        }


        this.setProgress("Generating data pack");

        {
            Path path = new File(properties.get("datapack-output").getAsString()).toPath();
            try {
                module.compile(path.toFile().getParentFile(), path.endsWith(".zip") ? ModulePackGenerator.OutputType.ZIP : ModulePackGenerator.OutputType.FOLDER);
            } catch(IOException x) {
                logException(x);
                finalizeCompilation();
            }
        }


        this.setProgress("Compilation completed with " + report.getTotalsString(), false);
        finalizeCompilation();
    }

    private void recursivelyParse(LazyLexer lex, File dir) {
        File[] files = dir.listFiles();
        if(files == null) return;
        for (File file : files) {
            String name = file.getName();
            if (file.isDirectory()) {
                if(!file.getParentFile().equals(rootDir) || file.getName().equals("datapack")) {
                    //This is not the resource pack directory.
                    recursivelyParse(lex, file);
                }
            } else {
                if(!name.endsWith(".tdn")) continue;
                Debug.log("Parsing " + name);

                try {
                    String str = new String(Files.readAllBytes(Paths.get(file.getPath())));
                    lex.tokenizeParse(file, str, new TridentLexerProfile());

                    if(lex.getMatchResponse().matched) {
                        filePatterns.put(file, lex.getMatchResponse().pattern);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void logException(Exception x) {
        this.report.addNotice(new Notice(NoticeType.ERROR, x.getMessage()));
        finalizeCompilation();
    }

    private void finalizeCompilation() {
        completionListeners.forEach(Runnable::run);
        progressListeners.clear();
        completionListeners.clear();
    }

    public void addProgressListener(ProgressListener l) {
        progressListeners.add(l);
    }

    private void removeProgressListener(ProgressListener l) {
        progressListeners.remove(l);
    }

    private void setProgress(String message) {
        setProgress(message, true);
    }

    private void setProgress(String message, boolean includeProjectName) {
        progressListeners.forEach(l -> l.onProgress(message + (includeProjectName ? ("... [" + rootDir.getName() + "]") : "")));
    }

    public CompilerReport getReport() {
        return report;
    }

    public CommandModule getModule() {
        return module;
    }

    public void setReport(CompilerReport report) {
        this.report = report;
    }

    public void addCompletionListener(Runnable r) {
        completionListeners.add(r);
    }

    public void removeCompletionListener(Runnable r) {
        completionListeners.remove(r);
    }

    public JsonObject getProperties() {
        return properties;
    }

    public TridentFile getFile(TridentUtil.ResourceLocation loc) {
        for(TridentFile file : files.values()) {
            if(file.getResourceLocation().equals(loc)) return file;
        }
        return null;
    }
}
