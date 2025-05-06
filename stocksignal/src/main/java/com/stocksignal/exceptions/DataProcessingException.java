package com.stocksignal.exceptions;

import com.stocksignal.utils.AppLogger;

public class DataProcessingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DataProcessingException(String message) {
        super(message);
        // Optionally log or handle internal processing
        AppLogger.error("DataProcessingException: " + message);
    }

    public DataProcessingException(String message, Throwable cause) {
        super(message, cause);
        // Optionally log or handle internal processing
        AppLogger.error("DataProcessingException: " + message, cause);
    }
}
