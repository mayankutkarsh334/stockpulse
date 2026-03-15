import datetime
import math
from typing import Optional

import numpy as np
import yfinance as yf
from scipy.stats import norm


# ── Black-Scholes pricing ─────────────────────────────────────────────────────

def _bs_call(S: float, K: float, T: float, r: float, sigma: float) -> float:
    if T <= 0 or sigma <= 0:
        return max(S - K, 0.0)
    d1 = (math.log(S / K) + (r + 0.5 * sigma ** 2) * T) / (sigma * math.sqrt(T))
    d2 = d1 - sigma * math.sqrt(T)
    return S * norm.cdf(d1) - K * math.exp(-r * T) * norm.cdf(d2)


def _bs_put(S: float, K: float, T: float, r: float, sigma: float) -> float:
    if T <= 0 or sigma <= 0:
        return max(K - S, 0.0)
    d1 = (math.log(S / K) + (r + 0.5 * sigma ** 2) * T) / (sigma * math.sqrt(T))
    d2 = d1 - sigma * math.sqrt(T)
    return K * math.exp(-r * T) * norm.cdf(-d2) - S * norm.cdf(-d1)


def _bs_vega(S: float, K: float, T: float, r: float, sigma: float) -> float:
    if T <= 0 or sigma <= 0:
        return 0.0
    d1 = (math.log(S / K) + (r + 0.5 * sigma ** 2) * T) / (sigma * math.sqrt(T))
    return S * norm.pdf(d1) * math.sqrt(T)


def _newton_iv(
    market_price: float,
    S: float, K: float, T: float, r: float,
    option_type: str,
    max_iter: int = 100,
    tol: float = 1e-6,
) -> Optional[float]:
    sigma = 0.3
    for _ in range(max_iter):
        price_fn = _bs_call if option_type == "call" else _bs_put
        diff = price_fn(S, K, T, r, sigma) - market_price
        vega = _bs_vega(S, K, T, r, sigma)
        if abs(vega) < 1e-10:
            return None
        sigma -= diff / vega
        if sigma <= 0:
            return None
        if abs(diff) < tol:
            return sigma
    return None


# ── Public interface ──────────────────────────────────────────────────────────

def run_black_scholes(
    ticker: str,
    current_price: float,
    risk_free_rate: float = 0.065,
) -> Optional[float]:
    """
    Return the market-implied volatility for `ticker` as an annualised %.

    Strategy:
      1. Pick the nearest expiry that is at least 21 days out
         (avoids near-expiry gamma distortions).
      2. Filter near-ATM strikes (±10% of current price).
      3. Use yfinance's pre-computed impliedVolatility where bid > 0;
         fall back to our own Newton-Raphson solver on mid-price otherwise.
      4. Return the median of all valid IVs.

    Returns None if options data is unavailable (e.g. NSE/BSE stocks).
    """
    try:
        tk = yf.Ticker(ticker)
        expiries = tk.options
        if not expiries:
            return None

        today = datetime.date.today()

        # Pick the nearest expiry >= 21 days out; fall back to >= 7 days
        chosen = None
        for min_days in (21, 7):
            for exp in expiries:
                if (datetime.date.fromisoformat(exp) - today).days >= min_days:
                    chosen = exp
                    break
            if chosen:
                break
        if chosen is None:
            return None

        chain = tk.option_chain(chosen)
        T = max((datetime.date.fromisoformat(chosen) - today).days / 365.0, 1 / 365.0)

        ivs: list[float] = []

        for side, df in (("call", chain.calls), ("put", chain.puts)):
            df = df.copy()
            # Keep near-ATM, liquid strikes only
            df = df[
                (df["strike"] >= current_price * 0.90) &
                (df["strike"] <= current_price * 1.10) &
                (df["bid"] > 0)
            ]
            for _, row in df.iterrows():
                K = float(row["strike"])

                # Prefer yfinance's own IV (already annualised 0-1 range)
                yf_iv = row.get("impliedVolatility", None)
                if yf_iv and 0.01 < float(yf_iv) < 5.0:
                    ivs.append(float(yf_iv) * 100.0)
                    continue

                # Fallback: solve from mid-price
                mid = (float(row["bid"]) + float(row["ask"])) / 2.0
                if mid <= 0:
                    continue
                iv = _newton_iv(mid, current_price, K, T, risk_free_rate, side)
                if iv is not None and 0.01 < iv < 5.0:
                    ivs.append(iv * 100.0)

        if not ivs:
            return None

        return round(float(np.median(ivs)), 2)

    except Exception:
        return None
