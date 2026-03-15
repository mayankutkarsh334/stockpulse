import time
import math
import logging

import yfinance as yf

logger = logging.getLogger(__name__)

_fund_cache: dict[str, tuple[float, list[dict]]] = {}
_FUND_TTL = 86400  # 24 hours

_INFO_FIELDS = {
    "trailingPE": "fund_pe_ratio",
    "priceToBook": "fund_pb_ratio",
    "returnOnEquity": "fund_roe",
    "returnOnAssets": "fund_roa",
    "debtToEquity": "fund_debt_to_equity",
    "profitMargins": "fund_profit_margin",
    "revenueGrowth": "fund_revenue_growth",
    "earningsGrowth": "fund_earnings_growth",
    "beta": "fund_beta",
    "currentRatio": "fund_current_ratio",
    "dividendYield": "fund_dividend_yield",
    "marketCap": "fund_market_cap_b",
}


def fetch_fundamental_features(ticker: str, dates: list[str]) -> list[dict]:
    """
    Fetch fundamental features from yfinance for the given ticker.
    Returns list[dict] with one constant entry per date (same values across all dates).
    Returns [] on any failure.
    """
    if not dates:
        return []

    now = time.time()
    if ticker in _fund_cache:
        ts, cached = _fund_cache[ticker]
        if now - ts < _FUND_TTL:
            # Re-expand for the (potentially different) dates list
            if cached:
                base = {k: v for k, v in cached[0].items() if k != "date"}
                return [{**base, "date": d} for d in dates]
            return []

    try:
        info = yf.Ticker(ticker).info or {}
        numeric: dict[str, float] = {}
        for src_key, feat_name in _INFO_FIELDS.items():
            val = info.get(src_key)
            if val is None:
                continue
            try:
                fval = float(val)
            except (TypeError, ValueError):
                continue
            if math.isnan(fval) or math.isinf(fval):
                continue
            # Normalize market cap to billions
            if feat_name == "fund_market_cap_b":
                fval = fval / 1e9
            numeric[feat_name] = fval

        if not numeric:
            _fund_cache[ticker] = (now, [])
            return []

        rows = [{**numeric, "date": d} for d in dates]
        _fund_cache[ticker] = (now, rows)
        return rows

    except Exception as e:
        logger.warning("fundamental_features: failed for %s: %s", ticker, e)
        return []
