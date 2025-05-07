package com.stocksignal.strategies;

import com.stocksignal.data.StockData;
import com.stocksignal.exceptions.DataProcessingException;
import com.stocksignal.indicators.technical.MACD;
import com.stocksignal.indicators.technical.SMA;

import java.util.List;

/**
 * A strategy that combines the Simple Moving Average (SMA) with the Moving Average Convergence Divergence (MACD)
 * indicator to generate trend-following buy and sell signals.
 *
 * <p>This strategy generates:</p>
 * <ul>
 *   <li>A buy signal when the stock price is above the SMA and the MACD line crosses above the MACD signal line.</li>
 *   <li>A sell signal when the stock price is below the SMA and the MACD line crosses below the MACD signal line.</li>
 * </ul>
 */
public class TrendFollowingStrategy implements Strategy {

    /** Historical stock data used for calculating indicators. */
    private List<StockData> historicalData;

    /** Period for the Simple Moving Average (SMA), typically 50 or 200. */
    private final int smaPeriod;

    /** MACD indicator used for calculating the MACD line and MACD signal line. */
    private final MACD macd;

    /** SMA indicator used for calculating the Simple Moving Average. */
    private final SMA sma;

    /** The most recent value of the Simple Moving Average (SMA). */
    private double currentSMA;

    /** The most recent value of the MACD line. */
    private double macdLine;

    /** The most recent value of the MACD signal line. */
    private double macdSignal;

    /**
     * Constructs a TrendFollowingStrategy with the specified historical data and SMA period.
     *
     * @param historicalData the list of historical stock data (typically closing prices) used to calculate indicators.
     * @param smaPeriod the period for the SMA calculation (e.g., 200 for long-term trend analysis).
     * @throws DataProcessingException if there is insufficient data to calculate the SMA.
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
     * Updates the historical stock data used for recalculating indicators.
     *
     * @param newHistoricalData the fresh historical stock data to use for recalculating indicators.
     */
    public void refreshHistoricalData(List<StockData> newHistoricalData) {
        this.historicalData = newHistoricalData;
    }

    /**
     * Updates the internal dataset and recalculates the indicators.
     *
     * <p>This method should be called whenever new market data is available to ensure the strategy uses the latest data for decision making.</p>
     *
     * @param newData the latest stock data entries.
     */
    @Override
    public void updateData(List<StockData> newData) {
        refreshHistoricalData(newData);
        calculateIndicators();
    }

    /**
     * Returns the number of historical data points required for indicator calculations.
     *
     * @return the number of historical data points required (equal to the SMA period).
     */
    @Override
    public int getLookbackPeriod() {
        return smaPeriod;
    }

    /**
     * Calculates the current SMA and MACD indicators using the most recent data.
     *
     * <p>The method calculates the SMA using the most recent data points for the specified SMA period,
     * and calculates the MACD line and signal line using the same data.</p>
     *
     * @throws DataProcessingException if there is an issue with the data processing or if there is insufficient data.
     */
    @Override
    public void calculateIndicators() {
        try {
            // Get the most recent data (the last 'smaPeriod' data points)
            List<StockData> recentData = historicalData.subList(historicalData.size() - smaPeriod, historicalData.size());

            // Calculate the current SMA
            this.currentSMA = sma.calculate(recentData);

            // Calculate the MACD and signal line values
            double[] macdValues = macd.calculate(recentData, true);
            this.macdLine = macdValues[0];  // MACD line
            this.macdSignal = macdValues[1];  // Signal line

        } catch (DataProcessingException e) {
            throw new DataProcessingException("Failed to calculate indicators: " + e.getMessage());
        }
    }

    /**
     * Determines if a buy signal is generated based on the current stock price, SMA, and MACD crossover.
     *
     * <p>A buy signal is generated when the stock price is above the SMA (indicating an uptrend) and the MACD line crosses above the MACD signal line (indicating bullish momentum).</p>
     *
     * @return {@code true} if a buy signal is generated, {@code false} otherwise.
     * @throws DataProcessingException if the historical data is empty or there is an issue with the data.
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
     * Determines if a sell signal is generated based on the current stock price, SMA, and MACD crossover.
     *
     * <p>A sell signal is generated when the stock price is below the SMA (indicating a downtrend) and the MACD line crosses below the MACD signal line (indicating bearish momentum).</p>
     *
     * @return {@code true} if a sell signal is generated, {@code false} otherwise.
     * @throws DataProcessingException if the historical data is empty or there is an issue with the data.
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
