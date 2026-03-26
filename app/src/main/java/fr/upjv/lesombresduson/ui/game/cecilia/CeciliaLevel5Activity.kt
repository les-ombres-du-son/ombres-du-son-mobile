package fr.upjv.lesombresduson.ui.game.cecilia

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import fr.upjv.lesombresduson.R
import fr.upjv.lesombresduson.data.remote.FirebaseHelper
import fr.upjv.lesombresduson.data.remote.RealtimeHelper
import fr.upjv.lesombresduson.ui.game.cecilia.logic.QuizData
import fr.upjv.lesombresduson.ui.game.cecilia.util.BackGameActivity
import java.util.Locale

/**
 * Niveau 5 (Final) : Le Quiz de Sensibilisation.
 * Évalue la compréhension du joueur sur les enjeux d'accessibilité via une interaction 100% vocale.
 * Utilise la synthèse vocale (TTS) pour poser les questions et la reconnaissance vocale pour analyser les réponses.
 */
class CeciliaLevel5Activity : BackGameActivity(), TextToSpeech.OnInitListener {

    override val sessionName = "niveau_5_quiz"

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var speechRecognizer: SpeechRecognizer? = null

    private val RECORD_AUDIO_REQUEST_CODE = 101
    private val TAG = "CeciliaLevel5"

    private var currentQuestionIndex = 0
    private var correctAnswersCount = 0
    private var isListeningForAnswer = false

    private var isQuizActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this, this)
        checkMicrophonePermission()
    }

    /**
     * Détermine le statut actuel du niveau pour le suivi Firebase en temps réel.
     */
    override fun isPlaying(): Boolean {
        // Le joueur est considéré uniquement pendant le quiz actif
        return isQuizActive
    }

    /**
     * Vérifie si l'application possède la permission d'utiliser le microphone.
     * Si oui, initialise le moteur vocal et le jeu. Sinon, demande la permission à l'utilisateur.
     */
    private fun checkMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST_CODE)
        } else {
            setupSpeechRecognizer()
            initGame()
        }
    }

    /**
     * Gère la réponse de l'utilisateur à la demande de permission d'enregistrement audio.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupSpeechRecognizer()
            initGame()
        } else {
            Toast.makeText(this, "Permission micro requise pour le quiz", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    /**
     * Initialise la séquence de jeu.
     * Vérifie dans l'Intent si l'introduction audio a déjà été écoutée.
     * Si oui, lance directement le quiz. Sinon, joue l'audio puis enregistre la progression.
     */
    private fun initGame() {
        val isLevel5IntroFinished = intent.getBooleanExtra("level5IntroFinished", false)

        if (isLevel5IntroFinished) {
            Toast.makeText(this, "Reprise du quiz...", Toast.LENGTH_SHORT).show()
            startQuiz()
        } else {
            Toast.makeText(this, "Écoutez l'introduction...", Toast.LENGTH_SHORT).show()

            audioManager.playIntro(R.raw.voix_off_niveau5) {
                audioManager.playIntro(R.raw.chanson_final) {

                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                    if (userId != null) {
                        FirebaseHelper.getInstance().updateGameProgress(
                            userId,
                            "Cécilia (cécité totale)",
                            "level5IntroFinished",
                            true
                        )
                    }
                    startQuiz()
                }
            }
        }
    }

    /**
     * Callback déclenché lorsque le moteur de synthèse vocale (Text-To-Speech) est initialisé.
     * Configure la langue (français) et ajoute les écouteurs pour créer le système de ping-pong vocal.
     */
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.FRANCE
            isTtsReady = true

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}

                override fun onDone(utteranceId: String?) {
                    runOnUiThread {
                        if (utteranceId == "QUESTION_READ") {
                            startListening()
                        } else if (utteranceId == "EXPLANATION_READ") {
                            currentQuestionIndex++
                            askNextQuestion()
                        }
                    }
                }
                override fun onError(utteranceId: String?) {}
            })
        } else {
            Log.e(TAG, "Erreur initialisation TTS")
        }
    }

    /**
     * Démarre le quiz
     */
    private fun startQuiz() {
        isQuizActive = true
        RealtimeHelper.updateGameStatus("playing")

        if (isTtsReady) {
            askNextQuestion()
        } else {
            btnBack.postDelayed({ startQuiz() }, 500)
        }
    }

    /**
     * Récupère la question actuelle depuis `QuizData` et demande à la synthèse vocale de la lire.
     * Si toutes les questions ont été posées, déclenche le calcul du score final.
     */
    private fun askNextQuestion() {
        if (currentQuestionIndex < QuizData.questions.size) {
            val q = QuizData.questions[currentQuestionIndex]

            RealtimeHelper.updateStep("Question_${currentQuestionIndex + 1}")

            val textToRead = "${q.questionText} ... ${q.choice1} ... ${q.choice2} ... ${q.choice3} ... Dites 1, 2 ou 3. Ou dites répéter."

            Toast.makeText(this, "Question ${currentQuestionIndex + 1}/6", Toast.LENGTH_SHORT).show()

            val params = Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "QUESTION_READ")
            tts?.speak(textToRead, TextToSpeech.QUEUE_FLUSH, params, "QUESTION_READ")
        } else {
            isQuizActive = false
            calculateAndSaveScore()
        }
    }

    /**
     * Initialise le moteur de reconnaissance vocale Android (SpeechRecognizer).
     * Configure les comportements en cas de succès, d'échec ou de silence.
     */
    private fun setupSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { isListeningForAnswer = true }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { isListeningForAnswer = false }

                override fun onError(error: Int) {
                    if (!isQuizActive) return

                    if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        startListening()
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        handlePlayerAnswer(matches[0])
                    } else {
                        startListening()
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    /**
     * Allume le microphone pour écouter la réponse orale du joueur.
     */
    private fun startListening() {
        if (!isQuizActive) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.FRANCE)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
        Toast.makeText(this, "Parlez (1, 2, 3 ou répéter)", Toast.LENGTH_SHORT).show()
    }

    /**
     * Analyse le texte reconnu par le microphone.
     * Vérifie s'il s'agit d'une bonne réponse, d'une mauvaise, ou d'une demande de répétition,
     * puis enclenche la voix off d'explication appropriée.
     *
     * @param spokenText Le texte brut capturé par le microphone.
     */
    private fun handlePlayerAnswer(spokenText: String) {
        if (!isQuizActive) return

        val extractedChoice = extractChoiceNumber(spokenText)

        if (extractedChoice != null) {
            // Utilisation de QuizData.questions !
            val q = QuizData.questions[currentQuestionIndex]

            if (extractedChoice == q.correctAnswer) {
                correctAnswersCount++
                Toast.makeText(this, "Bonne réponse !", Toast.LENGTH_SHORT).show()
                RealtimeHelper.updateStep("Q${currentQuestionIndex+1}_Correct")
            } else {
                Toast.makeText(this, "Mauvaise réponse...", Toast.LENGTH_SHORT).show()
                RealtimeHelper.updateStep("Q${currentQuestionIndex+1}_Erreur")
            }

            val params = Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "EXPLANATION_READ")
            tts?.speak(q.explanation, TextToSpeech.QUEUE_FLUSH, params, "EXPLANATION_READ")

        } else if (isRepeatRequest(spokenText)) {
            Toast.makeText(this, "Répétition de la question...", Toast.LENGTH_SHORT).show()
            askNextQuestion()

        } else {
            tts?.speak("Je n'ai pas compris. Veuillez dire un, deux, ou trois. Ou dites répéter.", TextToSpeech.QUEUE_FLUSH, null, null)
            btnBack.postDelayed({ startListening() }, 4500)
        }
    }

    /**
     * Nettoie le texte capturé vocalement pour isoler un choix valide (1, 2 ou 3),
     * en prenant en compte les erreurs de transcription courantes (un, de, etc.).
     *
     * @return Le numéro du choix sous forme de String, ou null si non reconnu.
     */
    private fun extractChoiceNumber(spokenText: String): String? {
        val text = spokenText.lowercase().trim()
        if (text.contains("1") || text.contains("un") || text.contains("une") || text.contains("premier")) return "1"
        if (text.contains("2") || text.contains("deux") || text.contains("de") || text.contains("second")) return "2"
        if (text.contains("3") || text.contains("trois") || text.contains("troisième")) return "3"
        return null
    }

    /**
     * Détermine si le texte capturé correspond à une demande de répétition de la part du joueur.
     */
    private fun isRepeatRequest(spokenText: String): Boolean {
        val text = spokenText.lowercase().trim()
        return text.contains("répéter") ||
                text.contains("répète") ||
                text.contains("compris") ||
                text.contains("encore") ||
                text.contains("pardon")
    }

    /**
     * Calcule le score final sur 100, détermine le profil d'inclusion du joueur,
     * sauvegarde les résultats dans Firebase et affiche la popup de fin de niveau.
     */
    private fun calculateAndSaveScore() {
        // Utilisation de QuizData.questions.size !
        val totalQuestions = QuizData.questions.size
        val finalScore = ((correctAnswersCount.toDouble() / totalQuestions) * 100).toInt()

        val profilJoueur = when {
            finalScore == 100 -> "L'Ambassadeur Inclusif"
            finalScore >= 60 -> "L'Allié Averti"
            else -> "Le Novice en Apprentissage"
        }

        val metrics = mapOf(
            "reponses_correctes" to correctAnswersCount,
            "total_questions" to totalQuestions
        )
        syncManager.checkNetworkAndSave("Niveau5_Quiz", finalScore, profilJoueur, metrics)

        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            FirebaseHelper.getInstance().updateGameProgress(uid, "Cécilia (cécité totale)", "gameFinished", true)
        }

        val details = "Questions réussies : $correctAnswersCount / $totalQuestions"
        val speechText = "Félicitations, vous avez terminé la formation. Votre score final de sensibilisation est de $finalScore pour cent. Votre profil est : $profilJoueur."

        showLevelCompleteDialog(
            score = finalScore,
            profil = profilJoueur,
            details = details,
            speechText = speechText,
            nextActivityClass = CeciliaFinalScoreActivity::class.java
        )
    }

    // =========================================================================
    //                      CYCLE DE VIE (Sécurité Micro & TTS)
    // =========================================================================

    override fun onPause() {
        super.onPause()

        if (isQuizActive) {
            speechRecognizer?.stopListening()
            tts?.stop()
            isListeningForAnswer = false
        }
    }

    override fun onResume() {
        super.onResume()

        if (isQuizActive) {
            askNextQuestion()
        }
    }

    /**
     * Nettoie les ressources vocales et de reconnaissance à la destruction de l'activité.
     */
    override fun onDestroy() {
        super.onDestroy()
        isQuizActive = false
        tts?.stop()
        tts?.shutdown()
        speechRecognizer?.destroy()
    }
}