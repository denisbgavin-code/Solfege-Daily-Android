package ru.solfege.daily

import android.content.SharedPreferences
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class SkillTag { PULSE, RHYTHM, PITCH, TONALITY, READING, WRITING, INTERVALS, KEYS, MEMORY, CREATIVE }
enum class ChallengeType { LISTEN_CHOICE, RHYTHM_CHOICE, NOTE_CHOICE, MELODY_BUILD, RHYTHM_BUILD, SINGING, CREATIVE }

data class AnswerOption(
    val id: String,
    val label: String,
    val payload: List<Int> = emptyList(),
    val rhythm: List<Double> = emptyList()
)

data class RoundSpec(
    val prompt: String,
    val instruction: String,
    val audioA: List<Int> = emptyList(),
    val audioB: List<Int> = emptyList(),
    val rhythmA: List<Double> = emptyList(),
    val rhythmB: List<Double> = emptyList(),
    val options: List<AnswerOption> = emptyList(),
    val correctOptionId: String? = null,
    val targetMelody: List<Int> = emptyList(),
    val targetRhythm: List<Double> = emptyList(),
    val hint: String,
    val explanation: String,
    val skill: SkillTag,
    val difficulty: Int
)

data class ChallengeSpec(
    val id: String,
    val title: String,
    val subtitle: String,
    val type: ChallengeType,
    val rounds: List<RoundSpec>,
    val requiredCorrect: Int,
    val rewardStars: Int,
    val skill: SkillTag
)

data class MissionSpec(
    val id: Int,
    val week: Int,
    val day: Int,
    val worldIndex: Int,
    val worldTitle: String,
    val worldEmoji: String,
    val title: String,
    val story: String,
    val objective: String,
    val challenges: List<ChallengeSpec>
)

data class SkillState(val score: Double, val attempts: Int, val lastLesson: Int)

object Worlds {
    val titles = listOf("Лес пульса","Долина ладов","Город ритма","Острова тональностей","Механическая бухта","Космос интервалов","Башня музыки")
    val emoji = listOf("🌲","🏞️","🏙️","🏝️","⚙️","🌌","🏰")
    fun indexForWeek(week: Int): Int = when (week) {
        in 1..5 -> 0; in 6..10 -> 1; in 11..15 -> 2; in 16..20 -> 3; in 21..24 -> 4; in 25..30 -> 5; else -> 6
    }
}

class ProgressStore(private val prefs: SharedPreferences) {
    val completedLesson: Int get() = prefs.getInt("quest_completed_lesson", 0)
    val stars: Int get() = prefs.getInt("quest_stars", 0)

    fun currentLesson(): Int = min(238, completedLesson + 1)
    fun isUnlocked(lessonId: Int): Boolean = lessonId <= completedLesson + 1

    fun completeLesson(lessonId: Int, earnedStars: Int) {
        prefs.edit()
            .putInt("quest_completed_lesson", max(completedLesson, lessonId))
            .putInt("quest_stars", stars + earnedStars)
            .putLong("quest_last_complete", System.currentTimeMillis())
            .apply()
    }

    fun saveMissionCursor(lessonId: Int, challenge: Int) {
        prefs.edit().putInt("quest_cursor_lesson", lessonId).putInt("quest_cursor_challenge", challenge).apply()
    }

    fun missionCursor(lessonId: Int): Int =
        if (prefs.getInt("quest_cursor_lesson", -1) == lessonId) prefs.getInt("quest_cursor_challenge", 0) else 0

    fun clearCursor() {
        prefs.edit().remove("quest_cursor_lesson").remove("quest_cursor_challenge").apply()
    }

    fun skillState(skill: SkillTag): SkillState {
        val prefix = "quest_skill_" + skill.name + "_"
        return SkillState(
            prefs.getFloat(prefix + "score", 0.5f).toDouble(),
            prefs.getInt(prefix + "attempts", 0),
            prefs.getInt(prefix + "last", 0)
        )
    }

    fun recordSkill(skill: SkillTag, result: Double, lessonId: Int) {
        val old = skillState(skill)
        val value = result.coerceIn(0.0, 1.0)
        val alpha = when { old.attempts < 4 -> 0.34; old.attempts < 12 -> 0.22; else -> 0.14 }
        val next = old.score * (1.0 - alpha) + value * alpha
        val prefix = "quest_skill_" + skill.name + "_"
        prefs.edit()
            .putFloat(prefix + "score", next.toFloat())
            .putInt(prefix + "attempts", old.attempts + 1)
            .putInt(prefix + "last", lessonId)
            .apply()
    }

    fun weakestSkill(): SkillTag = SkillTag.entries.minByOrNull { skillState(it).score } ?: SkillTag.PITCH

    fun resetAll() {
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith("quest_") }.forEach(editor::remove)
        editor.apply()
    }
}

object Curriculum {
    val weekTitles = listOf(
        "Ровный пульс","Музыкальное эхо","Длинный и короткий","Размер 2/4","Размер 3/4","Тоника — звук-дом",
        "Устойчивые ступени","Тяготение к опоре","До мажор и нотный стан","Пауза и внутренний слух","Ритм в размере 2/4",
        "Три доли и точка","Музыкальная фраза","Мажорная гамма","Полутон, диез и бемоль","Соль мажор","Фа мажор",
        "Ре мажор","Транспонирование","Размер 4/4","Затакт","Шестнадцатые","Смешанные ритмы","Главные ступени",
        "Что такое интервал","Секунда","Терция","Квинта","Октава","Секвенция и канон","Си-бемоль мажор",
        "Си-бемоль: практика","Одноимённые лады","Финальная музыкальная экспедиция"
    )

    val goals = listOf(
        "Слышать и удерживать ровные доли.","Точно повторять короткий звуковой рисунок.","Различать длительности внутри одного пульса.",
        "Чувствовать две доли и сильное начало такта.","Чувствовать три доли и сильное начало такта.","Слышать тонику как музыкальную точку покоя.",
        "Узнавать устойчивые ступени 1, 3 и 5.","Слышать, куда тянется неустойчивый звук.","Связать знакомое звучание с нотным станом.",
        "Удерживать фразу во внутреннем слухе во время паузы.","Читать и воспроизводить короткие ритмы 2/4.","Удерживать трёхдольность и длительность в три доли.",
        "Слышать музыкальный вопрос и завершённый ответ.","Слышать и петь устройство мажорной гаммы.","Различать тон и полутон, понимать знаки альтерации.",
        "Слышать и читать Соль мажор.","Слышать и читать Фа мажор.","Слышать и читать Ре мажор.","Переносить знакомый рисунок на другую высоту.",
        "Чувствовать четыре доли и целую длительность.","Слышать начало до первой сильной доли.","Ровно делить долю на четыре части.",
        "Свободно переключаться между изученными длительностями.","Слышать I, IV и V как главные ладовые опоры.","Сравнивать расстояние между двумя звуками.",
        "Узнавать секунду.","Узнавать терцию.","Узнавать квинту.","Узнавать октаву.","Повторять модель от другой высоты и удерживать канон.",
        "Освоить звуковой образ Си-бемоль мажора.","Применять Си-бемоль мажор в слухе и чтении.","Сравнивать мажор и минор с общей тоникой.",
        "Соединить слух, ритм, чтение, запись и творчество."
    )
}

object MissionFactory {
    fun create(lessonId: Int, store: ProgressStore): MissionSpec {
        val safe = lessonId.coerceIn(1, 238)
        val week = (safe - 1) / 7 + 1
        val day = (safe - 1) % 7 + 1
        val world = Worlds.indexForWeek(week)
        val random = Random(safe * 7919 + 17)
        val challenges = listOf(
            makeEarChallenge(safe, week, day, random, store),
            makeRhythmChallenge(safe, week, day, random, store),
            makeReadingChallenge(safe, week, day, random, store),
            makeBuildChallenge(safe, week, day, random, store),
            makeSingingChallenge(safe, week, day, random, store),
            makeCreativeChallenge(safe, week, day, store)
        )
        return MissionSpec(
            safe, week, day, world, Worlds.titles[world], Worlds.emoji[world],
            Curriculum.weekTitles[week - 1], storyFor(world, day), Curriculum.goals[week - 1], challenges
        )
    }

    private fun storyFor(world: Int, day: Int): String {
        val lines = listOf(
            listOf("В лесу погасли светлячки-пульсаторы. Верни им ровное мерцание.","Эхо спряталось между деревьями. Найди точное повторение.","Ручей меняет скорость. Удержи ровный шаг.","Два мостика открываются только в правильном размере.","Тропа закручивается в три шага. Не потеряй первую долю.","Лес запомнил твою музыку. Придумай ответ.","Сегодня лес проверяет память без подсказок."),
            listOf("В долине каждый звук ищет свой дом.","Опорные камни звучат устойчиво. Найди их.","Некоторые звуки хотят сделать ещё шаг. Услышь направление.","Ноты начинают появляться на карте долины.","Тишина стала частью мелодии.","Собери свой короткий маршрут по ступеням.","Узнай знакомые звуки в новом порядке."),
            listOf("Город работает на музыкальном ритме. Запусти механизмы.","Улицы делятся на такты. Найди правильную длину.","Башенные часы считают три доли.","Музыкальные фразы помогают открыть ворота.","На площади появилась мажорная лестница.","Создай свой городской сигнал.","Проверь, что сохранилось в памяти."),
            listOf("Каждый остров звучит в своей тональности.","На первом острове сияет фа-диез.","На втором спрятался си-бемоль.","На третьем нужно услышать два диеза.","Перенеси знакомый мотив на новый берег.","Собери музыкальный флаг острова.","Сравни маршруты между островами."),
            listOf("Механизмы бухты работают только при точном ритме.","Перед сильной долей появляется затакт.","Шестерёнка делит долю на четыре быстрых части.","Смешай длительности, не потеряв общий ход.","Настрой механизм и придумай свой ритм.","Проверь механизм без подсказок.","Бухта открывает путь дальше."),
            listOf("В космосе расстояния между звуками становятся видимыми.","Два соседних спутника образуют секунду.","Через одну ступень — терция.","Квинта звучит широко и устойчиво.","Октава — тот же звук в другом регистре.","Повтори звёздный рисунок выше.","Собери созвездие интервалов."),
            listOf("Башня музыки хранит последние ключи года.","В Си-бемоль мажоре нужно найти два бемоля.","Знакомые навыки появляются в новых сочетаниях.","Мажор и минор начинают с одной тоники, но звучат по-разному.","Башня смешивает слух, чтение и ритм.","Создай собственную финальную фразу.","Финальная дверь откроется после настоящего применения знаний.")
        )
        return lines[world][(day - 1).coerceIn(0, 6)]
    }

    private fun difficulty(store: ProgressStore, skill: SkillTag): Int {
        val s = store.skillState(skill)
        return when { s.attempts < 4 -> 1; s.score < 0.46 -> 0; s.score > 0.82 && s.attempts >= 6 -> 2; else -> 1 }
    }

    private fun makeEarChallenge(lessonId: Int, week: Int, day: Int, random: Random, store: ProgressStore): ChallengeSpec {
        val skill = when (week) {
            in 1..2 -> SkillTag.PITCH; in 3..5 -> SkillTag.PULSE; in 6..10 -> SkillTag.TONALITY
            in 11..15 -> SkillTag.RHYTHM; in 16..20 -> SkillTag.KEYS; in 21..24 -> SkillTag.RHYTHM
            in 25..30 -> SkillTag.INTERVALS; else -> SkillTag.KEYS
        }
        val d = difficulty(store, skill)
        val rounds = (0 until 4).map { earRound(week, day, it, random, d, skill) }
        return ChallengeSpec("ear-$lessonId","Ухо-детектив","Сначала слушай, потом выбирай.",ChallengeType.LISTEN_CHOICE,rounds,3,2,skill)
    }

    private fun earRound(week: Int, day: Int, idx: Int, random: Random, d: Int, skill: SkillTag): RoundSpec {
        if (week <= 2) {
            val first = 60 + random.nextInt(0, 5)
            val moves = if (d == 0) listOf(-2,0,2) else listOf(-2,0,2,4)
            val second = first + moves.random(random)
            val correct = when { second > first -> "up"; second < first -> "down"; else -> "same" }
            return RoundSpec(
                "Куда двинулся второй звук?","Нажми «Слушать», затем выбери направление.",
                audioA=listOf(first,second),
                options=listOf(AnswerOption("up","выше ↗"),AnswerOption("same","тот же →"),AnswerOption("down","ниже ↘")).shuffled(random),
                correctOptionId=correct,
                hint="Сравни только второй звук с первым. Представь этажи.",
                explanation=when(correct){"up"->"Второй звук выше первого.";"down"->"Второй звук ниже первого.";else->"Высота не изменилась."},
                skill=skill,difficulty=d
            )
        }

        if (week in 3..5 || week in 11..12 || week in 20..24) {
            val meter = when(week){5,12->3;20->4;else->2}
            return RoundSpec(
                "Через сколько долей возвращается сильная?","Слушай сильный щелчок и тихие после него.",
                rhythmA=List(meter*2){1.0},
                options=listOf(2,3,4).map{AnswerOption("m$it","$it доли")}.shuffled(random),
                correctOptionId="m$meter",
                hint="Считай от одного сильного щелчка до следующего.",
                explanation="Сильная доля возвращается через $meter доли: это ощущение размера $meter/4.",
                skill=skill,difficulty=d
            )
        }

        if (week in 6..10 || week == 24 || week == 33) {
            val stable = (idx + day) % 2 == 0
            val melody = if (stable) listOf(60,64,67,60) else listOf(60,64,67,62)
            return RoundSpec(
                "Как звучит конец?","Слушай последнюю ноту особенно внимательно.",audioA=melody,
                options=listOf(AnswerOption("home","закончилось дома"),AnswerOption("open","хочется продолжения")).shuffled(random),
                correctOptionId=if(stable)"home" else "open",
                hint="Сравни последний звук с первым. Тоника ощущается как точка покоя.",
                explanation=if(stable)"Последний звук — тоника, поэтому фраза завершена." else "Последний звук не тоника, поэтому хочется продолжения.",
                skill=skill,difficulty=d
            )
        }

        if (week in 16..19 || week in 31..32) {
            val key = when(week){16->"Соль мажор";17->"Фа мажор";18->"Ре мажор";31,32->"Си-бемоль мажор";else->"До мажор"}
            val signs = when(key){"Соль мажор"->"фа♯";"Фа мажор"->"си♭";"Ре мажор"->"фа♯ и до♯";"Си-бемоль мажор"->"си♭ и ми♭";else->"без знаков"}
            val distractors=listOf("без знаков","фа♯","си♭","фа♯ и до♯","си♭ и ми♭").filter{it!=signs}.shuffled(random).take(if(d==0)1 else 2)
            val opts=(listOf(signs)+distractors).mapIndexed{i,s->AnswerOption(if(s==signs)"correct" else "d$i",s)}.shuffled(random)
            return RoundSpec(
                "Какие ключевые знаки подходят к $key?","Сначала послушай гамму, затем выбери знаки.",
                audioA=scaleForKey(key),options=opts,correctOptionId="correct",
                hint="Вспомни, какой звук отличает эту тональность от До мажора.",explanation="$key: $signs.",
                skill=skill,difficulty=d
            )
        }

        if (week in 25..30) {
            val intervals=listOf(Triple("sec","секунда",2),Triple("third","терция",4),Triple("fifth","квинта",7),Triple("oct","октава",12))
            val active=when(week){26->intervals.take(2);27->intervals.take(3);else->intervals}
            val chosen=active[(idx+day)%active.size]
            return RoundSpec(
                "Какое расстояние прозвучало?","Послушай два звука подряд.",audioA=listOf(60,60+chosen.third),
                options=active.map{AnswerOption(it.first,it.second)}.shuffled(random),correctOptionId=chosen.first,
                hint="Сравни ширину расстояния: секунда самая узкая, октава — тот же звук выше.",
                explanation="Это " + chosen.second + ".",skill=skill,difficulty=d
            )
        }

        return RoundSpec(
            "Мелодия идёт по соседним ступеням или скачками?","Слушай расстояние между соседними звуками.",audioA=listOf(60,62,64,65,67),
            options=listOf(AnswerOption("steps","по соседним ступеням"),AnswerOption("jumps","скачками"),AnswerOption("same","на одном звуке")).shuffled(random),
            correctOptionId="steps",hint="Если каждый следующий звук совсем рядом, это поступенное движение.",
            explanation="Здесь мелодия движется по соседним ступеням.",skill=skill,difficulty=d
        )
    }

    private fun makeRhythmChallenge(lessonId:Int, week:Int, day:Int, random:Random, store:ProgressStore):ChallengeSpec{
        val skill=SkillTag.RHYTHM
        val d=difficulty(store,skill)
        val rounds=(0 until 4).map{idx->
            val target=rhythmPattern(week,idx+day,d)
            val d1=mutateRhythm(target,1)
            val d2=mutateRhythm(target,2)
            RoundSpec(
                "Какой ритм ты услышал?","Сначала слушай. Только потом сравни три рисунка.",rhythmA=target,
                options=listOf(AnswerOption("a",rhythmLabel(target),rhythm=target),AnswerOption("b",rhythmLabel(d1),rhythm=d1),AnswerOption("c",rhythmLabel(d2),rhythm=d2)).shuffled(random),
                correctOptionId="a",hint="Сначала найди место быстрых звуков, потом сравни.",
                explanation="Правильный рисунок: " + rhythmLabel(target) + ".",skill=skill,difficulty=d
            )
        }
        return ChallengeSpec("rhythm-$lessonId","Ритм-разведка","У каждого вопроса есть правдоподобные неверные варианты.",ChallengeType.RHYTHM_CHOICE,rounds,3,2,skill)
    }

    private fun makeReadingChallenge(lessonId:Int,week:Int,day:Int,random:Random,store:ProgressStore):ChallengeSpec{
        val skill=if(week>=15)SkillTag.KEYS else SkillTag.READING
        val d=difficulty(store,skill)
        val rounds=(0 until 4).map{idx->
            val target=melodyFor(week,day+idx,d)
            val note=target[(idx+day)%target.size]
            val candidates=nearbyMidi(note,d)
            RoundSpec(
                "Какую ноту ты услышал?","Сначала услышь, затем выбери имя.",audioA=listOf(note),
                options=candidates.map{midi->AnswerOption(midi.toString(),noteName(midi),listOf(midi))}.shuffled(random),
                correctOptionId=note.toString(),hint="Ошибка чаще всего рядом: сравни соседние ноты.",
                explanation="Это " + noteName(note) + ".",skill=skill,difficulty=d
            )
        }
        return ChallengeSpec("reading-$lessonId",if(week<9)"Карта высоты" else "Нотный навигатор","Услышать → назвать → увидеть.",ChallengeType.NOTE_CHOICE,rounds,3,2,skill)
    }

    private fun makeBuildChallenge(lessonId:Int,week:Int,day:Int,random:Random,store:ProgressStore):ChallengeSpec{
        val type=if(week<=5 || week in 11..12 || week in 20..23)ChallengeType.RHYTHM_BUILD else ChallengeType.MELODY_BUILD
        val skill=if(type==ChallengeType.RHYTHM_BUILD)SkillTag.WRITING else SkillTag.MEMORY
        val d=difficulty(store,skill)
        val rounds=(0 until 3).map{idx->
            if(type==ChallengeType.RHYTHM_BUILD){
                val target=rhythmPattern(week,day+idx,d)
                RoundSpec(
                    "Собери услышанный ритм.","Нажимай длительности в услышанном порядке. Ответ проверится только по кнопке «Проверить».",
                    rhythmA=target,targetRhythm=target,hint="Слушай место быстрых и длинных звуков.",
                    explanation="Сравни свой вариант с эталоном и исправь отличающиеся места.",skill=skill,difficulty=d
                )
            }else{
                val target=melodyFor(week,day+idx,d).take(if(d==0)3 else if(d==1)4 else 5)
                RoundSpec(
                    "Собери мелодию по слуху.","Нажимай ноты на клавиатуре. Проверка запускается отдельно.",audioA=target,targetMelody=target,
                    hint="Сначала пой фразу на «лу», затем ищи ноты по одной.",
                    explanation="После проверки послушай эталон и свой порядок ещё раз.",skill=skill,difficulty=d
                )
            }
        }
        return ChallengeSpec("build-$lessonId",if(type==ChallengeType.RHYTHM_BUILD)"Собери ритм" else "Мини-диктант","Здесь нельзя победить простым набором.",type,rounds,2,3,skill)
    }

    private fun makeSingingChallenge(lessonId:Int,week:Int,day:Int,random:Random,store:ProgressStore):ChallengeSpec{
        val skill=SkillTag.PITCH
        val d=difficulty(store,skill)
        val rounds=(0 until 2).map{idx->
            val melody=melodyFor(week,day+idx,d).take(if(d==2)5 else 3)
            RoundSpec(
                "Повтори фразу голосом.","Сначала послушай, потом пой на «лу». Микрофон — помощник, а не судья.",
                audioA=melody,targetMelody=melody,hint="Если трудно, спой первый и последний звук, потом добавь середину.",
                explanation="Цель — удержать направление и опорные звуки, а не получить идеальный компьютерный балл.",
                skill=skill,difficulty=d
            )
        }
        return ChallengeSpec("sing-$lessonId","Голосовой мост","Музыка должна перейти из слуха в собственный голос.",ChallengeType.SINGING,rounds,1,2,skill)
    }

    private fun makeCreativeChallenge(lessonId:Int,week:Int,day:Int,store:ProgressStore):ChallengeSpec{
        val d=difficulty(store,SkillTag.CREATIVE)
        val prompt=when(week){
            in 1..2->"Придумай ответ из 3 звуков: первый и последний должны различаться."
            in 3..5->"Придумай ритм на один такт и повтори его второй раз одинаково."
            in 6..10->"Придумай музыкальный вопрос и ответ, который заканчивается на тонике."
            in 11..15->"Измени ритм знакомого мотива, сохранив его направление."
            in 16..20->"Перенеси короткий мотив на другую высоту."
            in 21..24->"Сделай два разных ритма с одинаковой общей длительностью."
            in 25..30->"Придумай фразу, где есть шаг и один изученный интервал."
            else->"Собери короткую музыкальную фразу из слуха, ритма и одного осознанного выбора."
        }
        val round=RoundSpec(
            prompt,"Сначала придумай, потом исполни дважды. Если второй раз сильно отличается — упрости.",
            hint="Ограничение помогает творчеству: используй только знакомые элементы недели.",
            explanation="Творческое задание проверяет перенос навыка, а не запоминание готового ответа.",
            skill=SkillTag.CREATIVE,difficulty=d
        )
        return ChallengeSpec("creative-$lessonId","Твоя музыка","Последняя часть миссии — применение, а не угадывание.",ChallengeType.CREATIVE,listOf(round),1,2,SkillTag.CREATIVE)
    }

    private fun melodyFor(week:Int,seed:Int,d:Int):List<Int>{
        val root=when(week){16->67;17->65;18->62;31,32->70;else->60}
        val major=listOf(0,2,4,5,7,9,11,12)
        val patterns=listOf(listOf(0,1,2,1,0),listOf(0,2,4,2,0),listOf(0,1,2,3,4),listOf(4,3,2,1,0),listOf(0,2,1,3,2,0),listOf(0,4,3,2,1,0))
        val base=patterns[seed.mod(patterns.size)]
        val use=when(d){0->base.take(3);2->base+listOf(2,4);else->base}
        return use.map{root+major[it.coerceIn(0,7)]}
    }

    private fun rhythmPattern(week:Int,seed:Int,d:Int):List<Double>{
        val library=mutableListOf(
            listOf(1.0,1.0,1.0,1.0),listOf(1.0,0.5,0.5,1.0),listOf(0.5,0.5,1.0,1.0),listOf(1.0,1.0,0.5,0.5),listOf(0.5,0.5,0.5,0.5,1.0)
        )
        if(week>=12)library+=listOf(3.0,1.0)
        if(week>=22){library+=listOf(0.25,0.25,0.25,0.25,1.0);library+=listOf(1.0,0.5,0.25,0.25,1.0)}
        val base=library[seed.mod(library.size)]
        return when(d){0->base.take(min(4,base.size));2->if(base.size<6)base+base.take(2) else base;else->base}
    }

    private fun mutateRhythm(source:List<Double>,variant:Int):List<Double>{
        if(source.size<2)return source.reversed()
        val out=source.toMutableList()
        if(variant==1){val tmp=out[0];out[0]=out[out.lastIndex];out[out.lastIndex]=tmp}
        else{val i=min(1,out.lastIndex);out[i]=when(out[i]){1.0->0.5;0.5->1.0;0.25->0.5;3.0->1.0;else->1.0}}
        if(out==source)out.reverse()
        return out
    }

    private fun rhythmLabel(r:List<Double>):String=r.joinToString("  "){when{it>=2.9->"ТА-а-а";it>=1.9->"ТА-а";it>=0.9->"ТА";it>=0.49->"ти";else->"та-ка"}}
    private fun nearbyMidi(target:Int,d:Int):List<Int>{
        val offsets=when(d){0->listOf(0,2);2->listOf(-2,-1,0,1,2);else->listOf(-2,0,2)}
        return offsets.map{target+it}.distinct()
    }
    private fun noteName(midi:Int):String{
        val names=listOf("до","до♯","ре","ре♯","ми","фа","фа♯","соль","соль♯","ля","ля♯","си")
        return names[midi.mod(12)]
    }
    private fun scaleForKey(key:String):List<Int>{
        val root=when(key){"Соль мажор"->67;"Фа мажор"->65;"Ре мажор"->62;"Си-бемоль мажор"->70;else->60}
        return listOf(0,2,4,5,7,9,11,12).map{root+it}
    }
}
