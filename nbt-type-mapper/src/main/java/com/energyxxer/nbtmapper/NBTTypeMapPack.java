package com.energyxxer.nbtmapper;

import com.energyxxer.commodore.util.io.CompoundInput;

import java.io.*;
import java.util.ArrayList;

public class NBTTypeMapPack {
    private File rootFile = null;
    private ArrayList<String> rawFiles = new ArrayList<>();

    private NBTTypeMapPack() {}

    public static NBTTypeMapPack fromCompound(CompoundInput input) throws IOException {
        NBTTypeMapPack pack = new NBTTypeMapPack();
        pack.rootFile = input.getRootFile();
        try {
            input.open();
            InputStream fileListIS = input.get("");
            if (fileListIS != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(fileListIS))) {
                    String innerFileName;
                    ArrayList<String> typeMapsInside = new ArrayList<>();
                    while ((innerFileName = br.readLine()) != null) {
                        InputStream is = input.get(innerFileName);
                        if (is != null) {
                            typeMapsInside.add(readAllText(is));
                        }
                    }

                    pack.rawFiles.addAll(typeMapsInside);
                }
            }
        } finally {
            input.close();
        }
        return pack;
    }

    private static String readAllText(InputStream is) throws IOException {
        try(BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
            if(sb.length() > 0) sb.setLength(sb.length()-1);
            return sb.toString();
        }
    }

    public Iterable<String> getAllFileContents() {
        return rawFiles;
    }

    public File getRootFile() {
        return rootFile;
    }
}
