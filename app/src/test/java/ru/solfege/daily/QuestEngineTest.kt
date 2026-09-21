package ru.solfege.daily

import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

class QuestEngineTest {
    private fun prefs(): SharedPreferences {
        val values = mutableMapOf<String, Any?>()

        lateinit var editor: SharedPreferences.Editor
        editor = Proxy.newProxyInstance(
            SharedPreferences.Editor::class.java.classLoader,
            arrayOf(SharedPreferences.Editor::class.java)
        ) { _, method, args ->
            when (method.name) {
                "putInt", "putLong", "putFloat", "putString", "putBoolean" -> {
                    values[args!![0] as String] = args[1]
                    editor
                }
                "remove" -> {
                    values.remove(args!![0] as String)
                    editor
                }
                "clear" -> {
                    values.clear()
                    editor
                }
                "commit" -> true
                "apply" -> null
                else -> editor
            }
        } as SharedPreferences.Editor

        return Proxy.newProxyInstance(
            SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java)
        ) { _, method, args ->
            when (method.name) {
                "getInt" -> values[args!![0] as String] as? Int ?: args[1] as Int
                "getLong" -> values[args!![0] as String] as? Long ?: args[1] as Long
                "getFloat" -> values[args!![0] as String] as? Float ?: args[1] as Float
                "getString" -> values[args!![0] as String] as? String ?: args[1] as String?
                "getBoolean" -> values[args!![0] as String] as? Boolean ?: args[1] as Boolean
                "contains" -> values.containsKey(args!![0] as String)
                "getAll" -> values.toMap()
                "edit" -> editor
                "registerOnSharedPreferenceChangeListener", "unregisterOnSharedPreferenceChangeListener" -> null
                else -> null
            }
        } as SharedPreferences
    }

    @Test
    fun all238MissionsContainRealAnswerSpace() {
        val store = ProgressStore(prefs())

        for (id in 1..238) {
            val mission = MissionFactory.create(id, store)
            assertEquals(id, mission.id)
            assertEquals((id - 1) / 7 + 1, mission.week)
            assertEquals((id - 1) % 7 + 1, mission.day)
            assertEquals(6, mission.challenges.size)
            assertEquals(6, mission.challenges.map { it.id }.distinct().size)

            mission.challenges.forEach { challenge ->
                assertTrue(challenge.rounds.isNotEmpty())
                assertTrue(challenge.requiredCorrect in 1..challenge.rounds.size)

                challenge.rounds.forEach { round ->
                    when (challenge.type) {
                        ChallengeType.LISTEN_CHOICE,
                        ChallengeType.RHYTHM_CHOICE,
                        ChallengeType.NOTE_CHOICE -> {
                            assertTrue("mission $id must have alternatives", round.options.size >= 2)
                            assertEquals(round.options.size, round.options.map { it.id }.distinct().size)
                            assertEquals(round.options.size, round.options.map { it.label }.distinct().size)
                            assertNotNull(round.correctOptionId)
                            assertTrue(round.options.any { it.id == round.correctOptionId })
                            assertTrue(round.options.any { it.id != round.correctOptionId })
                        }
                        ChallengeType.MELODY_BUILD -> assertTrue(round.targetMelody.size >= 3)
                        ChallengeType.RHYTHM_BUILD -> assertTrue(round.targetRhythm.size >= 2)
                        ChallengeType.SINGING -> assertTrue(round.targetMelody.isNotEmpty())
                        ChallengeType.CREATIVE -> assertTrue(round.prompt.isNotBlank())
                    }
                    assertTrue(round.hint.isNotBlank())
                    assertTrue(round.explanation.isNotBlank())
                }
            }
        }
    }

    @Test
    fun completingMissionUnlocksNextDay() {
        val store = ProgressStore(prefs())
        assertEquals(1, store.currentLesson())
        assertTrue(store.isUnlocked(1))
        assertFalse(store.isUnlocked(2))

        store.completeLesson(1, 7)
        assertEquals(2, store.currentLesson())
        assertTrue(store.isUnlocked(2))
        assertEquals(7, store.stars)

        store.completeLesson(2, 6)
        assertEquals(3, store.currentLesson())
        assertTrue(store.isUnlocked(3))
        assertEquals(13, store.stars)
    }
}
