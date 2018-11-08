package com.energyxxer.trident.compiler;

import com.energyxxer.commodore.module.CommandModule;
import com.energyxxer.commodore.standard.StandardDefinitionPacks;
import com.energyxxer.enxlex.lexical_analysis.LazyLexer;
import com.energyxxer.enxlex.lexical_analysis.token.TokenStream;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.interfaces.ProgressListener;
import com.energyxxer.trident.compiler.lexer.TridentLexerProfile;
import com.energyxxer.trident.compiler.lexer.TridentProductions;
import com.energyxxer.util.logger.Debug;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class TridentCompiler {

    public static final String PROJECT_FILE_NAME = ".tdnproj";

    protected final File rootDir;
    protected final CommandModule module;

    private JsonObject properties = null;

    private ArrayList<ProgressListener> progressListeners = new ArrayList<>();
    private ArrayList<Runnable> completionListeners = new ArrayList<>();
    private CompilerReport report = null;

    private Thread thread;

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

    public void setReport(CompilerReport report) {
        this.report = report;
    }

    public void addCompletionListener(Runnable r) {
        completionListeners.add(r);
    }

    public void removeCompletionListener(Runnable r) {
        completionListeners.remove(r);
    }
}
