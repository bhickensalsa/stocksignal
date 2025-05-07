package com.stocksignal.exceptions;

import com.stocksignal.utils.AppLogger;

/**
 * ConfigurationException is a custom runtime exception thrown when there is an error related to the 
 * configuration of the stock signal application, such as invalid configurations, missing settings,
 * or misconfigured parameters.
 * <p>
 * This exception is intended to signal issues that are related to the application's configuration
 * and provides logging capabilities through the {@link AppLogger} utility.
 * </p>
 */
public class ConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new ConfigurationException with the specified detail message.
     *
     * @param message the detail message explaining the reason for the exception
     */
    public ConfigurationException(String message) {
        super(message);
        // Log the error message
        AppLogger.error("ConfigurationException: " + message);
    }

    /**
     * Constructs a new ConfigurationException with the specified detail message and cause.
     *
     * @param message the detail message explaining the reason for the exception
     * @param cause the cause of the exception (which can be retrieved later with {@link Throwable#getCause()})
     */
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
        // Log both the error message and the stack trace of the cause
        AppLogger.error("ConfigurationException: " + message, cause);
    }
}
