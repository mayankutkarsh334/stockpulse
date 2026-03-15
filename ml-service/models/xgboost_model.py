import numpy as np
from typing import Optional


def _compute_rsi(prices: np.ndarray, period: int = 14) -> np.ndarray:
    deltas = np.diff(prices)
    gains = np.where(deltas > 0, deltas, 0.0)
    losses = np.where(deltas < 0, -deltas, 0.0)
    avg_gain = np.convolve(gains, np.ones(period) / period, mode="valid")
    avg_loss = np.convolve(losses, np.ones(period) / period, mode="valid")
    rs = np.where(avg_loss == 0, np.inf, avg_gain / avg_loss)
    rsi = 100.0 - 100.0 / (1.0 + rs)
    return rsi


def _ema(prices: np.ndarray, span: int) -> np.ndarray:
    alpha = 2.0 / (span + 1)
    out = np.empty_like(prices)
    out[0] = prices[0]
    for i in range(1, len(prices)):
        out[i] = alpha * prices[i] + (1 - alpha) * out[i - 1]
    return out


def _extract_features_at(
    closes_arr: np.ndarray, i: int, log_ret: np.ndarray, rsi: np.ndarray,
    ema20: np.ndarray, ema50: np.ndarray
) -> list:
    return [
        log_ret[i - 1],                                                          # log_ret_1d
        np.sum(log_ret[i - 5 : i]),                                              # log_ret_5d
        np.sum(log_ret[i - 21 : i]),                                             # log_ret_21d
        np.std(log_ret[i - 21 : i]),                                             # volatility_21d
        np.std(log_ret[i - 63 : i]),                                             # volatility_63d
        (closes_arr[i] - closes_arr[i - 5]) / closes_arr[i - 5],                # momentum_5d
        (closes_arr[i] - closes_arr[i - 21]) / closes_arr[i - 21],              # momentum_21d
        (closes_arr[i] - closes_arr[i - 63]) / closes_arr[i - 63],              # momentum_63d
        float(rsi[i - 14]) if i - 14 >= 0 and i - 14 < len(rsi) else 50.0,     # rsi_14d
        closes_arr[i] / ema20[i] if ema20[i] > 0 else 1.0,                     # ema_ratio_20
        ema20[i] / ema50[i] if ema50[i] > 0 else 1.0,                          # ema_ratio_ema2050
    ]


def run_xgboost_full(
    closes: np.ndarray,
    entry_price: float,
    horizon: int = 504,
    dates: Optional[list[str]] = None,
    extra_indicators: Optional[list[dict]] = None,
) -> tuple[Optional[float], Optional[list[dict]], Optional[list[float]], list[str]]:
    """
    XGBoost regressor predicting 30-day forward log return.
    Returns (forecast_pct, top_features, price_path, extra_feat_names) or (None, None, None, []) on failure.
    price_path is a list of `horizon` absolute prices generated in 30-day chunks.
    """
    try:
        import xgboost as xgb

        if len(closes) < 150:
            return None, None, None, []

        HORIZON = 30
        log_ret = np.diff(np.log(closes))
        n = len(closes)

        ema20 = _ema(closes, 20)
        ema50 = _ema(closes, 50)
        rsi = _compute_rsi(closes, 14)

        FEATURE_NAMES = [
            "log_ret_1d",
            "log_ret_5d",
            "log_ret_21d",
            "volatility_21d",
            "volatility_63d",
            "momentum_5d",
            "momentum_21d",
            "momentum_63d",
            "rsi_14d",
            "ema_ratio_20",
            "ema_ratio_ema2050",
        ]

        # ── Build extra-indicator lookup ──────────────────────────────────────
        extra_feat_names: list[str] = []
        extra_feat_map: dict[str, list[float]] = {}  # date_str → values

        if extra_indicators and dates is not None:
            sample = extra_indicators[0]
            extra_feat_names = [k for k in sample.keys() if k.lower() != "date"]

            # Build raw map: normalize dates to YYYY-MM-DD
            raw_map: dict[str, list[float]] = {}
            for row in extra_indicators:
                raw_date = str(row.get("date", "")).strip()
                try:
                    from datetime import datetime
                    for fmt in ("%Y-%m-%d", "%d/%m/%Y", "%m/%d/%Y", "%d-%m-%Y"):
                        try:
                            raw_date = datetime.strptime(raw_date, fmt).strftime("%Y-%m-%d")
                            break
                        except ValueError:
                            pass
                except Exception:
                    pass
                vals = []
                for col in extra_feat_names:
                    try:
                        vals.append(float(row.get(col) or 0))
                    except (TypeError, ValueError):
                        vals.append(0.0)
                raw_map[raw_date] = vals

            # Forward-fill: for each trading date, find the most recent extra date <= it
            sorted_extra_dates = sorted(raw_map.keys())
            last_vals: list[float] = [0.0] * len(extra_feat_names)
            for d in dates:
                # binary-search style: find latest extra_date <= d
                lo, hi = 0, len(sorted_extra_dates) - 1
                found = None
                while lo <= hi:
                    mid = (lo + hi) // 2
                    if sorted_extra_dates[mid] <= d:
                        found = mid
                        lo = mid + 1
                    else:
                        hi = mid - 1
                if found is not None:
                    last_vals = raw_map[sorted_extra_dates[found]]
                extra_feat_map[d] = list(last_vals)

        ALL_FEATURE_NAMES = FEATURE_NAMES + extra_feat_names

        def _get_extra_at(date: str) -> list[float]:
            return extra_feat_map.get(date, [0.0] * len(extra_feat_names))

        min_lookback = 63 + 14  # largest window + RSI
        rows_X, rows_y = [], []

        for i in range(min_lookback, n - HORIZON):
            features = _extract_features_at(closes, i, log_ret, rsi, ema20, ema50)
            if extra_feat_names and dates is not None and i < len(dates):
                features = features + _get_extra_at(dates[i])
            target = np.sum(log_ret[i : i + HORIZON])
            rows_X.append(features)
            rows_y.append(target)

        X = np.array(rows_X, dtype=np.float32)
        y = np.array(rows_y, dtype=np.float32)

        split = int(len(X) * 0.8)
        X_train, y_train = X[:split], y[:split]

        dtrain = xgb.DMatrix(X_train, label=y_train, feature_names=ALL_FEATURE_NAMES)
        params = {
            "objective": "reg:squarederror",
            "max_depth": 4,
            "eta": 0.05,
            "subsample": 0.8,
            "colsample_bytree": 0.8,
            "seed": 42,
        }
        booster = xgb.train(params, dtrain, num_boost_round=200, verbose_eval=False)

        # Predict on latest row
        i_last = n - HORIZON - 1
        last_features = _extract_features_at(closes, i_last, log_ret, rsi, ema20, ema50)
        if extra_feat_names and dates is not None:
            last_date = dates[i_last] if i_last < len(dates) else (dates[-1] if dates else "")
            last_features = last_features + _get_extra_at(last_date)

        dpred = xgb.DMatrix([last_features], feature_names=ALL_FEATURE_NAMES)
        pred_log_return = float(booster.predict(dpred)[0])
        forecast_price = entry_price * np.exp(pred_log_return)
        gain_pct = (forecast_price - entry_price) / entry_price * 100.0

        # Feature importance (weight = number of times feature is used in splits)
        importance = booster.get_score(importance_type="gain")
        total = sum(importance.values()) or 1.0
        sorted_feats = sorted(importance.items(), key=lambda x: x[1], reverse=True)
        top_features = [
            {"name": name, "importance_pct": round(score / total * 100.0, 1)}
            for name, score in sorted_feats[:5]
        ]

        # Generate price path in 30-day chunks
        # We maintain a synthetic price series, recomputing features each chunk
        synthetic_closes = list(closes)
        price_path: list[float] = []
        current_price = entry_price
        # Use last known date for extra feature lookup in synthetic path
        current_extra_date = dates[-1] if dates else ""

        while len(price_path) < horizon:
            sc = np.array(synthetic_closes, dtype=np.float64)
            sc_n = len(sc)

            sc_log_ret = np.diff(np.log(sc))
            sc_ema20 = _ema(sc, 20)
            sc_ema50 = _ema(sc, 50)
            sc_rsi = _compute_rsi(sc, 14)

            sc_i = sc_n - 1  # last index
            # Ensure we have enough history for feature computation
            if sc_i < min_lookback or sc_i - 1 < 0:
                break

            chunk_features = _extract_features_at(sc, sc_i, sc_log_ret, sc_rsi, sc_ema20, sc_ema50)
            if extra_feat_names:
                chunk_features = chunk_features + _get_extra_at(current_extra_date)
            chunk_dpred = xgb.DMatrix([chunk_features], feature_names=ALL_FEATURE_NAMES)
            chunk_log_return = float(booster.predict(chunk_dpred)[0])

            # Divide the 30-day log return evenly into 30 daily returns
            daily_log_return = chunk_log_return / HORIZON
            chunk_prices: list[float] = []
            p = current_price
            for _ in range(HORIZON):
                if len(price_path) + len(chunk_prices) >= horizon:
                    break
                p = p * float(np.exp(daily_log_return))
                chunk_prices.append(p)

            price_path.extend(chunk_prices)
            current_price = chunk_prices[-1] if chunk_prices else current_price

            # Extend synthetic_closes with the generated prices for next iteration
            synthetic_closes.extend(chunk_prices)

        return round(gain_pct, 2), top_features, price_path[:horizon], extra_feat_names

    except Exception:
        return None, None, None, []


def run_xgboost(
    closes: np.ndarray,
    entry_price: float,
    dates: Optional[list[str]] = None,
    extra_indicators: Optional[list[dict]] = None,
) -> tuple[Optional[float], Optional[list[dict]]]:
    """
    XGBoost regressor predicting 30-day forward log return.
    Returns (forecast_pct, top_features) or (None, None) on failure.
    """
    gain_pct, top_features, _, _ = run_xgboost_full(closes, entry_price, dates=dates, extra_indicators=extra_indicators)
    return gain_pct, top_features
