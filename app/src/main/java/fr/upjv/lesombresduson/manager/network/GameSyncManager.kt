package fr.upjv.lesombresduson.manager.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import fr.upjv.lesombresduson.data.remote.FirebaseHelper
import fr.upjv.lesombresduson.data.remote.RealtimeHelper

/**
 * Gestionnaire de synchronisation des données de fin de niveau.
 */
class GameSyncManager(private val context: Context) {

    /**
     * Sauvegarde les données de fin de niveau et gère les cas de déconnexion.
     */
    fun checkNetworkAndSave(
        levelName: String,
        score: Int,
        profil: String,
        metrics: Map<String, Any>
    ) {
        // On récupère l'ID de l'utilisateur directement ici
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        // Sécurité : si le joueur n'est pas connecté, on ne fait rien
        if (userId == null) return

        // Sauvegarde Firebase
        FirebaseHelper.getInstance().saveLevelData(
            userId,
            "Cécilia (cécité totale)",
            levelName,
            score,
            profil,
            metrics
        )

        // Fin de la session temps réel
        // (Commenté pour aujourd'hui, à décommenter pour demain)
        // RealtimeHelper.endSession()

        // Vérification du réseau
        if (!isNetworkAvailable()) {
            Toast.makeText(
                context,
                "Connexion perdue. Sauvegarde locale effectuée, synchro en attente.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Vérifie si l'appareil est connecté à Internet.
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}