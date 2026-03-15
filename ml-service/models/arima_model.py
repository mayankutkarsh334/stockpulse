import numpy as np
from typing import Optional


def run_arima(closes: np.ndarray, entry_price: float) -> Optional[float]:
    """
    Fit ARIMA(1,1,0) on log prices and return a 30-day ahead forecast
    expressed as a percentage gain/loss relative to entry_price.

    Returns None if the model fails (too few points, convergence error, etc.).
    """
    try:
        from statsmodels.tsa.arima.model import ARIMA

        if len(closes) < 30:
            return None

        log_prices = np.log(closes)
        model = ARIMA(log_prices, order=(1, 1, 0))
        result = model.fit()
        forecast_log = result.forecast(steps=30)
        forecast_price = float(np.exp(np.asarray(forecast_log)[-1]))
        gain_pct = (forecast_price - entry_price) / entry_price * 100.0
        return round(gain_pct, 2)
    except Exception:
        return None
