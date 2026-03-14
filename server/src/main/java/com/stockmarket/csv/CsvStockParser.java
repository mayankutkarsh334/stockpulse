package com.stockmarket.csv;

import com.stockmarket.analysis.MetricSnapshot;
import com.stockmarket.model.enums.Exchange;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Slf4j
public class CsvStockParser {

    public record ParsedCsvStock(String companyName, String sector, MetricSnapshot snapshot) {}

    // Maps normalised CSV header → canonical metric key used when building the snapshot
    private static final Map<String, String> HEADER_TO_KEY = new LinkedHashMap<>();

    // Keys that represent metadata, not scoreable metrics
    private static final Set<String> NON_METRIC_KEYS = Set.of("NAME", "TICKER", "SECTOR", "SKIP");

    static {
        HEADER_TO_KEY.put("name",                              "NAME");
        HEADER_TO_KEY.put("ticker",                            "TICKER");
        HEADER_TO_KEY.put("sub-sector",                        "SECTOR");
        HEADER_TO_KEY.put("market cap",                        "MARKET_CAP");
        HEADER_TO_KEY.put("close price",                       "CLOSE_PRICE");
        HEADER_TO_KEY.put("pe ratio",                          "PE");
        HEADER_TO_KEY.put("1m return",                         "ONE_MONTH_RETURN");
        HEADER_TO_KEY.put("1d return",                         "SKIP");
        HEADER_TO_KEY.put("return on equity",                  "ROE");
        HEADER_TO_KEY.put("pb ratio",                          "PB_RATIO");
        HEADER_TO_KEY.put("pledged promoter holdings",         "PLEDGED_PROMOTER_HOLDINGS");
        HEADER_TO_KEY.put("debt to equity",                    "DEBT_TO_EQUITY");
        HEADER_TO_KEY.put("roce",                              "ROCE");
        HEADER_TO_KEY.put("1y forward eps growth",             "EPS_GROWTH");
        HEADER_TO_KEY.put("5y historical revenue growth",      "REVENUE_GROWTH");
        HEADER_TO_KEY.put("1y return vs nifty",                "RETURN_VS_NIFTY");
        HEADER_TO_KEY.put("rsi - 14d",                         "RSI_14");
        HEADER_TO_KEY.put("forward pe ratio",                  "FORWARD_PE");
        HEADER_TO_KEY.put("your filter 1",                     "SKIP");
        HEADER_TO_KEY.put("6m return",                         "SIX_MONTH_RETURN");
        HEADER_TO_KEY.put("1y return",                         "ONE_YEAR_RETURN");
        HEADER_TO_KEY.put("% away from 52w high",              "PCT_AWAY_52W_HIGH");
        HEADER_TO_KEY.put("50d ema",                           "SMA_50");
    }

    /**
     * Returns the set of scoreable metric keys present in the CSV header.
     * Excludes NAME, TICKER, SECTOR, and SKIP entries.
     */
    public Set<String> parseAvailableMetricKeys(String csvContent) {
        String[] lines = csvContent.split("\r?\n");
        if (lines.length == 0) return Collections.emptySet();
        Set<String> keys = new LinkedHashSet<>();
        for (String raw : parseLine(lines[0])) {
            String normalised = normaliseHeader(raw);
            String key = HEADER_TO_KEY.get(normalised);
            if (key != null && !NON_METRIC_KEYS.contains(key)) {
                keys.add(key);
            }
        }
        return keys;
    }

    /**
     * Parse raw CSV text into a list of stock snapshots.
     * Handles RFC 4180 quoted fields and the en-dash in "RSI – 14D".
     */
    public List<ParsedCsvStock> parse(String csvContent, Exchange exchange) {
        List<ParsedCsvStock> result = new ArrayList<>();
        String[] lines = csvContent.split("\r?\n");
        if (lines.length < 2) return result;

        // Parse header
        String[] rawHeaders = parseLine(lines[0]);
        Map<Integer, String> colKeyMap = new LinkedHashMap<>();
        for (int i = 0; i < rawHeaders.length; i++) {
            String normalised = normaliseHeader(rawHeaders[i]);
            String key = HEADER_TO_KEY.getOrDefault(normalised, null);
            if (key != null && !key.equals("SKIP")) {
                colKeyMap.put(i, key);
            }
        }

        // Parse data rows
        for (int r = 1; r < lines.length; r++) {
            String line = lines[r].trim();
            if (line.isBlank()) continue;
            String[] cols = parseLine(line);

            Map<String, String> row = new HashMap<>();
            for (Map.Entry<Integer, String> e : colKeyMap.entrySet()) {
                int idx = e.getKey();
                if (idx < cols.length) row.put(e.getValue(), cols[idx].trim());
            }

            String ticker = row.getOrDefault("TICKER", "UNKNOWN_" + r);
            String name   = row.getOrDefault("NAME", ticker);
            String sector = row.getOrDefault("SECTOR", null);

            MetricSnapshot snapshot = new MetricSnapshot(
                    ticker, exchange,
                    parseDouble(row.get("PE")),
                    null,                                          // eps
                    parseDouble(row.get("EPS_GROWTH")),
                    parseDouble(row.get("REVENUE_GROWTH")),
                    parseDouble(row.get("MARKET_CAP")),
                    null,                                          // dividendYield
                    parseDouble(row.get("DEBT_TO_EQUITY")),
                    null,                                          // currentRatio
                    null,                                          // roa
                    null,                                          // grossMargin
                    null,                                          // assetTurnover
                    null,                                          // retainedEarnings
                    null,                                          // ebit
                    null,                                          // workingCapital
                    null,                                          // totalAssets
                    null,                                          // totalLiabilities
                    null,                                          // operatingCashFlow
                    null,                                          // sharesOutstanding
                    sector,
                    null,                                          // industry
                    parseDouble(row.get("RSI_14")),
                    null,                                          // macdValue
                    null,                                          // macdSignal
                    null,                                          // macdHistogram
                    null,                                          // sma20
                    parseDouble(row.get("SMA_50")),
                    null,                                          // sma200
                    null,                                          // high52w
                    null,                                          // low52w
                    parseDouble(row.get("CLOSE_PRICE")),           // currentPrice
                    null,                                          // volume
                    null,                                          // avgVolume
                    // CSV-specific
                    name,
                    parseDouble(row.get("ROE")),
                    parseDouble(row.get("ROCE")),
                    parseDouble(row.get("PB_RATIO")),
                    parseDouble(row.get("FORWARD_PE")),
                    parseDouble(row.get("PLEDGED_PROMOTER_HOLDINGS")),
                    parseDouble(row.get("ONE_MONTH_RETURN")),
                    parseDouble(row.get("SIX_MONTH_RETURN")),
                    parseDouble(row.get("ONE_YEAR_RETURN")),
                    parseDouble(row.get("RETURN_VS_NIFTY")),
                    parseDouble(row.get("PCT_AWAY_52W_HIGH")),
                    parseDouble(row.get("CLOSE_PRICE"))
            );

            result.add(new ParsedCsvStock(name, sector, snapshot));
            log.debug("Parsed CSV row: {} ({})", name, ticker);
        }

        log.info("Parsed {} stocks from CSV", result.size());
        return result;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Normalise header: trim, replace en/em-dashes with hyphen, collapse spaces, lowercase. */
    private String normaliseHeader(String raw) {
        return raw.trim()
                .replace('\u2013', '-')   // en-dash
                .replace('\u2014', '-')   // em-dash
                .replaceAll("\\s+", " ")
                .toLowerCase();
    }

    /** RFC 4180-compliant single-line CSV field parser. */
    String[] parseLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        fields.add(cur.toString());
        return fields.toArray(new String[0]);
    }

    private Double parseDouble(String val) {
        if (val == null || val.isBlank() || val.equalsIgnoreCase("N/A") || val.equalsIgnoreCase("null")) {
            return null;
        }
        try {
            return Double.parseDouble(val.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
