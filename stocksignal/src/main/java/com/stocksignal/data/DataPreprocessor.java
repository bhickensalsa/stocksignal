package com.stocksignal.data;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Responsible for preparing raw stock data for analysis.
 */
public class DataPreprocessor {

    /**
     * Cleans and transforms raw stock data.
     * 
     * @param rawData List of raw StockData
     * @return List of cleaned and sorted StockData
     */
    public List<StockData> preprocess(List<StockData> rawData) {
        return rawData.stream()
            .filter(d -> d.getClose() > 0 && d.getVolume() > 0) // Remove bad data
            .sorted((d1, d2) -> d1.getDate().compareTo(d2.getDate())) // Sort by date
            .collect(Collectors.toList());
    }
}
