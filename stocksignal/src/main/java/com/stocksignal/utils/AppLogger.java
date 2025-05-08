package com.stocksignal.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class AppLogger {

    private static final Logger logger = LoggerFactory.getLogger(AppLogger.class);

    /**
     * Adds a key-value pair to the MDC for context (e.g., stock symbol, trade ID).
     * 
     * @param key the MDC key (e.g., "stockSymbol")
     * @param value the MDC value (e.g., "AAPL")
     */
    public static void addContext(String key, String value) {
        MDC.put(key, value);
    }

    /**
     * Removes a key-value pair from the MDC.
     * 
     * @param key the MDC key to remove
     */
    public static void removeContext(String key) {
        MDC.remove(key);
    }

    /**
     * Clears the MDC context.
     */
    public static void clearContext() {
        MDC.clear();
    }

    /**
     * Logs an info message with optional parameters.
     * 
     * @param message the message to log
     * @param params optional parameters to include in the log message
     */
    public static void info(String message, Object... params) {
        logger.info(message, params);
    }

    /**
     * Logs an error message with an exception.
     * 
     * @param message the error message to log
     * @param throwable the exception to log
     * @param params optional parameters to include in the log message
     */
    public static void error(String message, Throwable throwable, Object... params) {
        logger.error(message, throwable, params);
    }

    /**
     * Logs an error message without an exception.
     * 
     * @param message the error message to log
     * @param params optional parameters to include in the log message
     */
    public static void error(String message, Object... params) {
        logger.error(message, params);
    }

    /**
     * Logs a warning message with optional parameters.
     * 
     * @param message the warning message to log
     * @param params optional parameters to include in the log message
     */
    public static void warn(String message, Object... params) {
        logger.warn(message, params);
    }

    /**
     * Logs a debug message with optional parameters.
     * 
     * @param message the debug message to log
     * @param params optional parameters to include in the log message
     */
    public static void debug(String message, Object... params) {
        logger.debug(message, params);
    }
}
