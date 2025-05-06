package com.stocksignal.indicators.technical;

import java.util.List;

import com.stocksignal.data.StockData;
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
     */
    @Override
    public double calculate(List<StockData> data) {
        if (data.size() < longPeriod) {
            throw new IllegalArgumentException("Not enough data to calculate MACD");
        }

        double emaShort = calculateEMA(data, shortPeriod);
        double emaLong = calculateEMA(data, longPeriod);
        return emaShort - emaLong;
    }

    /**
     * Helper method to calculate EMA.
     */
    private double calculateEMA(List<StockData> data, int period) {
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
