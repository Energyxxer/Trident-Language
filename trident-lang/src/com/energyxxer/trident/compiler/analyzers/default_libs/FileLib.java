package com.energyxxer.trident.compiler.analyzers.default_libs;

import com.energyxxer.commodore.module.RawExportable;
import com.energyxxer.trident.compiler.TridentCompiler;
import com.energyxxer.trident.compiler.analyzers.general.AnalyzerMember;
import com.energyxxer.trident.compiler.resourcepack.ResourcePackGenerator;
import com.energyxxer.trident.compiler.semantics.Symbol;
import com.energyxxer.trident.compiler.semantics.custom.classes.CustomClass;
import com.energyxxer.trident.compiler.semantics.symbols.ISymbolContext;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.energyxxer.trident.compiler.analyzers.type_handlers.TridentNativeFunctionBranch.nativeMethodsToFunction;

@AnalyzerMember(key = "File")
public class FileLib implements DefaultLibraryProvider {
    @Override
    public void populate(ISymbolContext globalCtx, TridentCompiler compiler) {
        CustomClass fileLib = new CustomClass("File", "trident-util:native", globalCtx);
        fileLib.setNoConstructor();
        globalCtx.put(new Symbol("File", Symbol.SymbolVisibility.GLOBAL, fileLib));

        CustomClass in = new CustomClass("in", "trident-util:native", fileLib.getInnerStaticContext());
        in.setNoConstructor();
        fileLib.putStaticFinalMember("in", in);

        CustomClass out = new CustomClass("out", "trident-util:native", fileLib.getInnerStaticContext());
        out.setNoConstructor();
        fileLib.putStaticFinalMember("out", out);


        try {
            in.putStaticFunction(nativeMethodsToFunction(in.getInnerStaticContext(), FileLib.class.getMethod("read", String.class, ISymbolContext.class)));
            out.putStaticFunction(nativeMethodsToFunction(out.getInnerStaticContext(), FileLib.class.getMethod("writeResource", String.class, String.class, ISymbolContext.class)));
            out.putStaticFunction(nativeMethodsToFunction(out.getInnerStaticContext(), FileLib.class.getMethod("writeData", String.class, String.class, ISymbolContext.class)));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static Object read(String inPath, ISymbolContext callingCtx) throws IOException {
        String rawPath = inPath.replace("\\", "/");
        while(rawPath.startsWith("/")) {
            rawPath = rawPath.substring(1);
        }
        Path path = callingCtx.getCompiler().getRootCompiler().getRootDir().toPath().resolve(Paths.get(rawPath).normalize());
        if(!path.startsWith(callingCtx.getCompiler().getRootCompiler().getRootDir().toPath())) {
            throw new IllegalArgumentException("Cannot read files outside of the current project: " + path.toString().replace("\\", "/"));
        }
        if(Files.exists(path)) {
            if(Files.isDirectory(path)) return path.toFile().list();
            return new String(Files.readAllBytes(path), TridentCompiler.DEFAULT_CHARSET);
        } else throw new FileNotFoundException(path.toString().replace("\\", "/"));
    }

    public static Object writeResource(String outPath, String content, ISymbolContext callingCtx) {
        String rawPath = outPath.replace("\\", "/");
        while(rawPath.startsWith("/")) {
            rawPath = rawPath.substring(1);
        }
        Path path = Paths.get(rawPath).normalize();
        if(path.startsWith(Paths.get("../"))) {
            throw new IllegalArgumentException("Cannot write files outside the resource pack: " + path);
        }
        ResourcePackGenerator resourcePack = callingCtx.getCompiler().getRootCompiler().getResourcePackGenerator();
        if(resourcePack != null) {
            resourcePack.exportables.add(new RawExportable(path.toString().replace("\\", "/"), content.getBytes(TridentCompiler.DEFAULT_CHARSET)));
        }
        return null;
    }

    public static Object writeData(String outPath, String content, ISymbolContext callingCtx) {
        String rawPath = outPath.replace("\\", "/");
        while(rawPath.startsWith("/")) {
            rawPath = rawPath.substring(1);
        }
        Path path = Paths.get(rawPath).normalize();
        if(path.startsWith(Paths.get("../"))) {
            throw new IllegalArgumentException("Cannot write files outside the data pack: " + path);
        }
        callingCtx.getCompiler().getRootCompiler().getModule().exportables.add(new RawExportable(path.toString().replace("\\", "/"), content.getBytes(TridentCompiler.DEFAULT_CHARSET)));
        return null;

    }
}
