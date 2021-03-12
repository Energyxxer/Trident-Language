package com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection;

import com.energyxxer.commodore.module.RawExportable;
import com.energyxxer.trident.Trident;
import com.energyxxer.trident.compiler.resourcepack.ResourcePackGenerator;
import com.energyxxer.trident.worker.tasks.SetupModuleTask;
import com.energyxxer.trident.worker.tasks.SetupResourcePackTask;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class File {
    public static class in {
        public static Object read(String inPath, ISymbolContext callingCtx) throws IOException {
            String rawPath = inPath.replace(java.io.File.separator, "/");
            while(rawPath.startsWith("/")) {
                rawPath = rawPath.substring(1);
            }
            Path path = callingCtx.getCompiler().getRootCompiler().getRootPath().resolve(Paths.get(rawPath).normalize());
            if(!path.startsWith(callingCtx.getCompiler().getRootCompiler().getRootDir().toPath())) {
                throw new IllegalArgumentException("Cannot read files outside of the current project: " + path.toString().replace(java.io.File.separator, "/"));
            }
            if(Files.exists(path)) {
                if(Files.isDirectory(path)) return path.toFile().list();
                return new String(Files.readAllBytes(path), Trident.DEFAULT_CHARSET);
            } else throw new FileNotFoundException(path.toString().replace(java.io.File.separator, "/"));
        }

        public static Object exists(String inPath, ISymbolContext callingCtx) {
            String rawPath = inPath.replace("\\", "/");
            while(rawPath.startsWith("/")) {
                rawPath = rawPath.substring(1);
            }
            Path path = callingCtx.getCompiler().getRootCompiler().getRootPath().resolve(Paths.get(rawPath).normalize());
            if(!path.startsWith(callingCtx.getCompiler().getRootCompiler().getRootDir().toPath())) {
                throw new IllegalArgumentException("Cannot read files outside of the current project: " + path.toString().replace("\\", "/"));
            }
            return Files.exists(path);
        }

        public static Object isDirectory(String inPath, ISymbolContext callingCtx) {
            String rawPath = inPath.replace("\\", "/");
            while(rawPath.startsWith("/")) {
                rawPath = rawPath.substring(1);
            }
            Path path = callingCtx.getCompiler().getRootCompiler().getRootPath().resolve(Paths.get(rawPath).normalize());
            if(!path.startsWith(callingCtx.getCompiler().getRootCompiler().getRootDir().toPath())) {
                throw new IllegalArgumentException("Cannot read files outside of the current project: " + path.toString().replace("\\", "/"));
            }
            return Files.exists(path) && Files.isDirectory(path);
        }

        public static Object write(String inPath, String content, ISymbolContext callingCtx) throws IOException {
            String rawPath = inPath.replace(java.io.File.separator, "/");
            while(rawPath.startsWith("/")) {
                rawPath = rawPath.substring(1);
            }
            Path path = Paths.get(rawPath).normalize();
            if(path.startsWith(Paths.get("../"))) {
                throw new IllegalArgumentException("Cannot write files outside the project: " + path);
            }
            path = callingCtx.getCompiler().getRootCompiler().getRootPath().resolve(path);
            path.toFile().getParentFile().mkdirs();
            Files.write(path, content.getBytes(Trident.DEFAULT_CHARSET));
            return null;
        }

        public static boolean wasFileChanged(String inPath, ISymbolContext callingCtx) throws IOException {
            String rawPath = inPath.replace(java.io.File.separator, "/");
            while(rawPath.startsWith("/")) {
                rawPath = rawPath.substring(1);
            }
            Path relPath = Paths.get(rawPath).normalize();
            Path path = callingCtx.getCompiler().getRootCompiler().getRootPath().resolve(relPath);
            if(!path.startsWith(callingCtx.getCompiler().getRootCompiler().getRootDir().toPath())) {
                throw new IllegalArgumentException("Cannot read files outside of the current project: " + path.toString().replace(java.io.File.separator, "/"));
            }
            return callingCtx.getCompiler().getRootCompiler().getProjectReader().startQuery(relPath).perform().wasChangedSinceCached();
        }
    }

    public static class out {
        public static Object writeResource(String outPath, String content, ISymbolContext callingCtx) {
            String rawPath = outPath.replace(java.io.File.separator, "/");
            while(rawPath.startsWith("/")) {
                rawPath = rawPath.substring(1);
            }
            Path path = Paths.get(rawPath).normalize();
            if(path.startsWith(Paths.get("../"))) {
                throw new IllegalArgumentException("Cannot write files outside the resource pack: " + path);
            }
            ResourcePackGenerator resourcePack = callingCtx.get(SetupResourcePackTask.INSTANCE);
            if(resourcePack != null) {
                resourcePack.exportables.add(new RawExportable(path.toString().replace(java.io.File.separator, "/"), content.getBytes(Trident.DEFAULT_CHARSET)));
            }
            return null;
        }

        public static Object writeData(String outPath, String content, ISymbolContext callingCtx) {
            String rawPath = outPath.replace(java.io.File.separator, "/");
            while(rawPath.startsWith("/")) {
                rawPath = rawPath.substring(1);
            }
            Path path = Paths.get(rawPath).normalize();
            if(path.startsWith(Paths.get("../"))) {
                throw new IllegalArgumentException("Cannot write files outside the data pack: " + path);
            }
            callingCtx.get(SetupModuleTask.INSTANCE).exportables.add(new RawExportable(path.toString().replace(java.io.File.separator, "/"), content.getBytes(Trident.DEFAULT_CHARSET)));
            return null;

        }
    }
}
