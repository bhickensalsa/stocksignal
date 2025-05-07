package com.stocksignal.strategies;

import com.stocksignal.data.StockData;
import com.stocksignal.exceptions.DataProcessingException;
import com.stocksignal.indicators.technical.MACD;
import com.stocksignal.indicators.technical.SMA;

import java.util.List;

/**
 * The TrendFollowingStrategy combines a long-term Simple Moving Average (SMA) with the MACD (Moving Average Convergence Divergence)
 * to identify trend-based buy/sell signals.
 * 
 * This strategy works by:
 * - Generating a buy signal when the stock price is above the SMA and the MACD line crosses above the MACD signal line.
 * - Generating a sell signal when the stock price is below the SMA and the MACD line crosses below the MACD signal line.
 */
public class TrendFollowingStrategy implements Strategy {

    /** List of historical stock data used for calculating indicators. */
    private List<StockData> historicalData;

    /** Period for the Simple Moving Average (SMA). Typically, values like 50 or 200 are used. */
    private final int smaPeriod;

    /** MACD indicator used for calculating the MACD line and MACD signal line. */
    private final MACD macd;

    /** SMA indicator used for calculating the Simple Moving Average. */
    private final SMA sma;

    /** The current value of the Simple Moving Average (SMA) calculated for the most recent data. */
    private double currentSMA;

    /** The current MACD line value calculated for the most recent data. */
    private double macdLine;

    /** The current MACD signal line value calculated for the most recent data. */
    private double macdSignal;

    /**
     * Constructs a TrendFollowingStrategy with historical data and initializes the SMA and MACD indicators.
     * 
     * @param historicalData The list of historical stock data (typically closing prices) used to calculate indicators.
     * @param smaPeriod The period for the SMA calculation (e.g., 200 for a long-term trend).
     * @throws DataProcessingException If there is insufficient data for the SMA period.
     */
    public TrendFollowingStrategy(List<StockData> historicalData, int smaPeriod) {
        if (historicalData == null || historicalData.size() < smaPeriod) {
            throw new DataProcessingException("Insufficient data for TrendFollowingStrategy.");
        }
        this.historicalData = historicalData;
        this.smaPeriod = smaPeriod;
        this.sma = new SMA(smaPeriod);
        this.macd = new MACD(12, 26, 9);  // Typical MACD settings (fast=12, slow=26, signal=9)
    }

    /**
     * Refreshes the historical data with new stock data.
     * 
     * @param newHistoricalData The fresh historical data to use for recalculating indicators.
     */
    public void refreshHistoricalData(List<StockData> newHistoricalData) {
        this.historicalData = newHistoricalData;
    }

    /**
     * Precomputes the indicators (SMA and MACD) based on the most recent historical data.
     * This method calculates the current SMA and MACD values using the most recent 'smaPeriod' data points.
     * 
     * @throws DataProcessingException If there is an issue with the data processing or if the data is insufficient.
     */
    @Override
    public void calculateIndicators() {
        try {
            // Get the most recent data (the last 'smaPeriod' data points)
            List<StockData> recentData = historicalData.subList(historicalData.size() - smaPeriod, historicalData.size());

            // Calculate the current SMA using the recent data
            this.currentSMA = sma.calculate(recentData);

            // Calculate the MACD and signal line values using the recent data
            double[] macdValues = macd.calculate(recentData, true);
            this.macdLine = macdValues[0];  // MACD line
            this.macdSignal = macdValues[1];  // Signal line

        } catch (DataProcessingException e) {
            // Re-throw the exception with additional context
            throw new DataProcessingException("Failed to calculate indicators: " + e.getMessage());
        }
    }

    /**
     * Determines whether a buy signal is generated based on the current price, SMA, and MACD crossover.
     * 
     * A buy signal is generated when:
     * - The current stock price is above the calculated SMA, indicating an uptrend.
     * - The MACD line is above the MACD signal line, indicating bullish momentum.
     * 
     * @return true if the stock should be bought, false otherwise.
     * @throws DataProcessingException If the historical data is empty or there are issues with the data.
     */
    @Override
    public boolean shouldBuy() {
        // If historical data is empty, throw an exception
        if (historicalData.isEmpty()) {
            throw new DataProcessingException("Historical data is empty, cannot determine buy signal.");
        }

        // Get the most recent stock data (latest stock data in historicalData)
        StockData latestStockData = historicalData.get(historicalData.size() - 1);

        // A buy signal is generated when the stock price is above the SMA and MACD line is above the MACD signal line
        return latestStockData.getClose() > currentSMA && macdLine > macdSignal;
    }

    /**
     * Determines whether a sell signal is generated based on the current price, SMA, and MACD crossover.
     * 
     * A sell signal is generated when:
     * - The current stock price is below the calculated SMA, indicating a downtrend.
     * - The MACD line is below the MACD signal line, indicating bearish momentum.
     * 
     * @return true if the stock should be sold, false otherwise.
     * @throws DataProcessingException If the historical data is empty or there are issues with the data.
     */
    @Override
    public boolean shouldSell() {
        // If historical data is empty, throw an exception
        if (historicalData.isEmpty()) {
            throw new DataProcessingException("Historical data is empty, cannot determine sell signal.");
        }

        // Get the most recent stock data (latest stock data in historicalData)
        StockData latestStockData = historicalData.get(historicalData.size() - 1);

        // A sell signal is generated when the stock price is below the SMA and MACD line is below the MACD signal line
        return latestStockData.getClose() < currentSMA && macdLine < macdSignal;
    }
}
