package com.stocksignal.indicators.technical;

import com.stocksignal.data.StockData;
import com.stocksignal.exceptions.DataProcessingException;
import com.stocksignal.indicators.Indicator;

import java.util.List;

/**
 * Calculates the Simple Moving Average (SMA) based on the closing prices of the stock data.
 */
public class SMA implements Indicator {

    private final int windowSize;

    /**
     * Constructs a new SMA calculator with the specified window size.
     *
     * @param windowSize the number of days over which to calculate the moving average
     * @throws IllegalArgumentException if windowSize is not positive
     */
    public SMA(int windowSize) {
        if (windowSize <= 0) {
            throw new IllegalArgumentException("Window size must be more than 0");
        }
        this.windowSize = windowSize;
    }

    /**
     * Calculates the SMA over the last `windowSize` entries in the provided stock data list.
     *
     * @param data the list of StockData objects (assumed to be sorted in ascending date order)
     * @return the calculated simple moving average
     * @throws DataProcessingException if the data list is null, too small, or calculation fails
     */
    @Override
    public double calculate(List<StockData> data) {
        if (data == null || data.isEmpty()) {
            throw new DataProcessingException("Stock data is null or empty.");
        }

        if (data.size() < windowSize) {
            throw new DataProcessingException("Not enough data to calculate SMA. Required: " + windowSize + ", Available: " + data.size());
        }

        try {
            return data.subList(data.size() - windowSize, data.size())
                    .stream()
                    .mapToDouble(StockData::getClose)
                    .average()
                    .orElseThrow(() -> new DataProcessingException("Unable to calculate SMA: no values in window."));
        } catch (Exception e) {
            throw new DataProcessingException("Error occurred while calculating SMA: " + e.getMessage(), e);
        }
    }
}
