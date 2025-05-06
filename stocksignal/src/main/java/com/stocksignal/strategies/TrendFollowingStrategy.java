package com.stocksignal.strategies;

import com.stocksignal.indicators.technical.MovingAverage;
import com.stocksignal.indicators.technical.MACD;

/**
 * The TrendFollowingStrategy class implements the Strategy interface and represents a 
 * trend-following trading strategy that uses technical indicators such as Moving Average 
 * and MACD to decide when to buy or sell a stock.
 */
public class TrendFollowingStrategy implements Strategy {
    private MovingAverage movingAverage;
    private MACD macd;

    /**
     * Constructs a TrendFollowingStrategy instance and initializes the Moving Average and MACD indicators.
     */
    public TrendFollowingStrategy() {
        movingAverage = new MovingAverage();
        macd = new MACD();
    }

    /**
     * Determines whether the strategy signals a buy opportunity based on the 
     * Moving Average and MACD indicators.
     * 
     * @return true if both Moving Average and MACD indicate a buy signal, false otherwise
     */
    public boolean shouldBuy() {
        // Example logic for buying signal
        return movingAverage.calculate(null) > 0 && macd.calculate(null) > 0;
    }

    /**
     * Determines whether the strategy signals a sell opportunity based on the 
     * Moving Average and MACD indicators.
     * 
     * @return true if both Moving Average and MACD indicate a sell signal, false otherwise
     */
    public boolean shouldSell() {
        // Example logic for selling signal
        return movingAverage.calculate(null) < 0 && macd.calculate(null) < 0;
    }
}
