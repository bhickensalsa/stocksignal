package com.stocksignal.indicators;

import com.stocksignal.data.StockData;

/**
 * The Indicator interface defines the contract for technical indicators in the stock market analysis.
 * Any class implementing this interface must provide a method to calculate the indicator's value based on stock data.
 */
public interface Indicator {

    /**
     * Calculates the value of the technical indicator based on the provided stock data.
     * 
     * @param data the stock data to be used for indicator calculation
     * @return the calculated indicator value
     */
    double calculate(StockData data);
}
