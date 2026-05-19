package fr.upjv.lesombresduson.ui.game.lum

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import fr.upjv.lesombresduson.R
import fr.upjv.lesombresduson.data.remote.FirebaseHelper
import fr.upjv.lesombresduson.data.remote.RealtimeHelper
import fr.upjv.lesombresduson.manager.sensor.CameraManager
import fr.upjv.lesombresduson.ui.StartChoiseCharacter
import java.io.ByteArrayOutputStream

class LumGameActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var filterOverlay: ImageView
    private lateinit var textDiseaseName: TextView
    private lateinit var textInstruction: TextView
    private lateinit var cameraManager: CameraManager
    private lateinit var viewModel: LumGameViewModel

    // Variables pour l'utilisateur
    private var userId: String? = null
    private var characterName: String = "Lum (cécité partielle)"

    /**
     * Demande la permission de la caméra.
     */
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            cameraManager.startCamera()
        } else {
            Toast.makeText(this, "Permission caméra refusée.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gameplay_lum)

        // Connexion du ViewModel à cette activité
        viewModel = ViewModelProvider(this).get(LumGameViewModel::class.java)

        viewFinder = findViewById(R.id.viewFinder)
        filterOverlay = findViewById(R.id.filter_overlay)
        textDiseaseName = findViewById(R.id.text_disease_name)
        textInstruction = findViewById(R.id.text_instruction)

        val btnAnalyze = findViewById<Button>(R.id.button_analyze)
        val btnBack = findViewById<Button>(R.id.button_back)
        val btnStart = findViewById<Button>(R.id.button_start)

        cameraManager = CameraManager(this, this, viewFinder)
        RealtimeHelper.init(this)

        val savedVisionIndex = intent.getIntExtra("VISION_INDEX", -1)
        userId = intent.getStringExtra("USER_ID")
        characterName = intent.getStringExtra("CHARACTER_NAME") ?: "Lum (cécité partielle)"

        btnBack.setOnClickListener {
            val intent = Intent(this, StartChoiseCharacter::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Fonction locale pour mettre à jour l'affichage visuel depuis le ViewModel
        fun updateVisionUI() {
            filterOverlay.setImageResource(VisionData.filters[viewModel.currentFilterIndex])
            textDiseaseName.text = VisionData.diseaseNames[viewModel.currentFilterIndex]
        }

        btnAnalyze.setOnClickListener {
            val bitmap = viewFinder.bitmap
            if (bitmap != null) {
                Toast.makeText(this, "Analyse en cours par l'IA...", Toast.LENGTH_SHORT).show()
                btnAnalyze.isEnabled = false

                val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 480, 640, true)
                val baos = ByteArrayOutputStream()
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
                val base64Image = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)

                // On envoie la couleur stockée dans le ViewModel
                RealtimeHelper.sendImageForColorDetection(base64Image, viewModel.targetColorToFind)
            } else {
                Toast.makeText(this, "Erreur : Impossible de capturer l'image.", Toast.LENGTH_SHORT).show()
                btnAnalyze.isEnabled = true
            }
        }

        // === LOGIQUE DE JEU ===
        if (savedVisionIndex != -1) {
            // MODE "CONTINUER" : On reprend là où le joueur s'était arrêté
            viewModel.currentFilterIndex = savedVisionIndex
            updateVisionUI()

            btnStart.visibility = View.GONE
            filterOverlay.setOnClickListener(null)

            userId?.let { uid ->
                try {
                    FirebaseHelper.getInstance().getGameData(uid, characterName, object : FirebaseHelper.GameDataCallback {
                        override fun onDataLoaded(gameData: Map<String, Any>?) {
                            viewModel.currentScore = (gameData?.get("score_global") as? Number)?.toInt() ?: 0
                            startGameplay(btnAnalyze)
                        }

                        override fun onFailure(e: Exception) {
                            startGameplay(btnAnalyze)
                        }
                    })
                } catch (e: SecurityException) {
                    Toast.makeText(this, "Mode hors-ligne", Toast.LENGTH_SHORT).show()
                    viewModel.currentScore = 0
                    startGameplay(btnAnalyze)
                }
            } ?: startGameplay(btnAnalyze)

        } else {
            // MODE "NOUVELLE PARTIE" : On FORCE le départ à l'index 0 (Vision Normale)
            viewModel.currentFilterIndex = 0
            if (viewModel.targetColorToFind.isEmpty()) {
                viewModel.currentScore = 0
            }
            updateVisionUI()

            btnStart.setOnClickListener {
                btnStart.visibility = View.GONE
                filterOverlay.setOnClickListener(null)

                userId?.let {
                    FirebaseHelper.getInstance().updateGameProgress(it, characterName, "visionIndex", viewModel.currentFilterIndex)
                    FirebaseHelper.getInstance().updateGameProgress(it, characterName, "score_global", viewModel.currentScore)
                }

                startGameplay(btnAnalyze)
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraManager.startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    /**
     * Lance la mécanique de recherche de couleur avec minuteur adaptatif
     */
    private fun startGameplay(btnAnalyze: Button) {
        btnAnalyze.visibility = View.VISIBLE
        textInstruction.visibility = View.VISIBLE

        if (viewModel.targetColorToFind.isEmpty()) {
            viewModel.generateNewColor()
        }

        // 1. Branchement des actions du Chronomètre
        viewModel.onTickCallback = { secondsLeft ->
            // Changement dynamique du texte à chaque seconde
            textInstruction.text = "⏰ Temps restant : ${secondsLeft}s\nScore : ${viewModel.currentScore}\n\n🎯 Cherchez un objet : ${viewModel.targetColorToFind}"
        }

        viewModel.onTimeUpCallback = {
            // Comportement en cas d'échec de temps : pénalité légère et nouvelle couleur
            viewModel.currentScore = (viewModel.currentScore - 2).coerceIn(0, Int.MAX_VALUE)

            userId?.let {
                FirebaseHelper.getInstance().updateGameProgress(it, characterName, "score_global", viewModel.currentScore)
            }

            Toast.makeText(this, "⏱️ Temps écoulé ! La fatigue visuelle s'installe... (-2 pts)", Toast.LENGTH_LONG).show()

            // On relance une manche
            viewModel.generateNewColor()
            viewModel.startTimerForFilter()
        }

        // 2. DÉMARRAGE DU TIMER POUR CETTE MANCHE
        viewModel.startTimerForFilter()

        // 3. Écouteur Firebase IA
        RealtimeHelper.listenForColorDetectionResult { isWin, message ->
            btnAnalyze.isEnabled = true

            if (isWin) {
                // VICTOIRE : On coupe immédiatement le chrono pendant la transition
                viewModel.stopTimer()

                val pointsGagnes = viewModel.addPointsForWin()

                // On sauvegarde le score actuel du joueur
                userId?.let {
                    FirebaseHelper.getInstance().updateGameProgress(it, characterName, "score_global", viewModel.currentScore)
                }

                // On vérifie s'il y a un filtre suivant et on met à jour l'index
                val isParcoursFini = viewModel.advanceToNextFilter()

                // On affiche la popup pédagogique qui présente le PROCHAIN handicap
                showSensitizationDialog(pointsGagnes, isParcoursFini) {
                    // Ce code s'exécute quand le joueur clique sur "Relever le défi"

                    // 1. On applique visuellement le nouveau filtre et le nouveau nom
                    filterOverlay.setImageResource(VisionData.filters[viewModel.currentFilterIndex])
                    textDiseaseName.text = VisionData.diseaseNames[viewModel.currentFilterIndex]

                    // 2. On sauvegarde la progression de l'index du filtre dans Firebase
                    userId?.let { uid ->
                        FirebaseHelper.getInstance().updateGameProgress(uid, characterName, "visionIndex", viewModel.currentFilterIndex)
                    }

                    // 3. On génère la nouvelle couleur et relance le jeu
                    viewModel.generateNewColor()
                    viewModel.startTimerForFilter()
                }

            } else {
                // Le joueur s'est trompé, pas de pénalité de temps directe, il continue à chercher
                Toast.makeText(this, "Ce n'est pas la bonne couleur... $message", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Affiche la popup de sensibilisation expliquant le handicap
     */
    private fun showSensitizationDialog(pointsGagnes: Int, isGameOver: Boolean, onNextMancheRequested: () -> Unit) {
        if (isFinishing || isDestroyed) {
            // Si l'activité est en train de se fermer ou détruite, on quitte la fonction
            // immédiatement sans tenter d'ouvrir le Dialog pour éviter le crash.
            return
        }

        val currentIndex = viewModel.currentFilterIndex
        val diseaseName = VisionData.diseaseNames[currentIndex]
        val definition = VisionData.diseaseDefinitions[currentIndex]
        val realImpact = VisionData.diseaseRealImpacts[currentIndex]

        val title = if (isGameOver) "🏆 Parcours Terminé !" else "🎯 Étape Franchie !"

        val messageText = StringBuilder().apply {
            append("✨ Bravo ! Vous avez surmonté l'épreuve précédente.\n")
            append("💰 Points remportés : +$pointsGagnes pts\n")
            append("📊 Score total : ${viewModel.currentScore} pts\n\n")

            if (!isGameOver) {
                append("⚠️ ÉVOLUTION DU HANDICAP ⚠️\n")
                append("Votre vision change... Prochaine étape : $diseaseName\n\n")
                append("🔬 QU'EST-CE QUE C'EST ?\n")
                append("$definition\n\n")
                append("👁️ L'IMPACT AU QUOTIDIEN :\n")
                append(realImpact)
            } else {
                append("🎉 Félicitations ! Vous avez complété tout le parcours de sensibilisation et compris les difficultés visuelles quotidiennes.")
            }
        }.toString()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(messageText)
            .setPositiveButton(if (isGameOver) "Quitter" else "Relever le défi") { dialog, _ ->
                if (isGameOver) {
                    // Retour au menu principal ou choix des personnages
                    val intent = Intent(this, StartChoiseCharacter::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    onNextMancheRequested()
                }
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Arrêt du timer lorsque l'activité est mise en arrière-plan.
     */
    override fun onPause() {
        super.onPause()
        // On stoppe le timer si l'activité passe en arrière-plan (appel visio, écran verrouillé...)
        viewModel.stopTimer()
    }

    /**
     * Relance le timer lorsque l'activité est réactivée.
     */
    override fun onResume() {
        super.onResume()
        // Si le jeu était déjà lancé et qu'on revient dessus, on relance le timer
        if (viewModel.targetColorToFind.isNotEmpty() && textInstruction.visibility == View.VISIBLE) {
            viewModel.startTimerForFilter()
        }
    }

    /**
     * Arrêt propre de la caméra lorsque l'activité est détruite.
     */
    override fun onDestroy() {
        // Arrêt propre de la caméra
        cameraManager.shutdown()
        // Arrêt propre du timer
        viewModel.stopTimer()
        super.onDestroy()
    }
}