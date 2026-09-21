package ru.solfege.daily

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ProductionCourseTest {

    @Test
    fun everyMissionGeneratesTenPlayableChallenges() {
        for (missionId in 1..Course.TOTAL_LESSONS) {
            val challenges = ChallengeEngine.generateSession(missionId)

            assertEquals("mission " + missionId, 10, challenges.size)
            assertEquals(challenges.size, challenges.map { it.id }.distinct().size)

            challenges.forEach { challenge ->
                assertTrue(challenge.title.isNotBlank())
                assertTrue(challenge.instruction.isNotBlank())
                assertTrue(challenge.hint.isNotBlank())
                assertTrue(challenge.explanation.isNotBlank())

                val childText = (challenge.title + " " + challenge.instruction + " " + challenge.hint).lowercase()
                assertFalse(childText.contains("рабочая тетрад"))
                assertFalse(childText.contains("страница учебника"))

                when (challenge.type) {
                    GameType.AURAL_CHOICE,
                    GameType.RHYTHM_CHOICE,
                    GameType.TONAL_CHOICE,
                    GameType.NOTATION_CHOICE -> {
                        assertTrue("mission " + missionId, challenge.options.size >= 3)
                        assertNotNull(challenge.correctOptionId)
                        assertEquals(
                            1,
                            challenge.options.count { it.id == challenge.correctOptionId }
                        )
                        assertTrue(
                            "must have a real wrong answer",
                            challenge.options.any { it.id != challenge.correctOptionId }
                        )
                        assertEquals(
                            challenge.options.size,
                            challenge.options.map { it.id }.distinct().size
                        )
                    }

                    GameType.MELODY_BUILD,
                    GameType.STAFF_BUILD -> {
                        assertTrue(challenge.targetMidi.size >= 2)
                    }

                    GameType.MEASURE_BUILD -> {
                        assertTrue(challenge.targetRhythmBeats > 0)
                        assertTrue(challenge.allowedDurations.size >= 2)
                        assertTrue(challenge.audioRhythm.size >= 2)
                        assertTrue(
                            abs(challenge.audioRhythm.sum() - challenge.targetRhythmBeats) < 0.001
                        )
                    }

                    GameType.PITCH_PRODUCTION -> assertTrue(challenge.targetMidi.isNotEmpty())
                    GameType.RHYTHM_PRODUCTION -> assertTrue(challenge.audioRhythm.size >= 2)
                    GameType.CREATIVE_TRANSFER -> assertFalse(challenge.scored)
                }
            }
        }
    }

    @Test
    fun missionProgressionAlwaysUnlocksTheImmediateNextMissionAfterCompletion() {
        var completed = 0

        for (mission in 1..Course.TOTAL_LESSONS) {
            assertTrue(MissionProgressRules.isUnlocked(completed, mission))
            completed = MissionProgressRules.completedAfter(completed, mission)
            assertEquals(mission, completed)

            if (mission < Course.TOTAL_LESSONS) {
                assertTrue(
                    "next mission must open immediately",
                    MissionProgressRules.isUnlocked(completed, mission + 1)
                )
            }
        }
    }

    @Test
    fun replayingOldMissionCannotSkipAheadOrRelockProgress() {
        val completed = 37

        assertEquals(
            completed,
            MissionProgressRules.completedAfter(completed, 12)
        )
        assertTrue(MissionProgressRules.isUnlocked(completed, 38))
        assertFalse(MissionProgressRules.isUnlocked(completed, 39))
    }

    @Test
    fun reviewAndTeacherRunsDoNotAdvanceChildProgress() {
        assertEquals(
            10,
            MissionProgressRules.completedAfter(
                currentCompleted = 10,
                missionId = 11,
                reviewMode = true
            )
        )
        assertEquals(
            10,
            MissionProgressRules.completedAfter(
                currentCompleted = 10,
                missionId = 50,
                teacherMode = true
            )
        )
    }

    @Test
    fun correctAnswersAreNotAlwaysInTheSamePosition() {
        val positions = mutableSetOf<Int>()

        for (missionId in 1..80) {
            ChallengeEngine.generateSession(missionId)
                .filter {
                    it.type == GameType.AURAL_CHOICE ||
                            it.type == GameType.RHYTHM_CHOICE ||
                            it.type == GameType.TONAL_CHOICE ||
                            it.type == GameType.NOTATION_CHOICE
                }
                .forEach { challenge ->
                    positions += challenge.options.indexOfFirst {
                        it.id == challenge.correctOptionId
                    }
                }
        }

        assertTrue("correct answer should move between positions", positions.size >= 3)
    }

    @Test
    fun eachWorldHasAStableNarrativeIdentity() {
        val worlds = (1..Course.TOTAL_LESSONS)
            .map { ChallengeEngine.missionMeta(it) }
            .groupBy { it.world }

        assertEquals(8, worlds.size)
        worlds.forEach { (world, missions) ->
            assertTrue(world in 1..8)
            assertTrue(missions.isNotEmpty())
            assertEquals(1, missions.map { it.worldTitle }.distinct().size)
        }
    }
}
