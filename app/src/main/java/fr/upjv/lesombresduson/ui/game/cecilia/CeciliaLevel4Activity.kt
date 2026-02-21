package fr.upjv.lesombresduson.ui.game.cecilia

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import fr.upjv.lesombresduson.R
import fr.upjv.lesombresduson.ui.game.cecilia.util.BackGameActivity

/**
 * Activité basique du Niveau 4 (Cecilia).
 * Structure vide prête à recevoir la nouvelle logique de jeu.
 */
class CeciliaLevel4Activity : BackGameActivity() {

    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gameplay_cecilia)

        // Initialisation des vues
        btnBack = findViewById(R.id.button_back)

        // Configuration du bouton retour (hérité de BackGameActivity)
        setupBackButton(btnBack)

        audioManager.playIntro(R.raw.voix_off_niveau4) {
            startGame()
        }
    }

    private fun startGame(){

    }
}