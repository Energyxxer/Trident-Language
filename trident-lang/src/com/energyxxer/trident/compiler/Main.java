package com.energyxxer.trident.compiler;

import com.energyxxer.enxlex.report.Notice;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

public class Main {
    private static int previousProgress = 0;

    public static void main(String[] args) {
        System.out.println("Trident Language Compiler version " + TridentCompiler.TRIDENT_LANGUAGE_VERSION);
        if(args.length == 0) {
            System.out.println("No arguments passed to the program.\nFirst argument should be the path to the root directory of the project to compile.");
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


        TridentCompiler c = new TridentCompiler(rootDir);

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
