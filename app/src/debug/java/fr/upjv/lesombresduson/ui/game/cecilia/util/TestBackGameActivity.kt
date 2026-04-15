package fr.upjv.lesombresduson.ui.game.cecilia.util

// Assurez-vous que les imports correspondent bien à votre projet
class TestBackGameActivity : BackGameActivity() {

    override val sessionName: String = "TestSession"

    var mockIsPlaying: Boolean = false
    override fun isPlaying(): Boolean = mockIsPlaying

    fun getExposedGameStatus(): String {
        return getCurrentGameStatus()
    }
}