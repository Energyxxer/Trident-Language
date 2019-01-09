package com.energyxxer.trident.util;

import com.energyxxer.util.logger.Debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by User on 1/21/2017.
 */
public class ResourceReader {
    public static String read(String file) {
        try(BufferedReader br = new BufferedReader(new InputStreamReader(ResourceReader.class.getResourceAsStream(file)))) {
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
    
    private ResourceReader() {}
}
