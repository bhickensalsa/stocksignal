package com.stocksignal.strategies;

import com.stocksignal.data.StockData;
import com.stocksignal.exceptions.ConfigurationException;
import com.stocksignal.exceptions.DataProcessingException;
import com.stocksignal.indicators.fundamental.EarningsGrowth;
import com.stocksignal.indicators.fundamental.PE_Ratio;
import com.stocksignal.utils.AppLogger;

import java.util.List;

public class ValueInvestingStrategy implements Strategy {

    private final double maxPERatio;
    private final double minEarningsGrowth;

    private List<StockData> historicalData;
    private double calculatedPE;
    private double earningsGrowth;

    public ValueInvestingStrategy(double maxPERatio, double minEarningsGrowth, List<StockData> historicalData) {
        if (maxPERatio <= 0 || minEarningsGrowth <= 0) {
            throw new ConfigurationException("PE ratio and earnings growth must be positive values.");
        }
        this.maxPERatio = maxPERatio;
        this.minEarningsGrowth = minEarningsGrowth;
        this.historicalData = historicalData;
    }

    @Override
    public void calculateIndicators() {
        if (historicalData == null || historicalData.isEmpty()) {
            throw new DataProcessingException("Historical stock data must be set before calculating indicators.");
        }

        // Use the most recent stock data from the historical data
        StockData currentStock = historicalData.get(historicalData.size() - 1);

        double epsCurrent = currentStock.getCurrentEarningsPerShare();
        double epsPrevious = currentStock.getPreviousEarningsPerShare();

        if (epsCurrent <= 0 || epsPrevious <= 0) {
            throw new DataProcessingException(
                "Invalid earnings data for stock " + currentStock.getSymbol()
                + ": current EPS = " + epsCurrent + ", previous EPS = " + epsPrevious
            );
        }

        PE_Ratio pe = new PE_Ratio(currentStock.getClose(), epsCurrent);
        EarningsGrowth growth = new EarningsGrowth(epsCurrent, epsPrevious);

        this.calculatedPE = pe.calculate();
        this.earningsGrowth = growth.calculateGrowth();
    }

    @Override
    public boolean shouldBuy() {
        if (historicalData == null || historicalData.isEmpty()) {
            throw new DataProcessingException("Historical stock data is required to determine buy signal.");
        }

        // Use the most recent stock data from the historical data
        StockData currentStock = historicalData.get(historicalData.size() - 1);
        calculateIndicators();

        boolean buy = calculatedPE < maxPERatio && earningsGrowth >= minEarningsGrowth;
        if (buy) {
            AppLogger.info("Buy signal: Stock {} | PE = {} | Earnings Growth = {}",
                currentStock.getSymbol(), calculatedPE, earningsGrowth);
        }

        return buy;
    }

    @Override
    public boolean shouldSell() {
        if (historicalData == null || historicalData.isEmpty()) {
            throw new DataProcessingException("Historical stock data is required to determine sell signal.");
        }

        // Use the most recent stock data from the historical data
        StockData currentStock = historicalData.get(historicalData.size() - 1);
        calculateIndicators();

        boolean sell = calculatedPE >= maxPERatio || earningsGrowth < 0;
        if (sell) {
            AppLogger.info("Sell signal: Stock {} | PE = {} | Earnings Growth = {}",
                currentStock.getSymbol(), calculatedPE, earningsGrowth);
        }

        return sell;
    }
}
