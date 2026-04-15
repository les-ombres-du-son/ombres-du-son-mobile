package fr.upjv.lesombresduson.ui.game.lum

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import fr.upjv.lesombresduson.R
import fr.upjv.lesombresduson.ui.StartChoiseCharacter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class LumGameActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var filterOverlay: ImageView
    private lateinit var textDiseaseName: TextView // Nouveau TextView
    private lateinit var cameraExecutor: ExecutorService

    // 1. Liste des filtres
    private val filters = listOf(
        android.R.color.transparent,
        R.drawable.filtre_tache_centrale,
        R.drawable.filtre_moitie_ecran,
        R.drawable.filtre_glaucome_tunnel,
        R.drawable.filtre_cataracte,
        R.drawable.filtre_retinopathie_taches,
        R.drawable.filtre_vision_floue
    )

    // 2. Liste des noms associés aux filtres
    private val diseaseNames = listOf(
        "Vision Normale",
        "DMLA (Tache centrale)",
        "Hémianopsie (Moitié d'écran)",
        "Glaucome (Vision en tunnel)",
        "Cataracte (Voile opaque)",
        "Rétinopathie (Taches)",
        "Myopie Sévère (Vision floue)"
    )

    private var currentFilterIndex = 0

    // Gestionnaire de demande de permission
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Permission caméra refusée. Le gameplay ne peut pas fonctionner.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gameplay_lum)

        // 3. Initialisation des vues
        viewFinder = findViewById(R.id.viewFinder)
        filterOverlay = findViewById(R.id.filter_overlay)
        textDiseaseName = findViewById(R.id.text_disease_name) // Récupération du TextView

        val btnBack = findViewById<Button>(R.id.button_back)
        val btnChangeFilter = findViewById<Button>(R.id.button_change_filter)

        // Initialisation du premier texte
        textDiseaseName.text = diseaseNames[currentFilterIndex]

        // Action : Bouton Retour
        btnBack.setOnClickListener {
            val intent = Intent(this, StartChoiseCharacter::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // 4. Action : Changer de filtre et de texte
        btnChangeFilter.setOnClickListener {
            // Passe à l'index suivant
            currentFilterIndex = (currentFilterIndex + 1) % filters.size

            // Applique la nouvelle ressource visuelle
            filterOverlay.setImageResource(filters[currentFilterIndex])

            // Met à jour le texte de la maladie
            textDiseaseName.text = diseaseNames[currentFilterIndex]
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Vérification des permissions au lancement
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Provider
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Configuration du Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            // Sélection de la caméra arrière par défaut
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Dé-binder les cas d'usage existants avant de re-binder
                cameraProvider.unbindAll()

                // Binder les cas d'usage à la caméra
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview
                )

            } catch (exc: Exception) {
                Log.e("LumGameActivity", "Erreur lors de l'initialisation de la caméra", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}