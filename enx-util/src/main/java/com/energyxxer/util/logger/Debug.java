package com.energyxxer.util.logger;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Debug {

    public enum MessageType {
        INFO, ERROR, WARN
    }

    private static ArrayList<OutputStream> streams = new ArrayList<>();

    private static void logRaw(String message) {
        for(OutputStream stream : streams) {
            try {
                stream.write(message.getBytes());
                stream.flush();
            } catch(IOException x) {
                //idk what to do now
            }
        }
    }

    public static void log(Object object) {
        log(String.valueOf(object));
    }

    public static void log(String message) {
        log(message, MessageType.INFO);
    }

    public static void log(Object object, MessageType type) {
        log(String.valueOf(object), type);
    }

    public static void log(String message, MessageType type) {
        DateTimeFormatter format = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ENGLISH);
        logRaw("[" + format.format(LocalDateTime.now()) + "] [" + getCallerClassName() + "/" + type + "] " + message + "\n");
    }

    public static void addStream(OutputStream stream) {
        streams.add(stream);
    }

    public static String getCallerClassName() {
        StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
        for (int i=1; i<stElements.length; i++) {
            StackTraceElement ste = stElements[i];
            if (!ste.getClassName().equals(Debug.class.getName()) && ste.getClassName().indexOf("java.lang.Thread")!=0) {
                return ste.getFileName().replace(".java", "");
            }
        }
        return null;
    }
}
