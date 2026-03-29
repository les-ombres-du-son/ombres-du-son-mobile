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
    private var countDownTimer: android.os.CountDownTimer? = null

    private var hasWon = false // État actuel de l'IA (true = satisfaite, false = non satisfaite)
    private var isGameOver = false // true quand les 60s de validation sont écoulées

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkMicrophonePermission()

        // 1. Initialiser le TTS
        tts = android.speech.tts.TextToSpeech(this) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                tts.language = java.util.Locale.FRANCE

                tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d(TAG, "L'IA commence à parler...")
                    }

                    override fun onDone(utteranceId: String?) {
                        Log.d(TAG, "L'IA a fini de parler.")
                        runOnUiThread {
                            // Si la partie est finie (les 60s sont passées), on ne relance rien
                            if (isGameOver) {
                                Log.d(TAG, "Fin de partie validée, on ne relance plus le micro.")
                                return@runOnUiThread
                            }

                            // Sinon, on continue la conversation (peu importe si isWin est true ou false)
                            Log.d(TAG, "On relance le micro pour continuer l'interaction.")
                            isGameRunning = true
                            Toast.makeText(this@CeciliaLevel4Activity, "À vous...", Toast.LENGTH_SHORT).show()
                            startListening()
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        runOnUiThread {
                            if (!isGameOver) {
                                isGameRunning = true
                                startListening()
                            }
                        }
                    }
                })
            }
        }

        // 2. Écouter Firebase
        aiListener = RealtimeHelper.listenForAIResponse { texteRecu, isWin ->

            // --- NOUVELLE LOGIQUE DU CHRONO ---
            if (isWin && !hasWon) {
                // L'IA passe de FALSE à TRUE : On DÉMARRE le chrono
                Log.d(TAG, "L'IA est convaincue ! Démarrage du chrono de 60s.")
                Toast.makeText(this, "L'IA est satisfaite ! Maintenez ça 60s...", Toast.LENGTH_LONG).show()
                startTimer()
            }
            else if (!isWin && hasWon) {
                // L'IA passe de TRUE à FALSE : On ANNULE le chrono
                Log.d(TAG, "L'IA n'est plus convaincue. Chrono annulé.")
                Toast.makeText(this, "Attention, l'IA doute ! Chrono annulé.", Toast.LENGTH_LONG).show()
                countDownTimer?.cancel()
                countDownTimer = null
            }

            // On met à jour l'état actuel
            hasWon = isWin
            Log.d(TAG, "Statut actuel isWin : $hasWon")

            // On fait parler l'IA
            speakOut(texteRecu)
        }
    }

    private fun speakOut(text: String) {
        val utteranceId = java.util.UUID.randomUUID().toString()
        tts.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    override fun isPlaying(): Boolean {
        return isGameRunning
    }

    private fun checkMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST_CODE)
        } else {
            initGame()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initGame()
        } else {
            Toast.makeText(this, "Permission micro requise", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initGame() {
        startGame()
        // audioManager.playIntro(R.raw.voix_off_niveau4) { startGame() }
    }

    private fun startGame() {
        isGameRunning = true
        hasWon = false
        isGameOver = false

        RealtimeHelper.updateGameStatus("playing")
        RealtimeHelper.updateStep("Phase_Reconnaissance_Vocale")

        Toast.makeText(this, "Parlez pour interagir avec l'IA...", Toast.LENGTH_SHORT).show()
        setupSpeechRecognizer()

        // ATTENTION : On ne lance plus le timer ici ! On attend que isWin passe à true.
        startListening()
    }

    /**
     * Démarre le chrono de 60 secondes.
     */
    private fun startTimer() {
        // Sécurité : on coupe tout chrono existant avant d'en lancer un nouveau
        countDownTimer?.cancel()

        countDownTimer = object : android.os.CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                Log.d(TAG, "Temps de maintien restant : $secondsLeft secondes")
            }

            override fun onFinish() {
                // Si on arrive ici, c'est que le chrono n'a pas été annulé !
                // Le joueur a donc maintenu l'IA à "true" pendant 60 secondes complètes.
                isGameOver = true
                isGameRunning = false
                speechRecognizer?.stopListening()

                Toast.makeText(this@CeciliaLevel4Activity, "FIN DE PARTIE ! Vous avez réussi.", Toast.LENGTH_LONG).show()
                Log.d(TAG, "Victoire validée : 60s maintenues avec succès.")

                // Lancer la suite du jeu ici plus tard
                /*val intent = Intent(this@CeciliaLevel4Activity, CeciliaLevel5Activity::class.java)
                startActivity(intent)
                finish()*/
            }
        }.start()
    }

    private fun setupSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {

                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d(TAG, "Micro prêt, en attente de la voix...")
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    if (!isGameRunning) {
                        return
                    }

                    if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        startListening()
                    } else {
                        Log.e(TAG, "Erreur micro : $error. Pause avant relance...")
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            if (isGameRunning) startListening()
                        }, 500)
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val spokenText = matches[0]
                        handlePlayerSpeech(spokenText)
                    } else {
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

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun handlePlayerSpeech(text: String) {
        Log.d(TAG, "Texte capturé : $text")

        RealtimeHelper.sendPlayerSpeech(text)

        // On coupe le micro le temps que l'IA réponde
        isGameRunning = false
        speechRecognizer?.stopListening()

        Log.d(TAG, "Attente de la réponse de l'IA sur Firebase...")
    }

    override fun onPause() {
        super.onPause()
        if (isGameRunning) speechRecognizer?.stopListening()
    }

    override fun onResume() {
        super.onResume()
        if (isGameRunning) startListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        aiListener?.let { RealtimeHelper.stopListening(it) }

        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        isGameRunning = false
        speechRecognizer?.destroy()
    }
}