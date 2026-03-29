package fr.upjv.lesombresduson.ui.game.cecilia

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import fr.upjv.lesombresduson.data.remote.RealtimeHelper
import fr.upjv.lesombresduson.ui.game.cecilia.util.BackGameActivity
import fr.upjv.lesombresduson.ui.game.cecilia.util.ShakeManagerlevel4
import fr.upjv.lesombresduson.ui.game.cecilia.util.VoiceManagerlevel4

/**
 * Niveau 4 : Interaction avec l'IA.
 * Le joueur doit convaincre l'IA et maintenir son état "satisfait" pendant 60s.
 * Changement de perso en secouant le téléphone.
 */
class CeciliaLevel4Activity : BackGameActivity() {

    override val sessionName = "Niveau4"
    private val RECORD_AUDIO_REQUEST_CODE = 101
    private val TAG = "CeciliaLevel4"

    private var aiListener: com.google.firebase.database.ValueEventListener? = null
    private var countDownTimer: CountDownTimer? = null

    private var hasWon = false
    private var isGameOver = false
    private var currentCharacter = 1
    private val MAX_CHARACTERS = 10
    private lateinit var shakeManager: ShakeManagerlevel4
    private lateinit var voiceManager: VoiceManagerlevel4

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initManagers()
        checkMicrophonePermission()
        setupFirebaseListener()
    }

    /**
     * Initialise les gestionnaires dédiés pour les capteurs physiques (ShakeManager)
     * et la reconnaissance/synthèse vocale (VoiceManager).
     */
    private fun initManagers() {
        // Initialiser le gestionnaire de secousses
        shakeManager = ShakeManagerlevel4(this) {
            changeCharacter()
        }

        // Initialiser le gestionnaire vocal
        voiceManager = VoiceManagerlevel4(
            activity = this,
            onSpeechResult = { text -> handlePlayerSpeech(text) },
            onTtsDone = { handleTtsDone() },
            onSpeechError = { handleSpeechError() }
        )
    }

    /**
     * Met en place l'écouteur Firebase pour réceptionner les réponses textuelles de l'IA
     * ainsi que son état de satisfaction (isWin). Gère dynamiquement le démarrage ou
     * l'annulation du chronomètre de victoire selon l'humeur de l'IA.
     */
    private fun setupFirebaseListener() {
        aiListener = RealtimeHelper.listenForAIResponse { texteRecu, isWin ->
            if (isWin && !hasWon) {
                Log.d(TAG, "L'IA est convaincue ! Démarrage du chrono de 60s.")
                Toast.makeText(this, "L'IA est satisfaite ! Maintenez ça 60s...", Toast.LENGTH_LONG).show()
                startTimer()
            } else if (!isWin && hasWon) {
                Log.d(TAG, "L'IA n'est plus convaincue. Chrono annulé.")
                Toast.makeText(this, "Attention, l'IA doute ! Chrono annulé.", Toast.LENGTH_LONG).show()
                countDownTimer?.cancel()
                countDownTimer = null
            }

            hasWon = isWin
            voiceManager.speak(texteRecu)
        }
    }

    /**
     * Fait défiler le personnage sélectionné (de 1 à MAX_CHARACTERS) suite à une
     * détection de secousse par le capteur, et synchronise ce choix sur Firebase.
     */
    private fun changeCharacter() {
        currentCharacter = if (currentCharacter < MAX_CHARACTERS) currentCharacter + 1 else 1
        Toast.makeText(this, "Vous avez choisi le personnage $currentCharacter", Toast.LENGTH_SHORT).show()
        RealtimeHelper.updateSelectedCharacter(currentCharacter)
    }

    /**
     * Réceptionne la transcription de la voix du joueur, l'envoie à la base de données
     * pour analyse par l'IA, et coupe l'écoute en attendant la réponse.
     *
     * @param text Le texte prononcé par le joueur.
     */
    private fun handlePlayerSpeech(text: String) {
        Log.d(TAG, "Texte capturé : $text")
        RealtimeHelper.sendPlayerSpeech(text)
        voiceManager.stopListening()
    }

    /**
     * Callback déclenché à la fin de la synthèse vocale (TTS) de l'IA.
     * Relance automatiquement l'écoute du microphone pour poursuivre la boucle de conversation.
     */
    private fun handleTtsDone() {
        if (isGameOver) return

        voiceManager.isListeningEnabled = true
        Toast.makeText(this, "À vous...", Toast.LENGTH_SHORT).show()
        voiceManager.startListening()
    }

    /**
     * Callback de secours déclenché en cas d'erreur de la synthèse vocale.
     * Tente de relancer le microphone pour éviter de bloquer l'interaction du joueur.
     */
    private fun handleSpeechError() {
        if (!isGameOver) {
            voiceManager.isListeningEnabled = true
            voiceManager.startListening()
        }
    }

    // =========================================================================
    //                      MÉCANIQUE DE JEU ET CHRONO
    // =========================================================================

    /**
     * Configure les données initiales de la partie (personnage par défaut sur Firebase)
     * et amorce le lancement du jeu.
     */
    private fun initGame() {
        RealtimeHelper.updateSelectedCharacter(currentCharacter)
        Toast.makeText(this, "Personnage $currentCharacter sélectionné. Secouez pour changer.", Toast.LENGTH_LONG).show()
        startGame()
    }

    /**
     * Démarre activement la boucle d'interaction : met à jour le statut de session
     * sur Firebase, informe le joueur et ouvre le microphone.
     */
    private fun startGame() {
        voiceManager.isListeningEnabled = true
        hasWon = false
        isGameOver = false

        RealtimeHelper.updateGameStatus("playing")
        RealtimeHelper.updateStep("Phase_Reconnaissance_Vocale")

        Toast.makeText(this, "Parlez pour interagir avec l'IA...", Toast.LENGTH_SHORT).show()
        voiceManager.startListening()
    }

    /**
     * Démarre le chronomètre de condition de victoire (60 secondes).
     * Si le chronomètre arrive à son terme sans être annulé, la partie est validée.
     */
    private fun startTimer() {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                Log.d(TAG, "Temps de maintien restant : ${millisUntilFinished / 1000} secondes")
            }

            override fun onFinish() {
                isGameOver = true
                voiceManager.stopListening()
                shakeManager.stop()
                Toast.makeText(this@CeciliaLevel4Activity, "FIN DE PARTIE ! Vous avez réussi.", Toast.LENGTH_LONG).show()
            }
        }.start()
    }

    // =========================================================================
    //                      CYCLE DE VIE ET PERMISSIONS
    // =========================================================================

    /**
     * Indique si la boucle de gameplay est active en vérifiant l'état d'écoute
     * du gestionnaire vocal. Utilisé pour les statistiques temps réel.
     */
    override fun isPlaying(): Boolean = voiceManager.isListeningEnabled

    /**
     * Vérifie que l'application dispose des droits d'enregistrement audio.
     * Demande la permission à l'utilisateur si nécessaire, sinon initialise le jeu.
     */
    private fun checkMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST_CODE)
        } else {
            initGame()
        }
    }

    /**
     * Traite le résultat de la demande d'autorisation d'accès au microphone.
     * Si refusée, le niveau est interrompu car injouable.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initGame()
        } else {
            Toast.makeText(this, "Permission micro requise", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    /**
     * Met en pause les écoutes matérielles (microphone et accéléromètre)
     * lorsque l'activité passe en arrière-plan pour économiser les ressources.
     */
    override fun onPause() {
        super.onPause()
        voiceManager.stopListening()
        shakeManager.stop()
    }

    /**
     * Relance les écoutes matérielles (microphone et accéléromètre)
     * lors du retour au premier plan, sauf si la partie est déjà terminée.
     */
    override fun onResume() {
        super.onResume()
        if (!isGameOver) {
            voiceManager.startListening()
            shakeManager.start()
        }
    }

    /**
     * Nettoie et libère l'intégralité des ressources (chronomètres, écouteurs Firebase,
     * capteurs, moteur vocal) à la destruction de l'activité pour éviter les fuites de mémoire.
     */
    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        aiListener?.let { RealtimeHelper.stopListening(it) }
        shakeManager.stop()
        voiceManager.destroy()
    }
}