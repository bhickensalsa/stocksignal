package com.stocksignal.indicators.fundamental;

import com.stocksignal.exceptions.ConfigurationException;
import com.stocksignal.exceptions.DataProcessingException;

/**
 * This class calculates the Price-to-Earnings (PE) Ratio.
 */
public class PE_Ratio {

    private final double currentPrice;
    private final double earningsPerShare;

    /**
     * Constructs a PE_Ratio calculator with current price and earnings per share.
     *
     * @param currentPrice     the current stock price
     * @param earningsPerShare the earnings per share
     */
    public PE_Ratio(double currentPrice, double earningsPerShare) {
        if (currentPrice <= 0) {
            throw new ConfigurationException("Current price must be greater than zero.");
        }
        if (earningsPerShare <= 0) {
            throw new ConfigurationException("Earnings per share must be greater than zero.");
        }

        this.currentPrice = currentPrice;
        this.earningsPerShare = earningsPerShare;
    }

    /**
     * Calculates the PE ratio.
     *
     * @return the calculated PE ratio
     * @throws DataProcessingException if earnings per share is zero or negative
     */
    public double calculate() {
        try {
            return currentPrice / earningsPerShare;
        } catch (ArithmeticException e) {
            throw new DataProcessingException("Failed to calculate PE Ratio due to arithmetic error.", e);
        }
    }
}
