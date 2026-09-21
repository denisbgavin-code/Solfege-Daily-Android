package ru.solfege.daily

import kotlin.math.abs
import kotlin.random.Random

object ChallengeEngine {
    private val noteNames = listOf("до", "до♯", "ре", "ре♯", "ми", "фа", "фа♯", "соль", "соль♯", "ля", "ля♯", "си")

    fun missionMeta(id: Int): MissionMeta {
        val safe = id.coerceIn(1, Course.TOTAL_LESSONS)
        val week = (safe - 1) / 7 + 1
        val day = (safe - 1) % 7 + 1
        val world = when (week) {
            in 1..5 -> 1
            in 6..10 -> 2
            in 11..15 -> 3
            in 16..20 -> 4
            in 21..24 -> 5
            in 25..30 -> 6
            in 31..33 -> 7
            else -> 8
        }
        val worldTitle = listOf(
            "Гавань пульса",
            "Лес тональности",
            "Мастерская ритма",
            "Горы ключей",
            "Город движения",
            "Обсерватория интервалов",
            "Зеркальный дворец",
            "Большая сцена"
        )[world - 1]
        val lesson = Course.lesson(safe)
        return MissionMeta(
            id = safe,
            week = week,
            day = day,
            world = world,
            worldTitle = worldTitle,
            title = lesson.weekTitle,
            subtitle = Course.dayTitle(day)
        )
    }

    fun generateSession(
        missionId: Int,
        mastery: (SkillDomain) -> Double = { 0.5 }
    ): List<Challenge> {
        val safe = missionId.coerceIn(1, Course.TOTAL_LESSONS)
        val lesson = Course.lesson(safe)
        val week = lesson.week
        val rng = Random(safe * 9973 + 41)

        val difficulty = difficultyLevel(week, mastery)
        val challenges = mutableListOf<Challenge>()

        if (safe > 7) {
            challenges += retrievalChallenge(safe, difficulty, rng)
        } else {
            challenges += contourChallenge(safe, lesson, difficulty, rng, "Разминка слуха")
        }

        challenges += contourChallenge(safe, lesson, difficulty, rng, "Слуховой детектив")
        challenges += rhythmChoiceChallenge(safe, lesson, difficulty, rng)
        challenges += tonalChallenge(safe, lesson, difficulty, rng)
        challenges += notationChallenge(safe, lesson, difficulty, rng)

        if (week <= 8 || week in setOf(11, 12, 20, 21, 22, 23)) {
            challenges += measureChallenge(safe, lesson, difficulty)
        } else {
            challenges += staffChallenge(safe, lesson, difficulty)
        }

        challenges += melodyBuildChallenge(safe, lesson, difficulty)
        challenges += productionChallenge(safe, lesson, difficulty)
        challenges += contrastChallenge(safe, lesson, difficulty, rng)
        challenges += creativeChallenge(safe, lesson, difficulty)

        return challenges.mapIndexed { index, challenge ->
            challenge.copy(id = "m" + safe + "_q" + (index + 1) + "_" + challenge.type.name.lowercase())
        }
    }

    private fun difficultyLevel(week: Int, mastery: (SkillDomain) -> Double): Int {
        val relevant = when {
            week <= 5 -> listOf(SkillDomain.PULSE, SkillDomain.RHYTHM, SkillDomain.PITCH)
            week <= 10 -> listOf(SkillDomain.TONALITY, SkillDomain.PITCH, SkillDomain.MEMORY)
            week <= 15 -> listOf(SkillDomain.RHYTHM, SkillDomain.READING)
            week <= 24 -> listOf(SkillDomain.KEYS, SkillDomain.RHYTHM, SkillDomain.READING)
            week <= 30 -> listOf(SkillDomain.INTERVALS, SkillDomain.WRITING)
            else -> listOf(SkillDomain.KEYS, SkillDomain.MEMORY, SkillDomain.CREATIVITY)
        }
        val mean = relevant.map(mastery).average()
        return when {
            mean < 0.45 -> 0
            mean > 0.78 -> 2
            else -> 1
        }
    }

    private fun retrievalChallenge(
        missionId: Int,
        difficulty: Int,
        rng: Random
    ): Challenge {
        val sourceId = (missionId - 7).coerceAtLeast(1)
        val oldLesson = Course.lesson(sourceId)
        return if (oldLesson.week <= 5) {
            rhythmChoiceChallenge(missionId, oldLesson, difficulty, rng).copy(
                title = "Эхо недели",
                instruction = "Неделю назад был похожий ритм. Послушай и выбери его запись.",
                skill = SkillDomain.MEMORY,
                explanation = "Возврат к старому материалу помогает вызывать его из памяти без подсказки."
            )
        } else {
            contourChallenge(missionId, oldLesson, difficulty, rng, "Эхо недели").copy(
                skill = SkillDomain.MEMORY,
                instruction = "Послушай знакомый рисунок. Как он в основном движется?"
            )
        }
    }

    private fun contourChallenge(
        missionId: Int,
        lesson: Course.Lesson,
        difficulty: Int,
        rng: Random,
        title: String
    ): Challenge {
        val midi = lesson.melody.map { it.midi }
        val contour = contourLabel(midi)
        val baseOptions = mutableListOf("вверх", "вниз", "возвращается", "повторяет один звук")
        if (difficulty == 0) baseOptions.remove("повторяет один звук")
        val correct = when (contour) {
            "up" -> "вверх"
            "down" -> "вниз"
            "same" -> "повторяет один звук"
            else -> "возвращается"
        }
        if (correct !in baseOptions) baseOptions += correct

        val options = optionList(baseOptions.distinct().shuffled(rng), correct)
        return Challenge(
            id = "placeholder",
            missionId = missionId,
            type = GameType.AURAL_CHOICE,
            skill = SkillDomain.PITCH,
            title = title,
            instruction = if (difficulty == 0)
                "Послушай мелодию. Куда она движется: вверх, вниз или возвращается?"
            else
                "Послушай весь рисунок. Как лучше всего описать его направление?",
            audioMidi = midi,
            options = options,
            correctOptionId = options.first { it.label == correct }.id,
            maxReplays = if (difficulty == 2) 2 else 4,
            hint = "Следи только за первым и последним звуком, потом послушай середину.",
            explanation = when (correct) {
                "вверх" -> "Последний звук оказался выше первого."
                "вниз" -> "Последний звук оказался ниже первого."
                "повторяет один звук" -> "Высота не изменилась."
                else -> "Мелодия ушла от начального звука и вернулась к нему."
            },
            difficulty = difficulty
        )
    }

    private fun rhythmChoiceChallenge(
        missionId: Int,
        lesson: Course.Lesson,
        difficulty: Int,
        rng: Random
    ): Challenge {
        val correct = lesson.rhythm.toList()
        val variants = mutableListOf<List<Double>>()
        variants += correct

        fun addIfUnique(v: List<Double>) {
            if (v.isNotEmpty() && variants.none { sameRhythm(it, v) }) variants += v
        }

        addIfUnique(correct.reversed())
        if (correct.size >= 2) {
            val swapped = correct.toMutableList()
            val i = if (correct.size > 2) 1 else 0
            val j = (i + 1).coerceAtMost(correct.lastIndex)
            val temp = swapped[i]
            swapped[i] = swapped[j]
            swapped[j] = temp
            addIfUnique(swapped)
        }
        addIfUnique(correct.mapIndexed { index, d ->
            if (index == 0) {
                when {
                    d >= 1.0 -> 0.5
                    d >= 0.5 -> 1.0
                    else -> 0.5
                }
            } else d
        })
        addIfUnique(correct.mapIndexed { index, d ->
            if (index == correct.lastIndex) {
                when {
                    d >= 1.0 -> 0.5
                    d >= 0.5 -> 1.0
                    else -> 0.5
                }
            } else d
        })

        var guard = 0
        while (variants.size < 4 && guard < 20) {
            val generated = List(correct.size) { i ->
                val source = correct[i]
                if (i == variants.size % correct.size) {
                    if (source >= 1.0) 0.5 else 1.0
                } else source
            }
            addIfUnique(generated)
            if (variants.size < 4 && correct.isNotEmpty()) {
                addIfUnique(List(correct.size) { if (it % 2 == 0) 1.0 else 0.5 })
            }
            guard++
        }
        while (variants.size < 4) {
            val base = MutableList(correct.size.coerceAtLeast(2)) { 1.0 }
            val index = (variants.size - 1).coerceAtMost(base.lastIndex)
            base[index] = if (variants.size % 2 == 0) 0.5 else 2.0
            addIfUnique(base)
        }

        val selected = variants.take(if (difficulty == 0) 3 else 4).shuffled(rng)
        val correctKey = rhythmKey(correct)
        val options = selected.mapIndexed { index, rhythm ->
            AnswerOption(
                id = "r" + index + "_" + rhythmKey(rhythm),
                label = rhythmText(rhythm),
                rhythm = rhythm
            )
        }

        return Challenge(
            id = "placeholder",
            missionId = missionId,
            type = GameType.RHYTHM_CHOICE,
            skill = SkillDomain.RHYTHM,
            title = "Ритм-код",
            instruction = "Послушай ритм целиком. Потом выбери только один рисунок, который совпадает.",
            audioRhythm = correct,
            options = options,
            correctOptionId = options.first { rhythmKey(it.rhythm) == correctKey }.id,
            maxReplays = if (difficulty == 2) 2 else 4,
            hint = "Сравни сначала начало рисунков, затем конец. Не выбирай по одному знакомому символу.",
            explanation = "Совпали порядок и относительная длительность всех звуков.",
            difficulty = difficulty
        )
    }

    private fun tonalChallenge(
        missionId: Int,
        lesson: Course.Lesson,
        difficulty: Int,
        rng: Random
    ): Challenge {
        val tonic = tonicForWeek(lesson.week)
        val melody = lesson.melody.map { it.midi }.toMutableList()
        if (melody.size < 3) {
            melody += tonic + 2
            melody += tonic
        }

        val mode = (missionId + lesson.day) % 3
        val finalMidi = when (mode) {
            0 -> tonic
            1 -> tonic + 7
            else -> tonic + 2
        }
        melody[melody.lastIndex] = finalMidi

        val label = when (mode) {
            0 -> "домой — на тонику"
            1 -> "на устойчивую опору, но не домой"
            else -> "в пути — хочется продолжения"
        }
        val baseLabels = listOf(
            "домой — на тонику",
            "на устойчивую опору, но не домой",
            "в пути — хочется продолжения",
            "закончилась случайно"
        )
        val labels = if (difficulty == 0) {
            baseLabels.take(3)
        } else {
            baseLabels
        }.toMutableList()
        if (label !in labels) {
            labels[labels.lastIndex] = label
        }

        val options = optionList(labels.distinct().shuffled(rng), label)
        return Challenge(
            id = "placeholder",
            missionId = missionId,
            type = GameType.TONAL_CHOICE,
            skill = SkillDomain.TONALITY,
            title = "Где остановилась мелодия?",
            instruction = "Послушай последний звук. Что ты чувствуешь: настоящий дом, устойчивую остановку или продолжение?",
            audioMidi = melody,
            options = options,
            correctOptionId = options.first { it.label == label }.id,
            maxReplays = 4,
            hint = "Сравни последний звук с первым. Тоника часто ощущается как самая полная остановка.",
            explanation = when (mode) {
                0 -> "Фраза пришла на тонику — главный звуковой дом."
                1 -> "Квинта устойчива, но обычно ощущается менее окончательно, чем тоника."
                else -> "Соседняя ступень не даёт полного покоя и тянется дальше."
            },
            difficulty = difficulty
        )
    }

    private fun notationChallenge(
        missionId: Int,
        lesson: Course.Lesson,
        difficulty: Int,
        rng: Random
    ): Challenge {
        val target = lesson.melody[(lesson.day + lesson.week) % lesson.melody.size]
        val correct = noteName(target.midi)
        val candidate = mutableListOf(correct)
        var offset = 1
        val needed = if (difficulty == 0) 3 else 4
        while (candidate.size < needed) {
            val up = noteName(target.midi + offset)
            val down = noteName(target.midi - offset)
            if (up !in candidate) candidate += up
            if (candidate.size < needed && down !in candidate) candidate += down
            offset++
        }
        val labels = candidate.take(needed).shuffled(rng)
        val options = optionList(labels, correct)

        return Challenge(
            id = "placeholder",
            missionId = missionId,
            type = GameType.NOTATION_CHOICE,
            skill = SkillDomain.READING,
            title = "Нота под фонарём",
            instruction = "Послушай один звук из сегодняшней мелодии. Как называется эта нота?",
            audioMidi = listOf(target.midi),
            options = options,
            correctOptionId = options.first { it.label == correct }.id,
            maxReplays = 4,
            hint = "Если сомневаешься, сначала найди звук на экранной клавиатуре на слух.",
            explanation = "Этот звук называется «" + correct + "».",
            difficulty = difficulty
        )
    }

    private fun measureChallenge(
        missionId: Int,
        lesson: Course.Lesson,
        difficulty: Int
    ): Challenge {
        val beats = meterBeats(lesson.week)
        val allowed = when {
            lesson.week >= 22 -> listOf(2.0, 1.0, 0.5, 0.25)
            lesson.week >= 3 -> listOf(2.0, 1.0, 0.5)
            else -> listOf(1.0, 0.5)
        }
        val pattern = when (beats.toInt()) {
            2 -> when ((missionId + lesson.day) % 3) {
                0 -> listOf(1.0, 1.0)
                1 -> listOf(0.5, 0.5, 1.0)
                else -> listOf(1.0, 0.5, 0.5)
            }
            3 -> when ((missionId + lesson.day) % 3) {
                0 -> listOf(1.0, 1.0, 1.0)
                1 -> listOf(2.0, 1.0)
                else -> listOf(0.5, 0.5, 2.0)
            }
            else -> when ((missionId + lesson.day) % 4) {
                0 -> listOf(1.0, 1.0, 1.0, 1.0)
                1 -> listOf(2.0, 1.0, 1.0)
                2 -> listOf(1.0, 0.5, 0.5, 2.0)
                else -> if (lesson.week >= 22) listOf(1.0, 0.25, 0.25, 0.25, 0.25, 2.0) else listOf(0.5, 0.5, 1.0, 2.0)
            }
        }
        return Challenge(
            id = "placeholder",
            missionId = missionId,
            type = GameType.MEASURE_BUILD,
            skill = SkillDomain.WRITING,
            title = "Собери услышанный такт",
            instruction = "Послушай ритм и собери его из длительностей. Сначала закончи весь ответ, потом нажми «Проверить».",
            audioRhythm = pattern,
            targetRhythmBeats = beats,
            allowedDurations = allowed,
            hint = "Сначала проверь, сколько долей занимает весь такт. Потом сравни порядок длинных и коротких звуков.",
            explanation = "Совпасть должна не только сумма длительностей, но и их порядок.",
            difficulty = difficulty
        )
    }

    private fun staffChallenge(
        missionId: Int,
        lesson: Course.Lesson,
        difficulty: Int
    ): Challenge {
        val count = when (difficulty) {
            0 -> 3
            2 -> 5
            else -> 4
        }.coerceAtMost(lesson.melody.size)
        return Challenge(
            id = "placeholder",
            missionId = missionId,
            type = GameType.STAFF_BUILD,
            skill = SkillDomain.WRITING,
            title = "Запиши на стане",
            instruction = "Послушай фрагмент, затем поставь все ноты. Проверка появится только после полной записи.",
            audioMidi = lesson.melody.take(count).map { it.midi },
            targetMidi = lesson.melody.take(count).map { it.midi },
            maxReplays = if (difficulty == 2) 2 else 4,
            hint = "Определи сначала направление. Потом уточняй конкретные ноты.",
            explanation = "Музыкальный диктант соединяет слух, память и нотную запись.",
            difficulty = difficulty
        )
    }

    private fun melodyBuildChallenge(
        missionId: Int,
        lesson: Course.Lesson,
        difficulty: Int
    ): Challenge {
        val count = when (difficulty) {
            0 -> 3
            2 -> 5
            else -> 4
        }.coerceAtMost(lesson.melody.size)
        val target = lesson.melody.take(count).map { it.midi }
        return Challenge(
            id = "placeholder",
            missionId = missionId,
            type = GameType.MELODY_BUILD,
            skill = SkillDomain.MEMORY,
            title = "Поймай мелодию",
            instruction = "Послушай фрагмент, затем собери его на клавиатуре целиком. До нажатия «Проверить» приложение не скажет, где ошибка.",
            audioMidi = target,
            targetMidi = target,
            maxReplays = if (difficulty == 2) 2 else 4,
            hint = "Сначала найди первый звук, затем сравни каждый следующий: выше, ниже или тот же.",
            explanation = "Собирая весь фрагмент до проверки, ты действительно удерживаешь мелодию в памяти.",
            difficulty = difficulty
        )
    }

    private fun productionChallenge(
        missionId: Int,
        lesson: Course.Lesson,
        difficulty: Int
    ): Challenge {
        val useRhythm = lesson.day % 2 == 0
        return if (useRhythm) {
            Challenge(
                id = "placeholder",
                missionId = missionId,
                type = GameType.RHYTHM_PRODUCTION,
                skill = SkillDomain.RHYTHM,
                title = "Эхо без подсказки",
                instruction = "Послушай ритм, дождись сигнала и отбей его. Сначала весь ответ — потом сравнение.",
                audioRhythm = lesson.rhythm.toList(),
                targetRhythmBeats = lesson.rhythm.sum(),
                maxReplays = if (difficulty == 2) 2 else 4,
                hint = "Если трудно, тихо считай ровные доли во время прослушивания.",
                explanation = "Важно сохранить отношения длительностей, а не копировать точный темп.",
                difficulty = difficulty
            )
        } else {
            Challenge(
                id = "placeholder",
                missionId = missionId,
                type = GameType.PITCH_PRODUCTION,
                skill = SkillDomain.PITCH,
                title = "Голосовой ответ",
                instruction = "Послушай короткий рисунок и повтори его голосом. Микрофон — помощник, а не судья.",
                audioMidi = lesson.melody.take(3).map { it.midi },
                targetMidi = lesson.melody.take(3).map { it.midi },
                maxReplays = if (difficulty == 2) 2 else 4,
                hint = "Начни с первого звука. Потом следи прежде всего за направлением мелодии.",
                explanation = "Пение связывает внутреннее слышание с реальным музыкальным действием.",
                difficulty = difficulty
            )
        }
    }

    private fun contrastChallenge(
        missionId: Int,
        lesson: Course.Lesson,
        difficulty: Int,
        rng: Random
    ): Challenge {
        val root = tonicForWeek(lesson.week)
        val small = listOf(root, root + 2)
        val big = when {
            lesson.week >= 28 -> listOf(root, root + 7)
            lesson.week >= 27 -> listOf(root, root + 4)
            else -> listOf(root, root + 5)
        }
        val playBig = (missionId + lesson.day) % 2 == 0
        val audio = if (playBig) big else small
        val correct = if (playBig) "больше" else "меньше"
        val labels = listOf("меньше", "больше", "одинаково").shuffled(rng)
        val options = optionList(labels, correct)
        return Challenge(
            id = "placeholder",
            missionId = missionId,
            type = GameType.AURAL_CHOICE,
            skill = if (lesson.week >= 25) SkillDomain.INTERVALS else SkillDomain.PITCH,
            title = "Сравни расстояние",
            instruction = "Послушай расстояние между двумя звуками. Оно маленькое, широкое или звуки повторились?",
            audioMidi = audio,
            options = options,
            correctOptionId = options.first { it.label == correct }.id,
            maxReplays = 4,
            hint = "Не называй интервал сразу. Сначала просто почувствуй ширину расстояния.",
            explanation = if (playBig)
                "Между этими звуками расстояние широкое."
            else
                "Эти звуки находятся ближе друг к другу.",
            difficulty = difficulty
        )
    }

    private fun creativeChallenge(
        missionId: Int,
        lesson: Course.Lesson,
        difficulty: Int
    ): Challenge {
        val prompt = when {
            lesson.week <= 5 -> "Придумай один новый ритм на " + formatBeats(meterBeats(lesson.week)) + " доли и повтори его два раза одинаково."
            lesson.week <= 10 -> "Придумай музыкальный вопрос из 3–4 звуков и ответ, который заканчивается на звуковом «доме»."
            lesson.week <= 20 -> "Возьми три звука сегодняшней тональности и придумай короткую фразу с понятным концом."
            lesson.week <= 24 -> "Придумай такт, где встречаются две разные длительности, и исполни его под ровный пульс."
            lesson.week <= 30 -> "Придумай два звука с интервалом недели, затем сделай похожий интервал от другой высоты."
            else -> "Сочини фразу на 4–8 долей: начало, движение, один заметный скачок и ясное окончание."
        }
        return Challenge(
            id = "placeholder",
            missionId = missionId,
            type = GameType.CREATIVE_TRANSFER,
            skill = SkillDomain.CREATIVITY,
            title = "Твоя музыка",
            instruction = prompt,
            audioMidi = listOf(tonicForWeek(lesson.week)),
            hint = "Сначала придумай очень короткую версию. Потом, если хочется, расширь её.",
            explanation = "Творческая задача проверяет, можешь ли ты использовать правило самостоятельно.",
            difficulty = difficulty,
            scored = false
        )
    }

    private fun optionList(labels: List<String>, correct: String): List<AnswerOption> {
        val unique = labels.distinct().toMutableList()
        if (correct !in unique) unique += correct
        require(unique.size >= 3)
        return unique.mapIndexed { index, label -> AnswerOption("o" + index, label) }
    }

    private fun contourLabel(midi: List<Int>): String {
        if (midi.distinct().size == 1) return "same"
        val first = midi.first()
        val last = midi.last()
        return when {
            abs(last - first) <= 1 -> "return"
            last > first -> "up"
            else -> "down"
        }
    }

    private fun noteName(midi: Int): String = noteNames[Math.floorMod(midi, 12)]

    private fun tonicForWeek(week: Int): Int = when (week) {
        16, 19 -> 67
        17 -> 65
        18 -> 62
        31, 32 -> 70
        else -> 60
    }

    private fun meterBeats(week: Int): Double = when (week) {
        4, 11 -> 2.0
        5, 12 -> 3.0
        20 -> 4.0
        else -> if (week <= 4) 2.0 else 4.0
    }

    private fun rhythmText(values: List<Double>): String =
        values.joinToString("  ") {
            when {
                it >= 3.0 -> "𝅗𝅥·"
                it >= 2.0 -> "𝅗𝅥"
                it >= 1.0 -> "♩"
                it >= 0.5 -> "♪"
                else -> "♬"
            }
        }

    private fun rhythmKey(values: List<Double>): String =
        values.joinToString("_") { ((it * 100).toInt()).toString() }

    private fun sameRhythm(a: List<Double>, b: List<Double>): Boolean =
        a.size == b.size && a.indices.all { abs(a[it] - b[it]) < 0.001 }

    private fun formatBeats(beats: Double): String =
        if (abs(beats - beats.toInt()) < 0.001) beats.toInt().toString() else beats.toString()
}
