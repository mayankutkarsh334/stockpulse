import numpy as np
from typing import Optional


def run_transformer_full(
    closes: np.ndarray, entry_price: float, horizon: int = 504
) -> tuple[Optional[float], Optional[list[float]]]:
    """
    PatchTST-inspired Transformer: divide log-return series into patches of 16 days,
    apply 1-layer multi-head self-attention (2 heads, d_model=32), then predict
    30-day cumulative % gain from entry_price.
    Returns (30d_gain_pct, price_path) where price_path is a list of `horizon`
    absolute prices generated autoregressively, or (None, None) on failure.
    """
    try:
        import torch
        import torch.nn as nn
        from sklearn.preprocessing import StandardScaler

        if len(closes) < 150:
            return None, None

        log_returns = np.diff(np.log(closes)).astype(np.float32)

        PATCH = 16
        HORIZON = 30
        D_MODEL = 32
        N_HEADS = 2
        EPOCHS = 40
        WINDOW = 96  # must be divisible by PATCH (96 / 16 = 6 patches)

        scaler = StandardScaler()
        scaled = scaler.fit_transform(log_returns.reshape(-1, 1)).flatten()

        # Build (X, y): X is (N, WINDOW), y is (N, HORIZON)
        X_list, y_list = [], []
        for i in range(len(scaled) - WINDOW - HORIZON + 1):
            X_list.append(scaled[i : i + WINDOW])
            y_list.append(scaled[i + WINDOW : i + WINDOW + HORIZON])

        X_np = np.array(X_list, dtype=np.float32)
        y_np = np.array(y_list, dtype=np.float32)

        split = int(len(X_np) * 0.8)
        X_train = torch.tensor(X_np[:split])
        y_train = torch.tensor(y_np[:split])

        class PatchTransformer(nn.Module):
            def __init__(self):
                super().__init__()
                n_patches = WINDOW // PATCH
                self.patch_proj = nn.Linear(PATCH, D_MODEL)
                self.pos_emb = nn.Parameter(torch.zeros(1, n_patches, D_MODEL))
                encoder_layer = nn.TransformerEncoderLayer(
                    d_model=D_MODEL,
                    nhead=N_HEADS,
                    dim_feedforward=64,
                    dropout=0.1,
                    batch_first=True,
                )
                self.encoder = nn.TransformerEncoder(encoder_layer, num_layers=1)
                self.head = nn.Linear(D_MODEL * n_patches, HORIZON)

            def forward(self, x):
                # x: (B, WINDOW)
                B = x.size(0)
                n_patches = WINDOW // PATCH
                patches = x.view(B, n_patches, PATCH)          # (B, n_patches, PATCH)
                tok = self.patch_proj(patches) + self.pos_emb  # (B, n_patches, D_MODEL)
                enc = self.encoder(tok)                         # (B, n_patches, D_MODEL)
                flat = enc.reshape(B, -1)                       # (B, n_patches*D_MODEL)
                return self.head(flat)                          # (B, HORIZON)

        model = PatchTransformer()
        optimizer = torch.optim.Adam(model.parameters(), lr=1e-3)
        criterion = nn.MSELoss()

        model.train()
        for _ in range(EPOCHS):
            optimizer.zero_grad()
            loss = criterion(model(X_train), y_train)
            loss.backward()
            optimizer.step()

        model.eval()
        with torch.no_grad():
            x_pred = torch.tensor(scaled[-WINDOW:]).unsqueeze(0)  # (1, WINDOW)
            pred_scaled = model(x_pred).numpy().flatten()

        pred_returns = scaler.inverse_transform(pred_scaled.reshape(-1, 1)).flatten()
        forecast_price = entry_price * float(np.exp(np.sum(pred_returns)))
        gain_pct = (forecast_price - entry_price) / entry_price * 100.0
        gain_pct = round(gain_pct, 2)

        # Generate price path autoregressively
        sliding_window = list(scaled[-WINDOW:])
        price_path: list[float] = []
        current_price = entry_price

        while len(price_path) < horizon:
            window_arr = np.array(sliding_window[-WINDOW:], dtype=np.float32)
            x_step = torch.tensor(window_arr).unsqueeze(0)  # (1, WINDOW)
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


def run_transformer(closes: np.ndarray, entry_price: float) -> Optional[float]:
    """
    PatchTST-inspired Transformer: divide log-return series into patches of 16 days,
    apply 1-layer multi-head self-attention (2 heads, d_model=32), then predict
    30-day cumulative % gain from entry_price.
    Returns None on failure.
    """
    gain_pct, _ = run_transformer_full(closes, entry_price)
    return gain_pct
