package com.stocksignal.utils;

/**
 * The Logger class provides a simple utility for logging messages to the console.
 * It is used to output log messages for debugging or informational purposes.
 */
public class Logger {

    /**
     * Logs a message to the console with a "Log:" prefix.
     * 
     * @param message the message to be logged
     */
    public static void log(String message) {
        System.out.println("Log: " + message);
    }
}
