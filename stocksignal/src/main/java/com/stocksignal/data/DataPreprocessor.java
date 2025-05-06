package com.stocksignal.data;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.stocksignal.exceptions.DataProcessingException;

/**
 * Responsible for preparing raw stock data for analysis.
 */
public class DataPreprocessor {

    /**
     * Cleans and transforms raw stock data.
     *
     * Filters out invalid entries and sorts data by date in ascending order.
     *
     * @param rawData List of raw StockData
     * @return List of cleaned and sorted StockData
     * @throws DataProcessingException if data is null, empty, or all entries are filtered out
     */
    public List<StockData> preprocess(List<StockData> rawData) {
        if (rawData == null || rawData.isEmpty()) {
            throw new DataProcessingException("Stock data list is null or empty.");
        }

        List<StockData> preProcessedData = rawData.stream()
            .filter(Objects::nonNull)
            .filter(d -> d.getClose() > 0 && d.getVolume() > 0)
            .sorted((d1, d2) -> d1.getDate().compareTo(d2.getDate()))
            .collect(Collectors.toList());

        if (preProcessedData.isEmpty()) {
            throw new DataProcessingException("No valid stock data available after preprocessing.");
        }

        return preProcessedData;
    }
}
