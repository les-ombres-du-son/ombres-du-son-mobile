# 🎧 Les Ombres du Son

Projet d'application mobile Android immersive développé dans le cadre du Master CCM 2 à l'INSSET (Université de Picardie Jules Verne).

---

## 🌍 Contexte et Vision du Projet
De nos jours, de plus en plus de personnes sont touchées par des troubles de la vue, qu'il s'agisse de cécité partielle ou totale. 

L'objectif principal de **Les Ombres du Son** est de sensibiliser le grand public à ce handicap à travers une expérience ludique et sensorielle unique.

Le jeu vise à développer l'empathie en faisant comprendre que si les personnes déficientes visuelles rencontrent des défis quotidiens, elles disposent d'outils et de clés pour réussir en toute autonomie. 

L'application combat les stéréotypes : il ne s'agit pas de stigmatiser ou d'infantiliser la personne, mais de comprendre son univers et de valoriser l'entraide et l'inclusion.

---

## 📖 Le Scénario et les Personnages
Le joueur suit le parcours parallèle de deux personnages à travers les situations de la vie quotidienne (maison, rue, supermarché, école) :

*   **Cécilia (Cécité totale)** : Plongé dans le noir absolu, le joueur écoute Cécilia raconter son histoire. Pour progresser, il doit se fier exclusivement à ses autres sens.
*   **Lum (Cécité partielle)** : À travers une série de mini-jeux, le joueur expérimente différents troubles de la vision (vision en tunnel, flou sévère, altération des contrastes) pour comprendre concrètement la gêne occasionnée au quotidien.

---

## 📱 Gameplay & Expérience Joueur (Partie Android)
L'application exploite au maximum les capacités matérielles des smartphones pour remplacer la vue par d'autres canaux sensoriels :

*   **Déplacements par capteurs (Accéléromètre & Gyroscope)** : Le joueur se déplace en inclinant le téléphone (avant/arrière pour marcher, gauche/droite pour tourner). Secouer le téléphone déclenche une interaction.
*   **Audio Spatialisé 3D (SoundPool / ExoPlayer)** : Les indices sonores (bruit d'une voiture, bip d'un four) se déplacent dynamiquement à gauche ou à droite du casque audio pour guider le joueur.
*   **Retours Haptiques (Vibrator API)** : Des vibrations d'intensités variables simulent la proximité d'un obstacle ou un choc direct.
*   **Mécanique du Microphone** : Le joueur peut utiliser le micro pour interagir avec l'environnement (souffler, parler pour attirer l'attention d'un personnage).
*   **Filtres Graphiques** : Reproduction visuelle des pathologies de la vue pour le mode de jeu avec Lum.

---

## ☁️ Infrastructure & Pipeline Analytique (Partie Cloud)
Pour évaluer l'impact pédagogique et analyser les comportements de jeu (courbe d'apprentissage, temps de complétion, collisions), le projet s'appuie sur une architecture Cloud robuste sur **Google Cloud Platform (GCP)**.

### Synthèse des Services GCP utilisés :

| Catégorie | Services GCP | Rôle dans le projet |
| :--- | :--- | :--- |
| **Authentification** | Firebase Auth | Gestion sécurisée des profils et comptes joueurs. |
| **Stockage & Données** | Firestore & Cloud Storage | Stockage NoSQL en temps réel de la progression et des assets audio. |
| **Pipeline Streaming** | Pub/Sub & Dataflow | Collecte et transformation des données de gameplay en temps réel. |
| **Analytique & IA** | BigQuery & Vertex AI | Analyse comportementale et détection des patterns de difficulté. |
| **Visualisation** | Looker Studio / Grafana | Tableaux de bord d'analyse de l'impact social et des performances. |
| **Sécurité & Compute** | Cloud Run & Secret Manager | Hébergement des microservices API et isolation des clés secrètes. |

---

## 🛠️ Stack Technique
- **Langages** : Kotlin & Java 21
- **Gestionnaire de dépendances** : Gradle (Version Catalog - `libs.versions.toml`)
- **API Android majeures** : Android SDK 36 (Min SDK 30), CameraX, Core Sensors
- **CI/CD** : GitHub Actions

---

## 📅 Planning et Organisation du Projet (Gantt)

Afin de mener à bien le développement de l'application et de l'infrastructure Cloud dans les temps impartis, nous avons structuré notre travail selon le diagramme de Gantt suivant :

<img width="878" height="807" alt="Diagramme de Gantt du projet Les Ombres du Son" src="https://github.com/user-attachments/assets/12850715-d3ac-444e-90e5-36ba89f3f764" />

---

## 🤖 Téléchargement direct de l'APK (CI/CD via GitHub Actions)

Grâce à notre pipeline d'intégration continue, une version de l'application est compilée automatiquement à chaque mise à jour du code. 

Vous pouvez tester l'application directement sur votre téléphone sans ouvrir Android Studio :

1. En haut de cette page GitHub, cliquez sur l'onglet **"Actions"**.
2. Dans la liste des exécutions, cliquez sur le tout dernier workflow (indiqué par une pastille verte ✅).
3. Faites défiler la page de résumé tout en bas jusqu'à la section **"Artifacts"**.
4. Cliquez sur le lien **`app-release-apk`** pour télécharger le dossier compressé contenant l'APK d'installation.

---

## ⚙️ Configuration et Installation Locale (Pour les développeurs)

Si vous souhaitez modifier le projet ou le lancer depuis votre environnement de développement :

### 1. Clonage du dépôt
```bash
git clone [https://github.com/VOTRE_PSEUDO/Les-Ombres-du-Son.git](https://github.com/VOTRE_PSEUDO/Les-Ombres-du-Son.git)
cd Les-Ombres-du-Son
```