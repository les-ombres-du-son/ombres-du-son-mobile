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

    // --- Variables de Scoring (Le Profiler) ---
    private var interactionsWithBadAI = 0 // Parle à un personnage mal intentionné
    private var goodAISkipped = 0         // Secoue le téléphone alors que le perso était gentil
    private var totalShakes = 0           // Nombre total de changements de perso
    private var isFirstInteractionWithChar = true

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
        totalShakes++
        isFirstInteractionWithChar = true

        // Si le joueur fuit un personnage qui était en train de valider (hasWon = true)
        if (hasWon) {
            goodAISkipped++
            Log.d(TAG, "Erreur : Le joueur a fui un personnage bien intentionné !")
        }

        currentCharacter = if (currentCharacter < MAX_CHARACTERS) currentCharacter + 1 else 1
        Toast.makeText(this, "Vous avez choisi le personnage $currentCharacter", Toast.LENGTH_SHORT).show()

        // On réinitialise l'état pour le nouveau personnage
        hasWon = false
        countDownTimer?.cancel()

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

        // Si l'état actuel est false (méchant) et que le joueur continue de parler au lieu de secouer
        if (isFirstInteractionWithChar) {
            // C'est la première phrase au personnage, on le laisse tranquille
            isFirstInteractionWithChar = false
        } else if (!hasWon) {
            // S'il continue de parler ALORS QUE l'IA a déjà montré qu'elle était méchante
            interactionsWithBadAI++
        }

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
                calculateAndSaveScore()
            }
        }.start()
    }


    // =========================================================================
    //                      SCORING & PERSISTENCE
    // =========================================================================

    /**
     * Calcule les métriques de performance et détermine le profil du joueur.
     * - Score Discernement (70%) : Capacité à repérer les bonnes/mauvaises intentions.
     * - Score Intuition (30%) : Capacité à trouver la bonne personne rapidement.
     */
    private fun calculateAndSaveScore() {
        // 1. Discernement (Basé sur la psychologie)
        // Grosse pénalité (-30) s'il fuit un gentil. Petite pénalité (-10) s'il discute trop avec un méchant.
        val scoreDiscernement = (100 - (goodAISkipped * 30) - (interactionsWithBadAI * 10)).coerceIn(0, 100)

        // 2. Intuition (Basé sur la recherche)
        // Perd 5 points par personnage testé (s'il en teste 10, il a 50%)
        val scoreIntuition = (100 - (totalShakes * 5)).coerceIn(0, 100)

        // Score pondéré : 70% Discernement, 30% Intuition
        val globalScore = ((scoreDiscernement * 0.7) + (scoreIntuition * 0.3)).toInt()

        // Détermination du profil basé sur ses erreurs
        val profilJoueur = when {
            goodAISkipped > 0 -> "Le Paranoïaque"          // A fui une bonne personne
            interactionsWithBadAI > 3 -> "L'Oreille Naïve" // A trop discuté avec les méchants
            scoreDiscernement == 100 && totalShakes <= 2 -> "Le Profiler Expert" // A trouvé direct
            else -> "L'Enquêteur Prudent"
        }

        saveToFirebase(globalScore, profilJoueur, scoreDiscernement, scoreIntuition)

        // Affichage UI via BackGameActivity
        val details = "👁️ Discernement : $scoreDiscernement%\n🔮 Intuition : $scoreIntuition%\n🏃 Bons persos fuis : $goodAISkipped\n💬 Mots aux méchants : $interactionsWithBadAI"

        // Texte vocal lu à la fin du niveau
        val speechText = """
            Niveau quatre terminé. Score global : $globalScore sur cent. 
            Votre profil est : $profilJoueur. 
            Score de discernement des intentions : $scoreDiscernement pour cent. 
            Score d'intuition : $scoreIntuition pour cent.
        """.trimIndent()

        showLevelCompleteDialog(
            score = globalScore,
            profil = profilJoueur,
            details = details,
            speechText = speechText,
            nextActivityClass = CeciliaLevel5Activity::class.java
        )
    }

    /**
     * Met à jour la progression globale et stocke les métriques détaillées pour l'analytics.
     */
    private fun saveToFirebase(score: Int, profil: String, discernement: Int, intuition: Int) {
        val metrics = mapOf(
            "naivete_count" to interactionsWithBadAI,
            "paranoia_count" to goodAISkipped,
            "shake_count" to totalShakes,
            "score_discernement" to discernement,
            "score_intuition" to intuition
        )

        // Utilise la méthode de sauvegarde de BackGameActivity
        syncManager.checkNetworkAndSave("Niveau4", score, profil, metrics)
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