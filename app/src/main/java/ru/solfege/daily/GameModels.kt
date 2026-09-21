package ru.solfege.daily

enum class SkillDomain(val title: String) {
    PULSE("Пульс"),
    RHYTHM("Ритм"),
    PITCH("Высота"),
    TONALITY("Лад и ступени"),
    READING("Чтение нот"),
    WRITING("Музыкальная запись"),
    INTERVALS("Интервалы"),
    KEYS("Тональности"),
    MEMORY("Музыкальная память"),
    CREATIVITY("Музыкальное мышление")
}

enum class GameType {
    AURAL_CHOICE,
    RHYTHM_CHOICE,
    TONAL_CHOICE,
    NOTATION_CHOICE,
    MELODY_BUILD,
    MEASURE_BUILD,
    STAFF_BUILD,
    PITCH_PRODUCTION,
    RHYTHM_PRODUCTION,
    CREATIVE_TRANSFER
}

data class AnswerOption(
    val id: String,
    val label: String,
    val rhythm: List<Double> = emptyList(),
    val midi: List<Int> = emptyList()
)

data class Challenge(
    val id: String,
    val missionId: Int,
    val type: GameType,
    val skill: SkillDomain,
    val title: String,
    val instruction: String,
    val audioMidi: List<Int> = emptyList(),
    val audioRhythm: List<Double> = emptyList(),
    val options: List<AnswerOption> = emptyList(),
    val correctOptionId: String? = null,
    val targetMidi: List<Int> = emptyList(),
    val targetRhythmBeats: Double = 0.0,
    val allowedDurations: List<Double> = emptyList(),
    val maxReplays: Int = 3,
    val hint: String = "",
    val explanation: String = "",
    val difficulty: Int = 1,
    val scored: Boolean = true
) {
    init {
        if (type in setOf(
                GameType.AURAL_CHOICE,
                GameType.RHYTHM_CHOICE,
                GameType.TONAL_CHOICE,
                GameType.NOTATION_CHOICE
            )) {
            require(options.size >= 3) { "Multiple-choice challenge must have at least 3 options" }
            require(correctOptionId != null) { "Multiple-choice challenge requires a correct option" }
            require(options.count { it.id == correctOptionId } == 1) { "Exactly one correct option required" }
            require(options.map { it.id }.distinct().size == options.size) { "Option ids must be unique" }
        }
        if (type == GameType.MELODY_BUILD || type == GameType.STAFF_BUILD) {
            require(targetMidi.size >= 2) { "Constructive melodic challenge requires a target" }
        }
        if (type == GameType.MEASURE_BUILD) {
            require(targetRhythmBeats > 0.0)
            require(allowedDurations.size >= 2)
        }
    }
}

data class ChallengeResult(
    val challengeId: String,
    val skill: SkillDomain,
    val correct: Boolean,
    val attempts: Int,
    val hintsUsed: Int,
    val score: Double
)

data class SessionSummary(
    val missionId: Int,
    val totalChallenges: Int,
    val scoredChallenges: Int,
    val correctFirstTry: Int,
    val totalScore: Double,
    val stars: Int,
    val skillScores: Map<SkillDomain, Double>
)

data class MissionMeta(
    val id: Int,
    val week: Int,
    val day: Int,
    val world: Int,
    val worldTitle: String,
    val title: String,
    val subtitle: String
)

data class SkillStat(
    val domain: SkillDomain,
    val attempts: Int,
    val mastery: Double
)
