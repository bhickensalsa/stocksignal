package com.stocksignal.strategies;

import com.stocksignal.data.StockData;
import com.stocksignal.exceptions.ConfigurationException;
import com.stocksignal.exceptions.DataProcessingException;
import com.stocksignal.indicators.technical.MACD;
import com.stocksignal.indicators.technical.SMA;

import java.util.ArrayList;
import java.util.List;

/**
 * A trend-following trading strategy combining a Simple Moving Average (SMA)
 * for trend identification and the Moving Average Convergence Divergence (MACD)
 * indicator for momentum and signal generation.
 *
 * <p>This strategy generates:</p>
 * <ul>
 * <li>A buy signal when the stock price is above the SMA (confirming an uptrend)
 * AND the MACD line crosses above the MACD signal line (indicating bullish momentum).</li>
 * <li>A sell signal when the stock price is below the SMA (confirming a downtrend)
 * AND the MACD line crosses below the MACD signal line (indicating bearish momentum).</li>
 * </ul>
 *
 * <p>The strategy requires sufficient historical data to calculate both indicators accurately
 * for the current period and the immediately preceding period to detect crossovers.
 * It is designed to work with incremental data updates during backtesting.</p>
 */
public class TrendFollowingStrategy implements Strategy {

    /**
     * The historical stock data accumulated by the strategy.
     * Assumed to be in chronological order (oldest to newest).
     * This list will grow during backtesting as new data is added.
     */
    private List<StockData> historicalData;

    /** Period for the Simple Moving Average (SMA). */
    private final int smaPeriod;

    /** Fast period for the MACD's EMA calculation. */
    private final int macdFastPeriod;

    /** Slow period for the MACD's EMA calculation. */
    private final int macdSlowPeriod;

    /** Signal period for the MACD's signal line EMA calculation. */
    private final int macdSignalPeriod;

    /** MACD indicator instance. */
    private final MACD macd;

    /** SMA indicator instance. */
    private final SMA sma;

    /** The minimum number of historical data points required for this strategy to calculate indicators for the current *and* previous period. */
    private final int minimumRequiredData;

    /** The most recently calculated value of the Simple Moving Average (SMA). */
    private double currentSMA;

    /** The most recently calculated value of the MACD line. */
    private double currentMacdLine;

    /** The most recently calculated value of the MACD signal line. */
    private double currentMacdSignal;

    /** The value of the MACD line for the period immediately preceding the current. */
    private double previousMacdLine;

    /** The value of the MACD signal line for the period immediately preceding the current. */
    private double previousMacdSignal;

    /**
     * Constructs a TrendFollowingStrategy with specified parameters and initial historical data.
     *
     * <p>Initial data must be provided upon construction. The strategy requires enough
     * data points to calculate all indicators for both the current and previous periods
     * to enable crossover detection. During backtesting, this initial data should be
     * at least {@code getLookbackPeriod()} data points.</p>
     *
     * @param initialHistoricalData The initial list of stock data (must be in ascending chronological order).
     * Should be at least {@code getLookbackPeriod()} in size.
     * @param smaPeriod             The period for the SMA calculation (e.g., 200 for long-term trend analysis). Must be positive.
     * @param macdFastPeriod        The fast period for the MACD (e.g., 12). Must be positive and less than macdSlowPeriod.
     * @param macdSlowPeriod        The slow period for the MACD (e.g., 26). Must be positive and greater than macdFastPeriod.
     * @param macdSignalPeriod      The signal period for the MACD (e.g., 9). Must be positive.
     * @throws ConfigurationException If any of the periods are non-positive or if MACD fast period >= slow period.
     * @throws DataProcessingException If the initial historical data is null, empty, or contains insufficient data
     * to perform the initial indicator calculation for both current and previous periods.
     */
    public TrendFollowingStrategy(List<StockData> initialHistoricalData, int smaPeriod,
                                  int macdFastPeriod, int macdSlowPeriod, int macdSignalPeriod) {

        // --- Validate Indicator Periods ---
        if (smaPeriod <= 0) {
            throw new ConfigurationException("SMA period must be positive.");
        }
        if (macdFastPeriod <= 0) {
            throw new ConfigurationException("MACD fast period must be positive.");
        }
        if (macdSlowPeriod <= 0) {
            throw new ConfigurationException("MACD slow period must be positive.");
        }
        if (macdSignalPeriod <= 0) {
            throw new ConfigurationException("MACD signal period must be positive.");
        }
        if (macdFastPeriod >= macdSlowPeriod) {
            throw new ConfigurationException("MACD fast period (" + macdFastPeriod + ") must be less than MACD slow period (" + macdSlowPeriod + ").");
        }

        this.smaPeriod = smaPeriod;
        this.macdFastPeriod = macdFastPeriod;
        this.macdSlowPeriod = macdSlowPeriod;
        this.macdSignalPeriod = macdSignalPeriod;

        // --- Determine Minimum Required Data ---
        // To calculate the current MACD line and signal line, MACD needs data from index 0 up to the current index.
        // The first valid MACD value is at index macdSlowPeriod - 1.
        // The first valid MACD signal value is at index (macdSlowPeriod - 1) + macdSignalPeriod - 1 = macdSlowPeriod + macdSignalPeriod - 2.
        // So, to get the current MACD line and signal, we need at least macdSlowPeriod + macdSignalPeriod - 1 data points.
        // To calculate the previous MACD line and signal, we need data ending one step earlier, requiring macdSlowPeriod + macdSignalPeriod - 1 + 1 = macdSlowPeriod + macdSignalPeriod data points.
        // The SMA needs smaPeriod data points for the current value.
        // To calculate the previous SMA, we need smaPeriod + 1 data points.
        // We need enough data to calculate both the current and previous values for all indicators.
        // The minimum data required is the maximum of the data needed for the longest lookback *plus one* to get the previous value.
        // For SMA, we need smaPeriod for current, smaPeriod + 1 for previous. Max lookback requiring previous is smaPeriod + 1.
        // For MACD, the calculation of the signal line at index i depends on MACD line values up to index i, which themselves depend on EMAs.
        // To get the MACD line and signal at index `dataSize - 1`, MACD.calculate needs data from index 0 up to `dataSize - 1`. The internal EMAs and signal line calculation handle the warming up.
        // The MACD.calculate method provided seems to return the *last* calculated MACD line and signal line from the provided list.
        // So, if we provide a list of size N to MACD.calculate, it returns the MACD line/signal corresponding to the N-1 index of the *original* data stream from which this list was taken.
        // To get the MACD line/signal for the data point at index `dataSize - 1` of our historicalData, we need to pass historicalData (or its relevant tail) to MACD.calculate.
        // The MACD calculation itself requires a warming-up period. The first valid MACD line is at index macdSlowPeriod - 1. The first valid signal line is at index (macdSlowPeriod - 1) + macdSignalPeriod - 1.
        // So, to get a valid MACD line and signal line at the *end* of a list of size N, N must be at least macdSlowPeriod + macdSignalPeriod - 1.
        // To get the MACD line and signal line for the data point at index `dataSize - 1`, we need historicalData.subList(dataSize - (macdSlowPeriod + macdSignalPeriod - 1), dataSize). This list has size `macdSlowPeriod + macdSignalPeriod - 1`.
        // To get the MACD line and signal line for the data point at index `dataSize - 2`, we need historicalData.subList(dataSize - 1 - (macdSlowPeriod + macdSignalPeriod - 1), dataSize - 1). This list also has size `macdSlowPeriod + macdSignalPeriod - 1`.
        // Thus, to calculate both current (at size-1) and previous (at size-2) MACD line and signal, we need a total history of size `(size-1) - (size - 1 - (macdSlowPeriod + macdSignalPeriod - 1)) + 1`? No, this is getting complicated.
        // Let's simplify: MACD(fast, slow, signal) requires a warming period. The total minimum data points needed to get the first valid MACD signal is `slow + signal - 1`. To get the value at the end of a list of size N, N must be at least `slow + signal - 1`.
        // To get the MACD values for the *last* data point (index size-1), we need a list ending at size-1 with sufficient history. The MACD needs `slow + signal - 1` points to have a valid signal line at the end. So we need data from `size - (slow + signal - 1)` to `size - 1`. The number of points is `slow + signal - 1`.
        // To get the MACD values for the *second to last* data point (index size-2), we need a list ending at size-2 with sufficient history. We need data from `size - 1 - (slow + signal - 1)` to `size - 2`. The number of points is `slow + signal - 1`.
        // Therefore, to calculate both, we need data covering the period from `size - (slow + signal - 1) - 1` to `size - 1`. This is a range of `slow + signal` data points.
        // So, the minimum data required for MACD current and previous is `macdSlowPeriod + macdSignalPeriod`.
        // The minimum data required for SMA current is `smaPeriod`. The minimum data required for SMA previous is `smaPeriod + 1`.
        // The overall minimum data required for the strategy to calculate current and previous values of *all* indicators is the maximum of the individual requirements.
        // Minimum for SMA current + previous: smaPeriod + 1.
        // Minimum for MACD current + previous: macdSlowPeriod + macdSignalPeriod.
        // The minimum data needed in the `historicalData` list to calculate both current and previous values is `max(smaPeriod + 1, macdSlowPeriod + macdSignalPeriod)`.
        // However, the MACD.calculate method already calculates based on the *entire* provided list. So providing it with `macdSlowPeriod + macdSignalPeriod` points will give the MACD values for the last point. To get the previous, we need one more point in the history.
        // Let's reconsider the requirement based on how the indicators are used. We need the value of SMA at the current point (index size-1) and MACD line/signal at size-1 and size-2.
        // SMA at size-1 needs data from size-smaPeriod to size-1. Minimum size for SMA current is smaPeriod.
        // MACD at size-1 needs data from index 0 up to size-1. The first valid signal line is at index slow + signal - 2. So to have a valid signal at size-1, we need size-1 >= slow + signal - 2, meaning size >= slow + signal - 1.
        // MACD at size-2 needs data from index 0 up to size-2. To have a valid signal at size-2, we need size-2 >= slow + signal - 2, meaning size >= slow + signal.
        // Thus, to calculate MACD at both size-1 and size-2, the historicalData list needs to be at least `macdSlowPeriod + macdSignalPeriod` in size.
        // To calculate SMA at size-1, the historicalData list needs to be at least `smaPeriod` in size.
        // The overall minimum size of `historicalData` to perform the calculations in `calculateIndicators` is `max(smaPeriod, macdSlowPeriod + macdSignalPeriod)`. This allows calculation of current SMA and current/previous MACD line/signal *if* MACD.calculate can handle shorter lists and produce the last few values correctly.
        // Looking at the provided MACD.calculate, it takes a list and returns the *last* calculated MACD and Signal values based on that list.
        // So, to get the current MACD (at index size-1), we need to pass a sublist ending at size-1 to MACD.calculate. This sublist needs to be at least `macdSlowPeriod + macdSignalPeriod - 1` in size to have a valid signal at its end. So the sublist would be `historicalData.subList(dataSize - (macdSlowPeriod + macdSignalPeriod - 1), dataSize)`. The size of this sublist is `macdSlowPeriod + macdSignalPeriod - 1`.
        // To get the previous MACD (at index size-2), we need to pass a sublist ending at size-2. This sublist needs to be at least `macdSlowPeriod + macdSignalPeriod - 1` in size. So the sublist would be `historicalData.subList(dataSize - 1 - (macdSlowPeriod + macdSignalPeriod - 1), dataSize - 1)`. The size is `macdSlowPeriod + macdSignalPeriod - 1`.
        // For the previous sublist to be valid (start index >= 0), we need `dataSize - 1 - (macdSlowPeriod + macdSignalPeriod - 1) >= 0`.
        // `dataSize - 1 - macdSlowPeriod - macdSignalPeriod + 1 >= 0`
        // `dataSize - macdSlowPeriod - macdSignalPeriod >= 0`
        // `dataSize >= macdSlowPeriod + macdSignalPeriod`.
        // So, the historicalData needs to be at least `macdSlowPeriod + macdSignalPeriod` for the previous MACD calculation.
        // For the previous SMA calculation (if needed directly, though not explicitly stored currently), we'd need `smaPeriod + 1` points.
        // The minimum data needed to calculate current SMA and current/previous MACD line/signal is `max(smaPeriod, macdSlowPeriod + macdSignalPeriod)`. This seems correct for the size of `historicalData`.

        this.minimumRequiredData = Math.max(smaPeriod, macdSlowPeriod + macdSignalPeriod);


        // --- Initialize Indicators and Data ---
        // The initial data provided to the constructor should be at least the minimum required.
        if (initialHistoricalData == null || initialHistoricalData.size() < minimumRequiredData) {
            throw new DataProcessingException("Insufficient initial data (" + (initialHistoricalData == null ? 0 : initialHistoricalData.size())
                    + "). Required for initial calculation of indicators and their previous values: " + minimumRequiredData + " data points.");
        }

        // Make a defensive copy of the initial data
        this.historicalData = new ArrayList<>(initialHistoricalData);
        this.sma = new SMA(smaPeriod);
        this.macd = new MACD(macdFastPeriod, macdSlowPeriod, macdSignalPeriod);

        // Perform initial indicator calculation upon construction
        // This will calculate indicators based on the initial data provided.
        calculateIndicators();
    }


    /**
     * Updates the internal dataset with new data points and recalculates the indicators.
     * This method should be called whenever new market data becomes available,
     * typically with one new data point during a backtesting iteration.
     *
     * @param newData A list of new stock data entries to append. Should ideally be in chronological order.
     * @throws DataProcessingException if the calculation fails after adding new data.
     */
    @Override
    public void updateData(List<StockData> newData) {
        if (newData == null || newData.isEmpty()) {
            // No new data to process, simply return.
            return;
        }

        // Append the new data points to the historical data
        this.historicalData.addAll(newData);

        // Optional: Trim old data to prevent the historicalData list from growing indefinitely,
        // while ensuring enough history is kept for indicator calculations.
        // Keep enough data for the largest lookback required by the indicators (maximum of SMA period and MACD periods)
        // plus an additional buffer (e.g., 20 points) to allow for minor variations in indicator calculation needs
        // and to ensure we have enough history for "previous" values.
        int requiredHistorySize = Math.max(smaPeriod, macdSlowPeriod + macdSignalPeriod) + 20; // Added buffer
        if (this.historicalData.size() > requiredHistorySize) {
            this.historicalData = new ArrayList<>(this.historicalData.subList(
                    this.historicalData.size() - requiredHistorySize, this.historicalData.size()));
        }

        // Recalculate indicators based on the updated historical data.
        // This will use the latest data points added.
        calculateIndicators();
    }

    /**
     * Returns the minimum number of historical data points required for this strategy
     * to calculate all indicators reliably for both the current and previous periods.
     * This is determined by the indicator with the longest lookback requirement.
     *
     * @return The minimum number of data points required.
     */
    @Override
    public int getLookbackPeriod() {
        // The minimum required data is the maximum of the lookback periods for
        // SMA (smaPeriod) and MACD (macdSlowPeriod + macdSignalPeriod - 1)
        // plus one more data point to allow calculation of the *previous* indicator values.
        // MACD needs slow + signal - 1 points to have a valid signal line at the end of a list.
        // To get the previous MACD signal, we need one more point in the history.
        // So, min data for MACD current and previous is slow + signal.
        // Min data for SMA current is smaPeriod. Min data for SMA previous is smaPeriod + 1.
        // Overall minimum is max(smaPeriod + 1, macdSlowPeriod + macdSignalPeriod).
        // Let's stick to the simpler logic based on providing enough data for MACD.calculate
        // to work for current and previous points, which requires `macdSlowPeriod + macdSignalPeriod` points in the history.
        // And for SMA, we need `smaPeriod` for current, and implicitly `smaPeriod + 1` if we were to calculate previous SMA.
        // The minimum data needed in the historicalData list *ending at the current point*
        // to calculate indicators for the current point and the previous point is
        // the maximum of the data required for the longest indicator calculation ending at the current point (e.g., SMA needs smaPeriod points)
        // and the data required for the longest indicator calculation ending at the previous point (e.g., SMA needs smaPeriod points ending one step earlier).
        // For MACD, to get the signal at the end of a list of size N, N must be >= slow + signal - 1.
        // To get the signal at the second to last point (index N-2), the list must end at index N-2 and have size >= slow + signal - 1.
        // So the original list must have size N-1 >= slow + signal - 1, thus N >= slow + signal.
        // So, to calculate MACD at size-1 and size-2, we need a historical list of size at least slow + signal.
        // For SMA, we need smaPeriod for current, and smaPeriod + 1 for previous if we calculated it.
        // The overall minimum data points needed in the `historicalData` list is the maximum of the data required for the longest lookback *plus one* to calculate the previous value.
        // This is max(smaPeriod, macdSlowPeriod + macdSignalPeriod - 1) + 1 = max(smaPeriod + 1, macdSlowPeriod + macdSignalPeriod).
        // Let's set minimumRequiredData based on this more robust logic. Re-calculating minimumRequiredData in constructor.

        // The minimum data required to calculate MACD line/signal for the last *two* data points
        // is macdSlowPeriod + macdSignalPeriod.
        // The minimum data required to calculate SMA for the last *two* data points (if needed) is smaPeriod + 1.
        // So, the absolute minimum data points the strategy needs in its historicalData list at any point
        // to perform its calculations is the maximum of these two requirements.
        return Math.max(smaPeriod + 1, macdSlowPeriod + macdSignalPeriod);
    }


    /**
     * Calculates the current and previous SMA and MACD indicators using the historical data.
     * This method updates the internal state variables (`currentSMA`, `currentMacdLine`, etc.).
     * It uses the *end* of the accumulated historical data list.
     *
     * @throws DataProcessingException if there is an issue with the data processing or if there is insufficient data.
     */
    @Override
    public void calculateIndicators() {
        try {
            int dataSize = historicalData.size();

            // Data validation - ensure we have enough data after appending new points
            if (dataSize < minimumRequiredData) {
                // This state should ideally not be reached if updateData is called correctly
                // with enough initial data and subsequent single data points.
                throw new DataProcessingException("Insufficient data (" + dataSize + ") to calculate indicators. Required: " + minimumRequiredData);
            }

            // --- Calculate Current SMA (ending at dataSize - 1) ---
            // SMA needs the last 'smaPeriod' data points. Ensure we have at least smaPeriod points.
            if (dataSize < smaPeriod) {
                 throw new DataProcessingException("Insufficient data (" + dataSize + ") for SMA calculation. Required: " + smaPeriod);
            }
            List<StockData> currentDataForSma = historicalData.subList(dataSize - smaPeriod, dataSize);
            this.currentSMA = sma.calculate(currentDataForSma);

            // --- Calculate MACD for the Current Period (ending at dataSize - 1) ---
            // MACD.calculate needs a list ending at dataSize - 1.
            // The size of this list needs to be at least macdSlowPeriod + macdSignalPeriod - 1
            // to have a valid signal line at its end.
            int macdRequiredSize = macdSlowPeriod + macdSignalPeriod - 1;
            if (dataSize < macdRequiredSize) {
                 throw new DataProcessingException("Insufficient data (" + dataSize + ") for current MACD calculation. Required: " + macdRequiredSize);
            }
            List<StockData> currentDataForMacd = historicalData.subList(dataSize - macdRequiredSize, dataSize);
            double[] currentMacdValues = macd.calculate(currentDataForMacd, true);
            this.currentMacdLine = currentMacdValues[0];
            this.currentMacdSignal = currentMacdValues[1];

            // --- Calculate MACD for the Previous Period (ending at dataSize - 2) ---
            // We need a data window ending one step earlier (at index dataSize - 2).
            // This sublist needs to be at least macdSlowPeriod + macdSignalPeriod - 1 in size.
            // So, the list goes from (dataSize - 2) - (macdRequiredSize - 1) to dataSize - 2.
            // Start index: dataSize - 2 - macdRequiredSize + 1 = dataSize - macdRequiredSize - 1.
            // Ensure start index >= 0. dataSize - macdRequiredSize - 1 >= 0 => dataSize >= macdRequiredSize + 1.
            // macdRequiredSize + 1 = macdSlowPeriod + macdSignalPeriod - 1 + 1 = macdSlowPeriod + macdSignalPeriod.
            // So, historicalData needs to be at least macdSlowPeriod + macdSignalPeriod in size for previous MACD.
             if (dataSize < macdSlowPeriod + macdSignalPeriod) {
                 throw new DataProcessingException("Insufficient data (" + dataSize + ") for previous MACD calculation. Required: " + (macdSlowPeriod + macdSignalPeriod));
            }
             List<StockData> previousDataForMacd = historicalData.subList(dataSize - (macdSlowPeriod + macdSignalPeriod), dataSize - 1);
            double[] previousMacdValues = macd.calculate(previousDataForMacd, true);
            this.previousMacdLine = previousMacdValues[0];
            this.previousMacdSignal = previousMacdValues[1];

        } catch (DataProcessingException e) {
            // Re-throw DataProcessingException with added context
            throw new DataProcessingException("Error occurred during indicator calculation: " + e.getMessage(), e);
        } catch (IndexOutOfBoundsException e) {
            // Catch potential index errors from subList. This indicates an issue with
            // the data size checks or sublist logic.
            throw new DataProcessingException("Index out of bounds while calculating indicators. Data size: " + historicalData.size() +
                    ", SMA Period: " + smaPeriod + ", MACD Periods: (" + macdFastPeriod + ", " + macdSlowPeriod + ", " + macdSignalPeriod + ")" +
                    ", Required: " + minimumRequiredData, e);
        } catch (Exception e) {
            // Catch any other unexpected exceptions
            throw new DataProcessingException("An unexpected error occurred during indicator calculation: " + e.getMessage(), e);
        }
    }

    /**
     * Determines if a buy signal is generated based on the current conditions.
     *
     * <p>A buy signal is generated when the latest closing price is above the SMA
     * AND a bullish MACD crossover occurred in the most recent period
     * (i.e., previous MACD line was <= previous signal line, and current MACD line is > current signal line).</p>
     *
     * <p>Requires that {@link #calculateIndicators()} has been called to update indicator values
     * based on the latest data. This method should only be called when there is sufficient data
     * in the strategy's historical data list (i.e., at least {@code getLookbackPeriod()} points).</p>
     *
     * @return {@code true} if a buy signal is generated, {@code false} otherwise.
     * @throws DataProcessingException if there is insufficient historical data to determine the signal.
     */
    @Override
    public boolean shouldBuy() {
        // Ensure we have enough data to make a decision.
        if (historicalData == null || historicalData.size() < minimumRequiredData) {
            // This state indicates that updateData was not called with sufficient initial data
            // or the backtesting engine did not provide data correctly.
             throw new DataProcessingException("Insufficient historical data (" + (historicalData == null ? 0 : historicalData.size())
                                                + ") to determine buy signal. Required: " + minimumRequiredData);
        }

        // Get the most recent stock data
        StockData latestStockData = historicalData.get(historicalData.size() - 1);

        // Check Buy Conditions:
        // 1. Price is above the SMA (Trend filter)
        boolean priceAboveSMA = latestStockData.getClose() > currentSMA;

        // 2. Bullish MACD Crossover occurred in the most recent period (Momentum/Signal)
        boolean macdBullishCrossover = previousMacdLine <= previousMacdSignal && currentMacdLine > currentMacdSignal;

        return priceAboveSMA && macdBullishCrossover;
    }

    /**
     * Determines if a sell signal is generated based on the current conditions.
     *
     * <p>A sell signal is generated when the latest closing price is below the SMA
     * AND a bearish MACD crossover occurred in the most recent period
     * (i.e., previous MACD line was >= previous signal line, and current MACD line is < current signal line).</p>
     *
     * <p>Requires that {@link #calculateIndicators()} has been called to update indicator values
     * based on the latest data. This method should only be called when there is sufficient data
     * in the strategy's historical data list (i.e., at least {@code getLookbackPeriod()} points).</p>
     *
     * @return {@code true} if a sell signal is generated, {@code false} otherwise.
     * @throws DataProcessingException if there is insufficient historical data to determine the signal.
     */
    @Override
    public boolean shouldSell() {
        // Ensure we have enough data to make a decision.
        if (historicalData == null || historicalData.size() < minimumRequiredData) {
             // This state indicates that updateData was not called with sufficient initial data
            // or the backtesting engine did not provide data correctly.
             throw new DataProcessingException("Insufficient historical data (" + (historicalData == null ? 0 : historicalData.size())
                                                + ") to determine sell signal. Required: " + minimumRequiredData);
        }

        // Get the most recent stock data
        StockData latestStockData = historicalData.get(historicalData.size() - 1);

        // Check Sell Conditions:
        // 1. Price is below the SMA (Trend filter)
        boolean priceBelowSMA = latestStockData.getClose() < currentSMA;

        // 2. Bearish MACD Crossover occurred in the most recent period (Momentum/Signal)
        boolean macdBearishCrossover = previousMacdLine >= previousMacdSignal && currentMacdLine < currentMacdSignal;

        return priceBelowSMA && macdBearishCrossover;
    }
}