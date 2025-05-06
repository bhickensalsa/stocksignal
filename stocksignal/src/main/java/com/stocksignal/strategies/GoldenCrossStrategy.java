package com.stocksignal.strategies;

import com.stocksignal.data.StockData;
import com.stocksignal.exceptions.ConfigurationException;
import com.stocksignal.exceptions.DataProcessingException;
import com.stocksignal.indicators.technical.SMA;

import java.util.List;

/**
 * A strategy that uses the Golden Cross signal to determine buy and sell opportunities.
 * A Golden Cross occurs when a short-term moving average crosses above a long-term moving average.
 * A Death Cross (used for selling) is the opposite.
 */
public class GoldenCrossStrategy implements Strategy {

    private final List<StockData> historicalData;
    private final int shortPeriod;
    private final int longPeriod;

    // Field variables to store calculated indicator values
    private double currentShortSMA;
    private double currentLongSMA;
    private double previousShortSMA;
    private double previousLongSMA;

    /**
     * Constructs a GoldenCrossStrategy.
     *
     * @param historicalData the list of historical stock data (must be sorted by date ascending)
     * @param shortPeriod    the short-term SMA period (e.g., 50)
     * @param longPeriod     the long-term SMA period (e.g., 200)
     */
    public GoldenCrossStrategy(List<StockData> historicalData, int shortPeriod, int longPeriod) {
        if (shortPeriod >= longPeriod) {
            throw new ConfigurationException("Short period must be less than long period.");
        }
        if (historicalData == null || historicalData.size() < longPeriod + 1) {
            throw new DataProcessingException("Not enough data to evaluate strategy.");
        }
        this.historicalData = historicalData;
        this.shortPeriod = shortPeriod;
        this.longPeriod = longPeriod;
    }

    @Override
    public void calculateIndicators() {
        try {
            SMA shortSMA = new SMA(shortPeriod);
            SMA longSMA = new SMA(longPeriod);

            // Use the most recent data for current SMA calculations
            List<StockData> recentData = historicalData.subList(historicalData.size() - longPeriod, historicalData.size());
            currentShortSMA = shortSMA.calculate(recentData);
            currentLongSMA = longSMA.calculate(recentData);

            // Use the previous data for previous SMA calculations
            List<StockData> previousData = historicalData.subList(historicalData.size() - longPeriod - 1, historicalData.size() - 1);
            previousShortSMA = shortSMA.calculate(previousData);
            previousLongSMA = longSMA.calculate(previousData);
        } catch (DataProcessingException e) {
            throw new DataProcessingException("Error calculating indicators: " + e.getMessage());
        }
    }

    @Override
    public boolean shouldBuy() {
        // The latest data (last element in historicalData) is used for decision
        calculateIndicators();
        return previousShortSMA <= previousLongSMA && currentShortSMA > currentLongSMA;
    }

    @Override
    public boolean shouldSell() {
        // The latest data (last element in historicalData) is used for decision
        calculateIndicators();
        return previousShortSMA >= previousLongSMA && currentShortSMA < currentLongSMA;
    }
}
