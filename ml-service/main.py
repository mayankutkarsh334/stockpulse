import re
import time
from concurrent.futures import ThreadPoolExecutor
from typing import Optional

import numpy as np
import yfinance as yf
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

from models.arima_model import run_arima
from models.black_scholes import run_black_scholes
from models.fundamental_features import fetch_fundamental_features
from models.garch_model import run_garch
from models.lstm_model import run_lstm_full
from models.market_features import _BENCHMARK, fetch_market_features
from models.monte_carlo import run_simulation
from models.sentiment_features import fetch_sentiment_features
from models.transformer_model import run_transformer_full
from models.xgboost_model import run_xgboost_full

app = FastAPI(title="ML Forecast Service", version="1.0.0")

# ── Price cache: (ticker, period) → (timestamp, dates, closes np.ndarray) ──
_cache: dict[tuple[str, str], tuple[float, list[str], np.ndarray]] = {}

# ── Model cache: (ticker, period) → (timestamp, model_results dict) ─────────
_model_cache: dict[tuple[str, str], tuple[float, dict]] = {}

_CACHE_TTL_SECONDS = 3600  # 1 hour

EXCHANGE_SUFFIX = {
    "NSE": ".NS",
    "BSE": ".BO",
    "NYSE": "",
    "NASDAQ": "",
}


def _build_ticker(symbol: str, exchange: str) -> str:
    suffix = EXCHANGE_SUFFIX.get(exchange.upper(), "")
    return f"{symbol.upper()}{suffix}"


def _fetch_closes(ticker: str, period: str = "5y") -> tuple[list[str], np.ndarray]:
    """Returns (dates, closes) where dates are YYYY-MM-DD strings."""
    cache_key = (ticker, period)
    now = time.time()
    if cache_key in _cache:
        ts, dates, closes = _cache[cache_key]
        if now - ts < _CACHE_TTL_SECONDS:
            return dates, closes

    hist = yf.Ticker(ticker).history(period=period)
    closes_series = hist["Close"].dropna()
    dates = [d.strftime("%Y-%m-%d") for d in closes_series.index]
    closes = closes_series.values.flatten().astype(float)
    _cache[cache_key] = (now, dates, closes)
    return dates, closes


# ── Screener CSV columns that are not numeric / should be skipped ────────────
_SCREENER_SKIP = {"name", "ticker", "sub-sector", "sub sector", "subsector"}


def _clean_col_name(col: str) -> str:
    """'PE Ratio' → 'sc_pe_ratio', '1M Return' → 'sc_return_1m', 'RSI – 14D' → 'sc_rsi_14d'"""
    col = col.strip()
    # Move leading digits to suffix: '1M Return' → 'Return_1M'
    col = re.sub(r'^(\d+\w*)\s+(.+)', lambda m: m.group(2) + "_" + m.group(1), col)
    col = re.sub(r'[^a-zA-Z0-9]+', '_', col).strip('_').lower()
    return f"sc_{col}"


def _preprocess_extra_indicators(
    extra_indicators: list[dict],
    symbol: str,
    dates: list[str],
) -> Optional[list[dict]]:
    """
    Detects screener CSV format (no 'date' column, has 'Ticker' column).
    Finds the row matching `symbol`, extracts numeric features, and expands
    to a time-series list (one constant entry per date) so the existing
    XGBoost pipeline can consume it without any other changes.

    Returns the original list unchanged if it's already time-series format,
    or None if screener format but no matching row found.
    """
    if not extra_indicators:
        return extra_indicators

    first_row = extra_indicators[0]
    has_date = any(k.lower() == "date" for k in first_row.keys())
    if has_date:
        return extra_indicators  # already time-series — pass through unchanged

    # ── Screener format ───────────────────────────────────────────────────────
    # Find the row whose Ticker matches the symbol (strip .NS / .BO suffixes)
    symbol_clean = symbol.upper().replace(".NS", "").replace(".BO", "")
    matched_row: Optional[dict] = None
    for row in extra_indicators:
        ticker_raw = str(row.get("Ticker", row.get("ticker", ""))).strip().upper()
        ticker_clean = ticker_raw.replace(".NS", "").replace(".BO", "")
        if ticker_clean == symbol_clean:
            matched_row = row
            break

    if matched_row is None:
        return None  # stock not found in screener — skip extra indicators

    # Extract numeric columns with clean names
    numeric: dict[str, float] = {}
    for k, v in matched_row.items():
        if k.lower() in _SCREENER_SKIP:
            continue
        try:
            numeric[_clean_col_name(k)] = float(v)
        except (TypeError, ValueError):
            pass

    if not numeric:
        return None

    # Expand: one entry per historical date (all same values — constant features)
    return [{**numeric, "date": d} for d in dates]


def _fetch_auto_indicators(
    ticker: str,
    dates: list[str],
    closes: np.ndarray,
    exchange: str,
    period: str,
) -> list[dict]:
    """Fetch fundamentals, sentiment, and market-relative features; merge by date."""
    benchmark = _BENCHMARK.get(exchange.upper(), "^NSEI")

    fund_rows: list[dict] = []
    sent_rows: list[dict] = []
    mkt_rows: list[dict] = []

    try:
        fund_rows = fetch_fundamental_features(ticker, dates)
    except Exception:
        pass

    try:
        sent_rows = fetch_sentiment_features(ticker, dates)
    except Exception:
        pass

    try:
        mkt_rows = fetch_market_features(ticker, dates, closes, period, benchmark)
    except Exception:
        pass

    if not fund_rows and not sent_rows and not mkt_rows:
        return []

    # Merge all three into a single list[dict] keyed by date
    merged_by_date: dict[str, dict] = {}

    for row in fund_rows:
        d = row["date"]
        merged_by_date.setdefault(d, {"date": d}).update(
            {k: v for k, v in row.items() if k != "date"}
        )

    for row in sent_rows:
        d = row["date"]
        merged_by_date.setdefault(d, {"date": d}).update(
            {k: v for k, v in row.items() if k != "date"}
        )

    for row in mkt_rows:
        d = row["date"]
        merged_by_date.setdefault(d, {"date": d}).update(
            {k: v for k, v in row.items() if k != "date"}
        )

    return sorted(merged_by_date.values(), key=lambda r: r["date"])


def _merge_indicators(auto: list[dict], user: list[dict]) -> list[dict]:
    """
    Merge auto-fetched and user-provided indicators.
    User values override auto values for the same date+key.
    All dates from both sources are included.
    """
    merged: dict[str, dict] = {}

    for row in auto:
        d = row["date"]
        merged[d] = dict(row)

    for row in user:
        d = row["date"]
        if d in merged:
            merged[d].update(row)
        else:
            merged[d] = dict(row)

    return sorted(merged.values(), key=lambda r: r["date"])


def _run_ml_models(
    closes: np.ndarray,
    entry_price: float,
    ticker: str,
    exchange: str = "NSE",
    period: str = "5y",
    dates: Optional[list[str]] = None,
    extra_indicators: Optional[list[dict]] = None,
) -> dict:
    """Run LSTM, XGBoost, and Transformer concurrently; return cached results if fresh."""
    auto_indicators = _fetch_auto_indicators(ticker, dates or [], closes, exchange, period)

    if extra_indicators and auto_indicators:
        merged = _merge_indicators(auto_indicators, extra_indicators)
    elif extra_indicators:
        merged = extra_indicators
    elif auto_indicators:
        merged = auto_indicators
    else:
        merged = None

    # Only use model cache when no indicators are present at all
    if not merged:
        cache_key = (ticker, period)
        now = time.time()
        if cache_key in _model_cache:
            ts, results = _model_cache[cache_key]
            if now - ts < _CACHE_TTL_SECONDS:
                return results

    with ThreadPoolExecutor(max_workers=3) as ex:
        f_lstm = ex.submit(run_lstm_full, closes, entry_price)
        f_xgb = ex.submit(
            run_xgboost_full, closes, entry_price, 504, dates, merged
        )
        f_transformer = ex.submit(run_transformer_full, closes, entry_price)

    lstm_forecast, lstm_path = f_lstm.result()
    xgb_forecast, top_features, xgb_path, extra_feat_names = f_xgb.result()
    trans_forecast, trans_path = f_transformer.result()

    results = {
        "lstm_30d_forecast_pct": lstm_forecast,
        "xgb_30d_forecast_pct": xgb_forecast,
        "transformer_30d_forecast_pct": trans_forecast,
        "top_features": top_features,
        "lstm_price_path": lstm_path,
        "xgb_price_path": xgb_path,
        "transformer_price_path": trans_path,
        "extra_feature_names": extra_feat_names if extra_feat_names else None,
    }

    if not merged:
        cache_key = (ticker, period)
        now = time.time()
        _model_cache[cache_key] = (now, results)

    return results


class ForecastRequest(BaseModel):
    symbol: str
    exchange: str = "NSE"
    entry_price: float
    stop_loss: float
    target_pct: float = 30.0
    num_paths: int = 10_000
    horizon_days: int = 504
    extra_indicators: Optional[list[dict]] = None


class ForecastResponse(BaseModel):
    symbol: str
    ticker: str
    data_points_used: int
    annualized_vol_pct: float
    prob_hit_target_pct: float
    prob_stop_hit_pct: float
    prob_expired_pct: float
    pct5_gain_pct: float
    pct50_gain_pct: float
    pct95_gain_pct: float
    expected_gain_pct: float
    arima_30d_forecast_pct: Optional[float]
    garch_vol_pct: Optional[float]
    bs_implied_vol_pct: Optional[float]
    lstm_30d_forecast_pct: Optional[float]
    xgb_30d_forecast_pct: Optional[float]
    transformer_30d_forecast_pct: Optional[float]
    top_features: Optional[list[dict]]
    num_paths: int
    horizon_days: int
    lstm_price_path: Optional[list[float]]
    xgb_price_path: Optional[list[float]]
    transformer_price_path: Optional[list[float]]
    historical_closes: list[float]
    extra_feature_names: Optional[list[str]]


@app.post("/forecast", response_model=ForecastResponse)
def forecast(req: ForecastRequest) -> ForecastResponse:
    ticker = _build_ticker(req.symbol, req.exchange)

    try:
        dates, closes = _fetch_closes(ticker)
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Failed to fetch price data for {ticker}: {e}")

    if len(closes) < 30:
        raise HTTPException(
            status_code=422,
            detail=f"Insufficient price data for {ticker}: only {len(closes)} data points",
        )

    mc = run_simulation(
        closes=closes,
        entry_price=req.entry_price,
        stop_loss=req.stop_loss,
        num_paths=req.num_paths,
        horizon_days=req.horizon_days,
        target_pct=req.target_pct,
    )

    arima_forecast = run_arima(closes, req.entry_price)
    garch_vol = run_garch(closes)
    bs_iv = run_black_scholes(ticker, req.entry_price)

    processed_indicators = (
        _preprocess_extra_indicators(req.extra_indicators, req.symbol, dates)
        if req.extra_indicators else None
    )

    ml_results = _run_ml_models(
        closes, req.entry_price, ticker,
        exchange=req.exchange,
        dates=dates,
        extra_indicators=processed_indicators,
    )

    # Last ~252 trading days (~1 year) for chart context
    historical_closes = closes[-252:].tolist()

    return ForecastResponse(
        symbol=req.symbol.upper(),
        ticker=ticker,
        data_points_used=len(closes),
        annualized_vol_pct=mc["annualized_vol_pct"],
        prob_hit_target_pct=mc["prob_hit_target_pct"],
        prob_stop_hit_pct=mc["prob_stop_hit_pct"],
        prob_expired_pct=mc["prob_expired_pct"],
        pct5_gain_pct=mc["pct5_gain_pct"],
        pct50_gain_pct=mc["pct50_gain_pct"],
        pct95_gain_pct=mc["pct95_gain_pct"],
        expected_gain_pct=mc["expected_gain_pct"],
        arima_30d_forecast_pct=arima_forecast,
        garch_vol_pct=garch_vol,
        bs_implied_vol_pct=bs_iv,
        lstm_30d_forecast_pct=ml_results["lstm_30d_forecast_pct"],
        xgb_30d_forecast_pct=ml_results["xgb_30d_forecast_pct"],
        transformer_30d_forecast_pct=ml_results["transformer_30d_forecast_pct"],
        top_features=ml_results["top_features"],
        num_paths=req.num_paths,
        horizon_days=req.horizon_days,
        lstm_price_path=ml_results["lstm_price_path"],
        xgb_price_path=ml_results["xgb_price_path"],
        transformer_price_path=ml_results["transformer_price_path"],
        historical_closes=historical_closes,
        extra_feature_names=ml_results.get("extra_feature_names"),
    )


@app.get("/price/{symbol}")
def get_price(symbol: str, exchange: str = "NSE") -> dict:
    """Return the latest closing price for a symbol."""
    ticker = _build_ticker(symbol, exchange)
    try:
        _, closes = _fetch_closes(ticker)
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Failed to fetch price for {ticker}: {e}")
    if len(closes) == 0:
        raise HTTPException(status_code=404, detail=f"No price data for {ticker}")
    return {"symbol": symbol.upper(), "ticker": ticker, "price": round(float(closes[-1]), 2)}


@app.get("/health")
def health() -> dict:
    return {"status": "ok"}
