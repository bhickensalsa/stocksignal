package com.stocksignal.indicators.technical;

import com.stocksignal.data.StockData;
import com.stocksignal.indicators.Indicator;

/**
 * The MovingAverage class implements the Indicator interface and represents the 
 * Simple Moving Average (SMA) technical indicator used in stock market analysis.
 */
public class MovingAverage implements Indicator {

    /**
     * Calculates the Simple Moving Average (SMA) based on the provided stock data.
     * This is a placeholder calculation that simulates SMA calculation.
     * 
     * @param data the stock data to be used for SMA calculation
     * @return the calculated Simple Moving Average (SMA) value
     */
    public double calculate(StockData data) {
        // Example logic for calculating Simple Moving Average (SMA)
        return data.getPrice() * 0.95;  // Placeholder calculation
    }
}
