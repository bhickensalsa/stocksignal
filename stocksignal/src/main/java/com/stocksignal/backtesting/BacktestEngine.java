package com.stocksignal.backtesting;

import com.stocksignal.strategies.Strategy;

/**
 * The BacktestEngine class is responsible for running and evaluating backtests 
 * using a given trading strategy.
 */
public class BacktestEngine {
    
    private Strategy strategy;

    /**
     * Constructs a BacktestEngine with the specified strategy.
     *
     * @param strategy the trading strategy to use in the backtest
     */
    public BacktestEngine(Strategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Runs the backtest using the current strategy.
     * Placeholder logic for backtest execution.
     */
    public void runBacktest() {
        System.out.println("Running backtest with strategy: " + strategy.getClass().getSimpleName());
    }

    /**
     * Evaluates the performance of the current strategy.
     * Placeholder logic for strategy evaluation.
     */
    public void evaluateStrategy() {
        System.out.println("Evaluating strategy performance...");
    }
}
