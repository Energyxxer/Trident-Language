package com.energyxxer.trident.compiler.analyzers.default_libs;

import com.energyxxer.commodore.module.RawExportable;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.analyzers.type_handlers.DictionaryObject;
import com.energyxxer.trident.compiler.analyzers.type_handlers.MethodWrapper;
import com.energyxxer.trident.compiler.resourcepack.ResourcePackGenerator;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.SymbolStack;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@AnalyzerMember(key = "File")
public class FileLib implements DefaultLibraryProvider {
    @Override
    public void populate(SymbolStack stack, TridentCompiler compiler) {
        DictionaryObject fileLib = new DictionaryObject();
        DictionaryObject in = new DictionaryObject();
        fileLib.put("in", in);

        in.put("read",
                new MethodWrapper<>("read", ((instance, params) -> {
                    String rawPath = ((String) params[0]).replace("\\", "/");
                    while(rawPath.startsWith("/")) {
                        rawPath = rawPath.substring(1);
                    }
                    Path path = compiler.getRootDir().toPath().resolve(Paths.get(rawPath).normalize());
                    if(!path.startsWith(compiler.getRootDir().toPath())) {
                        throw new IllegalArgumentException("Cannot read files outside of the current project: " + path);
                    }
                    if(Files.exists(path)) {
                        if(Files.isDirectory(path)) return path.toFile().list();
                        return new String(Files.readAllBytes(path), TridentCompiler.DEFAULT_CHARSET);
                    } else throw new FileNotFoundException(path.toString());
                }), String.class).createForInstance(null));
        stack.getGlobal().put(new Symbol("File", Symbol.SymbolVisibility.GLOBAL, fileLib));


        DictionaryObject out = new DictionaryObject();
        fileLib.put("out", out);

        out.put("writeResource", new MethodWrapper<>("writeResource", ((instance, params) -> {
            String rawPath = ((String) params[0]).replace("\\", "/");
            while(rawPath.startsWith("/")) {
                rawPath = rawPath.substring(1);
            }
            Path path = Paths.get(rawPath).normalize();
            if(path.startsWith(Paths.get("../"))) {
                throw new IllegalArgumentException("Cannot write files outside the resource pack: " + path);
            }
            ResourcePackGenerator resourcePack = compiler.getResourcePackGenerator();
            if(resourcePack != null) {
                resourcePack.exportables.add(new RawExportable(path.toString(), ((String) params[1]).getBytes(TridentCompiler.DEFAULT_CHARSET)));
            }
            return null;
        }), String.class, String.class).createForInstance(null));

        out.put("writeData", new MethodWrapper<>("writeData", ((instance, params) -> {
            String rawPath = ((String) params[0]).replace("\\", "/");
            while(rawPath.startsWith("/")) {
                rawPath = rawPath.substring(1);
            }
            Path path = Paths.get(rawPath).normalize();
            if(path.startsWith(Paths.get("../"))) {
                throw new IllegalArgumentException("Cannot write files outside the data pack: " + path);
            }
            compiler.getModule().exportables.add(new RawExportable(path.toString(), ((String) params[1]).getBytes(TridentCompiler.DEFAULT_CHARSET)));
            return null;
        }), String.class, String.class).createForInstance(null));
    }
}
