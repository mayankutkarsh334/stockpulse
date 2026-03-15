import numpy as np
from typing import Optional


def run_lstm_full(
    closes: np.ndarray, entry_price: float, horizon: int = 504
) -> tuple[Optional[float], Optional[list[float]]]:
    """
    2-layer LSTM trained on 60-day sliding windows of log-returns.
    Returns (30d_gain_pct, price_path) where price_path is a list of `horizon`
    absolute prices generated autoregressively, or (None, None) on failure.
    """
    try:
        import torch
        import torch.nn as nn
        from sklearn.preprocessing import StandardScaler

        if len(closes) < 120:
            return None, None

        log_returns = np.diff(np.log(closes)).astype(np.float32)

        WINDOW = 60
        HORIZON = 30
        HIDDEN = 64
        LAYERS = 2
        EPOCHS = 40

        scaler = StandardScaler()
        scaled = scaler.fit_transform(log_returns.reshape(-1, 1)).flatten()

        # Build (X, y) pairs
        X_list, y_list = [], []
        for i in range(len(scaled) - WINDOW - HORIZON + 1):
            X_list.append(scaled[i : i + WINDOW])
            y_list.append(scaled[i + WINDOW : i + WINDOW + HORIZON])

        X = np.array(X_list, dtype=np.float32)  # (N, 60)
        y = np.array(y_list, dtype=np.float32)  # (N, 30)

        split = int(len(X) * 0.8)
        X_train = torch.tensor(X[:split]).unsqueeze(-1)  # (N, 60, 1)
        y_train = torch.tensor(y[:split])

        class LSTMModel(nn.Module):
            def __init__(self):
                super().__init__()
                self.lstm = nn.LSTM(
                    input_size=1,
                    hidden_size=HIDDEN,
                    num_layers=LAYERS,
                    batch_first=True,
                    dropout=0.1,
                )
                self.fc = nn.Linear(HIDDEN, HORIZON)

            def forward(self, x):
                out, _ = self.lstm(x)
                return self.fc(out[:, -1, :])

        model = LSTMModel()
        optimizer = torch.optim.Adam(model.parameters(), lr=1e-3)
        criterion = nn.MSELoss()

        model.train()
        for _ in range(EPOCHS):
            optimizer.zero_grad()
            loss = criterion(model(X_train), y_train)
            loss.backward()
            optimizer.step()

        # Predict using the last WINDOW returns for the 30d scalar
        model.eval()
        with torch.no_grad():
            x_pred = torch.tensor(scaled[-WINDOW:]).unsqueeze(0).unsqueeze(-1)
            pred_scaled = model(x_pred).numpy().flatten()

        # Inverse-transform to actual log-return scale
        pred_returns = scaler.inverse_transform(pred_scaled.reshape(-1, 1)).flatten()

        # Cumulative price from entry (30d scalar)
        forecast_price = entry_price * float(np.exp(np.sum(pred_returns)))
        gain_pct = (forecast_price - entry_price) / entry_price * 100.0
        gain_pct = round(gain_pct, 2)

        # Generate price path autoregressively
        # sliding_window holds the scaled log-returns we extend as we predict
        sliding_window = list(scaled[-WINDOW:])
        price_path: list[float] = []
        current_price = entry_price

        while len(price_path) < horizon:
            window_arr = np.array(sliding_window[-WINDOW:], dtype=np.float32)
            x_step = torch.tensor(window_arr).unsqueeze(0).unsqueeze(-1)
            with torch.no_grad():
                step_scaled = model(x_step).numpy().flatten()  # (30,)
            step_returns = scaler.inverse_transform(step_scaled.reshape(-1, 1)).flatten()

            for r in step_returns:
                if len(price_path) >= horizon:
                    break
                current_price = current_price * float(np.exp(r))
                price_path.append(current_price)
            # Extend sliding window with the predicted scaled returns
            sliding_window.extend(step_scaled.tolist())

        return gain_pct, price_path[:horizon]

    except Exception:
        return None, None


def run_lstm(closes: np.ndarray, entry_price: float) -> Optional[float]:
    """
    2-layer LSTM trained on 60-day sliding windows of log-returns.
    Returns 30-day cumulative % gain from entry_price, or None on failure.
    """
    gain_pct, _ = run_lstm_full(closes, entry_price)
    return gain_pct
