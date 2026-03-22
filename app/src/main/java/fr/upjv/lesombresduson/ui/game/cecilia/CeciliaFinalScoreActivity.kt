package fr.upjv.lesombresduson.ui.game.cecilia

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import fr.upjv.lesombresduson.R
import fr.upjv.lesombresduson.ui.game.cecilia.util.BackGameActivity

/**
 * Écran de bilan final pour le personnage Cécilia.
 * Récupère l'ensemble des scores sauvegardés dans Firestore pour la session en cours
 */
class CeciliaFinalScoreActivity : BackGameActivity() {

    private lateinit var btnBackMainMenu: Button
    private lateinit var tvScoresTitle: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var llScoresContainer: LinearLayout
    private lateinit var tvAverageScoreSummary: TextView
    private lateinit var cardGlobalScore: CardView
    private lateinit var tvWebsiteLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cecilia_final_score)

        btnBackMainMenu = findViewById(R.id.button_back_menu)
        tvScoresTitle = findViewById(R.id.tv_scores_title)
        progressBar = findViewById(R.id.progress_bar_scores)
        llScoresContainer = findViewById(R.id.ll_scores_container)
        tvAverageScoreSummary = findViewById(R.id.tv_average_score_summary)
        cardGlobalScore = findViewById(R.id.card_global_score)
        tvWebsiteLink = findViewById(R.id.tv_website_link)

        setupBackButton(btnBackMainMenu)

        // Configuration du lien web
        tvWebsiteLink.setOnClickListener {
            val url = "https://www.upjv.fr/"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        // Affichage du loader pendant la requête réseau
        progressBar.visibility = View.VISIBLE
        llScoresContainer.visibility = View.GONE
        cardGlobalScore.visibility = View.GONE

        fetchScoresFromFirebase()
    }

    /**
     * Interroge Firestore pour récupérer les statistiques de chaque niveau joué.
     * Construit ensuite l'interface visuelle dynamiquement
     * et préparation du texte complet qui sera lu par la synthèse vocale.
     */
    private fun fetchScoresFromFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            showErrorAndSpeak("Erreur : Utilisateur non connecté.")
            return
        }

        val characterName = intent.getStringExtra("CHARACTER_NAME") ?: "Cécilia (cécité totale)"
        val db = FirebaseFirestore.getInstance()

        // Nettoyage de sécurité avant d'ajouter les nouvelles vues
        llScoresContainer.removeAllViews()

        // Requête ciblant spécifiquement la sous-collection des statistiques de fin de niveau
        db.collection("Users")
            .document(userId)
            .collection("Games")
            .document(characterName)
            .collection("LevelStats")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    showErrorAndSpeak("Aucun score trouvé pour cette session.")
                    return@addOnSuccessListener
                }

                var totalScore = 0
                var levelsCount = 0
                val speechBuilder = StringBuilder("Bilan final de l'expérience de Cécilia. ")

                // Inflater utilisé pour convertir le fichier XML "item_score_level" en objet View
                val inflater = LayoutInflater.from(this)

                for (document in documents) {
                    val levelId = document.id
                    val score = document.getLong("score_global")?.toInt() ?: 0
                    val profil = document.getString("profil") ?: "Inconnu"

                    totalScore += score
                    levelsCount++

                    // Instanciation dynamique d'une carte de score pour ce niveau
                    val itemView = inflater.inflate(R.layout.item_score_level, llScoresContainer, false)

                    val tvIcon = itemView.findViewById<TextView>(R.id.tv_item_icon)
                    val tvLevelName = itemView.findViewById<TextView>(R.id.tv_item_level_name)
                    val tvProfile = itemView.findViewById<TextView>(R.id.tv_item_profile)
                    val tvScoreValue = itemView.findViewById<TextView>(R.id.tv_item_score_value)

                    tvIcon.text = when {
                        levelId.contains("Intro", true) -> "🎬"
                        levelId.contains("5", true) -> "🎓"
                        else -> "📍"
                    }

                    // Formatage du texte brut de Firestore pour l'affichage UI
                    tvLevelName.text = levelId.replace("Niveau", "Niveau ")
                    tvProfile.text = "Profil : $profil"
                    tvScoreValue.text = score.toString()

                    // Code couleur (Vert/Jaune/Rouge) basé sur la performance
                    when {
                        score >= 90 -> tvScoreValue.setTextColor(Color.parseColor("#4CAF50"))
                        score < 50 -> tvScoreValue.setTextColor(Color.parseColor("#F44336"))
                        else -> tvScoreValue.setTextColor(Color.parseColor("#FFC107"))
                    }

                    llScoresContainer.addView(itemView)

                    // Construction progressive du script pour la voix off
                    speechBuilder.append("Pour ${tvLevelName.text}, vous avez obtenu $score sur cent. ")
                }

                // Calcul et affichage du score moyen global
                val averageScore = if (levelsCount > 0) totalScore / levelsCount else 0
                tvAverageScoreSummary.text = "$averageScore/100"

                // Couleur du score global selon le résultat
                when {
                    averageScore >= 90 -> tvAverageScoreSummary.setTextColor(Color.parseColor("#4CAF50"))
                    averageScore < 50 -> tvAverageScoreSummary.setTextColor(Color.parseColor("#F44336"))
                    else -> tvAverageScoreSummary.setTextColor(Color.parseColor("#FFC107"))
                }

                speechBuilder.append("Votre score moyen global est de $averageScore sur cent. Félicitations pour avoir terminé cette formation de sensibilisation !")

                displayAndSpeakScores(speechBuilder.toString())
            }
            .addOnFailureListener { e ->
                android.util.Log.e("ScoreBilan", "Erreur Firestore: ", e)
                showErrorAndSpeak("Erreur lors de la récupération de vos résultats.")
            }
    }

    /**
     * Bascule l'état de l'interface (masque le loader, affiche les scores)
     * et déclenche la lecture du bilan audio.
     *
     * @param speechText Le texte complet généré à partir des données Firebase.
     */
    private fun displayAndSpeakScores(speechText: String) {
        progressBar.visibility = View.GONE
        cardGlobalScore.visibility = View.VISIBLE
        llScoresContainer.visibility = View.VISIBLE

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            audioManager.speak(speechText, android.speech.tts.TextToSpeech.QUEUE_FLUSH, "FINAL_SCORE_ID")
        }, 800)
    }

    /**
     * Gère visuellement et vocalement les erreurs (réseau, authentification).
     *
     * @param errorMessage Le message d'erreur à afficher et à lire.
     */
    private fun showErrorAndSpeak(errorMessage: String) {
        progressBar.visibility = View.GONE

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            audioManager.speak(errorMessage, android.speech.tts.TextToSpeech.QUEUE_FLUSH, "ERROR_ID")
        }, 800)
    }

    /**
     * Libère les ressources du Text-to-Speech
     * pour éviter les fuites de mémoire.
     */
    override fun onDestroy() {
        super.onDestroy()
        audioManager.release()
    }
}