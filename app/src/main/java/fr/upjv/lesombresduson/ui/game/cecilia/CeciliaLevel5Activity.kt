package fr.upjv.lesombresduson.ui.game.cecilia

import android.os.Bundle
import android.widget.Button
import fr.upjv.lesombresduson.R
import fr.upjv.lesombresduson.ui.game.cecilia.util.BackGameActivity

class CeciliaLevel5Activity : BackGameActivity() {

    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gameplay_cecilia)

        btnBack = findViewById(R.id.button_back)
        setupBackButton(btnBack)
    }
}