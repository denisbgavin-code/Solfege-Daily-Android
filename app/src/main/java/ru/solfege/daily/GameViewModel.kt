package ru.solfege.daily

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

enum class AppScreen {
    HOME, MAP, GAME, PROGRESS, LAB, ADULT
}

enum class AnswerPhase {
    ANSWERING, FEEDBACK, SESSION_COMPLETE
}

data class GameState(
    val screen: AppScreen = AppScreen.HOME,
    val missionId: Int = 1,
    val reviewMode: Boolean = false,
    val challenges: List<Challenge> = emptyList(),
    val challengeIndex: Int = 0,
    val phase: AnswerPhase = AnswerPhase.ANSWERING,
    val selectedOptionId: String? = null,
    val melodyInput: List<Int> = emptyList(),
    val measureInput: List<Double> = emptyList(),
    val attemptsOnCurrent: Int = 0,
    val hintsUsedCurrent: Int = 0,
    val replayCount: Int = 0,
    val feedbackCorrect: Boolean? = null,
    val feedbackTitle: String = "",
    val feedbackText: String = "",
    val canRetry: Boolean = false,
    val results: List<ChallengeResult> = emptyList(),
    val summary: SessionSummary? = null,
    val completedMission: Int = 0,
    val totalStars: Int = 0,
    val streak: Int = 0,
    val skillStats: List<SkillStat> = emptyList(),
    val teacherMode: Boolean = false
) {
    val currentChallenge: Challenge?
        get() = challenges.getOrNull(challengeIndex)

    val progressFraction: Float
        get() = if (challenges.isEmpty()) 0f else challengeIndex.toFloat() / challenges.size.toFloat()
}

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = ProgressRepository(application)

    private val _state = MutableStateFlow(
        GameState(
            missionId = repo.nextMission(),
            completedMission = repo.completedMission(),
            totalStars = repo.totalStars(),
            streak = repo.streak(),
            skillStats = repo.stats()
        )
    )
    val state: StateFlow<GameState> = _state.asStateFlow()

    fun go(screen: AppScreen) {
        _state.value = refreshMeta(_state.value.copy(screen = screen))
    }

    fun setTeacherMode(enabled: Boolean) {
        _state.value = _state.value.copy(teacherMode = enabled)
    }

    fun startNextMission() {
        val next = repo.nextMission()
        startMission(next, review = false)
    }

    fun startMission(id: Int, review: Boolean = false) {
        val safe = id.coerceIn(1, Course.TOTAL_LESSONS)
        if (!review && !_state.value.teacherMode && !repo.isUnlocked(safe)) return

        val challenges = ChallengeEngine.generateSession(safe) { repo.mastery(it) }
        _state.value = refreshMeta(
            GameState(
                screen = AppScreen.GAME,
                missionId = safe,
                reviewMode = review,
                challenges = challenges,
                challengeIndex = 0,
                phase = AnswerPhase.ANSWERING,
                completedMission = repo.completedMission(),
                totalStars = repo.totalStars(),
                streak = repo.streak(),
                skillStats = repo.stats(),
                teacherMode = _state.value.teacherMode
            )
        )
    }

    fun startSmartReview() {
        if (repo.completedMission() == 0) {
            startMission(1, review = true)
            return
        }
        startMission(repo.smartReviewMission(), review = true)
    }

    fun selectOption(id: String) {
        val s = _state.value
        if (s.phase != AnswerPhase.ANSWERING) return
        val challenge = s.currentChallenge ?: return
        if (challenge.type !in choiceTypes) return
        if (challenge.options.none { it.id == id }) return
        _state.value = s.copy(selectedOptionId = id)
    }

    fun addMidi(midi: Int) {
        val s = _state.value
        if (s.phase != AnswerPhase.ANSWERING) return
        val challenge = s.currentChallenge ?: return
        if (challenge.type !in setOf(GameType.MELODY_BUILD, GameType.STAFF_BUILD)) return
        if (s.melodyInput.size >= challenge.targetMidi.size) return
        _state.value = s.copy(melodyInput = s.melodyInput + midi)
    }

    fun undoMidi() {
        val s = _state.value
        if (s.phase != AnswerPhase.ANSWERING || s.melodyInput.isEmpty()) return
        _state.value = s.copy(melodyInput = s.melodyInput.dropLast(1))
    }

    fun clearMidi() {
        val s = _state.value
        if (s.phase != AnswerPhase.ANSWERING) return
        _state.value = s.copy(melodyInput = emptyList())
    }

    fun addDuration(duration: Double) {
        val s = _state.value
        if (s.phase != AnswerPhase.ANSWERING) return
        val challenge = s.currentChallenge ?: return
        if (challenge.type != GameType.MEASURE_BUILD) return
        if (duration !in challenge.allowedDurations) return
        val next = s.measureInput + duration
        val total = next.sum()
        if (total > challenge.targetRhythmBeats + 0.001) return
        _state.value = s.copy(measureInput = next)
    }

    fun undoDuration() {
        val s = _state.value
        if (s.phase != AnswerPhase.ANSWERING || s.measureInput.isEmpty()) return
        _state.value = s.copy(measureInput = s.measureInput.dropLast(1))
    }

    fun clearDuration() {
        val s = _state.value
        if (s.phase != AnswerPhase.ANSWERING) return
        _state.value = s.copy(measureInput = emptyList())
    }

    fun useHint() {
        val s = _state.value
        if (s.phase != AnswerPhase.ANSWERING) return
        _state.value = s.copy(hintsUsedCurrent = s.hintsUsedCurrent + 1)
    }

    fun replayUsed() {
        val s = _state.value
        if (s.phase != AnswerPhase.ANSWERING) return
        val challenge = s.currentChallenge ?: return
        if (s.replayCount >= challenge.maxReplays) return
        _state.value = s.copy(replayCount = s.replayCount + 1)
    }

    fun submitCurrent() {
        val s = _state.value
        if (s.phase != AnswerPhase.ANSWERING) return
        val challenge = s.currentChallenge ?: return

        when (challenge.type) {
            GameType.AURAL_CHOICE,
            GameType.RHYTHM_CHOICE,
            GameType.TONAL_CHOICE,
            GameType.NOTATION_CHOICE -> submitChoice(challenge, s)

            GameType.MELODY_BUILD,
            GameType.STAFF_BUILD -> submitMelody(challenge, s)

            GameType.MEASURE_BUILD -> submitMeasure(challenge, s)

            GameType.PITCH_PRODUCTION,
            GameType.RHYTHM_PRODUCTION,
            GameType.CREATIVE_TRANSFER -> Unit
        }
    }

    fun completeProduction(score: Double = 0.75) {
        val s = _state.value
        if (s.phase != AnswerPhase.ANSWERING) return
        val challenge = s.currentChallenge ?: return
        if (challenge.type !in setOf(
                GameType.PITCH_PRODUCTION,
                GameType.RHYTHM_PRODUCTION,
                GameType.CREATIVE_TRANSFER
            )) return

        val effective = if (challenge.scored) score.coerceIn(0.2, 1.0) else 0.85
        finalizeAnswer(
            s = s,
            challenge = challenge,
            correct = true,
            score = effective,
            title = if (challenge.scored) "Готово" else "Твоя версия принята",
            text = challenge.explanation
        )
    }

    fun retryCurrent() {
        val s = _state.value
        if (s.phase != AnswerPhase.FEEDBACK || !s.canRetry) return
        _state.value = s.copy(
            phase = AnswerPhase.ANSWERING,
            selectedOptionId = null,
            melodyInput = emptyList(),
            measureInput = emptyList(),
            feedbackCorrect = null,
            feedbackTitle = "",
            feedbackText = "",
            canRetry = false
        )
    }

    fun nextChallenge() {
        val s = _state.value
        if (s.phase != AnswerPhase.FEEDBACK || s.canRetry) return

        val nextIndex = s.challengeIndex + 1
        if (nextIndex >= s.challenges.size) {
            finishSession(s)
            return
        }

        _state.value = s.copy(
            challengeIndex = nextIndex,
            phase = AnswerPhase.ANSWERING,
            selectedOptionId = null,
            melodyInput = emptyList(),
            measureInput = emptyList(),
            attemptsOnCurrent = 0,
            hintsUsedCurrent = 0,
            replayCount = 0,
            feedbackCorrect = null,
            feedbackTitle = "",
            feedbackText = "",
            canRetry = false
        )
    }

    fun exitSession() {
        _state.value = refreshMeta(_state.value.copy(screen = AppScreen.HOME))
    }

    fun resetProgress() {
        repo.reset()
        _state.value = GameState(
            screen = AppScreen.HOME,
            missionId = 1,
            completedMission = 0,
            totalStars = 0,
            streak = 0,
            skillStats = repo.stats(),
            teacherMode = _state.value.teacherMode
        )
    }

    private fun submitChoice(challenge: Challenge, s: GameState) {
        val selected = s.selectedOptionId ?: return
        val correct = selected == challenge.correctOptionId
        val attempt = s.attemptsOnCurrent + 1

        if (correct) {
            val score = scoreFor(attempt, s.hintsUsedCurrent)
            finalizeAnswer(
                s.copy(attemptsOnCurrent = attempt),
                challenge,
                true,
                score,
                if (attempt == 1) "Точно" else "Теперь получилось",
                challenge.explanation
            )
        } else if (attempt == 1) {
            _state.value = s.copy(
                phase = AnswerPhase.FEEDBACK,
                attemptsOnCurrent = attempt,
                feedbackCorrect = false,
                feedbackTitle = "Не совпало",
                feedbackText = diagnosticFeedback(challenge, selected),
                canRetry = true
            )
        } else {
            val correctLabel = challenge.options.firstOrNull { it.id == challenge.correctOptionId }?.label.orEmpty()
            finalizeAnswer(
                s.copy(attemptsOnCurrent = attempt),
                challenge,
                false,
                scoreForWrong(attempt, s.hintsUsedCurrent),
                "Разберём ответ",
                "Правильный вариант: «" + correctLabel + "». " + challenge.explanation
            )
        }
    }

    private fun submitMelody(challenge: Challenge, s: GameState) {
        if (s.melodyInput.size != challenge.targetMidi.size) return
        val attempt = s.attemptsOnCurrent + 1
        val exact = s.melodyInput == challenge.targetMidi
        val perNote = s.melodyInput.zip(challenge.targetMidi).count { it.first == it.second }
        val fraction = perNote.toDouble() / challenge.targetMidi.size.toDouble()

        if (exact) {
            finalizeAnswer(
                s.copy(attemptsOnCurrent = attempt),
                challenge,
                true,
                scoreFor(attempt, s.hintsUsedCurrent),
                "Мелодия собрана",
                challenge.explanation
            )
        } else if (attempt == 1) {
            val firstWrong = s.melodyInput.indices.firstOrNull { s.melodyInput[it] != challenge.targetMidi[it] }
            val direction = if (firstWrong == null) "" else {
                val got = s.melodyInput[firstWrong]
                val target = challenge.targetMidi[firstWrong]
                if (got < target) "В первом несовпавшем месте твой звук ниже нужного."
                else "В первом несовпавшем месте твой звук выше нужного."
            }
            _state.value = s.copy(
                phase = AnswerPhase.FEEDBACK,
                attemptsOnCurrent = attempt,
                feedbackCorrect = false,
                feedbackTitle = "Почти",
                feedbackText = "Совпало " + perNote + " из " + challenge.targetMidi.size + " звуков. " + direction,
                canRetry = true
            )
        } else {
            finalizeAnswer(
                s.copy(attemptsOnCurrent = attempt),
                challenge,
                false,
                (0.25 + 0.5 * fraction).coerceAtMost(0.72),
                "Сравним по нотам",
                "Совпало " + perNote + " из " + challenge.targetMidi.size + ". " + challenge.explanation
            )
        }
    }

    private fun submitMeasure(challenge: Challenge, s: GameState) {
        val target = challenge.audioRhythm
        if (s.measureInput.isEmpty()) return
        if (abs(s.measureInput.sum() - challenge.targetRhythmBeats) > 0.001) {
            _state.value = s.copy(
                feedbackCorrect = false,
                feedbackTitle = "Такт ещё не заполнен",
                feedbackText = "Сейчас " + formatBeat(s.measureInput.sum()) + " из " + formatBeat(challenge.targetRhythmBeats) + " долей."
            )
            return
        }

        val attempt = s.attemptsOnCurrent + 1
        val exact = sameRhythm(s.measureInput, target)
        val samePositions = s.measureInput.zip(target).count { abs(it.first - it.second) < 0.001 }
        val fraction = samePositions.toDouble() / maxOf(1, target.size).toDouble()

        if (exact) {
            finalizeAnswer(
                s.copy(attemptsOnCurrent = attempt),
                challenge,
                true,
                scoreFor(attempt, s.hintsUsedCurrent),
                "Ритм совпал",
                challenge.explanation
            )
        } else if (attempt == 1) {
            _state.value = s.copy(
                phase = AnswerPhase.FEEDBACK,
                attemptsOnCurrent = attempt,
                feedbackCorrect = false,
                feedbackTitle = "Сумма верная, порядок — нет",
                feedbackText = "Ты заполнил весь такт, но длинные и короткие звуки расположены иначе. Послушай ещё раз.",
                canRetry = true
            )
        } else {
            finalizeAnswer(
                s.copy(attemptsOnCurrent = attempt),
                challenge,
                false,
                (0.25 + 0.45 * fraction).coerceAtMost(0.68),
                "Сравним рисунок",
                "Такт заполнен, но последовательность отличается. " + challenge.explanation
            )
        }
    }

    private fun finalizeAnswer(
        s: GameState,
        challenge: Challenge,
        correct: Boolean,
        score: Double,
        title: String,
        text: String
    ) {
        val result = ChallengeResult(
            challengeId = challenge.id,
            skill = challenge.skill,
            correct = correct,
            attempts = maxOf(1, s.attemptsOnCurrent),
            hintsUsed = s.hintsUsedCurrent,
            score = score.coerceIn(0.0, 1.0)
        )
        if (challenge.scored) repo.recordChallenge(result)

        _state.value = refreshMeta(
            s.copy(
                phase = AnswerPhase.FEEDBACK,
                feedbackCorrect = correct,
                feedbackTitle = title,
                feedbackText = text,
                canRetry = false,
                results = s.results + result
            )
        )
    }

    private fun finishSession(s: GameState) {
        val scoredResults = s.results.filter { result ->
            s.challenges.firstOrNull { it.id == result.challengeId }?.scored == true
        }
        val scoredCount = scoredResults.size.coerceAtLeast(1)
        val mean = scoredResults.sumOf { it.score } / scoredCount.toDouble()
        val firstTry = scoredResults.count { it.correct && it.attempts <= 1 && it.hintsUsed == 0 }
        val totalHints = scoredResults.sumOf { it.hintsUsed }

        val stars = when {
            mean >= 0.86 && firstTry >= (scoredCount * 0.65).toInt() && totalHints <= 2 -> 3
            mean >= 0.62 -> 2
            else -> 1
        }

        val bySkill = scoredResults.groupBy { it.skill }.mapValues { (_, list) ->
            list.map { it.score }.average()
        }

        val summary = SessionSummary(
            missionId = s.missionId,
            totalChallenges = s.challenges.size,
            scoredChallenges = scoredResults.size,
            correctFirstTry = firstTry,
            totalScore = mean,
            stars = stars,
            skillScores = bySkill
        )

        if (!s.reviewMode && !s.teacherMode) {
            repo.completeMission(summary)
        }

        _state.value = refreshMeta(
            s.copy(
                phase = AnswerPhase.SESSION_COMPLETE,
                summary = summary,
                feedbackTitle = "",
                feedbackText = "",
                canRetry = false
            )
        )
    }

    private fun diagnosticFeedback(challenge: Challenge, selectedId: String): String {
        val selected = challenge.options.firstOrNull { it.id == selectedId }?.label.orEmpty()
        return when (challenge.type) {
            GameType.RHYTHM_CHOICE ->
                "Ты выбрал «" + selected + "». Сравни начало двух рисунков и послушай, где длинный звук меняется на короткие."
            GameType.TONAL_CHOICE ->
                "Ты выбрал «" + selected + "». Сосредоточься только на последнем звуке и сравни его с первым."
            GameType.NOTATION_CHOICE ->
                "Ты выбрал «" + selected + "». Послушай звук ещё раз и попробуй найти его на клавиатуре до нового ответа."
            else ->
                "Ты выбрал «" + selected + "». Послушай ещё раз, но теперь следи только за одним признаком."
        }
    }

    private fun scoreFor(attempt: Int, hints: Int): Double {
        val base = when (attempt) {
            1 -> 1.0
            2 -> 0.78
            else -> 0.62
        }
        return (base - hints * 0.08).coerceAtLeast(0.45)
    }

    private fun scoreForWrong(attempt: Int, hints: Int): Double =
        (0.40 - (attempt - 2) * 0.05 - hints * 0.04).coerceAtLeast(0.18)

    private fun refreshMeta(state: GameState): GameState =
        state.copy(
            completedMission = repo.completedMission(),
            totalStars = repo.totalStars(),
            streak = repo.streak(),
            skillStats = repo.stats()
        )

    private fun sameRhythm(a: List<Double>, b: List<Double>): Boolean =
        a.size == b.size && a.indices.all { abs(a[it] - b[it]) < 0.001 }

    private fun formatBeat(value: Double): String {
        if (abs(value - value.toInt()) < 0.001) return value.toInt().toString()
        return when {
            abs(value - 0.5) < 0.001 -> "½"
            abs(value - 1.5) < 0.001 -> "1½"
            abs(value - 2.5) < 0.001 -> "2½"
            abs(value - 3.5) < 0.001 -> "3½"
            else -> "%.2f".format(value)
        }
    }

    companion object {
        private val choiceTypes = setOf(
            GameType.AURAL_CHOICE,
            GameType.RHYTHM_CHOICE,
            GameType.TONAL_CHOICE,
            GameType.NOTATION_CHOICE
        )
    }
}

class GameViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            return GameViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
