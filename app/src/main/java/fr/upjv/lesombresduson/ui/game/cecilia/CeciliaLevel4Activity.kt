package fr.upjv.lesombresduson.ui.game.cecilia

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import fr.upjv.lesombresduson.R
import fr.upjv.lesombresduson.data.remote.RealtimeHelper
import fr.upjv.lesombresduson.ui.game.cecilia.util.BackGameActivity
import java.util.Locale

class CeciliaLevel4Activity : BackGameActivity() {

    override val sessionName = "Niveau4"
    private var speechRecognizer: SpeechRecognizer? = null

    private val RECORD_AUDIO_REQUEST_CODE = 101
    private val TAG = "CeciliaLevel4"

    private var isGameRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Vérification des permissions avant de lancer l'audio
        checkMicrophonePermission()
    }

    /**
     * Détermine le statut actuel du niveau pour le suivi Firebase en temps réel.
     */
    override fun isPlaying(): Boolean {
        return isGameRunning
    }

    /**
     * Vérification de la permission audio.
     * Si elle n'est pas accordée, on la demande.
     */
    private fun checkMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST_CODE)
        } else {
            initGame()
        }
    }

    /**
     * Redemande la permission à l'utilisateur si elle a été précédemment refusée
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initGame()
        } else {
            Toast.makeText(this, "Permission micro requise", Toast.LENGTH_LONG).show()
            finish() // On quitte si refusé, le niveau est injouable sans
        }
    }

    /**
     * Initialisation de la partie
     */
    private fun initGame() {
        // Joue la narration d'introduction
        audioManager.playIntro(R.raw.voix_off_niveau4) {
            startGame()
        }
    }

    /**
     * Démarre la partie
     */
    private fun startGame() {
        isGameRunning = true

        // --- AJOUT FIREBASE ---
        RealtimeHelper.updateGameStatus("playing")
        RealtimeHelper.updateStep("Phase_Reconnaissance_Vocale")

        Toast.makeText(this, "À vous de parler...", Toast.LENGTH_SHORT).show()
        setupSpeechRecognizer()
        startListening()
    }

    /**
     * Configuration du micro pour la reconnaissance vocale.
     */
    private fun setupSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {

                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d(TAG, "Micro prêt, en attente de la voix...")
                }

                override fun onBeginningOfSpeech() {
                    Log.d(TAG, "Le joueur commence à parler.")
                }

                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    Log.d(TAG, "Le joueur a fini de parler. Analyse en cours...")
                }

                override fun onError(error: Int) {
                    // Si le joueur ne dit rien ou qu'il y a un bruit de fond non reconnu
                    // On relance l'écoute silencieusement pour que ça soit persistant
                    if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        Log.d(TAG, "Rien entendu, on relance l'écoute.")
                        startListening()
                    } else {
                        Log.e(TAG, "Erreur micro : $error")
                        startListening() // On tente de relancer quand même
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val spokenText = matches[0]
                        handlePlayerSpeech(spokenText)
                    } else {
                        Log.d(TAG, "Aucun texte compris, on relance.")
                        startListening()
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        } else {
            Toast.makeText(this, "Reconnaissance vocale non disponible sur cet appareil", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Démarre la reconnaissance vocale.
     */
    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    /**
     * Gère la réception du texte transcrit par le micro.
     */
    private fun handlePlayerSpeech(text: String) {
        Log.d(TAG, "Texte capturé : $text")
        Toast.makeText(this, "Envoyé : $text", Toast.LENGTH_SHORT).show()

        // Envoi à Firebase Realtime Database
        RealtimeHelper.sendPlayerSpeech(text)

        // Fin du niveau
        isGameRunning = false
        speechRecognizer?.stopListening()

        val intent = Intent(this, CeciliaLevel5Activity::class.java)
        startActivity(intent)
        finish()
    }

    // =========================================================================
    //                      CYCLE DE VIE
    // =========================================================================


    override fun onPause() {
        super.onPause()
        if (isGameRunning) {
            speechRecognizer?.stopListening()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isGameRunning) {
            startListening()
        }
    }

    /**
     * Nettoyage des ressources avant destruction de l'activité.
     */
    override fun onDestroy() {
        super.onDestroy()
        isGameRunning = false
        speechRecognizer?.destroy()
    }
}