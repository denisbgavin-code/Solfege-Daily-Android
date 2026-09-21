package ru.solfege.daily

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

private val Ink = Color(0xFF182033)
private val Muted = Color(0xFF6D7484)
private val Blue = Color(0xFF4358D6)
private val Cyan = Color(0xFF43A6D6)
private val Violet = Color(0xFF7C56DA)
private val Green = Color(0xFF2B9A68)
private val Amber = Color(0xFFE89B32)
private val Rose = Color(0xFFE25D7B)
private val Paper = Color(0xFFF6F7FB)

class QuestActivity : ComponentActivity() {
    private lateinit var audio: AudioEngine
    private lateinit var progress: ProgressStore
    private lateinit var pitchDetector: PitchDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audio = AudioEngine()
        progress = ProgressStore(getSharedPreferences("solfege_progress_v2", MODE_PRIVATE))
        pitchDetector = PitchDetector()
        setContent {
            val scheme = lightColorScheme(
                primary = Blue, secondary = Violet, tertiary = Cyan,
                background = Paper, surface = Color.White, onSurface = Ink, onBackground = Ink
            )
            MaterialTheme(colorScheme = scheme) {
                QuestApp(audio, progress, pitchDetector)
            }
        }
    }

    override fun onPause() {
        audio.stop()
        pitchDetector.stop()
        super.onPause()
    }

    override fun onDestroy() {
        audio.close()
        pitchDetector.stop()
        super.onDestroy()
    }
}

private enum class Screen { HOME, MAP, TRAIN, PROGRESS, MISSION }

@Composable
private fun QuestApp(audio: AudioEngine, store: ProgressStore, pitchDetector: PitchDetector) {
    var screen by remember { mutableStateOf(Screen.HOME) }
    var selectedLesson by remember { mutableIntStateOf(store.currentLesson()) }
    var refresh by remember { mutableIntStateOf(0) }
    var practiceMode by remember { mutableStateOf(false) }

    val openMission: (Int) -> Unit = {
        practiceMode = false
        selectedLesson = it
        screen = Screen.MISSION
    }
    val openPractice: (Int) -> Unit = {
        practiceMode = true
        selectedLesson = it
        screen = Screen.MISSION
    }

    Scaffold(
        containerColor = Paper,
        bottomBar = {
            if (screen != Screen.MISSION) {
                NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(screen == Screen.HOME, { screen = Screen.HOME }, { Icon(Icons.Default.Home, null) }, label = { Text("Сегодня") })
                    NavigationBarItem(screen == Screen.MAP, { screen = Screen.MAP }, { Icon(Icons.Default.Map, null) }, label = { Text("Карта") })
                    NavigationBarItem(screen == Screen.TRAIN, { screen = Screen.TRAIN }, { Icon(Icons.Default.AutoAwesome, null) }, label = { Text("Тренировка") })
                    NavigationBarItem(screen == Screen.PROGRESS, { screen = Screen.PROGRESS }, { Icon(Icons.Default.Insights, null) }, label = { Text("Прогресс") })
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (screen) {
                Screen.HOME -> HomeScreen(store, refresh, openMission, { screen = Screen.MAP }, { screen = Screen.TRAIN })
                Screen.MAP -> MapScreen(store, refresh, openMission)
                Screen.TRAIN -> TrainingScreen(store, openPractice)
                Screen.PROGRESS -> ProgressScreen(store, refresh)
                Screen.MISSION -> MissionScreen(
                    MissionFactory.create(selectedLesson, store), audio, store, pitchDetector,
                    practiceMode = practiceMode,
                    onExit = { screen = Screen.HOME },
                    onMissionComplete = { refresh++; screen = Screen.HOME }
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(store: ProgressStore, refresh: Int, onStart: (Int) -> Unit, onMap: () -> Unit, onTrain: () -> Unit) {
    val current = store.currentLesson()
    val mission = MissionFactory.create(current, store)
    val completed = store.completedLesson

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Музыкальная экспедиция", fontSize = 29.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
            Text("Сольфеджио как серия настоящих музыкальных задач", color = Muted, fontSize = 14.sp)
        }
        item { HeroMissionCard(mission, completed, store.stars, onStart) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickCard(Modifier.weight(1f), Icons.Default.Map, "Маршрут", "34 недели", Cyan, onMap)
                QuickCard(Modifier.weight(1f), Icons.Default.AutoAwesome, "Умная тренировка", skillName(store.weakestSkill()), Violet, onTrain)
            }
        }
        item {
            SectionTitle("Почему миссия устроена так", "Текущая тема + повторение + применение")
            SurfaceCard {
                Text(mission.objective, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Каждый тест содержит правдоподобные неверные варианты. Диктанты проверяются только после самостоятельного ответа — приложение не подсказывает правильную последовательность по ходу.",
                    color = Muted, fontSize = 14.sp
                )
            }
        }
        item {
            SectionTitle("Шесть этапов", "Один и тот же навык проходит через разные действия")
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val labels = listOf("Ухо","Ритм","Ноты","Диктант","Голос","Творчество")
                labels.forEachIndexed { index, label ->
                    AssistChip(onClick = {}, label = { Text((index + 1).toString() + ". " + label) })
                }
            }
        }
        item { Spacer(Modifier.height(70.dp)) }
    }
}

@Composable
private fun HeroMissionCard(mission: MissionSpec, completed: Int, stars: Int, onStart: (Int) -> Unit) {
    val gradient = Brush.linearGradient(worldGradient(mission.worldIndex))
    Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
        Column(Modifier.background(gradient).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(mission.worldEmoji, fontSize = 38.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(mission.worldTitle.uppercase(), color = Color.White.copy(alpha = .78f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Неделя " + mission.week + " • день " + mission.day, color = Color.White, fontSize = 14.sp)
                }
                Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = .16f)) {
                    Text("★ " + stars, Modifier.padding(horizontal = 12.dp, vertical = 7.dp), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(mission.title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(7.dp))
            Text(mission.story, color = Color.White.copy(alpha = .92f), lineHeight = 21.sp)
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { (completed / 238f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
                color = Color.White, trackColor = Color.White.copy(alpha = .20f)
            )
            Spacer(Modifier.height(9.dp))
            Text(completed.toString() + " / 238 миссий", color = Color.White.copy(alpha = .78f), fontSize = 12.sp)
            Spacer(Modifier.height(15.dp))
            Button(
                onClick = { onStart(mission.id) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Blue),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text("Начать миссию", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun MapScreen(store: ProgressStore, refresh: Int, onOpen: (Int) -> Unit) {
    val completed = store.completedLesson
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Карта года", fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
            Text("7 миров • 34 недели • 238 миссий", color = Muted)
        }
        Worlds.titles.forEachIndexed { worldIndex, world ->
            val weeks = (1..34).filter { Worlds.indexForWeek(it) == worldIndex }
            item { Text(Worlds.emoji[worldIndex] + "  " + world, fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(top = 8.dp)) }
            items(weeks) { week ->
                val first = (week - 1) * 7 + 1
                val weekDone = (completed - first + 1).coerceIn(0, 7)
                val unlocked = store.isUnlocked(first)
                SurfaceCard(Modifier.clickable(enabled = unlocked) {
                    val target = if (completed < first) first else minOf(first + 6, completed + 1)
                    onOpen(target)
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(46.dp).clip(CircleShape).background(if (weekDone == 7) Green.copy(alpha=.14f) else if (unlocked) Blue.copy(alpha=.12f) else Color(0xFFE9EBF1)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (weekDone == 7) Icon(Icons.Default.Check, null, tint = Green)
                            else Text(week.toString(), fontWeight = FontWeight.Bold, color = if (unlocked) Blue else Muted)
                        }
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text(Curriculum.weekTitles[week - 1], fontWeight = FontWeight.Bold)
                            Text(weekDone.toString() + " из 7 дней", color = Muted, fontSize = 13.sp)
                            Spacer(Modifier.height(7.dp))
                            LinearProgressIndicator(
                                progress = { weekDone / 7f },
                                modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                                color = if (weekDone == 7) Green else Blue,
                                trackColor = Color(0xFFE8EAF0)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Icon(if (unlocked) Icons.Default.ChevronRight else Icons.Default.Lock, null, tint = Muted)
                    }
                }
            }
        }
        item { Spacer(Modifier.height(70.dp)) }
    }
}

@Composable
private fun TrainingScreen(store: ProgressStore, onStart: (Int) -> Unit) {
    val weak = store.weakestSkill()
    val completed = store.completedLesson
    val suggested = smartLessonForSkill(weak, completed)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Умная тренировка", fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
            Text("Не больше заданий — точнее выбранное повторение", color = Muted)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Violet.copy(alpha=.10f)), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Violet, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("Сейчас полезнее: " + skillName(weak), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Повторяем уже открытый материал, где этот навык нужен в музыкальной задаче.", color = Muted, modifier = Modifier.padding(top = 6.dp))
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { if (completed > 0) onStart(suggested) }, enabled = completed > 0, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Text("Запустить тренировочную миссию")
                    }
                }
            }
        }
        item { SectionTitle("Карта навыков", "Это внутренний адаптивный профиль, не школьные оценки") }
        items(SkillTag.entries) { skill ->
            val state = store.skillState(skill)
            SurfaceCard(Modifier.clickable(enabled = completed > 0) { onStart(smartLessonForSkill(skill, completed)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(skillName(skill), fontWeight = FontWeight.SemiBold)
                        Text(if (state.attempts == 0) "ещё нет данных" else state.attempts.toString() + " попыток", color = Muted, fontSize = 12.sp)
                    }
                    Text((state.score * 100).roundToInt().toString() + "%", fontWeight = FontWeight.Bold, color = masteryColor(state.score))
                    Spacer(Modifier.width(7.dp))
                    Icon(Icons.Default.ChevronRight, null, tint = Muted)
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { state.score.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = masteryColor(state.score), trackColor = Color(0xFFE8EAF0)
                )
            }
        }
        item { Spacer(Modifier.height(70.dp)) }
    }
}

@Composable
private fun ProgressScreen(store: ProgressStore, refresh: Int) {
    val completed = store.completedLesson
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item {
            Text("Прогресс", fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
            Text("Не только сколько пройдено, но и что стало устойчивее", color = Muted)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(Modifier.weight(1f), "Миссии", completed.toString() + "/238", Blue)
                StatCard(Modifier.weight(1f), "Звёзды", "★ " + store.stars, Amber)
            }
        }
        item { SectionTitle("Музыкальные навыки", "Слабое место возвращается в следующую умную тренировку") }
        items(SkillTag.entries) { skill ->
            val state = store.skillState(skill)
            SurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(masteryColor(state.score)))
                    Spacer(Modifier.width(10.dp))
                    Text(skillName(skill), Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text(masteryLabel(state), color = masteryColor(state.score), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
        item {
            SurfaceCard {
                Text("Важно", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text("Проценты — техническая оценка для адаптации задач. Это не диагноз, не отметка и не стандартизированный тест музыкальных способностей.", color = Muted, fontSize = 13.sp)
            }
        }
        item { Spacer(Modifier.height(70.dp)) }
    }
}

@Composable
private fun MissionScreen(
    mission: MissionSpec,
    audio: AudioEngine,
    store: ProgressStore,
    pitchDetector: PitchDetector,
    practiceMode: Boolean,
    onExit: () -> Unit,
    onMissionComplete: () -> Unit
) {
    var challengeIndex by remember(mission.id) { mutableIntStateOf(store.missionCursor(mission.id).coerceIn(0, 5)) }
    var missionStars by remember(mission.id) { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    if (finished) {
        MissionCompleteScreen(mission, missionStars, practiceMode) {
            onMissionComplete()
        }
        return
    }

    Column(Modifier.fillMaxSize().background(Paper)) {
        MissionHeader(mission, challengeIndex, onExit)
        ChallengeRunner(
            challenge = mission.challenges[challengeIndex],
            missionId = mission.id,
            audio = audio,
            store = store,
            pitchDetector = pitchDetector,
            onComplete = { stars ->
                val totalStars = missionStars + stars
                missionStars = totalStars
                if (challengeIndex >= mission.challenges.lastIndex) {
                    if (!practiceMode) {
                        store.completeLesson(mission.id, totalStars)
                        store.clearCursor()
                    }
                    finished = true
                } else {
                    challengeIndex++
                    store.saveMissionCursor(mission.id, challengeIndex)
                }
            }
        )
    }
}

@Composable
private fun MissionHeader(mission: MissionSpec, challengeIndex: Int, onExit: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Brush.horizontalGradient(worldGradient(mission.worldIndex)))
            .padding(start=12.dp,end=16.dp,top=10.dp,bottom=13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onExit) { Icon(Icons.Default.Close, null, tint = Color.White) }
            Column(Modifier.weight(1f)) {
                Text(mission.worldEmoji + " " + mission.worldTitle, color = Color.White.copy(alpha=.78f), fontSize=12.sp, fontWeight=FontWeight.Bold)
                Text("День " + mission.day + " • " + mission.title, color=Color.White, fontWeight=FontWeight.Bold)
            }
            Text((challengeIndex+1).toString()+"/6", color=Color.White, fontWeight=FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { (challengeIndex+1)/6f },
            modifier=Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            color=Color.White, trackColor=Color.White.copy(alpha=.2f)
        )
    }
}

@Composable
private fun ChallengeRunner(
    challenge: ChallengeSpec,
    missionId: Int,
    audio: AudioEngine,
    store: ProgressStore,
    pitchDetector: PitchDetector,
    onComplete: (Int) -> Unit
) {
    var roundIndex by remember(challenge.id) { mutableIntStateOf(0) }
    var correct by remember(challenge.id) { mutableIntStateOf(0) }
    var wrong by remember(challenge.id, roundIndex) { mutableIntStateOf(0) }
    var feedback by remember(challenge.id, roundIndex) { mutableStateOf<String?>(null) }
    var feedbackGood by remember(challenge.id, roundIndex) { mutableStateOf(false) }
    var roundFinished by remember(challenge.id, roundIndex) { mutableStateOf(false) }
    var answered by remember(challenge.id, roundIndex) { mutableStateOf(setOf<String>()) }
    val round = challenge.rounds[roundIndex]

    fun finishRound(result: Double, wasCorrect: Boolean, message: String) {
        store.recordSkill(round.skill, result, missionId)
        if (wasCorrect) correct++
        feedback = message
        feedbackGood = wasCorrect
        roundFinished = true
    }

    fun nextRound() {
        if (roundIndex >= challenge.rounds.lastIndex) {
            val ratio = correct.toDouble() / challenge.rounds.size
            val stars = if (correct >= challenge.requiredCorrect) challenge.rewardStars else if (ratio >= .5) maxOf(1, challenge.rewardStars-1) else 1
            onComplete(stars)
        } else roundIndex++
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding=PaddingValues(18.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(challengeColor(challenge.type).copy(alpha=.13f)),
                    contentAlignment=Alignment.Center
                ) { Icon(challengeIconByType(challenge.type),null,tint=challengeColor(challenge.type)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(challenge.title,fontSize=22.sp,fontWeight=FontWeight.ExtraBold)
                    Text(challenge.subtitle,color=Muted,fontSize=13.sp)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                challenge.rounds.indices.forEach { i ->
                    val color=when { i<roundIndex->Green; i==roundIndex->Blue; else->Color(0xFFE0E3EB) }
                    Box(Modifier.weight(1f).height(5.dp).clip(CircleShape).background(color))
                }
            }
        }
        item {
            SurfaceCard {
                Text(round.prompt,fontSize=21.sp,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(round.instruction,color=Muted,lineHeight=20.sp)
            }
        }
        item {
            when(challenge.type) {
                ChallengeType.LISTEN_CHOICE,ChallengeType.RHYTHM_CHOICE,ChallengeType.NOTE_CHOICE ->
                    ChoiceRound(round,audio,answered,roundFinished) { id ->
                        if(!roundFinished) {
                            answered=answered+id
                            if(id==round.correctOptionId) {
                                finishRound(if(wrong==0)1.0 else .72,true,round.explanation)
                            } else {
                                wrong++
                                if(wrong>=2) {
                                    val label=round.options.firstOrNull{it.id==round.correctOptionId}?.label ?: ""
                                    finishRound(.32,false,"Правильный ответ: "+label+". "+round.explanation)
                                } else {
                                    feedback="Не совпало. "+round.hint
                                    feedbackGood=false
                                }
                            }
                        }
                    }
                ChallengeType.MELODY_BUILD -> MelodyBuildRound(round,audio,roundFinished) { score,exact ->
                    finishRound(score,exact,if(exact)"Мелодия собрана точно." else "Есть отличия. "+round.hint)
                }
                ChallengeType.RHYTHM_BUILD -> RhythmBuildRound(round,audio,roundFinished) { score,exact ->
                    finishRound(score,exact,if(exact)"Ритм собран точно." else "Есть отличия. "+round.hint)
                }
                ChallengeType.SINGING -> SingingRound(round,audio,pitchDetector,roundFinished) { score ->
                    finishRound(score,score>=.7,if(score>=.7)"Фраза выполнена." else "Попытка засчитана с поддержкой.")
                }
                ChallengeType.CREATIVE -> CreativeRound(round,audio,roundFinished) { finishRound(.82,true,round.explanation) }
            }
        }
        feedback?.let { message -> item { FeedbackCard(message,feedbackGood,roundFinished) } }
        if(roundFinished) {
            item {
                Button(onClick={nextRound()},modifier=Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(18.dp)) {
                    Text(if(roundIndex==challenge.rounds.lastIndex)"Завершить этап" else "Следующий вопрос",fontWeight=FontWeight.Bold)
                    Spacer(Modifier.width(6.dp)); Icon(Icons.Default.ArrowForward,null)
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun ChoiceRound(round:RoundSpec,audio:AudioEngine,answered:Set<String>,disabled:Boolean,onSelect:(String)->Unit) {
    var heard by remember(round) { mutableStateOf(false) }
    Column(verticalArrangement=Arrangement.spacedBy(9.dp)) {
        PlayButton(round,audio) { heard = true }
        if(!heard) Text("Сначала прослушай пример — варианты откроются после него.",color=Muted,fontSize=13.sp)
        round.options.forEach { option ->
            val used=option.id in answered
            OutlinedButton(
                onClick={onSelect(option.id)},enabled=heard&&!disabled&&!used,
                modifier=Modifier.fillMaxWidth().heightIn(min=54.dp),shape=RoundedCornerShape(16.dp),
                colors=ButtonDefaults.outlinedButtonColors(containerColor=if(used)Rose.copy(alpha=.07f) else Color.White)
            ) {
                if(option.rhythm.isNotEmpty()) { MiniRhythm(option.rhythm,Modifier.width(105.dp).height(36.dp)); Spacer(Modifier.width(9.dp)) }
                Text(option.label,fontSize=16.sp,fontWeight=FontWeight.SemiBold,color=Ink)
            }
        }
    }
}

@Composable
private fun PlayButton(round:RoundSpec,audio:AudioEngine,onPlayed:()->Unit={}) {
    var playing by remember { mutableStateOf(false) }
    Button(
        onClick={
            if(!playing) {
                playing=true
                when {
                    round.audioA.isNotEmpty()->audio.playMidi(round.audioA.toIntArray()){playing=false;onPlayed()}
                    round.rhythmA.isNotEmpty()->audio.playRhythm(round.rhythmA.toDoubleArray(),round.meter){playing=false;onPlayed()}
                    else->{playing=false;onPlayed()}
                }
            }
        },
        modifier=Modifier.fillMaxWidth().height(56.dp),shape=RoundedCornerShape(18.dp),
        colors=ButtonDefaults.buttonColors(containerColor=Blue)
    ) {
        Icon(if(playing)Icons.Default.GraphicEq else Icons.Default.PlayArrow,null)
        Spacer(Modifier.width(8.dp)); Text(if(playing)"Слушай…" else "Слушать",fontWeight=FontWeight.Bold,fontSize=16.sp)
    }
}

@Composable
private fun MelodyBuildRound(round:RoundSpec,audio:AudioEngine,disabled:Boolean,onCheck:(Double,Boolean)->Unit) {
    var entered by remember(round) { mutableStateOf(listOf<Int>()) }
    val target=round.targetMelody
    val palette=remember(target) {
        val lo=(target.minOrNull()?:60)-2; val hi=(target.maxOrNull()?:67)+2
        (lo..hi).filter{it in 55..84}.distinct()
    }
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        PlayButton(round,audio)
        SurfaceCard {
            Text("Твой ответ",fontWeight=FontWeight.Bold); Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                repeat(target.size) { i ->
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(if(i<entered.size)Blue.copy(alpha=.12f) else Color(0xFFEEF0F5)),
                        contentAlignment=Alignment.Center
                    ) { Text(if(i<entered.size)noteNameUi(entered[i]) else "?",fontWeight=FontWeight.Bold,color=if(i<entered.size)Blue else Muted) }
                }
            }
        }
        Text("Клавиатура",fontWeight=FontWeight.Bold)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            palette.forEach { midi ->
                Button(
                    onClick={if(!disabled&&entered.size<target.size){entered=entered+midi;audio.playHomeNote(midi,null)}},
                    enabled=!disabled,contentPadding=PaddingValues(horizontal=12.dp,vertical=10.dp),shape=RoundedCornerShape(12.dp),
                    colors=ButtonDefaults.buttonColors(containerColor=if(isBlackKey(midi))Ink else Color.White,contentColor=if(isBlackKey(midi))Color.White else Ink)
                ) { Text(noteNameUi(midi),fontSize=12.sp) }
            }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick={if(entered.isNotEmpty())entered=entered.dropLast(1)},enabled=!disabled,modifier=Modifier.weight(1f)){Text("Убрать")}
            Button(
                onClick={
                    val same=entered.zip(target).count{it.first==it.second}
                    val score=if(target.isEmpty())0.0 else same.toDouble()/target.size
                    onCheck(score,entered==target)
                },
                enabled=!disabled&&entered.size==target.size,modifier=Modifier.weight(1f)
            ){Text("Проверить")}
        }
    }
}

@Composable
private fun RhythmBuildRound(round:RoundSpec,audio:AudioEngine,disabled:Boolean,onCheck:(Double,Boolean)->Unit) {
    var entered by remember(round){mutableStateOf(listOf<Double>())}
    val target=round.targetRhythm
    val palette=listOf(2.0,1.0,.5,.25).filter{p->target.any{abs(it-p)<.001}||p in listOf(1.0,.5)}
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        PlayButton(round,audio)
        SurfaceCard {
            Text("Твой ритм",fontWeight=FontWeight.Bold); Spacer(Modifier.height(8.dp))
            if(entered.isEmpty())Text("Пока пусто. Нажми длительности ниже.",color=Muted)
            else MiniRhythm(entered,Modifier.fillMaxWidth().height(54.dp))
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            palette.forEach { d -> Button(onClick={if(!disabled&&entered.size<target.size+2)entered=entered+d},enabled=!disabled,shape=RoundedCornerShape(14.dp)){Text(durationLabel(d),fontSize=17.sp)} }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick={if(entered.isNotEmpty())entered=entered.dropLast(1)},enabled=!disabled,modifier=Modifier.weight(1f)){Text("Убрать")}
            Button(
                onClick={
                    val n=maxOf(target.size,entered.size)
                    val same=(0 until minOf(target.size,entered.size)).count{abs(target[it]-entered[it])<.001}
                    val score=if(n==0)0.0 else same.toDouble()/n
                    onCheck(score,entered==target)
                },
                enabled=!disabled&&entered.isNotEmpty(),modifier=Modifier.weight(1f)
            ){Text("Проверить")}
        }
    }
}

@Composable
private fun SingingRound(round:RoundSpec,audio:AudioEngine,detector:PitchDetector,disabled:Boolean,onDone:(Double)->Unit) {
    val context=LocalContext.current
    var status by remember(round){mutableStateOf("Послушай фразу, затем повтори голосом.")}
    var listening by remember(round){mutableStateOf(false)}
    var match by remember(round){mutableStateOf(false)}
    val target=round.targetMelody.firstOrNull()?:60
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->
        if(granted)listening=true else status="Микрофон не разрешён. Можно завершить после самостоятельного сравнения."
    }
    LaunchedEffect(listening) {
        if(!listening){detector.stop();return@LaunchedEffect}
        detector.start(target,object:PitchDetector.Listener{
            override fun onLevel(rms:Double){}
            override fun onPitch(hz:Double,midi:Double,centsFromTarget:Double,stableMatch:Boolean){
                val delta=centsFromTarget.roundToInt()
                status=when{stableMatch->"Первый звук совпал. Теперь пропой всю фразу.";abs(delta)<25->"Очень близко к первому звуку.";delta>0->"Первый звук немного выше.";else->"Первый звук немного ниже."}
                if(stableMatch)match=true
            }
            override fun onError(message:String){status=message}
        })
    }
    DisposableEffect(Unit){onDispose{detector.stop()}}
    Column(verticalArrangement=Arrangement.spacedBy(9.dp)) {
        PlayButton(round,audio)
        SurfaceCard{Text(status,color=if(match)Green else Muted,fontWeight=if(match)FontWeight.Bold else FontWeight.Normal)}
        OutlinedButton(
            onClick={
                val granted=context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED
                if(granted)listening=!listening else launcher.launch(Manifest.permission.RECORD_AUDIO)
            },
            enabled=!disabled,modifier=Modifier.fillMaxWidth()
        ){Icon(Icons.Default.Mic,null);Spacer(Modifier.width(7.dp));Text(if(listening)"Остановить помощник" else "Проверить первый звук микрофоном")}
        Button(onClick={detector.stop();listening=false;onDone(if(match)1.0 else .68)},enabled=!disabled,modifier=Modifier.fillMaxWidth()){Text("Я спел фразу и сравнил")}
        Text("Микрофон работает локально и не обязателен. Он не хранит запись и не решает за ребёнка, хорошо ли исполнена вся фраза.",color=Muted,fontSize=12.sp)
    }
}

@Composable
private fun CreativeRound(round:RoundSpec,audio:AudioEngine,disabled:Boolean,onDone:()->Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        SurfaceCard {
            Icon(Icons.Default.Lightbulb,null,tint=Amber,modifier=Modifier.size(30.dp))
            Spacer(Modifier.height(7.dp));Text("Правило творчества",fontWeight=FontWeight.Bold)
            Text(round.instruction,color=Muted,modifier=Modifier.padding(top=4.dp))
        }
        if(round.audioA.isNotEmpty())PlayButton(round,audio)
        Button(onClick=onDone,enabled=!disabled,modifier=Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(18.dp)){
            Icon(Icons.Default.CheckCircle,null);Spacer(Modifier.width(7.dp));Text("Я придумал и исполнил дважды")
        }
    }
}

@Composable
private fun FeedbackCard(message:String,good:Boolean,final:Boolean) {
    val color=if(good)Green else if(final)Amber else Rose
    Card(colors=CardDefaults.cardColors(containerColor=color.copy(alpha=.10f)),shape=RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(14.dp),verticalAlignment=Alignment.Top) {
            Icon(if(good)Icons.Default.CheckCircle else Icons.Default.TipsAndUpdates,null,tint=color)
            Spacer(Modifier.width(10.dp));Text(message,lineHeight=20.sp)
        }
    }
}

@Composable
private fun MissionCompleteScreen(mission:MissionSpec,stars:Int,practiceMode:Boolean,onContinue:()->Unit) {
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF3447BC),Color(0xFF7C56D9)))),
        contentAlignment=Alignment.Center
    ) {
        Column(Modifier.padding(26.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Text("✨",fontSize=68.sp);Spacer(Modifier.height(8.dp))
            Text(if(practiceMode)"Тренировка завершена" else "Миссия выполнена",color=Color.White,fontSize=28.sp,fontWeight=FontWeight.ExtraBold)
            Text("День "+mission.day+" • "+mission.title,color=Color.White.copy(alpha=.80f),textAlign=TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Surface(shape=RoundedCornerShape(20.dp),color=Color.White.copy(alpha=.13f)){Text("★ +"+stars,Modifier.padding(horizontal=22.dp,vertical=12.dp),color=Color.White,fontSize=22.sp,fontWeight=FontWeight.Bold)}
            Spacer(Modifier.height(20.dp))
            Text(if(practiceMode)"Результаты уже учтены в карте навыков. Основной маршрут не изменён." else "Следующий день откроется сейчас. Ошибки не блокируют маршрут: они меняют будущие повторения и сложность.",color=Color.White.copy(alpha=.88f),textAlign=TextAlign.Center,lineHeight=21.sp)
            Spacer(Modifier.height(22.dp))
            Button(onClick=onContinue,colors=ButtonDefaults.buttonColors(containerColor=Color.White,contentColor=Blue),modifier=Modifier.fillMaxWidth().height(55.dp),shape=RoundedCornerShape(18.dp)){Text(if(practiceMode)"Вернуться домой" else "Открыть следующий день",fontWeight=FontWeight.Bold)}
        }
    }
}

@Composable
private fun SurfaceCard(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit) {
    Card(modifier=modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=Color.White),shape=RoundedCornerShape(20.dp),elevation=CardDefaults.cardElevation(defaultElevation=1.dp)){
        Column(Modifier.padding(15.dp),content=content)
    }
}

@Composable
private fun QuickCard(modifier:Modifier,icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,text:String,color:Color,onClick:()->Unit) {
    Card(modifier=modifier.clickable(onClick=onClick),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=color.copy(alpha=.10f))) {
        Column(Modifier.padding(15.dp)) {
            Icon(icon,null,tint=color,modifier=Modifier.size(28.dp));Spacer(Modifier.height(10.dp))
            Text(title,fontWeight=FontWeight.Bold,fontSize=15.sp);Text(text,color=Muted,fontSize=12.sp,maxLines=1)
        }
    }
}

@Composable
private fun StatCard(modifier:Modifier,label:String,value:String,color:Color) {
    Card(modifier,shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=color.copy(alpha=.10f))){
        Column(Modifier.padding(16.dp)){Text(label,color=Muted,fontSize=13.sp);Spacer(Modifier.height(4.dp));Text(value,fontWeight=FontWeight.ExtraBold,fontSize=22.sp,color=color)}
    }
}

@Composable
private fun SectionTitle(title:String,subtitle:String) {
    Column{Text(title,fontWeight=FontWeight.ExtraBold,fontSize=20.sp);Text(subtitle,color=Muted,fontSize=13.sp)}
}

@Composable
private fun MiniRhythm(rhythm:List<Double>,modifier:Modifier=Modifier) {
    Canvas(modifier) {
        if(rhythm.isEmpty())return@Canvas
        val total=rhythm.sum().coerceAtLeast(.1)
        var x=0f
        rhythm.forEachIndexed { i,d ->
            val w=(size.width*(d/total)).toFloat()
            val center=x+w/2
            drawLine(Blue,Offset(center,size.height*.20f),Offset(center,size.height*.72f),strokeWidth=3f,cap=StrokeCap.Round)
            drawCircle(Blue,radius=6f,center=Offset(center,size.height*.72f))
            if(d<=.5&&i<rhythm.lastIndex)drawLine(Blue,Offset(center,size.height*.22f),Offset(minOf(size.width,center+w*.6f),size.height*.22f),strokeWidth=3f)
            x+=w
        }
    }
}

private fun challengeIconByType(type:ChallengeType)=when(type){
    ChallengeType.LISTEN_CHOICE->Icons.Default.Hearing
    ChallengeType.RHYTHM_CHOICE->Icons.Default.GraphicEq
    ChallengeType.NOTE_CHOICE->Icons.Default.MusicNote
    ChallengeType.MELODY_BUILD->Icons.Default.EditNote
    ChallengeType.RHYTHM_BUILD->Icons.Default.GridView
    ChallengeType.SINGING->Icons.Default.Mic
    ChallengeType.CREATIVE->Icons.Default.AutoAwesome
}
private fun challengeColor(type:ChallengeType)=when(type){
    ChallengeType.LISTEN_CHOICE->Blue
    ChallengeType.RHYTHM_CHOICE->Cyan
    ChallengeType.NOTE_CHOICE->Violet
    ChallengeType.MELODY_BUILD,ChallengeType.RHYTHM_BUILD->Green
    ChallengeType.SINGING->Rose
    ChallengeType.CREATIVE->Amber
}
private fun masteryColor(score:Double)=when{score<.45->Rose;score<.65->Amber;score<.82->Blue;else->Green}
private fun masteryLabel(state:SkillState)=when{state.attempts==0->"нет данных";state.score<.45->"нужна опора";state.score<.65->"развивается";state.score<.82->"уверенно";else->"устойчиво"}
private fun skillName(skill:SkillTag)=when(skill){
    SkillTag.PULSE->"ровный пульс";SkillTag.RHYTHM->"ритм";SkillTag.PITCH->"высота звука";SkillTag.TONALITY->"лад и ступени";SkillTag.READING->"чтение нот";
    SkillTag.WRITING->"музыкальная запись";SkillTag.INTERVALS->"интервалы";SkillTag.KEYS->"тональности";SkillTag.MEMORY->"музыкальная память";SkillTag.CREATIVE->"музыкальное мышление"
}
private fun smartLessonForSkill(skill:SkillTag,completed:Int):Int{
    if(completed<=0)return 1
    val weeks=when(skill){
        SkillTag.PULSE->listOf(1,4,5,20);SkillTag.RHYTHM->listOf(2,3,11,12,21,22,23);SkillTag.PITCH->listOf(1,2,6,8,14);
        SkillTag.TONALITY->listOf(6,7,8,14,24,33);SkillTag.READING->listOf(9,11,14,16,17,18,31);SkillTag.WRITING->listOf(9,11,16,17,18,26,27);
        SkillTag.INTERVALS->listOf(25,26,27,28,29,30);SkillTag.KEYS->listOf(15,16,17,18,31,32,33);SkillTag.MEMORY->listOf(10,13,19,30,34);SkillTag.CREATIVE->listOf(2,6,13,19,30,34)
    }
    val maxWeek=((completed+6)/7).coerceIn(1,34)
    val week=weeks.filter{it<=maxWeek}.maxOrNull()?:1
    return (((week-1)*7)+7).coerceIn(1,completed)
}
private fun noteNameUi(midi:Int):String{
    val n=listOf("до","до♯","ре","ре♯","ми","фа","фа♯","соль","соль♯","ля","ля♯","си")
    return n[Math.floorMod(midi,12)]
}
private fun isBlackKey(midi:Int)=Math.floorMod(midi,12) in setOf(1,3,6,8,10)
private fun durationLabel(d:Double)=when{d>=1.9->"𝅗𝅥 2";d>=.9->"♩ 1";d>=.49->"♪ ½";else->"♬ ¼"}

private fun worldGradient(index:Int):List<Color> = when(index){
    0->listOf(Color(0xFF236B57),Color(0xFF45A56E),Color(0xFF7BC985))
    1->listOf(Color(0xFF3D4FC4),Color(0xFF6A5CDB),Color(0xFF57A2DB))
    2->listOf(Color(0xFF4C536A),Color(0xFF536DA8),Color(0xFF43A6D6))
    3->listOf(Color(0xFF137A91),Color(0xFF2FA8A0),Color(0xFF64C8A8))
    4->listOf(Color(0xFF8A532B),Color(0xFFC47A35),Color(0xFFE6A84C))
    5->listOf(Color(0xFF302A74),Color(0xFF6641A6),Color(0xFFAA5CC4))
    else->listOf(Color(0xFF6B304B),Color(0xFF9D466F),Color(0xFFD05C78))
}
