package fr.upjv.lesombresduson.ui.game.cecilia

import android.os.Bundle
import android.widget.Button
import fr.upjv.lesombresduson.R
import fr.upjv.lesombresduson.data.remote.RealtimeHelper
import fr.upjv.lesombresduson.ui.game.cecilia.util.BackGameActivity

class CeciliaLevel5Activity : BackGameActivity() {

    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gameplay_cecilia)

        btnBack = findViewById(R.id.button_back)
        setupBackButton(btnBack)

        initGame()
    }

    /**
     * Initialisation de la partie
     */
    private fun initGame() {
        // Joue la narration d'introduction
        audioManager.playIntro(R.raw.voix_off_niveau5) {
            audioManager.playIntro(R.raw.chanson_final){

            }
        }
    }
}