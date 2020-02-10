package com.energyxxer.trident.compiler.plugin;

import com.energyxxer.commodore.util.io.CompoundInput;
import com.energyxxer.enxlex.lexical_analysis.EagerLexer;
import com.energyxxer.enxlex.lexical_analysis.LazyLexer;
import com.energyxxer.enxlex.lexical_analysis.token.TokenStream;
import com.energyxxer.trident.compiler.lexer.TridentProductions;
import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;

public class TridentPlugin {
    @NotNull
    private final Gson gson;
    @NotNull
    private final CompoundInput source;
    private final Path sourceFile;
    private boolean loaded;

    private HashMap<String, CommandDefinition> customCommands = new HashMap<>();

    public TridentPlugin(@NotNull CompoundInput source, File sourceFile) {
        this.loaded = false;
        this.source = source;
        this.gson = new Gson();
        this.sourceFile = sourceFile.toPath();
    }

    private EagerLexer eagerLexer;
    private LazyLexer lazyLexer;

    public synchronized void load() throws IOException {
        if(this.loaded) return;
        this.source.open();

        this.eagerLexer = new EagerLexer(new TokenStream(false));

        InputStream clis = source.get("commands/");
        if(clis != null) {
            try(BufferedReader br = new BufferedReader(new InputStreamReader(clis))) {

                String commandName;
                while ((commandName = br.readLine()) != null) {
                    loadCommandsFolder(commandName);
                    eagerLexer.getStream().tokens.clear();
                }
            }
        }

        this.loaded = true;
        this.source.close();
    }

    private void loadCommandsFolder(String commandName) throws IOException {
        String handlerPath = "commands/" + commandName + "/handler.tdn";
        String syntaxPath = "commands/" + commandName + "/syntax.tdnmeta";

        InputStream handlerIS = source.get(handlerPath);
        InputStream syntaxIS = source.get(syntaxPath);

        if(handlerIS == null || syntaxIS == null) throw new IOException("Missing handler.tdn or syntax.tdnmeta file inside 'commands/" + commandName + "/'");

        String handlerStr;
        String syntaxStr;
        try(BufferedReader br = new BufferedReader(new InputStreamReader(handlerIS))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
            handlerStr = sb.toString();
        }
        try(BufferedReader br = new BufferedReader(new InputStreamReader(syntaxIS))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
            syntaxStr = sb.toString();
        }
        CommandDefinition command = new CommandDefinition(commandName, syntaxStr, handlerStr);

        command.parseSyntaxFile(eagerLexer, sourceFile.resolve(syntaxPath).toFile());
        customCommands.put(commandName, command);
    }

    public void populateProductions(TridentProductions productions) throws IOException {
        load();
        this.lazyLexer = new LazyLexer(new TokenStream(false), productions.FILE);
        for(CommandDefinition command : customCommands.values()) {
            command.parseHandlerFile(lazyLexer, sourceFile.resolve("commands/" + command.getCommandName() + "/handler.tdn").toFile());
            command.createSyntax(productions);
        }
    }

    public CommandDefinition getCommand(String name) {
        return customCommands.get(name);
    }
}
