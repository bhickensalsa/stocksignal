package com.stocksignal.strategies;

/**
 * The Strategy interface defines the contract for trading strategies.
 * Any class implementing this interface must provide the logic for determining when to buy or sell a stock.
 */
public interface Strategy {

    /**
     * Determines whether the strategy signals a buy opportunity.
     * 
     * @return true if the strategy signals a buy, false otherwise
     */
    boolean shouldBuy();

    /**
     * Determines whether the strategy signals a sell opportunity.
     * 
     * @return true if the strategy signals a sell, false otherwise
     */
    boolean shouldSell();
}
