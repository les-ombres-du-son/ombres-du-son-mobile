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
}