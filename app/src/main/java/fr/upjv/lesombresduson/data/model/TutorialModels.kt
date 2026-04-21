package fr.upjv.lesombresduson.data.model

data class TutorialConfig(
    val tutorials: List<TutorialStep>
)

data class TutorialStep(
    val id: String,
    val character: String,
    val title: String,
    val description: String,
    val imageRes: String,
    var isCompleted: Boolean = false
)