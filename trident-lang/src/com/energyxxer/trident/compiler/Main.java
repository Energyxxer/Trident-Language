package com.energyxxer.trident.compiler;

import com.energyxxer.commodore.defpacks.DefinitionPack;
import com.energyxxer.commodore.util.io.DirectoryCompoundInput;
import com.energyxxer.commodore.versioning.compatibility.VersionFeatureManager;
import com.energyxxer.enxlex.report.Notice;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;

public class Main {
    private static int previousProgress = 0;

    public static void main(String[] args) {

        System.out.println("Trident Language Compiler version " + TridentCompiler.TRIDENT_LANGUAGE_VERSION);
        if(args.length == 0) {
            System.out.println("No arguments passed to the program.\nFirst argument should be the path to the root directory of the project to compile.");
            System.out.println("Second argument (optional) should be a list of folder paths referring to the definition packs to use, separated by the platform-specific path separator.");
            System.out.println("Third argument (optional) should be the path to the feature map to use for this module");
            System.out.println("Fourth argument (optional) should be a list of file paths referring to the NBT type maps to use, separated by the platform-specific path separator.");
            System.out.println("Path separator detected for this platform: '" + File.pathSeparatorChar + "' (" + Character.getName(File.pathSeparatorChar) + ")");
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


        ArrayList<File> rawDefPacks = new ArrayList<>();
        File featMap = null;
        ArrayList<File> rawTypeMapFiles = new ArrayList<>();
        if(args.length >= 2) {
            String[] paths = args[1].split(Matcher.quoteReplacement(File.pathSeparator));
            for(String path : paths) {
                File packDir = new File(path);
                if(!packDir.exists()) {
                    System.err.println("Given definition pack file does not exist: " + path);
                    return;
                }
                if(!packDir.isDirectory()) {
                    System.err.println("Given definition pack file is not a directory: " + path);
                    return;
                }
                rawDefPacks.add(packDir);
            }

            if(args.length >= 3) {
                featMap = new File(args[2]);
                if(!featMap.exists()) {
                    System.err.println("Given feature map file does not exist: " + args[2]);
                    return;
                }
                if(!featMap.isFile()) {
                    System.err.println("Given feature map is not a file: " + args[2]);
                    return;
                }

                if(args.length >= 4) {
                    String[] typeMapPaths = args[3].split(Matcher.quoteReplacement(File.pathSeparator));
                    for(String path : typeMapPaths) {
                        File tmFile = new File(path);
                        if(!tmFile.exists()) {
                            System.err.println("Given type map file does not exist: " + path);
                            return;
                        }
                        if(!tmFile.isFile()) {
                            System.err.println("Given type map is not a file");
                            return;
                        }
                        rawTypeMapFiles.add(tmFile);
                    }
                }
            }
        }

        DefinitionPack[] definitionPacks = new DefinitionPack[rawDefPacks.size()];
        for(int i = 0; i < rawDefPacks.size(); i++) {
            definitionPacks[i] = new DefinitionPack(new DirectoryCompoundInput(rawDefPacks.get(i)));

            try {
                definitionPacks[i].load();
            } catch(Exception x) {
                System.err.println("Exception while trying to load definition pack at path '" + rawDefPacks.get(i) + ":");
                x.printStackTrace();
                return;
            }
        }

        FileReader featMapReader = null;
        if(featMap != null) {
            try {
                featMapReader = new FileReader(featMap);
            } catch(Exception x) {
                System.err.println("Exception while creating a FileReader for the feature map:");
                x.printStackTrace();
                return;
            }
        }

        String[] rawTypeMaps = null;
        if(!rawTypeMapFiles.isEmpty()) {
            rawTypeMaps = new String[rawTypeMapFiles.size()];

            int i = 0;
            for(File file : rawTypeMapFiles) {
                try(BufferedReader br = new BufferedReader(new FileReader(file))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    rawTypeMaps[i] = sb.toString();
                } catch (IOException e) {
                    System.err.println("Exception while reading type map '" + file + "':");
                    e.printStackTrace();
                    return;
                }
                i++;
            }
        }

        TridentCompiler c = new TridentCompiler(rootDir);
        c.setStartingDefinitionPacks(definitionPacks);
        c.setStartingFeatureMap(featMap != null ? VersionFeatureManager.parseFeatureMap(featMapReader) : null);
        c.setStartingRawTypeMaps(rawTypeMaps);

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
