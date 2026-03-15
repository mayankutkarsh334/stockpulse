import numpy as np
from typing import Optional


def run_simulation(
    closes: np.ndarray,
    entry_price: float,
    stop_loss: float,
    num_paths: int,
    horizon_days: int,
    target_pct: float = 30.0,
) -> dict:
    """
    GBM Monte Carlo simulation.

    Returns probability of hitting +30% target, stop loss, or expiring neutral,
    plus percentile statistics on final prices.
    """
    log_returns = np.diff(np.log(closes))
    mu: float = float(log_returns.mean())
    sigma: float = float(log_returns.std())
    annualized_vol_pct: float = sigma * np.sqrt(252) * 100.0

    target_price = entry_price * (1.0 + target_pct / 100.0)

    hits_target = 0
    hits_stop = 0
    final_prices: list[float] = []

    rng = np.random.default_rng()

    for _ in range(num_paths):
        S = entry_price
        path_hit_target = False
        path_hit_stop = False
        for _ in range(horizon_days):
            Z = rng.standard_normal()
            S *= np.exp((mu - 0.5 * sigma ** 2) + sigma * Z)
            if S >= target_price:
                path_hit_target = True
                break
            if S <= stop_loss:
                path_hit_stop = True
                break
        final_prices.append(S)
        if path_hit_target:
            hits_target += 1
        elif path_hit_stop:
            hits_stop += 1

    final_arr = np.array(final_prices)
    gain_arr = (final_arr - entry_price) / entry_price * 100.0

    prob_hit_target_pct = hits_target / num_paths * 100.0
    prob_stop_hit_pct = hits_stop / num_paths * 100.0
    prob_expired_pct = (num_paths - hits_target - hits_stop) / num_paths * 100.0

    return {
        "annualized_vol_pct": round(annualized_vol_pct, 2),
        "prob_hit_target_pct": round(prob_hit_target_pct, 2),
        "prob_stop_hit_pct": round(prob_stop_hit_pct, 2),
        "prob_expired_pct": round(prob_expired_pct, 2),
        "pct5_gain_pct": round(float(np.percentile(gain_arr, 5)), 2),
        "pct50_gain_pct": round(float(np.percentile(gain_arr, 50)), 2),
        "pct95_gain_pct": round(float(np.percentile(gain_arr, 95)), 2),
        "expected_gain_pct": round(float(gain_arr.mean()), 2),
    }
