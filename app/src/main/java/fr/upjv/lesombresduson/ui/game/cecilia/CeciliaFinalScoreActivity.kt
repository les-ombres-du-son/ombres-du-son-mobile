package fr.upjv.lesombresduson.ui.game.cecilia

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import fr.upjv.lesombresduson.R
import fr.upjv.lesombresduson.data.model.LevelStat
import fr.upjv.lesombresduson.ui.game.cecilia.util.BackGameActivity
import fr.upjv.lesombresduson.ui.game.cecilia.viewmodel.CeciliaScoreViewModel

/**
 * Écran de bilan final pour le personnage Cécilia.
 * Récupère l'ensemble des scores sauvegardés dans Firestore pour la session en cours
 */
class CeciliaFinalScoreActivity : BackGameActivity() {

    private lateinit var viewModel: CeciliaScoreViewModel

    private lateinit var btnBackMainMenu: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var llScoresContainer: LinearLayout
    private lateinit var tvAverageScoreSummary: TextView
    private lateinit var cardGlobalScore: CardView
    private lateinit var tvWebsiteLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cecilia_final_score)

        initViews()

        // Initialisation du ViewModel
        viewModel = ViewModelProvider(this).get(CeciliaScoreViewModel::class.java)

        // Gérer la visibilité du ProgressBar en fonction du chargement
        viewModel.isLoading.observe(this) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        // Gérer l'affichage des scores quand ils arrivent
        viewModel.levelStats.observe(this) { stats ->
            if (stats.isNotEmpty()) {
                displayStats(stats)
            }
        }

        // Gérer le score moyen global
        viewModel.averageScore.observe(this) { avg ->
            tvAverageScoreSummary.text = "$avg/100"
            tvAverageScoreSummary.setTextColor(getScoreColor(avg))
            cardGlobalScore.visibility = View.VISIBLE
        }

        // Gérer la synthèse vocale
        viewModel.speechText.observe(this) { text ->
            // Petit délai pour laisser l'UI s'afficher avant de parler
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                audioManager.speak(text, TextToSpeech.QUEUE_FLUSH, "FINAL_SCORE_ID")
            }, 800)
        }

        // Gérer les erreurs
        viewModel.error.observe(this) { errorMessage ->
            audioManager.speak(errorMessage, TextToSpeech.QUEUE_FLUSH, "ERROR_ID")
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val charName = intent.getStringExtra("CHARACTER_NAME") ?: "Cécilia (cécité totale)"

        if (userId != null) {
            viewModel.loadScores(userId, charName)
        }
    }

    /**
     * Initialise la vue de l'interface du score utilisateur.
     */
    private fun initViews() {
        btnBackMainMenu = findViewById(R.id.button_back_menu)
        progressBar = findViewById(R.id.progress_bar_scores)
        llScoresContainer = findViewById(R.id.ll_scores_container)
        tvAverageScoreSummary = findViewById(R.id.tv_average_score_summary)
        cardGlobalScore = findViewById(R.id.card_global_score)
        tvWebsiteLink = findViewById(R.id.tv_website_link)

        setupBackButton(btnBackMainMenu)

        tvWebsiteLink.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.upjv.fr/"))
            startActivity(intent)
        }
    }

    /**
     * Affiche les statistiques de chaque niveau dans la vue.
     */
    private fun displayStats(stats: List<LevelStat>) {
        llScoresContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)

        stats.forEach { stat ->
            val itemView = inflater.inflate(R.layout.item_score_level, llScoresContainer, false)

            val tvIcon = itemView.findViewById<TextView>(R.id.tv_item_icon)
            val tvLevelName = itemView.findViewById<TextView>(R.id.tv_item_level_name)
            val tvProfile = itemView.findViewById<TextView>(R.id.tv_item_profile)
            val tvScoreValue = itemView.findViewById<TextView>(R.id.tv_item_score_value)

            // Logique d'icône
            tvIcon.text = when {
                stat.id.contains("Intro", true) -> "🎬"
                stat.id.contains("5", true) -> "🎓"
                else -> "📍"
            }

            tvLevelName.text = stat.id.replace("Niveau", "Niveau ")
            tvProfile.text = "Profil : ${stat.profil}"
            tvScoreValue.text = stat.score.toString()
            tvScoreValue.setTextColor(getScoreColor(stat.score))

            llScoresContainer.addView(itemView)
        }
        llScoresContainer.visibility = View.VISIBLE
    }

    /**
     * Fonction utilitaire pour centraliser la logique des couleurs
     */
    private fun getScoreColor(score: Int): Int {
        return when {
            score >= 90 -> Color.parseColor("#4CAF50") // Vert
            score < 50 -> Color.parseColor("#F44336")  // Rouge
            else -> Color.parseColor("#FFC107")        // Jaune
        }
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