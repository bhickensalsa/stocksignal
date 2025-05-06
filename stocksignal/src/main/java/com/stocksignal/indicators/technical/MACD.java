package com.stocksignal.indicators.technical;

import java.util.List;

import com.stocksignal.data.StockData;
import com.stocksignal.exceptions.DataProcessingException;
import com.stocksignal.indicators.Indicator;

/**
 * MACD (Moving Average Convergence Divergence) indicator implementation.
 * Calculates the latest MACD value using configurable short, long, and signal periods.
 */
public class MACD implements Indicator {
    private final int shortPeriod;
    private final int longPeriod;
    private final int signalPeriod;

    /**
     * Constructs a MACD indicator with the given periods.
     *
     * @param shortPeriod  the short-term EMA period (typically 12)
     * @param longPeriod   the long-term EMA period (typically 26)
     * @param signalPeriod the signal line EMA period (typically 9)
     */
    public MACD(int shortPeriod, int longPeriod, int signalPeriod) {
        if (shortPeriod <= 0 || longPeriod <= 0 || signalPeriod <= 0) {
            throw new IllegalArgumentException("Periods must be positive integers");
        }
        if (shortPeriod >= longPeriod) {
            throw new IllegalArgumentException("Short period must be less than long period");
        }
        this.shortPeriod = shortPeriod;
        this.longPeriod = longPeriod;
        this.signalPeriod = signalPeriod;
    }

    /**
     * Calculates the most recent MACD value (no signal/histogram).
     *
     * @param data the stock data (must be at least longPeriod in size)
     * @return the latest MACD value
     * @throws DataProcessingException if there is insufficient or null data
     */
    @Override
    public double calculate(List<StockData> data) {
        if (data == null || data.isEmpty()) {
            throw new DataProcessingException("Stock data is null or empty.");
        }

        if (data.size() < longPeriod) {
            throw new DataProcessingException("Not enough data to calculate MACD. Required: " + longPeriod + ", Available: " + data.size());
        }

        try {
            double emaShort = calculateEMA(data, shortPeriod);
            double emaLong = calculateEMA(data, longPeriod);
            return emaShort - emaLong;
        } catch (Exception e) {
            throw new DataProcessingException("Error calculating MACD: " + e.getMessage(), e);
        }
    }

    /**
     * Calculates the Exponential Moving Average (EMA) over a given period.
     *
     * @param data   the stock data list
     * @param period the EMA period
     * @return the calculated EMA
     * @throws DataProcessingException if period exceeds data size or other processing issues occur
     */
    private double calculateEMA(List<StockData> data, int period) {
        if (period > data.size()) {
            throw new DataProcessingException("Not enough data to calculate EMA for period: " + period);
        }

        int startIndex = data.size() - period;
        double k = 2.0 / (period + 1);
        double ema = data.get(startIndex).getClose();

        for (int i = startIndex + 1; i < data.size(); i++) {
            double price = data.get(i).getClose();
            ema = price * k + ema * (1 - k);
        }

        return ema;
    }

    public int getShortPeriod() {
        return shortPeriod;
    }

    public int getLongPeriod() {
        return longPeriod;
    }

    public int getSignalPeriod() {
        return signalPeriod;
    }
}
