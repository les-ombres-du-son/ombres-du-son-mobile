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
        val btnChangeFilter = findViewById<Button>(R.id.button_change_filter)
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

        btnChangeFilter.setOnClickListener {
            viewModel.nextFilter() // On dit au ViewModel de changer l'index
            updateVisionUI()       // On met à jour l'écran
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
            // MODE "CONTINUER"
            viewModel.currentFilterIndex = savedVisionIndex
            updateVisionUI()

            btnStart.visibility = View.GONE
            btnChangeFilter.visibility = View.GONE
            filterOverlay.setOnClickListener(null)

            userId?.let { uid ->
                FirebaseHelper.getInstance().getGameData(uid, characterName, object : FirebaseHelper.GameDataCallback {
                    override fun onDataLoaded(gameData: Map<String, Any>?) {
                        viewModel.currentScore = (gameData?.get("score_global") as? Number)?.toInt() ?: 0
                        startGameplay(btnAnalyze)
                    }

                    override fun onFailure(e: Exception) {
                        startGameplay(btnAnalyze)
                    }
                })
            } ?: startGameplay(btnAnalyze)

        } else {
            // MODE "NOUVELLE PARTIE"
            if (viewModel.targetColorToFind.isEmpty()) {
                viewModel.currentScore = 0 // Sécurité au démarrage
            }
            updateVisionUI()

            btnStart.setOnClickListener {
                btnStart.visibility = View.GONE
                btnChangeFilter.visibility = View.GONE
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
     * Lance la mécanique de recherche de couleur
     */
    private fun startGameplay(btnAnalyze: Button) {
        btnAnalyze.visibility = View.VISIBLE
        textInstruction.visibility = View.VISIBLE

        // Si la couleur est vide (au tout premier lancement de la partie)
        if (viewModel.targetColorToFind.isEmpty()) {
            viewModel.generateNewColor()
        }

        // Mise à jour de l'UI avec les données du ViewModel
        textInstruction.text = "Score : ${viewModel.currentScore}\nCherchez un objet : ${viewModel.targetColorToFind}"

        RealtimeHelper.listenForColorDetectionResult { isWin, message ->
            btnAnalyze.isEnabled = true

            if (isWin) {
                // Le ViewModel s'occupe de faire le calcul et de stocker le score !
                val pointsGagnes = viewModel.addPointsForWin()

                userId?.let {
                    FirebaseHelper.getInstance().updateGameProgress(it, characterName, "score_global", viewModel.currentScore)
                }

                Toast.makeText(this, "+$pointsGagnes points ! $message", Toast.LENGTH_LONG).show()

                // On dit au ViewModel de préparer la prochaine couleur
                viewModel.generateNewColor()
                textInstruction.text = "Score : ${viewModel.currentScore}\nBravo ! Trouvez : ${viewModel.targetColorToFind}"
            } else {
                Toast.makeText(this, "Raté... $message", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Détruit la caméra lorsque l'activité est détruite.
     */
    override fun onDestroy() {
        super.onDestroy()
        cameraManager.shutdown()
    }
}