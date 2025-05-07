package com.stocksignal.exceptions;

import com.stocksignal.utils.AppLogger;

/**
 * DataProcessingException is a custom runtime exception that is thrown when an error occurs during
 * the processing of stock data, such as invalid data or calculation failures.
 * <p>
 * This exception provides the option to log the error using a logging utility (e.g., {@link AppLogger}).
 * </p>
 */
public class DataProcessingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new DataProcessingException with the specified detail message.
     *
     * @param message the detail message explaining the reason for the exception
     */
    public DataProcessingException(String message) {
        super(message);
        // Log the error message
        AppLogger.error("DataProcessingException: " + message);
    }

    /**
     * Constructs a new DataProcessingException with the specified detail message and cause.
     *
     * @param message the detail message explaining the reason for the exception
     * @param cause the cause of the exception (which can be retrieved later with {@link Throwable#getCause()})
     */
    public DataProcessingException(String message, Throwable cause) {
        super(message, cause);
        // Log both the error message and the stack trace of the cause
        AppLogger.error("DataProcessingException: " + message, cause);
    }
}
