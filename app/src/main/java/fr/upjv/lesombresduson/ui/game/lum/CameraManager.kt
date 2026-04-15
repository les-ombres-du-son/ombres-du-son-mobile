package fr.upjv.lesombresduson.ui.game.lum

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val viewFinder: PreviewView
) {
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    /**
     * Démarre la caméra.
     */
    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

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
                    lifecycleOwner, cameraSelector, preview
                )

            } catch (exc: Exception) {
                Log.e("CameraManager", "Erreur lors de l'initialisation de la caméra", exc)
            }

        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Ferme le gestionnaire de caméra.
     */
    fun shutdown() {
        cameraExecutor.shutdown()
    }
}