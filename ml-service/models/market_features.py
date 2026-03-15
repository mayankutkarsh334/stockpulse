import time
import logging

import numpy as np
import yfinance as yf

logger = logging.getLogger(__name__)

_BENCHMARK: dict[str, str] = {
    "NSE": "^NSEI",
    "BSE": "^BSESN",
    "NYSE": "^GSPC",
    "NASDAQ": "^IXIC",
}

_mkt_cache: dict[str, tuple[float, list[dict]]] = {}
_MKT_TTL = 3600  # 1 hour


def fetch_market_features(
    ticker: str,
    dates: list[str],
    closes: np.ndarray,
    period: str = "5y",
    benchmark: str = "^NSEI",
) -> list[dict]:
    """
    Compute market-relative features:
      mkt_beta          – 252-day rolling beta vs benchmark (clipped [-5, 5])
      mkt_rel_strength_21d – 21-day cumulative return minus benchmark return
      mkt_corr_63d      – 63-day rolling correlation with benchmark

    Returns list[dict] with one entry per date (time-varying).
    Returns [] on any failure.
    """
    if not dates or len(closes) == 0:
        return []

    cache_key = f"{ticker}|{benchmark}|{period}"
    now = time.time()
    if cache_key in _mkt_cache:
        ts, cached = _mkt_cache[cache_key]
        if now - ts < _MKT_TTL:
            return cached

    try:
        bm_hist = yf.Ticker(benchmark).history(period=period)
        bm_closes_series = bm_hist["Close"].dropna()

        # Build pandas-indexed series for alignment
        import pandas as pd  # noqa: PLC0415

        stock_series = pd.Series(closes, index=pd.to_datetime(dates))
        bm_series = bm_closes_series.copy()
        bm_series.index = pd.to_datetime(bm_series.index).normalize()
        stock_series.index = stock_series.index.normalize()

        # Align on union of dates, forward-fill
        combined = pd.DataFrame({"stock": stock_series, "bm": bm_series})
        combined = combined.ffill().dropna()

        stock_aligned = combined["stock"].values.astype(float)
        bm_aligned = combined["bm"].values.astype(float)
        aligned_dates = [d.strftime("%Y-%m-%d") for d in combined.index]

        n = len(stock_aligned)
        stock_ret = np.diff(stock_aligned) / (stock_aligned[:-1] + 1e-10)
        bm_ret = np.diff(bm_aligned) / (bm_aligned[:-1] + 1e-10)

        # Pad returns arrays with leading 0 so index matches price arrays
        stock_ret = np.concatenate([[0.0], stock_ret])
        bm_ret = np.concatenate([[0.0], bm_ret])

        rows: list[dict] = []
        for i in range(n):
            mkt_beta = 0.0
            mkt_rel_strength_21d = 0.0
            mkt_corr_63d = 0.0

            # Beta (252-day window)
            if i >= 252:
                s_slice = stock_ret[i - 252: i]
                b_slice = bm_ret[i - 252: i]
                bm_var = np.var(b_slice)
                if bm_var > 1e-10:
                    mkt_beta = float(np.cov(s_slice, b_slice)[0, 1] / bm_var)
                    mkt_beta = float(np.clip(mkt_beta, -5.0, 5.0))

            # Relative strength (21-day)
            if i >= 21:
                mkt_rel_strength_21d = float(
                    np.sum(stock_ret[i - 21: i]) - np.sum(bm_ret[i - 21: i])
                )

            # Correlation (63-day)
            if i >= 63:
                s_slice = stock_ret[i - 63: i]
                b_slice = bm_ret[i - 63: i]
                if np.std(s_slice) > 1e-10 and np.std(b_slice) > 1e-10:
                    mkt_corr_63d = float(np.corrcoef(s_slice, b_slice)[0, 1])

            rows.append(
                {
                    "date": aligned_dates[i],
                    "mkt_beta": round(mkt_beta, 4),
                    "mkt_rel_strength_21d": round(mkt_rel_strength_21d, 4),
                    "mkt_corr_63d": round(mkt_corr_63d, 4),
                }
            )

        _mkt_cache[cache_key] = (now, rows)
        return rows

    except Exception as e:
        logger.warning("market_features: failed for %s: %s", ticker, e)
        return []
