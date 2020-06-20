package com.energyxxer.trident.compiler;

import com.energyxxer.enxlex.report.Notice;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import static com.energyxxer.trident.compiler.TridentCompiler.newFileObject;

public class Main {
    private static int previousProgress = 0;

    public static void main(String[] args) throws IOException {

        System.out.println("Trident Language Compiler version " + TridentCompiler.TRIDENT_LANGUAGE_VERSION);
        if(args.length == 0) {
            System.out.println("No arguments passed to the program.\nFirst argument should be the path to the root directory of the project to compile.");
            System.out.println("Second argument (optional) should be a path to the build configuration file to use for this project. Defaults to `(project root)/.tdnbuild`.");
            return;
        }

        File rootDir = new File(args[0]);
        if(!rootDir.exists()) {
            System.err.println("Given root file does not exist: " + rootDir);
            return;
        }
        if(!rootDir.isDirectory()) {
            System.err.println("Given root is not a directory: " + rootDir);
            return;
        }
        System.out.println("Compiling project at directory " + rootDir);
        System.out.println();

        File buildFile = rootDir.toPath().resolve(TridentCompiler.PROJECT_BUILD_FILE_NAME).toFile();

        if(args.length >= 2) {
            buildFile = newFileObject(args[1], rootDir);
        }

        TridentBuildConfiguration resources = new TridentBuildConfiguration();
        resources.populateFromJson(buildFile, rootDir);

        TridentCompiler c = new TridentCompiler(rootDir);
        c.setBuildConfig(resources);

        c.addProgressListener((process) -> {
            StringBuilder line = new StringBuilder(process.getStatus());
            if(process.getProgress() != -1) {
                line.append(" | ").append(100 * process.getProgress()).append("%");
            }
            final int thisLength = line.length();
            for(int i = line.length(); i < previousProgress; i++) {
                line.append(" ");
            }
            previousProgress = thisLength;

            System.out.print("\r" + line);
        });

        c.addCompletionListener((process, success) -> {
            System.out.println("\n");
            for(Map.Entry<String, ArrayList<Notice>> group : c.getReport().group().entrySet()) {
                System.out.println("In file " + group.getKey() + ":");
                for(Notice notice : group.getValue()) {
                    for(String line : ("(" + notice.getType().toString() + ") " + notice.getExtendedMessage()).split("\n")) {
                        System.out.println("    " + line);
                    }
                }
                System.out.println();
            }
        });
        c.start();
    }
}
