package com.stocksignal.indicators.technical;

import com.stocksignal.data.StockData;
import com.stocksignal.indicators.Indicator;

import java.util.List;

public class SMA implements Indicator {

    private final int windowSize;

    public SMA(int windowSize) {
        if (windowSize <= 0) {
            throw new IllegalArgumentException("Window size must be more than 0");
        }
        this.windowSize = windowSize;
    }

    /**
     * Calculates the simple moving average (SMA) over the last `windowSize` closing prices.
     * Assumes data is preprocessed: cleaned, sorted, and non-empty.
     */
    public double calculate(List<StockData> preprocessedData) {
        if (preprocessedData.size() < windowSize) {
            throw new IllegalArgumentException("Not enough data points to calculate SMA.");
        }

        int start = preprocessedData.size() - windowSize;
        return preprocessedData.subList(start, preprocessedData.size())
                               .stream()
                               .mapToDouble(StockData::getClose)
                               .average()
                               .orElseThrow(() -> new IllegalStateException("Unexpected error in SMA calculation"));
    }
}
