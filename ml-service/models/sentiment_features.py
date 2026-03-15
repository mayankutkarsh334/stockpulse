import time
import logging

import yfinance as yf

logger = logging.getLogger(__name__)

_sent_cache: dict[str, tuple[float, list[dict]]] = {}
_SENT_TTL = 3600  # 1 hour


def _extract_title(article: dict) -> str:
    content = article.get("content", {})
    if isinstance(content, dict):
        return content.get("title", "")
    return article.get("title", "")


def fetch_sentiment_features(ticker: str, dates: list[str]) -> list[dict]:
    """
    Fetch news sentiment features via VADER for the given ticker.
    Returns list[dict] with one constant entry per date.
    Returns [] on any failure or if vaderSentiment is not installed.
    """
    if not dates:
        return []

    now = time.time()
    if ticker in _sent_cache:
        ts, cached = _sent_cache[ticker]
        if now - ts < _SENT_TTL:
            if cached:
                base = {k: v for k, v in cached[0].items() if k != "date"}
                return [{**base, "date": d} for d in dates]
            return []

    try:
        from vaderSentiment.vaderSentiment import SentimentIntensityAnalyzer  # noqa: PLC0415
    except ImportError:
        logger.warning("sentiment_features: vaderSentiment not installed; skipping")
        return []

    try:
        news = yf.Ticker(ticker).news or []
        titles = [_extract_title(a) for a in news]
        titles = [t for t in titles if t]

        if not titles:
            _sent_cache[ticker] = (now, [])
            return []

        analyzer = SentimentIntensityAnalyzer()
        scores = [analyzer.polarity_scores(t)["compound"] for t in titles]

        compound_mean = sum(scores) / len(scores)
        recent5 = scores[:5]
        recent5_mean = sum(recent5) / len(recent5) if recent5 else compound_mean
        positive_ratio = sum(1 for s in scores if s > 0.05) / len(scores)

        numeric = {
            "sent_compound_mean": round(compound_mean, 4),
            "sent_recent5_mean": round(recent5_mean, 4),
            "sent_positive_ratio": round(positive_ratio, 4),
        }

        rows = [{**numeric, "date": d} for d in dates]
        _sent_cache[ticker] = (now, rows)
        return rows

    except Exception as e:
        logger.warning("sentiment_features: failed for %s: %s", ticker, e)
        return []
