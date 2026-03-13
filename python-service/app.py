import math
import os
import re
from typing import List, Dict

import numpy as np
import torch
from flask import Flask, jsonify, request
from torch import nn

# Basic Flask app
app = Flask(__name__)

# ── Mock mode ─────────────────────────────────────────────────────────────────
# Set BERT_MOCK=true to run without loading the BERT model (dev / CI / testing).
MOCK_MODE: bool = os.environ.get("BERT_MOCK", "false").lower() in ("true", "1", "yes")
MODEL_NAME: str = os.environ.get("BERT_MODEL", "bert-base-chinese")

LABEL_PROMPTS: Dict[str, str] = {
    "id_card": "身份证号码",
    "bank_card": "银行卡号",
    "phone": "手机号",
    "email": "邮箱地址",
    "address": "家庭住址",
    "name": "姓名",
    "unknown": "其他信息",
}

# ── Regex patterns used by mock classifier and fallback ───────────────────────
_RE_ID_CARD = re.compile(r"[1-9]\d{5}(19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[\dXx]")
_RE_PHONE   = re.compile(r"1[3-9]\d{9}")
_RE_BANK    = re.compile(r"\d{12,19}")
_RE_EMAIL   = re.compile(r"[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}")

def _regex_classify(text: str) -> Dict:
    """Rule-based fallback classifier (no model required)."""
    t = text or ""
    if _RE_ID_CARD.search(t):
        return {"label": "id_card",   "score": 0.72, "labelScores": []}
    if _RE_EMAIL.search(t):
        return {"label": "email",     "score": 0.68, "labelScores": []}
    if _RE_PHONE.search(t):
        return {"label": "phone",     "score": 0.70, "labelScores": []}
    if _RE_BANK.search(t):
        return {"label": "bank_card", "score": 0.60, "labelScores": []}
    return {"label": "unknown", "score": 0.0, "labelScores": []}


if not MOCK_MODE:
    from transformers import AutoModel, AutoTokenizer

    _tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME)
    _model = AutoModel.from_pretrained(MODEL_NAME)
    _model.eval()

    def _cls_embedding(text: str) -> torch.Tensor:
        inputs = _tokenizer(text, return_tensors="pt", truncation=True, max_length=256)
        with torch.no_grad():
            outputs = _model(**inputs)
        # Use CLS token representation
        return outputs.last_hidden_state[:, 0, :]  # (1, hidden)

    # Pre-compute label embeddings
    _label_embeddings: Dict[str, torch.Tensor] = {
        label: _cls_embedding(prompt) for label, prompt in LABEL_PROMPTS.items()
    }

    def classify_text(text: str) -> Dict:
        if not text:
            return {"label": "unknown", "score": 0.0, "labelScores": []}

        text_emb = _cls_embedding(text)
        sims = {}
        for label, emb in _label_embeddings.items():
            num = torch.sum(text_emb * emb, dim=1)
            denom = torch.norm(text_emb, dim=1) * torch.norm(emb, dim=1)
            sims[label] = (num / (denom + 1e-8)).item()

        ranked = sorted(sims.items(), key=lambda kv: kv[1], reverse=True)
        best_label, best_score = ranked[0]
        label_scores = [
            {"label": lbl, "score": round(sc, 4)} for lbl, sc in ranked
        ]
        return {"label": best_label, "score": round(best_score, 4), "labelScores": label_scores}
else:
    # Mock mode: use regex classifier, no model loaded
    def classify_text(text: str) -> Dict:  # type: ignore[misc]
        return _regex_classify(text)


class SimpleLSTM(nn.Module):
    def __init__(self, hidden_size: int = 16):
        super().__init__()
        self.lstm = nn.LSTM(input_size=1, hidden_size=hidden_size, batch_first=True)
        self.head = nn.Linear(hidden_size, 1)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        out, _ = self.lstm(x)
        return self.head(out[:, -1, :])


def forecast_risk(series: List[float], horizon: int = 7) -> List[float]:
    cleaned = [float(x) for x in series if x is not None]
    if len(cleaned) < 3:
        # Not enough data, repeat last
        last = cleaned[-1] if cleaned else 0.0
        return [last for _ in range(horizon)]

    torch.manual_seed(42)
    device = torch.device("cpu")

    data = torch.tensor(cleaned, dtype=torch.float32, device=device)
    mean = torch.mean(data)
    std = torch.std(data) + 1e-6
    normalized = (data - mean) / std

    look_back = min(7, len(normalized) - 1)
    xs, ys = [], []
    for i in range(len(normalized) - look_back):
        xs.append(normalized[i : i + look_back])
        ys.append(normalized[i + look_back])
    if not xs:
        last = cleaned[-1]
        return [last for _ in range(horizon)]

    x_tensor = torch.stack(xs).unsqueeze(-1)  # (batch, seq, 1)
    y_tensor = torch.stack(ys).unsqueeze(-1)

    model = SimpleLSTM(hidden_size=16).to(device)
    optim = torch.optim.Adam(model.parameters(), lr=0.01)
    loss_fn = nn.MSELoss()

    model.train()
    for _ in range(120):
        optim.zero_grad()
        preds = model(x_tensor)
        loss = loss_fn(preds, y_tensor)
        loss.backward()
        optim.step()

    # Forecast next values autoregressively
    model.eval()
    history = normalized.clone().tolist()
    for _ in range(horizon):
        window = history[-look_back:]
        window_tensor = torch.tensor(window, dtype=torch.float32, device=device).unsqueeze(0).unsqueeze(-1)
        with torch.no_grad():
            next_norm = model(window_tensor).item()
        history.append(next_norm)

    preds_denorm = [val * std.item() + mean.item() for val in history[-horizon:]]
    # Ensure non-negative risk counts
    return [max(0.0, round(v, 2)) for v in preds_denorm]


@app.route("/predict", methods=["POST"])
def predict():
    payload = request.get_json(force=True) or {}
    text = payload.get("text", "")
    result = classify_text(text)
    return jsonify(result)


@app.route("/batch_predict", methods=["POST"])
def batch_predict():
    payload = request.get_json(force=True) or {}
    texts = payload.get("texts", [])
    results = [classify_text(t) for t in texts]
    return jsonify({"results": results})


@app.route("/predict/risk", methods=["POST"])
def predict_risk():
    payload = request.get_json(force=True) or {}
    series = payload.get("series", [])
    if not isinstance(series, list):
        return jsonify({"error": "series must be a list"}), 400
    forecast = forecast_risk(series, horizon=payload.get("horizon", 7))
    return jsonify({"forecast": forecast})


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok", "model": MODEL_NAME, "mock": MOCK_MODE})


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5000))
    app.run(host="0.0.0.0", port=port)
