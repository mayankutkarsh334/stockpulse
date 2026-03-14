package com.stockmarket.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmarket.model.cache.StockFundamentals;
import com.stockmarket.model.cache.StockQuote;
import com.stockmarket.model.cache.StockTechnicals;
import com.stockmarket.model.enums.Exchange;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Slf4j
public class AlphaVantageResponseParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static StockQuote parseQuote(String json, String symbol, Exchange exchange) throws IOException {
        JsonNode root = MAPPER.readTree(json);
        JsonNode gq = root.get("Global Quote");
        if (gq == null || gq.isEmpty()) {
            log.warn("Empty Global Quote response for {}", symbol);
            return StockQuote.builder().symbol(symbol).exchange(exchange).price(BigDecimal.ZERO)
                    .change(BigDecimal.ZERO).changePercent(BigDecimal.ZERO).timestamp(Instant.now()).build();
        }
        return StockQuote.builder()
                .symbol(symbol)
                .exchange(exchange)
                .price(parseBigDecimal(gq, "05. price"))
                .change(parseBigDecimal(gq, "09. change"))
                .changePercent(parseChangePercent(gq))
                .volume(parseLong(gq, "06. volume"))
                .open(parseBigDecimal(gq, "02. open"))
                .high(parseBigDecimal(gq, "03. high"))
                .low(parseBigDecimal(gq, "04. low"))
                .previousClose(parseBigDecimal(gq, "08. previous close"))
                .timestamp(Instant.now())
                .build();
    }

    public static StockFundamentals parseFundamentals(String json, String symbol, Exchange exchange) throws IOException {
        JsonNode root = MAPPER.readTree(json);
        if (root.has("Note") || root.has("Information") || root.isEmpty()) {
            log.warn("Alpha Vantage returned no fundamentals data for {} (rate-limit or unknown symbol)", symbol);
            return StockFundamentals.builder().symbol(symbol).exchange(exchange).fetchedAt(Instant.now()).build();
        }
        return StockFundamentals.builder()
                .symbol(symbol)
                .exchange(exchange)
                .sector(textOrNull(root, "Sector"))
                .industry(textOrNull(root, "Industry"))
                .description(textOrNull(root, "Description"))
                .pe(parseDouble(root, "PERatio"))
                .eps(parseDouble(root, "EPS"))
                .epsGrowth(parseDouble(root, "QuarterlyEarningsGrowthYOY"))
                .revenueGrowth(parseDouble(root, "QuarterlyRevenueGrowthYOY"))
                .marketCap(parseLong(root, "MarketCapitalization"))
                .dividendYield(parseDouble(root, "DividendYield"))
                .debtToEquity(parseDouble(root, "DebtToEquityRatio"))
                .currentRatio(parseDouble(root, "CurrentRatio"))
                .roa(parseDouble(root, "ReturnOnAssetsTTM"))
                .grossMargin(parseDouble(root, "GrossProfitTTM"))
                .sharesOutstanding(parseLong(root, "SharesOutstanding"))
                .high52w(parseDouble(root, "52WeekHigh"))
                .low52w(parseDouble(root, "52WeekLow"))
                .fetchedAt(Instant.now())
                .build();
    }

    public static StockTechnicals parseTechnicals(String rsiJson, String macdJson,
                                                   String sma20Json, String sma50Json, String sma200Json,
                                                   String symbol, Exchange exchange) throws IOException {
        Double rsi = parseLatestTechnicalValue(rsiJson, "Technical Analysis: RSI", "RSI");
        Double[] macd = parseLatestMacd(macdJson);
        Double sma20 = parseLatestTechnicalValue(sma20Json, "Technical Analysis: SMA", "SMA");
        Double sma50 = parseLatestTechnicalValue(sma50Json, "Technical Analysis: SMA", "SMA");
        Double sma200 = parseLatestTechnicalValue(sma200Json, "Technical Analysis: SMA", "SMA");

        return StockTechnicals.builder()
                .symbol(symbol)
                .exchange(exchange)
                .rsi14(rsi)
                .macdValue(macd[0])
                .macdSignal(macd[1])
                .macdHistogram(macd[2])
                .sma20(sma20)
                .sma50(sma50)
                .sma200(sma200)
                .fetchedAt(Instant.now())
                .build();
    }

    private static Double parseLatestTechnicalValue(String json, String sectionKey, String valueKey) {
        try {
            if (json == null) return null;
            JsonNode root = MAPPER.readTree(json);
            if (root.has("Note") || root.has("Information")) {
                log.warn("Alpha Vantage rate-limit response when parsing {}", valueKey);
                return null;
            }
            JsonNode section = root.get(sectionKey);
            if (section == null || !section.fields().hasNext()) return null;
            JsonNode latest = section.fields().next().getValue();
            String val = latest.path(valueKey).asText(null);
            return val != null ? Double.parseDouble(val) : null;
        } catch (Exception e) {
            log.warn("Failed to parse technical value {}: {}", valueKey, e.getMessage());
            return null;
        }
    }

    private static Double[] parseLatestMacd(String json) {
        Double[] result = {null, null, null};
        try {
            if (json == null) return result;
            JsonNode root = MAPPER.readTree(json);
            if (root.has("Note") || root.has("Information")) {
                log.warn("Alpha Vantage rate-limit response when parsing MACD");
                return result;
            }
            JsonNode section = root.get("Technical Analysis: MACD");
            if (section == null || !section.fields().hasNext()) return result;
            JsonNode latest = section.fields().next().getValue();
            result[0] = parseDoubleText(latest, "MACD");
            result[1] = parseDoubleText(latest, "MACD_Signal");
            result[2] = parseDoubleText(latest, "MACD_Hist");
        } catch (Exception e) {
            log.warn("Failed to parse MACD: {}", e.getMessage());
        }
        return result;
    }

    private static BigDecimal parseBigDecimal(JsonNode node, String field) {
        try {
            String val = node.path(field).asText("0").trim();
            return new BigDecimal(val.isEmpty() ? "0" : val);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private static BigDecimal parseChangePercent(JsonNode node) {
        try {
            String val = node.path("10. change percent").asText("0%").replace("%", "").trim();
            return new BigDecimal(val.isEmpty() ? "0" : val);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private static Long parseLong(JsonNode node, String field) {
        try {
            String val = node.path(field).asText("0").trim();
            return val.isEmpty() || val.equals("None") ? null : Long.parseLong(val);
        } catch (Exception e) {
            return null;
        }
    }

    private static Double parseDouble(JsonNode node, String field) {
        try {
            String val = node.path(field).asText("None").trim();
            return val.isEmpty() || val.equals("None") ? null : Double.parseDouble(val);
        } catch (Exception e) {
            return null;
        }
    }

    private static Double parseDoubleText(JsonNode node, String field) {
        try {
            String val = node.path(field).asText("None").trim();
            return val.isEmpty() || val.equals("None") ? null : Double.parseDouble(val);
        } catch (Exception e) {
            return null;
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        String val = node.path(field).asText(null);
        return (val == null || val.isBlank() || val.equals("None")) ? null : val;
    }
}
