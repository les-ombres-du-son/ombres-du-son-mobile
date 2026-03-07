package fr.upjv.lesombresduson.ui.game.cecilia.logic

/**
 * Fournit les données (Data) pour le quiz final de sensibilisation du Niveau 5.
 */
object QuizData {

    /**
     * Modèle de données pour une question du quiz de sensibilisation.
     */
    data class QuizQuestion(
        val questionText: String,
        val choice1: String,
        val choice2: String,
        val choice3: String,
        val correctAnswer: String,
        val explanation: String
    )

    /**
     * Liste de questions et réponses pour le quiz de sensibilisation.
     * Chaque question est composée de trois réponses.
     */
    val questions = listOf(
        QuizQuestion(
            questionText = "Question 1. Au début du jeu, agir pendant que je parlais faisait baisser votre score. Dans un environnement de travail, pourquoi un bruit inattendu ou une coupure de parole est-il particulièrement désagréable pour un collègue non-voyant ?",
            choice1 = "Choix 1 : Parce que la perte de la vue provoque presque toujours une hypersensibilité médicale aux bruits environnants.",
            choice2 = "Choix 2 : Parce que l'ouïe est son radar : couvrir un son revient littéralement à l'aveugler au milieu d'une action.",
            choice3 = "Choix 3 : Parce qu'il doit fournir un effort de mémorisation double pour retenir les échanges oraux lors d'une réunion.",
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
}