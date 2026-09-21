package ru.solfege.daily

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate

object MissionProgressRules {
    fun completedAfter(currentCompleted: Int, missionId: Int, reviewMode: Boolean = false, teacherMode: Boolean = false): Int {
        if (reviewMode || teacherMode) return currentCompleted
        return if (missionId == currentCompleted + 1) missionId else currentCompleted
    }

    fun isUnlocked(currentCompleted: Int, missionId: Int, teacherMode: Boolean = false): Boolean =
        teacherMode || missionId <= currentCompleted + 1
}

class ProgressRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("solfege_production_v4", Context.MODE_PRIVATE)

    fun completedMission(): Int = prefs.getInt("completed_mission", 0)

    fun nextMission(): Int = (completedMission() + 1).coerceAtMost(Course.TOTAL_LESSONS)

    fun isUnlocked(missionId: Int): Boolean {
        val id = missionId.coerceIn(1, Course.TOTAL_LESSONS)
        return MissionProgressRules.isUnlocked(completedMission(), id)
    }

    fun stars(missionId: Int): Int =
        prefs.getInt("stars_" + missionId, 0)

    fun totalStars(): Int {
        var sum = 0
        for (id in 1..Course.TOTAL_LESSONS) {
            sum += stars(id)
        }
        return sum
    }

    fun streak(): Int = prefs.getInt("streak", 0)

    fun mastery(skill: SkillDomain): Double {
        val attempts = attempts(skill)
        if (attempts == 0) return 0.50
        return prefs.getFloat("mastery_" + skill.name, 0.50f).toDouble()
    }

    fun attempts(skill: SkillDomain): Int =
        prefs.getInt("attempts_" + skill.name, 0)

    fun stats(): List<SkillStat> =
        SkillDomain.entries.map { SkillStat(it, attempts(it), mastery(it)) }

    fun weakestSkill(): SkillDomain {
        val attempted = SkillDomain.entries.filter { attempts(it) > 0 }
        return (attempted.minByOrNull { mastery(it) } ?: SkillDomain.PITCH)
    }

    fun strongestSkill(): SkillDomain {
        val attempted = SkillDomain.entries.filter { attempts(it) > 0 }
        return (attempted.maxByOrNull { mastery(it) } ?: SkillDomain.PULSE)
    }

    fun recordChallenge(result: ChallengeResult) {
        if (!result.correct && result.score <= 0.0) {
            updateSkill(result.skill, 0.15)
        } else {
            updateSkill(result.skill, result.score.coerceIn(0.0, 1.0))
        }
    }

    private fun updateSkill(skill: SkillDomain, result: Double) {
        val oldAttempts = attempts(skill)
        val old = mastery(skill)
        val alpha = when {
            oldAttempts < 4 -> 0.32
            oldAttempts < 12 -> 0.22
            else -> 0.14
        }
        val next = old * (1.0 - alpha) + result * alpha

        prefs.edit()
            .putInt("attempts_" + skill.name, oldAttempts + 1)
            .putFloat("mastery_" + skill.name, next.toFloat())
            .apply()
    }

    fun completeMission(summary: SessionSummary) {
        val currentStars = stars(summary.missionId)
        val newStars = maxOf(currentStars, summary.stars)

        val editor = prefs.edit()
            .putInt("stars_" + summary.missionId, newStars)

        val completed = completedMission()
        val nextCompleted = MissionProgressRules.completedAfter(completed, summary.missionId)
        if (nextCompleted != completed) {
            editor.putInt("completed_mission", nextCompleted)
        }

        updateStreak(editor)
        editor.apply()
    }

    fun smartReviewMission(): Int {
        val completed = completedMission()
        if (completed <= 1) return 1

        val weak = weakestSkill()
        val maxWeek = ((completed - 1) / 7 + 1).coerceIn(1, Course.TOTAL_WEEKS)
        val candidateWeeks = when (weak) {
            SkillDomain.PULSE -> listOf(1, 4, 5, 20)
            SkillDomain.RHYTHM -> listOf(2, 3, 11, 12, 21, 22, 23)
            SkillDomain.PITCH -> listOf(1, 2, 6, 8, 14)
            SkillDomain.TONALITY -> listOf(6, 7, 8, 14, 24, 33)
            SkillDomain.READING -> listOf(9, 11, 14, 16, 17, 18, 31)
            SkillDomain.WRITING -> listOf(9, 11, 16, 17, 18, 26, 27)
            SkillDomain.INTERVALS -> listOf(25, 26, 27, 28, 29, 30)
            SkillDomain.KEYS -> listOf(15, 16, 17, 18, 31, 32, 33)
            SkillDomain.MEMORY -> listOf(10, 13, 19, 30, 34)
            SkillDomain.CREATIVITY -> listOf(2, 6, 13, 19, 30, 34)
        }
        val week = candidateWeeks.lastOrNull { it <= maxWeek } ?: 1
        val day = 7
        return ((week - 1) * 7 + day).coerceAtMost(completed)
    }

    fun reset() {
        prefs.edit().clear().apply()
    }

    private fun updateStreak(editor: SharedPreferences.Editor) {
        val today = LocalDate.now()
        val previousRaw = prefs.getString("last_date", "") ?: ""
        val oldStreak = streak()

        val next = if (previousRaw.isBlank()) {
            1
        } else {
            try {
                val previous = LocalDate.parse(previousRaw)
                when {
                    previous == today -> oldStreak.coerceAtLeast(1)
                    previous == today.minusDays(1) -> oldStreak + 1
                    else -> 1
                }
            } catch (_: Exception) {
                1
            }
        }

        editor.putInt("streak", next)
        editor.putString("last_date", today.toString())
    }
}
