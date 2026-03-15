import numpy as np
from typing import Optional


def run_garch(closes: np.ndarray) -> Optional[float]:
    """
    Fit GARCH(1,1) on log returns and return annualised conditional volatility
    as a percentage.

    Returns None if the model fails (too few points, convergence error, etc.).
    """
    try:
        from arch import arch_model

        if len(closes) < 30:
            return None

        log_returns = np.diff(np.log(closes)) * 100.0  # arch expects returns in pct
        model = arch_model(log_returns, vol="Garch", p=1, q=1, rescale=False)
        result = model.fit(disp="off", show_warning=False)
        # annualised vol from the last conditional variance
        daily_var = float(result.conditional_volatility[-1])
        annualized_vol_pct = daily_var * np.sqrt(252)
        return round(annualized_vol_pct, 2)
    except Exception:
        return None
