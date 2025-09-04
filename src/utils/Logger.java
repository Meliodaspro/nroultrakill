package utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class Logger {

    public static final int RESET = 0;
    public static final int YELLOW = 33;
    public static final int RED = 31;
    public static final int GREEN = 32;
    public static final int PURPLE = 35;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private Logger() {}

    private static String timestamp() {
        return DATE_FORMAT.format(new Date());
    }

    private static void printWithColor(int color, String message) {
        String prefix = "\u001B[" + color + "m";
        String reset = "\u001B[0m";
        System.out.print(prefix + "[" + timestamp() + "] " + message + reset);
    }

    public static void log(int color, String message) {
        printWithColor(color, message);
    }

    public static void warning(String message) {
        printWithColor(YELLOW, message);
    }

    public static void error(String message) {
        printWithColor(RED, message);
    }

    public static void success(String message) {
        printWithColor(GREEN, message);
    }

    public static void logException(Class<?> owner, Exception e) {
        error("Exception in " + (owner != null ? owner.getSimpleName() : "Unknown") + ": " + e + "\n");
        try {
            e.printStackTrace();
        } catch (Throwable ignored) {
        }
    }

    public static void logException(Class<?> owner, Exception e, String context) {
        error("Exception in " + (owner != null ? owner.getSimpleName() : "Unknown") + (context != null ? (" [" + context + "]") : "") + ": " + e + "\n");
        try {
            e.printStackTrace();
        } catch (Throwable ignored) {
        }
    }
}


