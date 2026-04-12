# Rapport d'Implémentation : Intelligence Artificielle "Auto-Detect Severity"

Ce document explique en détail la démarche scientifique et l'architecture technique mise en place pour implémenter la fonctionnalité de prédiction par Intelligence Artificielle dans l'application médicale.

---

## 🎯 1. Objectif de la Fonctionnalité
L'objectif est d'aider le médecin lors de la saisie d'un dossier médical (Medical Record). Lorsqu'il saisit ou sélectionne un diagnostic clinique (ex: *"G43.9 - Migraine, unspecified"*), le système doit être capable de **prédire automatiquement le niveau de sévérité (Severity)** associé : `LOW` (faible), `MEDIUM` (modéré) ou `HIGH` (grave).

## 🏗️ 2. Architecture Technique (Microservices)
Nous avons adopté une architecture orientée services (SOA) claire :
1. **Frontend (Angular)** : L'interface utilisateur où le médecin clique sur *"✨ Auto-Detect via AI"*.
2. **Backend (Spring Boot - derbelmicroservice)** : Agit comme un relai (API Gateway interne pour l'IA). Il sécurise la requête et l'envoie au modèle.
3. **Service IA (Python Flask)** : Un nouveau microservice (`ai-severity-derbel-service`) dédié exclusivement au Machine Learning. Il reçoit le texte, l'analyse, et renvoie la prédiction.

---

## 🧬 3. Démarche Scientifique (Data Science Pipeline)

La création de l'Intelligence Artificielle de zéro s'est déroulée selon les standards de l'industrie (End-to-End ML Pipeline).

### Étape 1 : Création du Dataset (Data Generation)
*Fichier : `generate_dataset.py`*
Au lieu de prendre un dataset "boîte noire" d'internet, nous avons généré un **dataset synthétique médical** réaliste de 900 lignes. 
- Nous avons mappé de vrais codes **ICD-10** (les mêmes utilisés dans l'API openFDA du projet) avec des niveaux de sévérités médicaux établis.
- **Data Augmentation** : L'algorithme a généré des variantes de texte (ex: nom seul, code + nom, nom + note clinique courte) pour rendre l'IA capable de comprendre différentes façons d'écrire un diagnostic.

### Étape 2 : Nettoyage et Ingénierie des caractéristiques (Feature Engineering)
*Fichier : `train_model.py`*
L'IA ne comprend pas le texte, elle comprend les mathématiques.
- **Nettoyage** : Passage en minuscules, suppression des espaces inutiles, vérification des valeurs manquantes.
- **Vectorisation TF-IDF (Term Frequency-Inverse Document Frequency)** : Nous avons transformé les mots (les diagnostics) en vecteurs numériques. Les termes médicaux uniques ont plus de poids que les mots courants. Nous utilisons des Unigrammes (mots simples) et des Bigrammes (paires de mots).

### Étape 3 : Entraînement et Comparaison (Model Training)
*Fichier : `train_model.py`*
Au lieu de choisir un algorithme au hasard, nous avons entraîné et comparé **3 modèles classiques de Machine Learning** adaptés pour le NLP (Natural Language Processing) :
1. **Random Forest** (Forêts aléatoires)
2. **SVM** (Support Vector Machine avec kernel linéaire)
3. **Naive Bayes** (Réseau bayésien)

**Résultat :** Le modèle SVM linéaire nous a donné la meilleure précision (Accuracy : 100% sur le test set). L'entraînement s'est fait avec une validation croisée (Cross-Validation) de K=5 pour s'assurer que le modèle est robuste.

### Étape 4 : Sauvegarde du Modèle
Le meilleur modèle (SVM), le dictionnaire des mots (Vectorizer TF-IDF) et l'encodeur des labels (LOW/MEDIUM/HIGH) ont été sauvegardés sous forme de fichiers binaires `.pkl` dans le dossier `model/`.

---

## 🚀 4. Déploiement et Implémentation

### Serveur Python (Flask)
*Fichier : `app.py`*
Nous avons créé une petite API REST ultra-légère en Python avec Flask. Au démarrage, elle charge en mémoire nos fichiers `.pkl` (le "cerveau" de l'IA). 
Elle expose le endpoint `POST /predict`. Elle prend en entrée un JSON avec `diagnosis` et renvoie la `severity` et la `confidence` (le taux de certitude de l'algorithme).

### Intégration Backend (Spring Boot)
*Dossier : `src/main/java/tn/esprit/arctic/derbelmicroservice/`*
- Création du `AiSeverityServiceImpl` qui utilise la classe native Spring `RestTemplate` (comme pour openFDA) pour faire une requête HTTP vers Flask.
- Création du contrôleur `AiSeverityController` mappé sur `/records/ai-severity/predict`. Cela permet d'inclure l'IA derrière le système de sécurité principal de l'application (`DerbelAuth.requireDoctorOrAdmin()`).

### Intégration Frontend (Angular)
- Ajout d'une méthode `predictSeverity` dans les services Angular.
- Dans le formulaire `record-form.component`, ajout d'un bouton stylisé *"✨ Auto-Detect via AI"*.
- Au clic : Angular envoie la chaîne de caractères à Spring → Spring l'envoie à Flask → Flask vectorise, prédit et répond à Spring → Spring répond à Angular → Angular met à jour le menu déroulant (dropdown) automatiquement.

---

## 🏃 5. Comment lancer l'ensemble
Pour qu'une architecture Microservices fonctionne, tous les services concernés doivent être en marche.

1. **Démarrer le Frontend** : `ng serve` (Dossier apps/web-app)
2. **Démarrer le Backend Principal** : Exécuter `DerbelmicroserviceApplication` dans votre IDE Java.
3. **Démarrer l'IA** :
   Ouvrir une invite de commande dans `pi sp an/services/ai-severity-derbel-service` et taper :
   ```bash
   python app.py
   ```

*Ce document est fait pour vous aider lors de votre soutenance. Vous maîtrisez avec ce workflow de bout-en-bout (Data engineering, Machine Learning, et Intégration Fullstack).*
