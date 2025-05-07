package com.stocksignal.strategies;

/**
 * The Strategy interface defines the contract for all trading strategies.
 * 
 * Any class that implements this interface must provide the logic for determining buy and sell opportunities 
 * based on various stock indicators or conditions. It also requires implementing the logic to calculate and 
 * update necessary indicators for decision-making.
 * 
 * Typical strategies might include value investing, trend following, momentum, etc. Each strategy will define 
 * its own criteria for generating buy and sell signals.
 */
public interface Strategy {

    /**
     * Determines whether the strategy signals a buy opportunity.
     * 
     * This method will evaluate whether the current market conditions (based on the strategy) meet the criteria 
     * for purchasing a stock.
     * 
     * @return true if the strategy signals a buy, false otherwise. The strategy should return true when 
     *         it is appropriate to buy according to its logic (e.g., certain technical or fundamental conditions).
     */
    boolean shouldBuy();

    /**
     * Determines whether the strategy signals a sell opportunity.
     * 
     * This method will evaluate whether the current market conditions (based on the strategy) meet the criteria 
     * for selling a stock.
     * 
     * @return true if the strategy signals a sell, false otherwise. The strategy should return true when 
     *         it is appropriate to sell according to its logic (e.g., a stock has reached a target price or
     *         market conditions have changed).
     */
    boolean shouldSell();

    /**
     * Calculates and updates all necessary indicators required for the strategy's decision-making.
     * 
     * Each strategy may rely on different indicators, such as moving averages, MACD, PE ratio, or earnings growth.
     * This method is responsible for calculating these indicators and updating any necessary internal state 
     * for subsequent buy/sell decisions.
     */
    void calculateIndicators();
}
