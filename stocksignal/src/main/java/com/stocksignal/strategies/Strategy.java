package com.stocksignal.strategies;

import com.stocksignal.data.StockData;

import java.util.List;

/**
 * The Strategy interface defines the contract for all trading strategies used within the Stock Signal application.
 * <p>
 * Implementing classes must provide logic to determine buy and sell signals based on financial data and indicators.
 * They must also support dynamic data updates and indicator recalculation, which is essential for backtesting
 * and live trading evaluation.
 * </p>
 * 
 * Typical strategies include technical (e.g., trend following, momentum) and fundamental (e.g., value investing) approaches.
 */
public interface Strategy {

    /**
     * Determines whether the strategy recommends a buy action based on current market conditions
     * and internal indicator logic.
     *
     * @return {@code true} if a buy signal is generated; {@code false} otherwise.
     */
    boolean shouldBuy();

    /**
     * Determines whether the strategy recommends a sell action based on current market conditions
     * and internal indicator logic.
     *
     * @return {@code true} if a sell signal is generated; {@code false} otherwise.
     */
    boolean shouldSell();

    /**
     * Calculates or updates any internal indicators required for decision-making.
     * <p>
     * This method should be called whenever new market data is set or refreshed. It typically computes
     * technical or fundamental indicators such as SMA, MACD, RSI, or earnings growth.
     * </p>
     */
    void calculateIndicators();

    /**
     * Updates the internal historical data used by the strategy and triggers indicator recalculation.
     *
     * @param newData The list of updated {@link StockData} objects to be used for strategy evaluation.
     */
    void updateData(List<StockData> newData);

    /**
     * Returns the number of historical data points required by the strategy to produce reliable results.
     * <p>
     * This allows consumers of the strategy (such as backtest engines) to ensure that a sufficient amount
     * of historical data is available before invoking trading logic.
     * </p>
     *
     * @return the number of data points needed for indicator calculations and decision logic.
     */
    int getLookbackPeriod();
}
