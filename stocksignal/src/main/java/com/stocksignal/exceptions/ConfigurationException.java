package com.stocksignal.exceptions;

import com.stocksignal.utils.AppLogger;

public class ConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConfigurationException(String message) {
        super(message);
        // Optionally log or handle internal processing
        AppLogger.error("ConfigurationException: " + message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
        // Optionally log or handle internal processing
        AppLogger.error("ConfigurationException: " + message, cause);
    }
}
