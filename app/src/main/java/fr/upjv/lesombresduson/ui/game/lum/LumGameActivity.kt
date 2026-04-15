package fr.upjv.lesombresduson.ui.game.lum

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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
import fr.upjv.lesombresduson.ui.StartChoiseCharacter

class LumGameActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var filterOverlay: ImageView
    private lateinit var textDiseaseName: TextView
    private lateinit var cameraManager: CameraManager

    private var currentFilterIndex = 0

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

        // Initialisation des vues
        viewFinder = findViewById(R.id.viewFinder)
        filterOverlay = findViewById(R.id.filter_overlay)
        textDiseaseName = findViewById(R.id.text_disease_name)

        val btnBack = findViewById<Button>(R.id.button_back)
        val btnChangeFilter = findViewById<Button>(R.id.button_change_filter)
        val btnStart = findViewById<Button>(R.id.button_start)

        // Initialisation de notre gestionnaire de caméra
        cameraManager = CameraManager(this, this, viewFinder)

        // Récupération des données envoyées
        val savedVisionIndex = intent.getIntExtra("VISION_INDEX", -1)
        val userId = intent.getStringExtra("USER_ID")
        val characterName = intent.getStringExtra("CHARACTER_NAME") ?: "Lum (cécité partielle)"

        // Action : Bouton Retour
        btnBack.setOnClickListener {
            val intent = Intent(this, StartChoiseCharacter::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Fonction locale pour changer de filtre (réutilisable)
        fun changeVisionFilter() {
            currentFilterIndex = (currentFilterIndex + 1) % VisionData.filters.size
            filterOverlay.setImageResource(VisionData.filters[currentFilterIndex])
            textDiseaseName.text = VisionData.diseaseNames[currentFilterIndex]
        }

        // === LOGIQUE DE RESTAURATION OU NOUVELLE PARTIE ===
        if (savedVisionIndex != -1) {
            // MODE "CONTINUER"
            currentFilterIndex = savedVisionIndex
            filterOverlay.setImageResource(VisionData.filters[currentFilterIndex])
            textDiseaseName.text = VisionData.diseaseNames[currentFilterIndex]

            btnStart.visibility = android.view.View.GONE
            btnChangeFilter.visibility = android.view.View.GONE
            filterOverlay.setOnClickListener(null)

        } else {
            // MODE "NOUVELLE PARTIE"
            textDiseaseName.text = VisionData.diseaseNames[currentFilterIndex]

            btnChangeFilter.setOnClickListener { changeVisionFilter() }

            btnStart.setOnClickListener {
                btnStart.visibility = android.view.View.GONE
                btnChangeFilter.visibility = android.view.View.GONE
                filterOverlay.setOnClickListener(null)

                // Sauvegarde Firebase
                userId?.let {
                    FirebaseHelper.getInstance().updateGameProgress(it, characterName, "visionIndex", currentFilterIndex)
                }

                val nomMaladie = textDiseaseName.text
                Toast.makeText(this, "La partie commence avec : $nomMaladie", Toast.LENGTH_SHORT).show()
            }
        }

        // Vérification des permissions au lancement
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraManager.startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    /**
     * Méthode appelée lorsque l'activité est détruite.
     */
    override fun onDestroy() {
        super.onDestroy()
        // On demande proprement au CameraManager de s'éteindre
        cameraManager.shutdown()
    }
}