package com.stocksignal.utils;

import org.slf4j.LoggerFactory;

public class AppLogger {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(AppLogger.class);

    /**
     * Logs an info message.
     * 
     * @param message the message to log
     * @param params optional parameters to include in the log message
     */
    public static void info(String message, Object... params) {
        logger.info(message, params);
    }

    /**
     * Logs an error message.
     * 
     * @param message the error message to log
     * @param params optional parameters to include in the log message
     */
    public static void error(String message, Object... params) {
        logger.error(message, params);
    }

    /**
     * Logs a warning message.
     * 
     * @param message the warning message to log
     * @param params optional parameters to include in the log message
     */
    public static void warn(String message, Object... params) {
        logger.warn(message, params);
    }

    /**
     * Logs a debug message.
     * 
     * @param message the debug message to log
     * @param params optional parameters to include in the log message
     */
    public static void debug(String message, Object... params) {
        logger.debug(message, params);
    }
}
