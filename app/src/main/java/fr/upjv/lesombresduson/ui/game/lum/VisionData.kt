package fr.upjv.lesombresduson.ui.game.lum

import fr.upjv.lesombresduson.R

object VisionData {
    // 1. Liste des filtres
    val filters = listOf(
        android.R.color.transparent,
        R.drawable.filtre_tache_centrale,
        R.drawable.filtre_moitie_ecran,
        R.drawable.filtre_glaucome_tunnel,
        R.drawable.filtre_cataracte,
        R.drawable.filtre_retinopathie_taches,
        R.drawable.filtre_vision_floue
    )

    // 2. Liste des noms associés aux filtres
    val diseaseNames = listOf(
        "Vision Normale",
        "DMLA (Tache centrale)",
        "Hémianopsie (Moitié d'écran)",
        "Glaucome (Vision en tunnel)",
        "Cataracte (Voile opaque)",
        "Rétinopathie (Taches)",
        "Myopie Sévère (Vision floue)"
    )

    // 3. Liste des points associés aux filtres
    val diseasePoints = listOf(1, 12, 2, 5, 8, 10, 4)

    // 4. Explications médicales du handicap
    val diseaseDefinitions = listOf(
        "Vision standard sans altération.",
        "Dégénérescence Maculaire Liée à l'Âge. Elle détruit sélectivement le centre de la rétine (la macula), laissant la vision périphérique intacte.",
        "Perte de la moitié du champ visuel d'un œil ou des deux yeux, fréquemment provoquée par une lésion cérébrale ou un accident vasculaire.",
        "Maladie liée à une pression oculaire trop élevée qui détruit progressivement le nerf optique depuis la périphérie vers le centre.",
        "Opacification progressive du cristallin (la lentille naturelle de l'œil), qui survient généralement avec le vieillissement.",
        "Altération des vaisseaux sanguins de la rétine causée par un excès de sucre prolongé dans le sang (complication du diabète).",
        "Anomalie de la réfraction de l'œil où les objets lointains sont flous parce que l'image se forme en avant de la rétine."
    )

    // 5. Impacts réels et concrets dans la vie quotidienne
    val diseaseRealImpacts = listOf(
        "Aucun impact au quotidien.",
        "Rend impossible la lecture, la conduite, et empêche de reconnaître le visage de la personne en face de soi. On doit regarder 'à côté' pour deviner.",
        "On se cogne constamment aux obstacles situés du côté aveugle. Manger devient complexe car on ne voit littéralement qu'une moitié de son assiette.",
        "Vision en 'canon de fusil'. On ne voit plus les voitures ou piétons arriver sur les côtés. Risque de chute extrême à cause des marches invisibles.",
        "Perte totale des contrastes et de la vivacité des couleurs (les bleus paraissent gris/verts). Éblouissement massif et dangereux par les phares la nuit.",
        "La vision fluctue d'un jour à l'autre avec des taches sombres mobiles. Planifier une simple sortie devient anxiogène et imprévisible.",
        "Sans lunettes, impossibilité de lire les panneaux de signalisation, de travailler sur écran ou de distinguer un ami d'un inconnu à deux mètres."
    )
}