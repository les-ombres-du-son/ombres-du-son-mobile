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
import fr.upjv.lesombresduson.R
import fr.upjv.lesombresduson.data.remote.FirebaseHelper
import fr.upjv.lesombresduson.data.remote.RealtimeHelper
import fr.upjv.lesombresduson.manager.sensor.CameraManager
import fr.upjv.lesombresduson.ui.StartChoiseCharacter
import java.io.ByteArrayOutputStream
import kotlin.random.Random

class LumGameActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var filterOverlay: ImageView
    private lateinit var textDiseaseName: TextView
    private lateinit var textInstruction: TextView // Nouveau : pour afficher la consigne
    private lateinit var cameraManager: CameraManager

    private var currentFilterIndex = 0
    private var targetColorToFind = ""

    // Liste des couleurs possibles
    private val colorsList = listOf("Rouge", "Bleu", "Vert", "Jaune")

    // Gestionnaire de demande de permission
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            cameraManager.startCamera()
        } else {
            Toast.makeText(this, "Permission caméra refusée. Le gameplay ne peut pas fonctionner.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gameplay_lum)

        // Initialisation des vues existantes
        viewFinder = findViewById(R.id.viewFinder)
        filterOverlay = findViewById(R.id.filter_overlay)
        textDiseaseName = findViewById(R.id.text_disease_name)

        // Nouvelles vues à ajouter dans ton activity_gameplay_lum.xml
        // textInstruction = findViewById(R.id.text_instruction)
        val btnAnalyze = findViewById<Button>(R.id.button_analyze) // Bouton pour scanner

        val btnBack = findViewById<Button>(R.id.button_back)
        val btnChangeFilter = findViewById<Button>(R.id.button_change_filter)
        val btnStart = findViewById<Button>(R.id.button_start)

        // Initialisation de notre gestionnaire de caméra
        cameraManager = CameraManager(this, this, viewFinder)

        // Initialiser RealtimeHelper (au cas où ce ne serait pas fait ailleurs)
        RealtimeHelper.init(this)

        // Récupération des données envoyées
        val savedVisionIndex = intent.getIntExtra("VISION_INDEX", -1)
        val userId = intent.getStringExtra("USER_ID")
        val characterName = intent.getStringExtra("CHARACTER_NAME") ?: "Lum (cécité partielle)"

        btnBack.setOnClickListener {
            val intent = Intent(this, StartChoiseCharacter::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        fun changeVisionFilter() {
            currentFilterIndex = (currentFilterIndex + 1) % VisionData.filters.size
            filterOverlay.setImageResource(VisionData.filters[currentFilterIndex])
            textDiseaseName.text = VisionData.diseaseNames[currentFilterIndex]
        }

        // Action : Bouton Analyser la caméra
        btnAnalyze.setOnClickListener {
            val bitmap = viewFinder.bitmap
            if (bitmap != null) {
                Toast.makeText(this, "Analyse en cours par l'IA...", Toast.LENGTH_SHORT).show()
                btnAnalyze.isEnabled = false // Désactiver pour éviter le spam

                // 1. Redimensionner l'image pour ne pas exploser la limite Firebase
                val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 480, 640, true)

                // 2. Convertir en Base64
                val baos = ByteArrayOutputStream()
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
                val base64Image = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)

                // 3. Envoyer à Firebase
                RealtimeHelper.sendImageForColorDetection(base64Image, targetColorToFind)
            } else {
                Toast.makeText(this, "Erreur : Impossible de capturer l'image.", Toast.LENGTH_SHORT).show()
            }
        }

        // === LOGIQUE DE JEU ===
        if (savedVisionIndex != -1) {
            // MODE "CONTINUER"
            currentFilterIndex = savedVisionIndex
            filterOverlay.setImageResource(VisionData.filters[currentFilterIndex])
            textDiseaseName.text = VisionData.diseaseNames[currentFilterIndex]

            btnStart.visibility = View.GONE
            btnChangeFilter.visibility = View.GONE
            filterOverlay.setOnClickListener(null)

            startGameplay(btnAnalyze)

        } else {
            // MODE "NOUVELLE PARTIE"
            textDiseaseName.text = VisionData.diseaseNames[currentFilterIndex]
            btnChangeFilter.setOnClickListener { changeVisionFilter() }

            btnStart.setOnClickListener {
                btnStart.visibility = View.GONE
                btnChangeFilter.visibility = View.GONE
                filterOverlay.setOnClickListener(null)

                userId?.let {
                    FirebaseHelper.getInstance().updateGameProgress(it, characterName, "visionIndex", currentFilterIndex)
                }

                startGameplay(btnAnalyze)
            }
        }

        // Vérification des permissions
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

        // Choisir une couleur aléatoire à trouver
        targetColorToFind = colorsList[Random.nextInt(colorsList.size)]

        // Afficher l'instruction (décommente la ligne ci-dessous si tu as ajouté textInstruction dans ton XML)
        // textInstruction.text = "Cherche un objet : $targetColorToFind"
        Toast.makeText(this, "Trouvez un objet : $targetColorToFind", Toast.LENGTH_LONG).show()

        // Écouter la réponse de l'IA
        RealtimeHelper.listenForColorDetectionResult { isWin, message ->
            btnAnalyze.isEnabled = true // Réactiver le bouton

            Toast.makeText(this, "L'IA dit : $message", Toast.LENGTH_LONG).show()

            if (isWin) {
                // Gagné ! Tu peux passer au niveau suivant ou donner une nouvelle couleur
                targetColorToFind = colorsList[Random.nextInt(colorsList.size)]
                textInstruction.text = "Bravo ! Maintenant, cherche : $targetColorToFind"
            }
        }
    }

    /**
     * Libère les ressources lorsque l'activité est détruite
     */
    override fun onDestroy() {
        super.onDestroy()
        cameraManager.shutdown()
    }
}