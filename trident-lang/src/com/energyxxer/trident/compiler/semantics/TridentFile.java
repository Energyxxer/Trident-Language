package com.energyxxer.trident.compiler.semantics;

import com.energyxxer.commodore.functionlogic.functions.Function;
import com.energyxxer.commodore.functionlogic.functions.FunctionComment;
import com.energyxxer.commodore.module.CommandModule;
import com.energyxxer.commodore.module.Namespace;
import com.energyxxer.commodore.types.defaults.FunctionReference;
import com.energyxxer.enxlex.pattern_matching.structures.*;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.trident.compiler.CompilerExtension;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.TridentUtil;
import com.energyxxer.trident.compiler.commands.RawCommand;
import com.energyxxer.util.logger.Debug;

import java.io.File;
import java.nio.file.Path;
import java.util.regex.Matcher;

public class TridentFile implements CompilerExtension {
    private final TridentCompiler compiler;
    private final CommandModule module;
    private final Namespace namespace;
    private TokenPattern<?> pattern;

    private Function function;

    private boolean compileOnly = false;

    public TridentFile(TridentCompiler compiler, Path relSourcePath, TokenPattern<?> filePattern) {
        this.compiler = compiler;
        this.module = compiler.getModule();
        this.namespace = module.createNamespace(relSourcePath.getName(0).toString());
        this.pattern = filePattern;

        String functionPath = relSourcePath.subpath(2, relSourcePath.getNameCount()).toString();
        functionPath = functionPath.substring(0, functionPath.length()-".tdn".length()).replaceAll(Matcher.quoteReplacement(File.separator), "/");
        this.function = namespace.functions.create(functionPath);

        TokenPattern<?> directiveList = filePattern.find("..DIRECTIVES");
        if(directiveList != null) {
            TokenPattern<?>[] directives = ((TokenList) directiveList).getContents();
            for(TokenPattern<?> rawDirective : directives) {
                TokenGroup directiveBody = (TokenGroup) (((TokenStructure) ((TokenGroup) rawDirective).getContents()[1]).getContents());

                switch(directiveBody.getName()) {
                    case "ON_DIRECTIVE": {
                        String on = ((TokenItem) (directiveBody.getContents()[1])).getContents().value;
                        if(on.equals("compile")) compileOnly = true;
                        break;
                    }
                    case "TAG_DIRECTIVE": {
                        TridentUtil.ResourceLocation loc = new TridentUtil.ResourceLocation(((TokenItem) (directiveBody.getContents()[1])).getContents());
                        module.createNamespace(loc.namespace).tags.functionTags.create(loc.body).addValue(new FunctionReference(this.function));
                        break;
                    }
                    default: {
                        reportNotice(new Notice(NoticeType.DEBUG, "Unknown directive type '" + directiveBody.getName() + "'", directiveBody));
                    }
                }
            }
        }
    }

    public TridentCompiler getCompiler() {
        return compiler;
    }

    public void resolveEntries() {
        Debug.log("Resolving entries for " + function);
        TokenPattern<?>[] entries = ((TokenList) this.pattern.find(".ENTRIES")).getContents();

        for(TokenPattern<?> pattern : entries) {
            if(!pattern.getName().equals("LINE_PADDING")) {
                TokenStructure entry = (TokenStructure) pattern.find("ENTRY");

                TokenPattern<?> inner = entry.getContents();
                Debug.log(inner.getName());

                switch(inner.getName()) {
                    case "COMMAND":
                        break;
                    case "COMMENT":
                        function.append(new FunctionComment(inner.flattenTokens().get(0).value.substring(1)));
                        break;
                    case "VERBATIM_COMMAND":
                        function.append(new RawCommand(inner.flattenTokens().get(0).value.substring(1)));
                        break;
                }
            }
        }
    }
}
