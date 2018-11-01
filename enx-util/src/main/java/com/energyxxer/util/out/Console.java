package com.energyxxer.util.out;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Created by User on 1/8/2017.
 */
public class Console {

    public static final PrintStream info;
    public static final PrintStream warn;
    public static final PrintStream err;
    public static final PrintStream debug;

    private static final MultiOutputStream infoOut = new MultiOutputStream();
    private static final MultiOutputStream warnOut = new MultiOutputStream();
    private static final MultiOutputStream errOut = new MultiOutputStream();
    private static final MultiOutputStream debugOut = new MultiOutputStream();

    static {
        info = new PrintStream(infoOut);
        warn = new PrintStream(warnOut);
        err = new PrintStream(errOut);
        debug = new PrintStream(debugOut);

        System.setOut(new PrintStream(new MultiOutputStream(info, System.out)));
        System.setErr(new PrintStream(new MultiOutputStream(err, System.err)));
    }

    public static void addInfoStream(OutputStream os) {
        infoOut.addStream(os);
    }

    public static void addWarnStream(OutputStream os) {
        warnOut.addStream(os);
    }

    public static void addErrStream(OutputStream os) {
        errOut.addStream(os);
    }

    public static void addDebugStream(OutputStream os) {
        debugOut.addStream(os);
    }
}
