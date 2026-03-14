"""
train_anomaly.py — 员工 AI 行为异常检测模型训练脚本
=====================================================
使用 Isolation Forest（孤立森林）对员工 AI 使用行为进行无监督异常检测。

选择孤立森林的原因：
  - 无需大量标注数据，适合实际场景中"异常样本稀少"的情况
  - 对高维数据表现稳定
  - 训练和推理速度快，适合定期重训
  - scikit-learn 内置，无需额外依赖

运行方法：
    cd python-service
    # 先生成数据（如未生成）：
    python gen_behavior_data.py
    # 再训练模型：
    python train_anomaly.py

输出文件：
    models/anomaly_model.joblib   —— 孤立森林模型
    models/anomaly_encoder.joblib —— 分类变量编码器
    models/anomaly_scaler.joblib  —— 数值归一化器
    models/anomaly_meta.json      —— 模型元信息（特征名、服务列表等）
"""

import csv
import json
import os
import joblib
import numpy as np
from pathlib import Path
from collections import Counter

from sklearn.ensemble import IsolationForest
from sklearn.preprocessing import LabelEncoder, StandardScaler
from sklearn.metrics import precision_score, recall_score, f1_score

# ── 配置 ──────────────────────────────────────────────────────────────────────
DATA_FILE  = Path(__file__).parent / "behavior_data.csv"
MODEL_DIR  = Path(__file__).parent / "models"
MODEL_FILE = MODEL_DIR / "anomaly_model.joblib"
ENC_FILE   = MODEL_DIR / "anomaly_encoder.joblib"
SCALER_FILE= MODEL_DIR / "anomaly_scaler.joblib"
META_FILE  = MODEL_DIR / "anomaly_meta.json"

# IsolationForest 超参数
# contamination=0.15 对应数据集中约 15% 的异常比例（与 gen_behavior_data.py 一致）。
# 在生产环境中，该值应基于实际标注数据中的异常比例重新调整。
# 若无标注数据，可使用 contamination='auto' 让模型自动估计。
# n_estimators=200 比默认 100 更稳定
IF_CONTAMINATION = float(os.environ.get("IF_CONTAMINATION", "0.15"))
IF_N_ESTIMATORS  = int(os.environ.get("IF_N_ESTIMATORS", "200"))
IF_RANDOM_STATE  = 42

CATEGORICAL_COLS = ["department", "ai_service"]
NUMERIC_COLS = [
    "hour_of_day", "day_of_week", "message_length",
    "topic_code", "session_duration_min", "is_new_service",
]


def load_data(filepath: Path) -> tuple:
    """加载 CSV 数据，返回 (records, labels) 两个列表。"""
    if not filepath.exists():
        raise FileNotFoundError(
            f"数据文件不存在: {filepath}\n"
            "请先运行: python gen_behavior_data.py"
        )
    records = []
    labels = []
    with open(filepath, newline="", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            records.append(row)
            labels.append(int(row.get("is_anomaly", 0)))
    print(f"[数据] 加载 {len(records)} 条记录，其中异常 {sum(labels)} 条")
    return records, labels


def build_features(records: list, encoders: dict = None, scaler=None, fit: bool = True):
    """
    将原始记录转换为特征矩阵。

    参数：
      records  — 原始字典列表
      encoders — 已训练的 LabelEncoder 字典（推理时使用）
      scaler   — 已训练的 StandardScaler（推理时使用）
      fit      — True 表示训练阶段（fit_transform），False 表示推理阶段（transform）

    返回：(X, encoders, scaler)
    """
    if encoders is None:
        encoders = {}

    # ── 分类特征编码（LabelEncoder）────────────────────────────────────────
    cat_arrays = []
    for col in CATEGORICAL_COLS:
        vals = [r.get(col, "unknown") for r in records]
        if fit:
            enc = LabelEncoder()
            # 添加 "unknown" 以处理推理时的未见类别
            all_vals = list(set(vals)) + ["unknown"]
            enc.fit(all_vals)
            encoders[col] = enc
        else:
            enc = encoders[col]
            # 对未见类别映射为 "unknown"
            known = set(enc.classes_)
            vals = [v if v in known else "unknown" for v in vals]
        cat_arrays.append(enc.transform(vals).reshape(-1, 1))

    # ── 数值特征 ──────────────────────────────────────────────────────────
    num_arrays = []
    for col in NUMERIC_COLS:
        arr = np.array([float(r.get(col, 0)) for r in records]).reshape(-1, 1)
        num_arrays.append(arr)

    # ── 合并特征矩阵 ──────────────────────────────────────────────────────
    X = np.hstack(cat_arrays + num_arrays).astype(float)

    # ── 标准化（StandardScaler）──────────────────────────────────────────
    if fit:
        scaler = StandardScaler()
        X = scaler.fit_transform(X)
    else:
        X = scaler.transform(X)

    return X, encoders, scaler


def evaluate(model, X, y_true):
    """使用孤立森林评估模型（将 -1 映射为 1=异常，1 映射为 0=正常）。"""
    raw_pred = model.predict(X)
    y_pred = (raw_pred == -1).astype(int)  # -1 → 异常=1, 1 → 正常=0

    cnt = Counter(y_pred)
    print(f"  预测结果：正常={cnt[0]}，异常={cnt[1]}")

    if sum(y_true) > 0:
        p = precision_score(y_true, y_pred, zero_division=0)
        r = recall_score(y_true, y_pred, zero_division=0)
        f = f1_score(y_true, y_pred, zero_division=0)
        print(f"  精确率 Precision: {p:.4f}")
        print(f"  召回率 Recall:    {r:.4f}")
        print(f"  F1 Score:        {f:.4f}")
        return {"precision": round(p, 4), "recall": round(r, 4), "f1": round(f, 4)}
    return {}


def train():
    """完整训练流程：加载数据 → 特征工程 → 训练模型 → 评估 → 保存。"""
    print("=" * 60)
    print("AegisAI 行为异常检测模型训练")
    print("=" * 60)

    # 1. 加载数据
    records, labels = load_data(DATA_FILE)
    y = np.array(labels)

    # 2. 特征工程（训练模式）
    print("\n[特征] 构建特征矩阵...")
    X, encoders, scaler = build_features(records, fit=True)
    print(f"  特征矩阵形状: {X.shape}")
    feature_names = CATEGORICAL_COLS + NUMERIC_COLS
    print(f"  特征列: {feature_names}")

    # 3. 训练孤立森林（仅使用正常样本，以避免异常样本污染基线）
    print(f"\n[训练] IsolationForest (n_estimators={IF_N_ESTIMATORS}, contamination={IF_CONTAMINATION})...")
    # 仅用"正常"样本来建立行为基线
    X_normal = X[y == 0]
    print(f"  训练样本（正常行为）: {len(X_normal)} 条")

    model = IsolationForest(
        n_estimators=IF_N_ESTIMATORS,
        contamination=IF_CONTAMINATION,
        random_state=IF_RANDOM_STATE,
        n_jobs=-1,
    )
    model.fit(X_normal)
    print("  训练完成 ✓")

    # 4. 全量评估
    print("\n[评估] 对全量数据（正常+异常）评估...")
    metrics = evaluate(model, X, y.tolist())

    # 5. 保存模型文件
    MODEL_DIR.mkdir(parents=True, exist_ok=True)
    joblib.dump(model,   MODEL_FILE)
    joblib.dump(encoders, ENC_FILE)
    joblib.dump(scaler,  SCALER_FILE)

    # 6. 保存元信息（API 推理时使用）
    # 收集所有出现过的 AI 服务名称，供 is_new_service 判断
    all_services = list(set(r.get("ai_service", "") for r in records))
    meta = {
        "feature_names": feature_names,
        "categorical_cols": CATEGORICAL_COLS,
        "numeric_cols": NUMERIC_COLS,
        "known_ai_services": all_services,
        "if_contamination": IF_CONTAMINATION,
        "if_n_estimators": IF_N_ESTIMATORS,
        "training_samples": len(records),
        "normal_samples": int(sum(y == 0)),
        "anomaly_samples": int(sum(y == 1)),
        "evaluation_metrics": metrics,
        "model_version": "1.0.0",
    }
    with open(META_FILE, "w", encoding="utf-8") as f:
        json.dump(meta, f, ensure_ascii=False, indent=2)

    print(f"\n[保存] 模型文件已保存至 {MODEL_DIR}/")
    print(f"  anomaly_model.joblib   ({MODEL_FILE.stat().st_size // 1024} KB)")
    print(f"  anomaly_encoder.joblib ({ENC_FILE.stat().st_size // 1024} KB)")
    print(f"  anomaly_scaler.joblib  ({SCALER_FILE.stat().st_size // 1024} KB)")
    print(f"  anomaly_meta.json")
    print("\n训练完成 ✓  模型可通过 POST /api/anomaly/check 使用")
    return meta


if __name__ == "__main__":
    train()
