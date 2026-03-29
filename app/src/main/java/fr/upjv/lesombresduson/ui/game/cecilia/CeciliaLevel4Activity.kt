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
    private var aiListener: com.google.firebase.database.ValueEventListener? = null
    private lateinit var tts: android.speech.tts.TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Vérification des permissions avant de lancer l'audio
        checkMicrophonePermission()

        // Initialiser le moteur de synthèse vocale
        tts = android.speech.tts.TextToSpeech(this) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                tts.language = java.util.Locale.FRANCE

                // Ajouter le listener pour savoir quand la voix a fini de parler
                tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d(TAG, "L'IA commence à parler...")
                    }

                    override fun onDone(utteranceId: String?) {
                        Log.d(TAG, "L'IA a fini de parler, on relance le micro.")
                        // On doit repasser sur le thread principal (UI) pour utiliser le micro et les Toast
                        runOnUiThread {
                            isGameRunning = true
                            Toast.makeText(this@CeciliaLevel4Activity, "À vous de parler...", Toast.LENGTH_SHORT).show()
                            startListening()
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        Log.e(TAG, "Erreur lors de la lecture TTS")
                        runOnUiThread {
                            isGameRunning = true
                            startListening() // On relance quand même en cas d'erreur
                        }
                    }
                })
            }
        }

        // Écouter la réponse de l'IA sur Firebase
        aiListener = RealtimeHelper.listenForAIResponse { texteRecu ->
            speakOut(texteRecu)
        }
    }

    /**
     * Prononce à voix haute le texte fourni en utilisant le moteur TextToSpeech.
     * @param text Le texte à convertir en parole
     */
    private fun speakOut(text: String) {
        // On génère un ID unique pour cette phrase
        val utteranceId = java.util.UUID.randomUUID().toString()

        // On utilise la version moderne de tts.speak (qui prend 4 paramètres)
        tts.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, utteranceId)
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
        startGame()
        // Joue la narration d'introduction
        /*audioManager.playIntro(R.raw.voix_off_niveau4) {
            startGame()
        }*/
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
                    // ON NE RELANCE QUE SI LE JEU EST ENCORE EN TRAIN DE TOURNER
                    if (isGameRunning) {
                        if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                            Log.d(TAG, "Rien entendu, on relance.")
                            startListening()
                        } else {
                            Log.e(TAG, "Erreur micro : $error")
                            startListening()
                        }
                    } else {
                        Log.d(TAG, "Micro coupé (isGameRunning = false), on n'écoute plus.")
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val spokenText = matches[0]
                        // handlePlayerSpeech va mettre isGameRunning à false
                        handlePlayerSpeech(spokenText)
                    } else {
                        // IDEM ICI : On ne relance que si nécessaire
                        if (isGameRunning) startListening()
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

        Log.d(TAG, "Attente de la réponse de l'IA sur Firebase...")

        /*val intent = Intent(this, CeciliaLevel5Activity::class.java)
        startActivity(intent)
        finish()*/
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
        aiListener?.let { RealtimeHelper.stopListening(it) }

        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        isGameRunning = false
        speechRecognizer?.destroy()
    }
}