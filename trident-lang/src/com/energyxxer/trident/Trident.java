package com.energyxxer.trident;

import com.energyxxer.commodore.versioning.ThreeNumberVersion;
import com.energyxxer.nbtmapper.NBTTypeMap;
import com.energyxxer.prismarine.plugins.syntax.PrismarineMetaLexerProfile;
import com.energyxxer.util.logger.Debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;

public class Trident {
    public static final String PROJECT_FILE_NAME = ".tdnproj";
    public static final String PROJECT_BUILD_FILE_NAME = ".tdnbuild";
    public static final String FUNCTION_EXTENSION = ".tdn";
    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    public static final ThreeNumberVersion TRIDENT_LANGUAGE_VERSION = new ThreeNumberVersion(1,4,2);

    public static int NUM_IO_THREADS = 0;

    static {
        PrismarineMetaLexerProfile.functionNames.add("storeVar");
        PrismarineMetaLexerProfile.functionNames.add("storeFlat");
    }

    private Trident() {}

    public static class Resources {
        public static final HashMap<String, String> defaults = new HashMap<>();

        static {
            defaults.put("common.nbttm", read("/typemaps/common.nbttm"));
            defaults.put("entities.nbttm", read("/typemaps/entities.nbttm"));
            defaults.put("block_entities.nbttm", read("/typemaps/block_entities.nbttm"));
            defaults.put("trident.nbttm", read("/typemaps/trident.nbttm"));
        }

        private static String read(String file) {
            try(BufferedReader br = new BufferedReader(new InputStreamReader(NBTTypeMap.class.getResourceAsStream(file)))) {
                StringBuilder sb = new StringBuilder();
                String line;
                for (; (line = br.readLine()) != null; ) {
                    sb.append(line);
                    sb.append("\n");
                }
                return sb.toString();
            } catch(NullPointerException x) {
                x.printStackTrace();
                Debug.log("File not found: " + file, Debug.MessageType.ERROR);
            } catch(IOException x) {
                Debug.log("Unable to access file: " + file, Debug.MessageType.ERROR);
            }
            return "";
        }

    }
}
