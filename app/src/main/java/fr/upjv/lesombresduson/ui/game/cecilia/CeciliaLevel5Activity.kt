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
import fr.upjv.lesombresduson.ui.game.cecilia.util.BackGameActivity
import java.util.Locale

/**
 * Niveau 5 (Final) : Le Quiz de Sensibilisation.
 * Évalue la compréhension du joueur sur les enjeux d'accessibilité via une interaction 100% vocale.
 */
class CeciliaLevel5Activity : BackGameActivity(), TextToSpeech.OnInitListener {

    private lateinit var btnBack: Button

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var speechRecognizer: SpeechRecognizer? = null

    private val RECORD_AUDIO_REQUEST_CODE = 101
    private val TAG = "CeciliaLevel5"

    private var currentQuestionIndex = 0
    private var correctAnswersCount = 0
    private var isListeningForAnswer = false

    data class QuizQuestion(
        val questionText: String,
        val choice1: String,
        val choice2: String,
        val choice3: String,
        val correctAnswer: String,
        val explanation: String
    )

    private val quizQuestions = listOf(
        QuizQuestion(
            questionText = "Question 1. Au début du jeu, agir pendant que je parlais faisait baisser votre score. Dans un environnement de travail, pourquoi un bruit inattendu ou une coupure de parole est-il particulièrement désagréable pour un collègue non-voyant ?",
            choice1 = "Choix 1 : Parce que la perte de la vue provoque presque toujours une hypersensibilité médicale aux bruits environnants.",
            choice2 = "Choix 2 : Parce que l'ouïe est son radar : couvrir un son revient littéralement à l'aveugler au milieu d'une action.",
            choice3 = "Choix 3 : Parce qu'il doit fournir un effort de mémorisation double pour retenir les échanges oral lors d'une réunion.",
            correctAnswer = "2",
            explanation = "La bonne réponse était la deux. Contrairement aux idées reçues, l'ouïe ne s'améliore pas médicalement avec la cécité. L'ouïe sert à cartographier l'espace et les situations. Interrompre une information sonore, c'est comme éteindre la lumière pour une personne voyante."
        ),
        QuizQuestion(
            questionText = "Question 2. Dans le premier niveau, générer trop de sons avec votre sonar vous pénalisait. Dans la rue, pourquoi un environnement urbain très bruyant, comme un carrefour en travaux, est-il un obstacle si critique pour une personne non-voyante ?",
            choice1 = "Choix 1 : Parce que cela l'oblige à arrêter d'utiliser sa canne blanche, dont le bruit de tapotement au sol doit absolument être entendu pour fonctionner.",
            choice2 = "Choix 2 : Parce que ce mur de bruit masque les repères sonores naturels indispensables pour s'orienter, créant un véritable brouillard spatial.",
            choice3 = "Choix 3 : Parce que les feux sonores pour piétons se désactivent automatiquement par mesure de sécurité lorsque le niveau de décibels ambiant est trop élevé.",
            correctAnswer = "2",
            explanation = "La bonne réponse est la deux. Le bruit ambiant agit comme un brouillard épais. Il masque l'écho des murs et le bruit de la circulation, qui sont les seuls repères pour marcher droit. La solution en tant que citoyen ? Si vous circulez à vélo ou en trottinette électrique dans une zone bruyante, signalez toujours vocalement votre présence, car la personne ne pourra pas entendre votre approche."
        ),
        QuizQuestion(
            questionText = "Question 3. Le niveau 2 exigeait d'attendre patiemment le bon signal pour avancer. Dans la réalité, lorsqu'une personne non-voyante attend pour traverser une rue, quelle action d'un passant, qui pense pourtant bien faire, est en fait la plus dangereuse ?",
            choice1 = "Choix 1 : Appuyer à sa place sur le bouton jaune situé sur le poteau du feu tricolore.",
            choice2 = "Choix 2 : Lui répéter de faire très attention car la circulation est dense et qu'elle risque de se faire écraser.",
            choice3 = "Choix 3 : L'attraper par le bras ou par sa canne sans prévenir pour la guider.",
            correctAnswer = "3",
            explanation = "La bonne réponse est la trois. Saisir une personne aveugle par surprise, c'est ce qu'on appelle le guidage sauvage. Cela lui fait perdre tous ses repères spatiaux et peut provoquer un accident. La solution est simple : demandez toujours oralement si la personne a besoin d'aide. Et si c'est le cas, ne la tirez pas, proposez-lui de prendre VOTRE bras."
        ),
        QuizQuestion(
            questionText = "Question 4. Dans le niveau 3, vous vous êtes repéré dans l'espace uniquement grâce au toucher et aux vibrations. Imaginez la situation suivante : dans votre école ou votre entreprise, une personne malvoyante se trompe régulièrement de salle de classe ou de réunion. Quel aménagement simple, basé sur le toucher, permet de résoudre ce problème définitivement ?",
            choice1 = "Choix 1 : Installer des plaques signalétiques avec des numéros en relief et en braille à côté de chaque poignée de porte.",
            choice2 = "Choix 2 : Lui demander de mémoriser le nombre exact de portes à compter depuis l'entrée principale du bâtiment.",
            choice3 = "Choix 3 : Lui fournir une application GPS d'intérieur sur son téléphone qui vibre devant la bonne salle.",
            correctAnswer = "1",
            explanation = "La bonne réponse est la une. La technologie a ses limites en intérieur, et compter les portes demande un effort mental épuisant. Placer des numéros en relief et en braille sur les portes des écoles, des mairies ou des entreprises est une solution simple, fiable et universelle. C'est cet aménagement concret qui garantit une véritable autonomie au quotidien."
        ),
        QuizQuestion(
            questionText = "Question 5. Dans le niveau 4, vous avez utilisé votre voix pour communiquer. À la maison, les personnes malvoyantes utilisent la voix de synthèse de leur téléphone pour lire internet. Pourtant, beaucoup de sites web leur sont totalement inaccessibles. Quelle en est la raison principale ?",
            choice1 = "Choix 1 : Les boutons et les images de ces sites n'ont pas de description invisible que la voix du téléphone pourrait lire.",
            choice2 = "Choix 2 : Elles doivent acheter un ordinateur ou un téléphone spécifique et très coûteux pour pouvoir entendre les pages web.",
            choice3 = "Choix 3 : Les sites internet n'intègrent pas de bouton microphone permettant de naviguer uniquement en parlant.",
            correctAnswer = "1",
            explanation = "La bonne réponse est la une. C'est ce qu'on appelle l'accessibilité numérique. Aujourd'hui, tous les téléphones sont équipés gratuitement de voix de synthèse très performantes. Le vrai problème vient des sites internet mal conçus : si un bouton ou une image n'a pas de texte alternatif caché dans son code, la voix du téléphone restera muette. Internet devient alors un mur invisible."
        ),
        QuizQuestion(
            questionText = "Question 6. Pour clôturer cette expérience, vous avez affronté la rue, exploré des bâtiments et navigué sur le numérique sans utiliser vos yeux. Quelle est la véritable définition de l'inclusion face au handicap visuel ?",
            choice1 = "Choix 1 : Qu'il faut traiter absolument tout le monde de manière strictement identique, sans aucune distinction, pour être parfaitement juste.",
            choice2 = "Choix 2 : Qu'elle consiste à fournir des outils et un environnement adaptés aux besoins spécifiques de chacun pour garantir la même autonomie.",
            choice3 = "Choix 3 : Qu'une société inclusive est une société où les personnes voyantes font systématiquement les choses à la place des personnes malvoyantes pour les protéger.",
            correctAnswer = "2",
            explanation = "La bonne réponse est la deux. C’est la grande leçon de ce jeu : comprendre la différence entre l'égalité et l'équité. L'égalité, c'est construire exactement la même ville pour tout le monde, quitte à laisser certains citoyens face à des obstacles infranchissables. L'équité, c'est d'adapter notre environnement pour que chacun ait la même liberté de mouvement et de réussite. L'inclusion ne consiste pas à agir à la place de l'autre par pitié, mais à aménager la société pour lui rendre sa totale autonomie. C'est maintenant à vous d'agir dans le monde réel !"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gameplay_cecilia)

        btnBack = findViewById(R.id.button_back)
        setupBackButton(btnBack)

        RealtimeHelper.startSession("niveau_5_quiz")

        tts = TextToSpeech(this, this)
        checkMicrophonePermission()
    }

    private fun checkMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST_CODE)
        } else {
            setupSpeechRecognizer()
            initGame()
        }
    }

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

    private fun startQuiz() {
        if (isTtsReady) {
            askNextQuestion()
        } else {
            btnBack.postDelayed({ startQuiz() }, 500)
        }
    }

    private fun askNextQuestion() {
        if (currentQuestionIndex < quizQuestions.size) {
            val q = quizQuestions[currentQuestionIndex]
            // On a rajouté "ou dites répéter" dans l'instruction
            val textToRead = "${q.questionText} ... ${q.choice1} ... ${q.choice2} ... ${q.choice3} ... Dites 1, 2 ou 3. Ou dites répéter."

            Toast.makeText(this, "Question ${currentQuestionIndex + 1}/6", Toast.LENGTH_SHORT).show()

            val params = Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "QUESTION_READ")
            tts?.speak(textToRead, TextToSpeech.QUEUE_FLUSH, params, "QUESTION_READ")
        } else {
            calculateAndSaveScore()
        }
    }

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

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.FRANCE)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
        Toast.makeText(this, "Parlez (1, 2, 3 ou répéter)", Toast.LENGTH_SHORT).show()
    }

    private fun handlePlayerAnswer(spokenText: String) {
        val extractedChoice = extractChoiceNumber(spokenText)

        if (extractedChoice != null) {
            val q = quizQuestions[currentQuestionIndex]

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
            // ---> AJOUT : Le joueur a demandé à répéter !
            Toast.makeText(this, "Répétition de la question...", Toast.LENGTH_SHORT).show()
            askNextQuestion() // On relance simplement la lecture de la question actuelle

        } else {
            // Modification du texte d'erreur pour rappeler la commande "répéter"
            tts?.speak("Je n'ai pas compris. Veuillez dire un, deux, ou trois. Ou dites répéter.", TextToSpeech.QUEUE_FLUSH, null, null)
            btnBack.postDelayed({ startListening() }, 4500)
        }
    }

    private fun extractChoiceNumber(spokenText: String): String? {
        val text = spokenText.lowercase().trim()
        if (text.contains("1") || text.contains("un") || text.contains("une") || text.contains("premier")) return "1"
        if (text.contains("2") || text.contains("deux") || text.contains("de") || text.contains("second")) return "2"
        if (text.contains("3") || text.contains("trois") || text.contains("troisième")) return "3"
        return null
    }

    /**
     * Analyse le texte capturé pour voir si le joueur demande à répéter la question.
     */
    private fun isRepeatRequest(spokenText: String): Boolean {
        val text = spokenText.lowercase().trim()
        return text.contains("répéter") ||
                text.contains("répète") ||
                text.contains("compris") ||
                text.contains("encore") ||
                text.contains("pardon")
    }

    private fun calculateAndSaveScore() {
        val finalScore = ((correctAnswersCount.toDouble() / quizQuestions.size) * 100).toInt()

        val profilJoueur = when {
            finalScore == 100 -> "L'Ambassadeur Inclusif"
            finalScore >= 60 -> "L'Allié Averti"
            else -> "Le Novice en Apprentissage"
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val metrics = mapOf(
                "reponses_correctes" to correctAnswersCount,
                "total_questions" to quizQuestions.size
            )
            syncManager.checkNetworkAndSave(userId, "Niveau5_Quiz", finalScore, profilJoueur, metrics)
            FirebaseHelper.getInstance().updateGameProgress(userId, "Cécilia (cécité totale)", "gameFinished", true)
        }

        val details = "Questions réussies : $correctAnswersCount / ${quizQuestions.size}"
        val speechText = "Félicitations, vous avez terminé la formation. Votre score final de sensibilisation est de $finalScore pour cent. Votre profil est : $profilJoueur."

        showLevelCompleteDialog(
            score = finalScore,
            profil = profilJoueur,
            details = details,
            speechText = speechText,
            nextActivityClass = null
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
        speechRecognizer?.destroy()
        RealtimeHelper.endSession()
    }
}