"""
============================================================
  SMART DRUG RECOMMENDER TRAINING
  Multi-label classification of diagnosis to drug list
============================================================
"""

import pandas as pd
import numpy as np
import os
import joblib
import matplotlib.pyplot as plt
from sklearn.model_selection import train_test_split
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.preprocessing import MultiLabelBinarizer
from sklearn.multiclass import OneVsRestClassifier
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score, hamming_loss

print("=" * 60)
print("  STEP 1: Load and Clean Drug Dataset")
print("=" * 60)

df = pd.read_csv("dataset/drug_recommendation.csv")
print(f"Dataset shape: {df.shape}")

# Clean text
df["diagnosis"] = df["diagnosis"].str.strip().str.lower()
df.drop_duplicates(inplace=True)
print(f"After dropping duplicates: {len(df)} rows")

# Convert comma-separated drugs to list
df["drugs_list"] = df["recommended_drugs"].apply(lambda x: [d.strip() for d in x.split(",")])

print("Sample rows:")
print(df[["diagnosis", "drugs_list"]].head())

# ═══════════════════════════════════════════════════════════
#  STEP 2: ENCODE LABELS AND FEATURES
# ═══════════════════════════════════════════════════════════

print("\n" + "=" * 60)
print("  STEP 2: Feature Engineering (TF-IDF & MLB)")
print("=" * 60)

# MultiLabelBinarizer for drugs
mlb = MultiLabelBinarizer()
y = mlb.fit_transform(df["drugs_list"])
print(f"Total Unique Drugs (Classes): {len(mlb.classes_)}")

# TF-IDF for diagnosis
tfidf = TfidfVectorizer(max_features=5000, stop_words="english", ngram_range=(1, 2))
X = tfidf.fit_transform(df["diagnosis"])
print(f"TF-IDF Shape: {X.shape}")

# Train/Test Split
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# ═══════════════════════════════════════════════════════════
#  STEP 3: TRAIN MULTI-LABEL MODEL
# ═══════════════════════════════════════════════════════════

print("\n" + "=" * 60)
print("  STEP 3: Training Random Forest Multi-Label")
print("=" * 60)

# RandomForest inherently supports multiclass-multioutput
clf = RandomForestClassifier(n_estimators=150, random_state=42, n_jobs=-1)
clf.fit(X_train, y_train)

# ═══════════════════════════════════════════════════════════
#  STEP 4: EVALUATION
# ═══════════════════════════════════════════════════════════

print("\n" + "=" * 60)
print("  STEP 4: Evaluation")
print("=" * 60)

y_pred = clf.predict(X_test)

acc = accuracy_score(y_test, y_pred)
hl = hamming_loss(y_test, y_pred)

print(f"Exact Match Ratio (Accuracy): {acc:.4f}")
print(f"Hamming Loss (lower is better): {hl:.4f}")

# ═══════════════════════════════════════════════════════════
#  STEP 5: SAVE MODEL
# ═══════════════════════════════════════════════════════════

print("\n" + "=" * 60)
print("  STEP 5: Saving Model")
print("=" * 60)

os.makedirs("model", exist_ok=True)
joblib.dump(clf, "model/recommender_model.pkl")
joblib.dump(tfidf, "model/recommender_tfidf.pkl")
joblib.dump(mlb, "model/recommender_mlb.pkl")

print("Saved: model/recommender_model.pkl")
print("Saved: model/recommender_tfidf.pkl")
print("Saved: model/recommender_mlb.pkl")

print("\n" + "=" * 60)
print("  DRUG RECOMMENDER TRAINING COMPLETE!")
print("=" * 60)
