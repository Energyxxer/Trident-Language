package com.energyxxer.trident.util;

import com.energyxxer.util.logger.Debug;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Created by User on 1/8/2017.
 */
public class LineReader {
    public static ArrayList<String> read(String file) throws IOException {
        ArrayList<String> lines = new ArrayList<>();
        try(BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        LineReader.
                                class.
                                getResourceAsStream(file)))) {
            String line;
            for (; (line = br.readLine()) != null; ) {
                if(line.length() > 0 && !line.startsWith("#")) lines.add(line.trim());
            }
            return lines;
        } catch(NullPointerException npe) {
            Debug.log("File not found: " + file, Debug.MessageType.ERROR);
            return new ArrayList<>();
        }
    }

    public static ArrayList<String> read(File file) throws IOException {
        ArrayList<String> lines = new ArrayList<>();

        try(FileInputStream inputStream = new FileInputStream(file.getPath())){
            Scanner sc = new Scanner(inputStream, "UTF-8");
            while(sc.hasNextLine()) {
                lines.add(sc.nextLine());
            }
            return lines;
        } catch(FileNotFoundException x) {
            Debug.log("File not found: " + file, Debug.MessageType.ERROR);
            return new ArrayList<>();
        }
    }
    
    private LineReader() {}
}