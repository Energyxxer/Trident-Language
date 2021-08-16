package com.energyxxer.trident.compiler.analyzers.default_libs.via_reflection;

import com.energyxxer.commodore.module.RawExportable;
import com.energyxxer.enxlex.pattern_matching.structures.TokenPattern;
import com.energyxxer.enxlex.report.Notice;
import com.energyxxer.enxlex.report.NoticeType;
import com.energyxxer.prismarine.symbols.contexts.ISymbolContext;
import com.energyxxer.trident.Trident;
import com.energyxxer.trident.compiler.analyzers.default_libs.DefaultLibraryProvider;
import com.energyxxer.trident.compiler.analyzers.type_handlers.ListObject;
import com.energyxxer.trident.compiler.resourcepack.ResourcePackGenerator;
import com.energyxxer.trident.worker.tasks.SetupModuleTask;
import com.energyxxer.trident.worker.tasks.SetupResourcePackTask;
import com.energyxxer.trident.worker.tasks.SetupRootDirectoryListTask;
import com.energyxxer.util.FileUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class File {
    public static class in {
        @DefaultLibraryProvider.HideFromCustomClass
        public static java.io.File inPathToFile(String inPath, ISymbolContext ctx) {
            Path path = Paths.get(inPath.replace("/",java.io.File.separator)).normalize();
            if(path.startsWith(Paths.get("../"))) {
                throw new IllegalArgumentException("Cannot write files outside the project: " + path);
            }

            for(java.io.File rootDir : ctx.get(SetupRootDirectoryListTask.INSTANCE)) {
                Path rootPath = rootDir.toPath();

                Path pathInThisRoot = rootPath.resolve(path);

                if(Files.exists(pathInThisRoot)) {
                    return pathInThisRoot.toFile();
                }
            }

            return null;
        }

        @DefaultLibraryProvider.HideFromCustomClass
        public static ArrayList<String> listSubFileNames(String inPath, ISymbolContext ctx) {
            ArrayList<String> all = new ArrayList<>();

            Path path = Paths.get(inPath.replace("/",java.io.File.separator)).normalize();
            if(path.startsWith(Paths.get("../"))) {
                throw new IllegalArgumentException("Cannot write files outside the project: " + path);
            }

            for(java.io.File rootDir : ctx.get(SetupRootDirectoryListTask.INSTANCE)) {
                Path rootPath = rootDir.toPath();

                Path pathInThisRoot = rootPath.resolve(path);

                if(Files.exists(pathInThisRoot) && Files.isDirectory(pathInThisRoot)) {
                    for(String filename : pathInThisRoot.toFile().list()) {
                        if(!all.contains(filename)) all.add(filename);
                    }
                }
            }

            return all;
        }

        public static Object read(String inPath, ISymbolContext callingCtx) throws IOException {
            java.io.File file = inPathToFile(inPath, callingCtx);
            if(file != null && file.exists()) {
                if (Files.isDirectory(file.toPath())) return new ListObject(callingCtx.getTypeSystem(), listSubFileNames(inPath, callingCtx));
                return new String(Files.readAllBytes(file.toPath()), Trident.DEFAULT_CHARSET);
            } else throw new FileNotFoundException(inPath.toString());
        }

        public static Object exists(String inPath, ISymbolContext callingCtx) {
            return inPathToFile(inPath, callingCtx) != null;
        }

        public static Object isDirectory(String inPath, ISymbolContext callingCtx) {
            java.io.File file = inPathToFile(inPath, callingCtx);
            return file != null && file.isDirectory();
        }

        public static Object write(String inPath, String content, ISymbolContext callingCtx) throws IOException {
            Path path = Paths.get(inPath.replace("/",java.io.File.separator)).normalize();
            if(path.startsWith(Paths.get("../"))) {
                throw new IllegalArgumentException("Cannot write files outside the project: " + path);
            }
            path = callingCtx.getCompiler().getRootCompiler().getRootPath().resolve(path);
            path.toFile().getParentFile().mkdirs();
            Files.write(path, content.getBytes(Trident.DEFAULT_CHARSET));
            return null;
        }

        public static void delete(String inPath, TokenPattern<?> callingPattern, ISymbolContext callingCtx) throws IOException {
            String rawPath = inPath.replace(java.io.File.separator, "/");
            while(rawPath.startsWith("/")) {
                rawPath = rawPath.substring(1);
            }
            Path path = Paths.get(rawPath).normalize();
            if(path.startsWith(Paths.get("../"))) {
                throw new IllegalArgumentException("Cannot delete files outside the project: " + path);
            }
            path = callingCtx.getCompiler().getRootCompiler().getRootPath().resolve(path);
            java.io.File file = path.toFile();
            if(file.exists()) {
                Notice notice = new Notice(NoticeType.DEBUG, path.toString().replace(java.io.File.separatorChar, '/'), callingPattern);
                notice.setGroup("Deleted files");
                callingCtx.getCompiler().getReport().addNotice(notice);
            }
            FileUtil.recursivelyDelete(path.toFile());
        }

        public static boolean wasFileChanged(String inPath, ISymbolContext callingCtx) throws IOException {
            Path relPath = Paths.get(inPath.replace("/",java.io.File.separator));
            try {
                return callingCtx.getCompiler().getRootCompiler().getProjectReader().startQuery(relPath, callingCtx.getCompiler().getRootCompiler().getWorker()).perform().wasChangedSinceCached();
            } catch(FileNotFoundException x) {
                return false;
            }
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
