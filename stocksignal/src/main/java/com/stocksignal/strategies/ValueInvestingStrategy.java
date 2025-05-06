package com.stocksignal.strategies;

import com.stocksignal.data.StockData;
import com.stocksignal.exceptions.ConfigurationException;
import com.stocksignal.exceptions.DataProcessingException;
import com.stocksignal.indicators.fundamental.EarningsGrowth;
import com.stocksignal.indicators.fundamental.PE_Ratio;
import com.stocksignal.utils.AppLogger;

public class ValueInvestingStrategy {

    private final double maxPERatio; // Max PE ratio threshold
    private final double minEarningsGrowth; // Min earnings growth threshold

    /**
     * Constructs a ValueInvestingStrategy with configurable PE ratio and earnings growth thresholds.
     * 
     * @param maxPERatio Maximum PE ratio to consider a stock as "cheap" (e.g., 20.0).
     * @param minEarningsGrowth Minimum earnings growth percentage to consider a stock as growing (e.g., 5.0).
     */
    public ValueInvestingStrategy(double maxPERatio, double minEarningsGrowth) {
        if (maxPERatio <= 0 || minEarningsGrowth <= 0) {
            throw new ConfigurationException("PE ratio and earnings growth must be positive values.");
        }        
        this.maxPERatio = maxPERatio;
        this.minEarningsGrowth = minEarningsGrowth;
    }

    /**
     * Decide whether to buy the stock based on its PE ratio and earnings growth.
     * 
     * @param stock The stock data.
     * @return true if the stock is a good buy according to the strategy.
     */
    public boolean shouldBuy(StockData stock) {
        if (stock == null) {
            throw new DataProcessingException("Stock data is null.");
        }

        // Validate earnings data
        if (stock.getCurrentEarningsPerShare() <= 0 || stock.getPreviousEarningsPerShare() <= 0) {
            throw new DataProcessingException("Invalid earnings data for stock {}: current EPS = {}, previous EPS = {}." 
            + stock.getSymbol() + stock.getCurrentEarningsPerShare() + stock.getPreviousEarningsPerShare());
        }

        PE_Ratio peRatio = new PE_Ratio(stock.getClose(), stock.getCurrentEarningsPerShare());
        EarningsGrowth earningsGrowth = new EarningsGrowth(stock.getCurrentEarningsPerShare(), stock.getPreviousEarningsPerShare());

        double calculatedPE = peRatio.calculate();
        double growth = earningsGrowth.calculateGrowth();

        // Buy conditions: low PE + strong earnings growth
        if (calculatedPE != -1 && calculatedPE < maxPERatio && growth >= minEarningsGrowth) {
            AppLogger.info("Buy signal: Stock {} | PE = {} | Earnings Growth = {}", stock.getSymbol(), calculatedPE, growth);
            return true;
        }

        return false;
    }

    /**
     * Decide whether to sell the stock based on its PE ratio and earnings growth.
     * 
     * @param stock The stock data.
     * @return true if the stock should be sold.
     */
    public boolean shouldSell(StockData stock) {
        if (stock == null) {
            throw new DataProcessingException("Stock data is null.");
        }

        // Validate earnings data
        if (stock.getCurrentEarningsPerShare() <= 0 || stock.getPreviousEarningsPerShare() <= 0) {
            if (stock.getCurrentEarningsPerShare() <= 0 || stock.getPreviousEarningsPerShare() <= 0) {
                throw new DataProcessingException("Invalid earnings data for stock {}: current EPS = {}, previous EPS = {}." 
                + stock.getSymbol() + stock.getCurrentEarningsPerShare() + stock.getPreviousEarningsPerShare());
            }
        }

        PE_Ratio peRatio = new PE_Ratio(stock.getClose(), stock.getCurrentEarningsPerShare());
        EarningsGrowth earningsGrowth = new EarningsGrowth(stock.getCurrentEarningsPerShare(), stock.getPreviousEarningsPerShare());

        double calculatedPE = peRatio.calculate();
        double growth = earningsGrowth.calculateGrowth();

        // Sell conditions: high PE + low or negative earnings growth
        if (calculatedPE >= maxPERatio || growth < 0) {
            AppLogger.info("Sell signal: Stock {} | PE = {} | Earnings Growth = {}", stock.getSymbol(), calculatedPE, growth);
            return true;
        }

        return false;
    }
}
