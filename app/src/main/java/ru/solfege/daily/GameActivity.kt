package ru.solfege.daily

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.abs
import kotlin.math.roundToInt
import java.util.Locale

class GameActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private lateinit var audio: AudioEngine
    private lateinit var pitch: PitchDetector
    private lateinit var tts: TextToSpeech
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audio = AudioEngine()
        pitch = PitchDetector()
        tts = TextToSpeech(this, this)

        setContent {
            SolfegeTheme {
                val vm: GameViewModel = viewModel(
                    factory = GameViewModelFactory(application as Application)
                )
                val state by vm.state.collectAsStateWithLifecycle()

                SolfegeApp(
                    state = state,
                    vm = vm,
                    audio = audio,
                    pitch = pitch,
                    speak = { text -> speakInstruction(text) }
                )
            }
        }
    }

    override fun onPause() {
        audio.stop()
        pitch.stop()
        if (::tts.isInitialized) tts.stop()
        super.onPause()
    }

    override fun onDestroy() {
        audio.close()
        pitch.stop()
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("ru", "RU"))
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
            tts.setSpeechRate(0.94f)
        }
    }

    private fun speakInstruction(text: String) {
        if (!ttsReady) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "solfege_instruction")
    }
}

private val Ink = Color(0xFF18202F)
private val Muted = Color(0xFF687287)
private val Indigo = Color(0xFF5267D8)
private val IndigoDark = Color(0xFF3448AE)
private val Sky = Color(0xFFEAF0FF)
private val Warm = Color(0xFFFFF4E7)
private val Good = Color(0xFF1F8B5B)
private val Error = Color(0xFFB84C49)
private val Border = Color(0xFFE2E7F0)
private val Page = Color(0xFFF6F8FC)

@Composable
private fun SolfegeTheme(content: @Composable () -> Unit) {
    val scheme = lightColorScheme(
        primary = Indigo,
        onPrimary = Color.White,
        primaryContainer = Sky,
        onPrimaryContainer = Ink,
        secondary = Color(0xFF6D5E9C),
        background = Page,
        surface = Color.White,
        onSurface = Ink,
        outline = Border,
        error = Error
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
private fun SolfegeApp(
    state: GameState,
    vm: GameViewModel,
    audio: AudioEngine,
    pitch: PitchDetector,
    speak: (String) -> Unit
) {
    BackHandler(enabled = state.screen != AppScreen.HOME) {
        audio.stop()
        pitch.stop()
        if (state.screen == AppScreen.GAME) vm.exitSession() else vm.go(AppScreen.HOME)
    }

    val showBottom = state.screen != AppScreen.GAME

    Scaffold(
        containerColor = Page,
        bottomBar = {
            if (showBottom) {
                MainBottomBar(state.screen, vm)
            }
        }
    ) { inner ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            AnimatedContent(targetState = state.screen, label = "screen") { screen ->
                when (screen) {
                    AppScreen.HOME -> HomeScreen(state, vm)
                    AppScreen.MAP -> MapScreen(state, vm)
                    AppScreen.GAME -> GameScreen(state, vm, audio, pitch, speak)
                    AppScreen.PROGRESS -> ProgressScreen(state, vm)
                    AppScreen.LAB -> LabScreen(audio, pitch, speak)
                    AppScreen.ADULT -> AdultScreen(state, vm)
                }
            }
        }
    }
}

@Composable
private fun MainBottomBar(screen: AppScreen, vm: GameViewModel) {
    val items = listOf(
        AppScreen.HOME to "Сегодня",
        AppScreen.MAP to "Карта",
        AppScreen.PROGRESS to "Навыки",
        AppScreen.LAB to "Лаборатория",
        AppScreen.ADULT to "Взрослым"
    )
    NavigationBar(containerColor = Color.White) {
        items.forEach { (target, label) ->
            NavigationBarItem(
                selected = screen == target,
                onClick = { vm.go(target) },
                icon = {
                    Text(
                        text = when (target) {
                            AppScreen.HOME -> "♪"
                            AppScreen.MAP -> "◉"
                            AppScreen.PROGRESS -> "▥"
                            AppScreen.LAB -> "⌁"
                            else -> "i"
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                label = { Text(label, fontSize = 11.sp) }
            )
        }
    }
}

@Composable
private fun CenteredPage(content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val horizontal = if (maxWidth > 900.dp) 40.dp else 16.dp
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontal, vertical = 18.dp)
                .widthIn(max = 920.dp)
                .align(Alignment.TopCenter),
            content = content
        )
    }
}

@Composable
private fun HomeScreen(state: GameState, vm: GameViewModel) {
    val next = (state.completedMission + 1).coerceAtMost(Course.TOTAL_LESSONS)
    val meta = ChallengeEngine.missionMeta(next)
    val courseProgress by animateFloatAsState(
        targetValue = state.completedMission.toFloat() / Course.TOTAL_LESSONS.toFloat(),
        label = "course_progress"
    )

    CenteredPage {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Музыкальная экспедиция", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Ink)
                Text(
                    "Сольфеджио как игра, слуховая практика и музыкальное мышление",
                    color = Muted,
                    fontSize = 14.sp
                )
            }
            StarPill(state.totalStars)
        }

        Spacer(Modifier.height(18.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF5368D9), Color(0xFF7886E8), Color(0xFF9E7DD7))
                        )
                    )
                    .padding(22.dp)
            ) {
                Column {
                    Text(
                        "МИР " + meta.world + " • " + meta.worldTitle.uppercase(),
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Миссия " + meta.id + " из " + Course.TOTAL_LESSONS,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(meta.title, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
                    Text(meta.subtitle, color = Color.White.copy(alpha = 0.9f), fontSize = 15.sp)

                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { courseProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.24f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        state.completedMission.toString() + " пройдено • серия " + state.streak + " дн.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { vm.startNextMission() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = IndigoDark
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (state.completedMission == 0) "Начать первую миссию" else "Продолжить",
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(vertical = 5.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        val weak = state.skillStats.filter { it.attempts > 0 }.minByOrNull { it.mastery }
        if (weak != null) {
            SectionCard {
                Text("Умная тренировка", fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text(
                    "Сейчас полезнее всего укрепить: " + weak.domain.title.lowercase() + ".",
                    color = Muted,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { vm.startSmartReview() },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Короткое повторение без потери прогресса")
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        Text("Как устроена миссия", fontSize = 20.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            contentPadding = PaddingValues(end = 8.dp)
        ) {
            items(
                listOf(
                    "Слух" to "Сравнить и различить",
                    "Ритм" to "Услышать и воспроизвести",
                    "Лад" to "Почувствовать функцию",
                    "Ноты" to "Прочитать",
                    "Диктант" to "Записать",
                    "Творчество" to "Применить самому"
                )
            ) { item ->
                MiniSkillCard(item.first, item.second)
            }
        }

        Spacer(Modifier.height(18.dp))

        Text("Принцип курса", fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text(
            "Сначала звук и действие. Потом название и знак. Проверка появляется только после законченного ответа — приложение не выдаёт решение по ходу задания.",
            color = Muted,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun MapScreen(state: GameState, vm: GameViewModel) {
    val worlds = (1..8).toList()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 18.dp, 16.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Карта экспедиции", fontSize = 29.sp, fontWeight = FontWeight.Black)
            Text(
                "Следующая миссия открывается сразу после завершения предыдущей. Слабые навыки возвращаются в будущих заданиях, но не блокируют ребёнка.",
                color = Muted,
                fontSize = 14.sp
            )
        }

        items(worlds) { world ->
            val missions = (1..Course.TOTAL_LESSONS).filter {
                ChallengeEngine.missionMeta(it).world == world
            }
            val firstMeta = ChallengeEngine.missionMeta(missions.first())
            SectionCard {
                Text(
                    "Мир " + world + " • " + firstMeta.worldTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(10.dp))
                val rows = missions.chunked(7)
                rows.forEach { row ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        row.forEach { id ->
                            MissionNode(
                                id = id,
                                completed = id <= state.completedMission,
                                unlocked = id <= state.completedMission + 1 || state.teacherMode,
                                stars = if (id <= state.completedMission) {
                                    // State exposes total stars only; exact stars are intentionally omitted from this screen.
                                    1
                                } else 0,
                                onClick = { vm.startMission(id) }
                            )
                        }
                        repeat(7 - row.size) { Spacer(Modifier.size(42.dp)) }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun MissionNode(
    id: Int,
    completed: Boolean,
    unlocked: Boolean,
    stars: Int,
    onClick: () -> Unit
) {
    val bg = when {
        completed -> Good
        unlocked -> Indigo
        else -> Border
    }
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(enabled = unlocked, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (completed) "✓" else if (unlocked) id.toString() else "•",
            color = if (unlocked || completed) Color.White else Muted,
            fontWeight = FontWeight.Bold,
            fontSize = if (id >= 100 && unlocked && !completed) 10.sp else 13.sp
        )
    }
}

@Composable
private fun GameScreen(
    state: GameState,
    vm: GameViewModel,
    audio: AudioEngine,
    pitch: PitchDetector,
    speak: (String) -> Unit
) {
    if (state.phase == AnswerPhase.SESSION_COMPLETE) {
        SessionCompleteScreen(state, vm)
        return
    }

    val challenge = state.currentChallenge ?: return
    val progress = (state.challengeIndex + 1).toFloat() / state.challenges.size.toFloat()

    Column(
        Modifier
            .fillMaxSize()
            .background(Page)
            .statusBarsPadding()
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    audio.stop()
                    pitch.stop()
                    vm.exitSession()
                }) {
                    Text("← Домой")
                }
                Spacer(Modifier.weight(1f))
                Text(
                    (state.challengeIndex + 1).toString() + " / " + state.challenges.size,
                    color = Muted,
                    fontWeight = FontWeight.Bold
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(CircleShape),
                color = Indigo,
                trackColor = Border
            )
        }

        BoxWithConstraints(Modifier.weight(1f)) {
            val maxCard = if (maxWidth > 900.dp) 820.dp else maxWidth
            Column(
                Modifier
                    .widthIn(max = maxCard)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .align(Alignment.TopCenter)
            ) {
                ChallengeHeader(challenge, state, vm, audio, speak)
                Spacer(Modifier.height(12.dp))

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        when (challenge.type) {
                            GameType.AURAL_CHOICE,
                            GameType.RHYTHM_CHOICE,
                            GameType.TONAL_CHOICE,
                            GameType.NOTATION_CHOICE ->
                                ChoiceChallenge(challenge, state, vm, audio)

                            GameType.MELODY_BUILD ->
                                MelodyBuildChallenge(challenge, state, vm, audio, showStaff = false)

                            GameType.STAFF_BUILD ->
                                MelodyBuildChallenge(challenge, state, vm, audio, showStaff = true)

                            GameType.MEASURE_BUILD ->
                                MeasureBuildChallenge(challenge, state, vm, audio)

                            GameType.RHYTHM_PRODUCTION ->
                                RhythmProductionChallenge(challenge, state, vm, audio)

                            GameType.PITCH_PRODUCTION ->
                                PitchProductionChallenge(challenge, state, vm, audio, pitch)

                            GameType.CREATIVE_TRANSFER ->
                                CreativeChallenge(challenge, state, vm, audio)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                HintArea(challenge, state, vm)
                Spacer(Modifier.height(110.dp))
            }
        }

        if (state.phase == AnswerPhase.FEEDBACK) {
            FeedbackPanel(state, vm, audio, pitch)
        }
    }
}

@Composable
private fun ChallengeHeader(
    challenge: Challenge,
    state: GameState,
    vm: GameViewModel,
    audio: AudioEngine,
    speak: (String) -> Unit
) {
    val meta = ChallengeEngine.missionMeta(state.missionId)
    Text(
        (if (state.reviewMode) "УМНАЯ ТРЕНИРОВКА" else "МИССИЯ " + state.missionId) +
                " • " + meta.worldTitle.uppercase(),
        fontSize = 11.sp,
        color = Indigo,
        fontWeight = FontWeight.Black
    )
    Spacer(Modifier.height(4.dp))
    Text(challenge.title, fontSize = 26.sp, fontWeight = FontWeight.Black, color = Ink)
    Spacer(Modifier.height(6.dp))
    Text(
        challenge.instruction,
        fontSize = 17.sp,
        lineHeight = 23.sp,
        color = Ink,
        fontWeight = FontWeight.Medium
    )
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { speak(challenge.instruction) },
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("🔊 Прочитать")
        }

        if (challenge.audioMidi.isNotEmpty() || challenge.audioRhythm.isNotEmpty()) {
            val canReplay = state.replayCount < challenge.maxReplays
            OutlinedButton(
                enabled = canReplay,
                onClick = {
                    vm.replayUsed()
                    playChallengeAudio(challenge, audio)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (canReplay)
                        "▶ Слушать " + (state.replayCount + 1) + "/" + challenge.maxReplays
                    else
                        "Прослушивания закончились"
                )
            }
        }
    }
}

@Composable
private fun ChoiceChallenge(
    challenge: Challenge,
    state: GameState,
    vm: GameViewModel,
    audio: AudioEngine
) {
    if (challenge.audioMidi.isNotEmpty() || challenge.audioRhythm.isNotEmpty()) {
        Button(
            onClick = {
                if (state.replayCount < challenge.maxReplays) {
                    vm.replayUsed()
                    playChallengeAudio(challenge, audio)
                }
            },
            enabled = state.replayCount < challenge.maxReplays,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(15.dp)
        ) {
            Text("▶ Послушать задание", modifier = Modifier.padding(vertical = 4.dp))
        }
        Spacer(Modifier.height(12.dp))
    }

    challenge.options.forEach { option ->
        val selected = state.selectedOptionId == option.id
        val bg by animateColorAsState(
            targetValue = if (selected) Sky else Color.White,
            label = "option"
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(enabled = state.phase == AnswerPhase.ANSWERING) {
                    vm.selectOption(option.id)
                },
            color = bg,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                if (selected) 2.dp else 1.dp,
                if (selected) Indigo else Border
            )
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .border(2.dp, if (selected) Indigo else Border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Box(
                            Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Indigo)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    option.label,
                    fontSize = if (challenge.type == GameType.RHYTHM_CHOICE) 24.sp else 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    Spacer(Modifier.height(10.dp))
    Button(
        onClick = { vm.submitCurrent() },
        enabled = state.selectedOptionId != null && state.phase == AnswerPhase.ANSWERING,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp)
    ) {
        Text("Проверить ответ", modifier = Modifier.padding(vertical = 5.dp))
    }
}

@Composable
private fun MelodyBuildChallenge(
    challenge: Challenge,
    state: GameState,
    vm: GameViewModel,
    audio: AudioEngine,
    showStaff: Boolean
) {
    Button(
        onClick = {
            if (state.replayCount < challenge.maxReplays) {
                vm.replayUsed()
                playChallengeAudio(challenge, audio)
            }
        },
        enabled = state.replayCount < challenge.maxReplays,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(15.dp)
    ) {
        Text("▶ Послушать фрагмент")
    }

    Spacer(Modifier.height(12.dp))

    if (showStaff) {
        StaffCanvas(
            midi = state.melodyInput,
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(Color(0xFFFAFBFE), RoundedCornerShape(16.dp))
        )
    } else {
        SelectedMelodyRow(state.melodyInput, challenge.targetMidi.size)
    }

    Spacer(Modifier.height(10.dp))
    Text(
        "Введено " + state.melodyInput.size + " из " + challenge.targetMidi.size +
                ". Проверка будет только после полного ответа.",
        color = Muted,
        fontSize = 13.sp
    )
    Spacer(Modifier.height(8.dp))

    PianoKeyboard(
        onNote = { midi ->
            if (state.melodyInput.size < challenge.targetMidi.size) {
                audio.playHomeNote(midi, null)
                vm.addMidi(midi)
            }
        }
    )

    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { vm.undoMidi() }, enabled = state.melodyInput.isNotEmpty()) {
            Text("← Убрать")
        }
        OutlinedButton(onClick = { vm.clearMidi() }, enabled = state.melodyInput.isNotEmpty()) {
            Text("Очистить")
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = { vm.submitCurrent() },
            enabled = state.melodyInput.size == challenge.targetMidi.size
        ) {
            Text("Проверить")
        }
    }
}

@Composable
private fun MeasureBuildChallenge(
    challenge: Challenge,
    state: GameState,
    vm: GameViewModel,
    audio: AudioEngine
) {
    Button(
        onClick = {
            if (state.replayCount < challenge.maxReplays) {
                vm.replayUsed()
                playChallengeAudio(challenge, audio)
            }
        },
        enabled = state.replayCount < challenge.maxReplays,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("▶ Послушать ритм")
    }

    Spacer(Modifier.height(12.dp))

    Surface(
        color = Color(0xFFFAFBFE),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "Твой такт",
                fontWeight = FontWeight.Bold,
                color = Muted,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(8.dp))
            if (state.measureInput.isEmpty()) {
                Text("Пока пусто", color = Muted)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(state.measureInput) { duration ->
                        DurationTile(duration, selected = true)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Сумма: " + formatBeat(state.measureInput.sum()) +
                        " / " + formatBeat(challenge.targetRhythmBeats) + " долей",
                fontWeight = FontWeight.Bold
            )
        }
    }

    Spacer(Modifier.height(12.dp))
    Text("Длительности", fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(challenge.allowedDurations) { duration ->
            Surface(
                color = Sky,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.clickable {
                    vm.addDuration(duration)
                }
            ) {
                Text(
                    durationSymbol(duration),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                )
            }
        }
    }

    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { vm.undoDuration() }, enabled = state.measureInput.isNotEmpty()) {
            Text("← Убрать")
        }
        OutlinedButton(onClick = { vm.clearDuration() }, enabled = state.measureInput.isNotEmpty()) {
            Text("Очистить")
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = { vm.submitCurrent() },
            enabled = state.measureInput.isNotEmpty()
        ) {
            Text("Проверить")
        }
    }

    if (state.phase == AnswerPhase.ANSWERING && state.feedbackTitle.isNotBlank()) {
        Spacer(Modifier.height(8.dp))
        Text(state.feedbackText, color = Error, fontSize = 13.sp)
    }
}

@Composable
private fun RhythmProductionChallenge(
    challenge: Challenge,
    state: GameState,
    vm: GameViewModel,
    audio: AudioEngine
) {
    val taps = remember(challenge.id) { mutableStateListOf<Long>() }
    var started by remember(challenge.id) { mutableStateOf(false) }
    var score by remember(challenge.id) { mutableDoubleStateOf(0.0) }

    Button(
        onClick = {
            if (state.replayCount < challenge.maxReplays) {
                vm.replayUsed()
                playChallengeAudio(challenge, audio)
            }
        },
        enabled = state.replayCount < challenge.maxReplays,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("▶ Послушать ритм")
    }

    Spacer(Modifier.height(12.dp))
    Text(
        if (!started)
            "Когда будешь готов, нажми «Начать ответ». Затем отбей " + challenge.audioRhythm.size + " звуков."
        else
            "Касаний: " + taps.size + " из " + challenge.audioRhythm.size,
        color = Muted
    )
    Spacer(Modifier.height(8.dp))

    if (!started) {
        OutlinedButton(
            onClick = {
                taps.clear()
                started = true
                score = 0.0
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Начать ответ")
        }
    } else {
        Button(
            onClick = {
                if (taps.size < challenge.audioRhythm.size) taps += System.currentTimeMillis()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp),
            enabled = taps.size < challenge.audioRhythm.size
        ) {
            Text("ХЛОП", fontSize = 26.sp, fontWeight = FontWeight.Black)
        }
    }

    if (taps.size == challenge.audioRhythm.size && taps.size >= 2) {
        LaunchedEffect(taps.size) {
            score = rhythmScore(taps, challenge.audioRhythm)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Ответ готов. Теперь можно проверить — до этого приложение не показывает, насколько он точен.",
            color = Muted,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                vm.completeProduction(score)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Проверить мой ритм")
        }
        OutlinedButton(
            onClick = {
                taps.clear()
                started = false
                score = 0.0
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Записать заново")
        }
    }
}

@Composable
private fun PitchProductionChallenge(
    challenge: Challenge,
    state: GameState,
    vm: GameViewModel,
    audio: AudioEngine,
    pitch: PitchDetector
) {
    val context = LocalContext.current
    var pitchText by remember(challenge.id) { mutableStateOf("Микрофон необязателен.") }
    var level by remember(challenge.id) { mutableFloatStateOf(0f) }
    var stableMatch by remember(challenge.id) { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startPitchCheck(
                pitch = pitch,
                targetMidi = challenge.targetMidi.firstOrNull() ?: 60,
                onLevel = { level = it.toFloat() },
                onText = { pitchText = it },
                onMatch = { stableMatch = true }
            )
        } else {
            pitchText = "Микрофон не разрешён. Урок всё равно можно пройти вручную."
        }
    }

    DisposableEffect(challenge.id) {
        onDispose { pitch.stop() }
    }

    Button(
        onClick = {
            if (state.replayCount < challenge.maxReplays) {
                vm.replayUsed()
                playChallengeAudio(challenge, audio)
            }
        },
        enabled = state.replayCount < challenge.maxReplays,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("▶ Послушать, затем повторить голосом")
    }

    Spacer(Modifier.height(12.dp))
    Text(
        "Пой на удобной октаве. Важнее направление и устойчивость звука, чем идеальная точность прибора.",
        color = Muted,
        fontSize = 14.sp
    )
    Spacer(Modifier.height(10.dp))

    LinearProgressIndicator(
        progress = { level.coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth(),
        color = if (stableMatch) Good else Indigo,
        trackColor = Border
    )
    Spacer(Modifier.height(6.dp))
    Text(pitchText, color = if (stableMatch) Good else Muted, fontSize = 13.sp)

    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    startPitchCheck(
                        pitch = pitch,
                        targetMidi = challenge.targetMidi.firstOrNull() ?: 60,
                        onLevel = { level = it.toFloat() },
                        onText = { pitchText = it },
                        onMatch = { stableMatch = true }
                    )
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        ) {
            Text("🎤 Проверить первый звук")
        }
        OutlinedButton(onClick = {
            pitch.stop()
            pitchText = "Проверка остановлена."
        }) {
            Text("Стоп")
        }
    }

    Spacer(Modifier.height(12.dp))
    Button(
        onClick = {
            pitch.stop()
            vm.completeProduction(if (stableMatch) 1.0 else 0.72)
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(if (stableMatch) "Готово — продолжить" else "Я спел и сравнил")
    }
}

@Composable
private fun CreativeChallenge(
    challenge: Challenge,
    state: GameState,
    vm: GameViewModel,
    audio: AudioEngine
) {
    Text(
        "Это перенос навыка, а не тест с одним правильным ответом. Условие должно быть выполнено, но музыкальная версия может быть твоей.",
        color = Muted,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
    Spacer(Modifier.height(12.dp))

    if (challenge.audioMidi.isNotEmpty()) {
        OutlinedButton(
            onClick = {
                audio.playMidi(challenge.audioMidi.toIntArray(), null)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("▶ Дать стартовый звук")
        }
        Spacer(Modifier.height(8.dp))
    }

    var selfCheck by remember(challenge.id) { mutableStateOf<String?>(null) }
    Text("Самопроверка", fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
    listOf(
        "Я могу повторить свой вариант второй раз",
        "Условие выполнено, но повторить трудно",
        "Хочу попробовать ещё раз"
    ).forEach { label ->
        FilterChip(
            selected = selfCheck == label,
            onClick = { selfCheck = label },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth()
        )
    }

    Spacer(Modifier.height(10.dp))
    Button(
        onClick = { vm.completeProduction(if (selfCheck?.startsWith("Я могу") == true) 0.9 else 0.75) },
        enabled = selfCheck != null && selfCheck != "Хочу попробовать ещё раз",
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Завершить творческую задачу")
    }
}

@Composable
private fun HintArea(challenge: Challenge, state: GameState, vm: GameViewModel) {
    var showHint by remember(challenge.id) { mutableStateOf(false) }

    if (!showHint) {
        TextButton(onClick = {
            showHint = true
            vm.useHint()
        }) {
            Text("Нужна подсказка?")
        }
    } else {
        Surface(
            color = Warm,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("Подсказка", fontWeight = FontWeight.Black)
                Text(challenge.hint, color = Ink, fontSize = 14.sp)
                if (challenge.difficulty == 2) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Сложный режим: подсказка немного уменьшает итоговую звезду, но не мешает проходить дальше.",
                        color = Muted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedbackPanel(
    state: GameState,
    vm: GameViewModel,
    audio: AudioEngine,
    pitch: PitchDetector
) {
    val correct = state.feedbackCorrect == true
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        color = if (correct) Color(0xFFECF8F2) else Color(0xFFFFF0EE),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                state.feedbackTitle,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = if (correct) Good else Error
            )
            Text(
                state.feedbackText,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Ink
            )
            Spacer(Modifier.height(10.dp))
            if (state.canRetry) {
                Button(
                    onClick = {
                        audio.stop()
                        pitch.stop()
                        vm.retryCurrent()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Попробовать ещё раз")
                }
            } else {
                Button(
                    onClick = {
                        audio.stop()
                        pitch.stop()
                        vm.nextChallenge()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Следующее задание")
                }
            }
        }
    }
}

@Composable
private fun SessionCompleteScreen(state: GameState, vm: GameViewModel) {
    val summary = state.summary ?: return
    val nextId = (state.missionId + 1).coerceAtMost(Course.TOTAL_LESSONS)
    val nextUnlocked = state.reviewMode || state.teacherMode || nextId <= state.completedMission + 1

    CenteredPage {
        Spacer(Modifier.height(20.dp))
        Text("Миссия завершена", fontSize = 31.sp, fontWeight = FontWeight.Black)
        Text(
            if (state.reviewMode) "Тренировка обновила карту навыков."
            else "Следующая миссия уже открыта.",
            color = Muted,
            fontSize = 15.sp
        )

        Spacer(Modifier.height(18.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Sky),
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "★".repeat(summary.stars) + "☆".repeat(3 - summary.stars),
                    fontSize = 42.sp,
                    color = Color(0xFFE7A927),
                    letterSpacing = 4.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Освоение миссии: " + (summary.totalScore * 100).roundToInt() + "%",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "С первого раза без подсказки: " + summary.correctFirstTry +
                            " из " + summary.scoredChallenges,
                    color = Muted
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        SectionCard {
            Text("Что изменилось в навыках", fontSize = 19.sp, fontWeight = FontWeight.Black)
            summary.skillScores.entries.sortedBy { it.key.ordinal }.forEach { entry ->
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.key.title, modifier = Modifier.weight(1f), fontSize = 14.sp)
                    Text(
                        (entry.value * 100).roundToInt().toString() + "%",
                        fontWeight = FontWeight.Bold,
                        color = Indigo
                    )
                }
                LinearProgressIndicator(
                    progress = { entry.value.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                    color = Indigo,
                    trackColor = Border
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        if (!state.reviewMode && state.missionId < Course.TOTAL_LESSONS) {
            Button(
                onClick = { vm.startMission(nextId) },
                enabled = nextUnlocked,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Открыть следующую миссию", modifier = Modifier.padding(vertical = 5.dp))
            }
            Spacer(Modifier.height(8.dp))
        }

        OutlinedButton(
            onClick = { vm.go(AppScreen.HOME) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Вернуться на главную")
        }
    }
}

@Composable
private fun ProgressScreen(state: GameState, vm: GameViewModel) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 18.dp, 16.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Карта навыков", fontSize = 29.sp, fontWeight = FontWeight.Black)
            Text(
                "Это адаптивные показатели для выбора заданий, а не школьные отметки.",
                color = Muted,
                fontSize = 14.sp
            )
        }

        items(state.skillStats) { stat ->
            SkillProgressCard(stat)
        }

        item {
            val weak = state.skillStats.filter { it.attempts > 0 }.minByOrNull { it.mastery }
            if (weak != null) {
                SectionCard {
                    Text("Следующий полезный шаг", fontSize = 19.sp, fontWeight = FontWeight.Black)
                    Text(
                        "Подтянуть: " + weak.domain.title.lowercase() + ". Приложение подберёт материал из уже открытой части курса.",
                        color = Muted,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { vm.startSmartReview() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Умная тренировка")
                    }
                }
            }
        }
    }
}

@Composable
private fun SkillProgressCard(stat: SkillStat) {
    val mastery = stat.mastery.toFloat()
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stat.domain.title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (stat.attempts == 0)
                        "Ещё нет данных"
                    else
                        "Попыток: " + stat.attempts + " • " + masteryLabel(stat.mastery),
                    color = Muted,
                    fontSize = 13.sp
                )
            }
            Text(
                if (stat.attempts == 0) "—" else (stat.mastery * 100).roundToInt().toString() + "%",
                fontWeight = FontWeight.Black,
                color = Indigo
            )
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { if (stat.attempts == 0) 0f else mastery },
            modifier = Modifier.fillMaxWidth(),
            color = Indigo,
            trackColor = Border
        )
    }
}

@Composable
private fun LabScreen(
    audio: AudioEngine,
    pitch: PitchDetector,
    speak: (String) -> Unit
) {
    val context = LocalContext.current
    var labPitch by remember { mutableStateOf("Выбери звук и попробуй спеть.") }
    var level by remember { mutableFloatStateOf(0f) }
    var target by remember { mutableIntStateOf(60) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startPitchCheck(
                pitch,
                target,
                { level = it.toFloat() },
                { labPitch = it },
                { labPitch = "Совпало. Попробуй повторить ещё раз без подсказки." }
            )
        } else {
            labPitch = "Микрофон не разрешён."
        }
    }

    DisposableEffect(Unit) {
        onDispose { pitch.stop() }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 18.dp, 16.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Музыкальная лаборатория", fontSize = 29.sp, fontWeight = FontWeight.Black)
            Text(
                "Свободные тренажёры без штрафов и без влияния на порядок курса.",
                color = Muted,
                fontSize = 14.sp
            )
        }

        item {
            SectionCard {
                Text("Пианино", fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text("Сравнивай звуки, ищи мелодии и интервалы.", color = Muted)
                Spacer(Modifier.height(10.dp))
                PianoKeyboard(onNote = { midi ->
                    target = midi
                    audio.playHomeNote(midi, null)
                })
            }
        }

        item {
            SectionCard {
                Text("Интервальный телескоп", fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text("Сначала сравни расстояния, только потом называй их.", color = Muted)
                Spacer(Modifier.height(10.dp))
                val intervals = listOf(
                    "секунда" to 2,
                    "терция" to 4,
                    "кварта" to 5,
                    "квинта" to 7,
                    "октава" to 12
                )
                intervals.forEach { pair ->
                    OutlinedButton(
                        onClick = { audio.playMidi(intArrayOf(60, 60 + pair.second), null) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("▶ " + pair.first)
                    }
                }
            }
        }

        item {
            SectionCard {
                Text("Зеркало высоты", fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text(labPitch, color = Muted)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { level },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = Border
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { audio.playHomeNote(target, null) }) {
                        Text("▶ Эталон")
                    }
                    Button(onClick = {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            startPitchCheck(
                                pitch,
                                target,
                                { level = it.toFloat() },
                                { labPitch = it },
                                { labPitch = "Совпало. Теперь попробуй удержать звук спокойно." }
                            )
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }) {
                        Text("🎤 Слушать голос")
                    }
                    TextButton(onClick = { pitch.stop() }) {
                        Text("Стоп")
                    }
                }
            }
        }
    }
}

@Composable
private fun AdultScreen(state: GameState, vm: GameViewModel) {
    var resetDialog by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 18.dp, 16.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Для взрослого", fontSize = 29.sp, fontWeight = FontWeight.Black)
            Text(
                "Что именно тренирует приложение и как оно принимает решения.",
                color = Muted,
                fontSize = 14.sp
            )
        }

        item {
            SectionCard {
                Text("Главное изменение", fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text(
                    "Ребёнок больше не «проходит экран». Каждая миссия содержит около десяти разных испытаний. "
                            + "Выбор не оценивается до кнопки «Проверить», у заданий есть правдоподобные неверные варианты, "
                            + "а конструктивные ответы проверяются только после завершения всей последовательности.",
                    color = Ink,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }

        item {
            SectionCard {
                Text("Освоение вместо жизней", fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text(
                    "Ошибки не закрывают следующий день. После честного завершения миссии следующая открывается сразу. "
                            + "Но слабый навык получает более простые подсказки и возвращается через умную тренировку и интервальное повторение. "
                            + "Так ребёнок не застревает, а трудный материал не исчезает.",
                    color = Ink,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }

        item {
            SectionCard {
                Text("Педагогическая компиляция", fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text(
                    "Kodály: пение и функция лада. Orff: действие, ритм, импровизация. Dalcroze: метр через движение. "
                            + "Gordon: аудиация и звук до символа. Conversational Solfege: музыкальная речь раньше чтения. "
                            + "Из цифровых решений заимствованы не внешний вид, а удачные механики: короткие самостоятельные задания, "
                            + "адаптивное усложнение, дополнительный вопрос после ошибки, разные способы ответа, рабочая карта навыков и ежедневная смешанная тренировка.",
                    color = Ink,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }

        item {
            SectionCard {
                Text("Режим преподавателя", fontSize = 20.sp, fontWeight = FontWeight.Black)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Разрешить просмотр любой миссии",
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp
                    )
                    Switch(
                        checked = state.teacherMode,
                        onCheckedChange = { vm.setTeacherMode(it) }
                    )
                }
                Text(
                    "В режиме преподавателя можно открывать закрытые миссии. Прогресс ребёнка при таком просмотре не должен использоваться как результат обучения.",
                    color = Muted,
                    fontSize = 12.sp
                )
            }
        }

        item {
            SectionCard {
                Text("Приватность", fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text(
                    "Основной курс работает офлайн. Микрофон включается только по отдельной кнопке в вокальной задаче, "
                            + "используется локально и не сохраняет запись.",
                    color = Ink,
                    fontSize = 14.sp
                )
            }
        }

        item {
            OutlinedButton(
                onClick = { resetDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сбросить весь прогресс")
            }
        }
    }

    if (resetDialog) {
        AlertDialog(
            onDismissRequest = { resetDialog = false },
            title = { Text("Сбросить прогресс?") },
            text = { Text("Будут удалены открытые миссии, звёзды и адаптивная карта навыков.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.resetProgress()
                    resetDialog = false
                }) {
                    Text("Сбросить", color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { resetDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun MiniSkillCard(title: String, subtitle: String) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
        modifier = Modifier.width(150.dp)
    ) {
        Column(Modifier.padding(13.dp)) {
            Text(title, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text(subtitle, color = Muted, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun StarPill(stars: Int) {
    Surface(color = Warm, shape = CircleShape) {
        Text(
            "★ " + stars,
            color = Color(0xFF9A6C00),
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun SelectedMelodyRow(midi: List<Int>, targetSize: Int) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        itemsIndexed((0 until targetSize).toList()) { index, _ ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (index < midi.size) Sky else Color(0xFFF3F5F9),
                border = androidx.compose.foundation.BorderStroke(1.dp, Border)
            ) {
                Text(
                    if (index < midi.size) noteName(midi[index]) else "•",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    fontWeight = FontWeight.Bold,
                    color = if (index < midi.size) Ink else Muted
                )
            }
        }
    }
}

@Composable
private fun PianoKeyboard(onNote: (Int) -> Unit) {
    val scroll = rememberScrollState()
    Box(
        Modifier
            .fillMaxWidth()
            .height(178.dp)
            .horizontalScroll(scroll)
    ) {
        val width = 780.dp
        Canvas(
            Modifier
                .requiredWidth(width)
                .height(170.dp)
                .pointerInput(Unit) {
                    detectTapGestures { point ->
                        val totalWidth = size.width
                        val whiteW = totalWidth / 15f
                        val blackH = size.height * 0.60f
                        val blackPositions = intArrayOf(0, 1, 3, 4, 5, 7, 8, 10, 11, 12)
                        val blackMidi = intArrayOf(61, 63, 66, 68, 70, 73, 75, 78, 80, 82)
                        val blackW = whiteW * 0.58f

                        if (point.y <= blackH) {
                            for (i in blackPositions.indices) {
                                val cx = (blackPositions[i] + 1) * whiteW
                                if (point.x >= cx - blackW / 2f && point.x <= cx + blackW / 2f) {
                                    onNote(blackMidi[i])
                                    return@detectTapGestures
                                }
                            }
                        }
                        val whiteMidi = intArrayOf(60, 62, 64, 65, 67, 69, 71, 72, 74, 76, 77, 79, 81, 83, 84)
                        val index = (point.x / whiteW).toInt().coerceIn(0, whiteMidi.lastIndex)
                        onNote(whiteMidi[index])
                    }
                }
        ) {
            drawPiano()
        }
    }
}

private fun DrawScope.drawPiano() {
    val whiteMidi = intArrayOf(60, 62, 64, 65, 67, 69, 71, 72, 74, 76, 77, 79, 81, 83, 84)
    val whiteW = size.width / whiteMidi.size.toFloat()

    for (i in whiteMidi.indices) {
        drawRect(
            color = Color.White,
            topLeft = Offset(i * whiteW, 0f),
            size = androidx.compose.ui.geometry.Size(whiteW, size.height)
        )
        drawRect(
            color = Color(0xFF8C93A4),
            topLeft = Offset(i * whiteW, 0f),
            size = androidx.compose.ui.geometry.Size(whiteW, size.height),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.3f)
        )
    }

    val blackPositions = intArrayOf(0, 1, 3, 4, 5, 7, 8, 10, 11, 12)
    val blackW = whiteW * 0.58f
    val blackH = size.height * 0.60f
    blackPositions.forEach { position ->
        val cx = (position + 1) * whiteW
        drawRoundRect(
            color = Color(0xFF242A35),
            topLeft = Offset(cx - blackW / 2f, 0f),
            size = androidx.compose.ui.geometry.Size(blackW, blackH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f)
        )
    }
}

@Composable
private fun StaffCanvas(midi: List<Int>, modifier: Modifier = Modifier) {
    Canvas(modifier.padding(12.dp)) {
        val left = 10f
        val right = size.width - 10f
        val top = size.height * 0.25f
        val gap = size.height * 0.10f

        for (i in 0 until 5) {
            val y = top + i * gap
            drawLine(Color(0xFF5C6472), Offset(left, y), Offset(right, y), strokeWidth = 2f)
        }

        if (midi.isEmpty()) return@Canvas
        val dx = (right - left) / (midi.size + 1).toFloat()
        midi.forEachIndexed { index, note ->
            val step = diatonicStaffStep(note)
            val y = top + 4 * gap - (step - 2) * gap / 2f
            val x = left + dx * (index + 1)

            if (step <= 0) {
                var s = 0
                while (s >= step) {
                    val ly = top + 4 * gap - (s - 2) * gap / 2f
                    drawLine(Color(0xFF5C6472), Offset(x - 16f, ly), Offset(x + 16f, ly), strokeWidth = 2f)
                    s -= 2
                }
            }
            if (step >= 12) {
                var s = 12
                while (s <= step) {
                    val ly = top + 4 * gap - (s - 2) * gap / 2f
                    drawLine(Color(0xFF5C6472), Offset(x - 16f, ly), Offset(x + 16f, ly), strokeWidth = 2f)
                    s += 2
                }
            }

            drawOval(
                color = Indigo,
                topLeft = Offset(x - 11f, y - 7f),
                size = androidx.compose.ui.geometry.Size(22f, 14f)
            )
            drawLine(Indigo, Offset(x + 10f, y), Offset(x + 10f, y - 42f), strokeWidth = 3f)
        }
    }
}

@Composable
private fun DurationTile(duration: Double, selected: Boolean) {
    Surface(
        color = if (selected) Sky else Color.White,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Text(
            durationSymbol(duration),
            fontSize = 25.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

private fun playChallengeAudio(challenge: Challenge, audio: AudioEngine) {
    when {
        challenge.audioMidi.isNotEmpty() ->
            audio.playMidi(challenge.audioMidi.toIntArray(), null)
        challenge.audioRhythm.isNotEmpty() ->
            audio.playRhythm(challenge.audioRhythm.toDoubleArray(), 0, null)
    }
}

private fun startPitchCheck(
    pitch: PitchDetector,
    targetMidi: Int,
    onLevel: (Double) -> Unit,
    onText: (String) -> Unit,
    onMatch: () -> Unit
) {
    pitch.start(targetMidi, object : PitchDetector.Listener {
        override fun onLevel(rms: Double) {
            onLevel(rms.coerceIn(0.0, 1.0))
        }

        override fun onPitch(hz: Double, midi: Double, centsFromTarget: Double, stableMatch: Boolean) {
            val direction = when {
                abs(centsFromTarget) < 18 -> "очень близко"
                centsFromTarget > 0 -> "чуть выше"
                else -> "чуть ниже"
            }
            onText("Слышу " + PitchDetector.noteName(midi) + " • " + direction)
            if (stableMatch) onMatch()
        }

        override fun onError(message: String) {
            onText(message)
        }
    })
}

private fun rhythmScore(taps: List<Long>, expected: List<Double>): Double {
    if (taps.size < 2 || expected.size < 2) return 0.55
    val count = minOf(taps.size - 1, expected.size - 1)
    val actual = DoubleArray(count) { i -> (taps[i + 1] - taps[i]).toDouble() }
    val actualSum = actual.sum()
    val expectedSum = expected.take(count).sum()
    if (actualSum <= 0.0 || expectedSum <= 0.0) return 0.35

    val scale = actualSum / expectedSum
    var error = 0.0
    for (i in 0 until count) {
        val target = expected[i] * scale
        error += abs(actual[i] - target) / target.coerceAtLeast(1.0)
    }
    val meanError = error / count.toDouble()
    return when {
        meanError < 0.16 -> 1.0
        meanError < 0.26 -> 0.82
        meanError < 0.38 -> 0.65
        else -> 0.42
    }
}

private fun durationSymbol(duration: Double): String = when {
    duration >= 3.0 -> "𝅗𝅥·"
    duration >= 2.0 -> "𝅗𝅥"
    duration >= 1.0 -> "♩"
    duration >= 0.5 -> "♪"
    else -> "♬"
}

private fun formatBeat(value: Double): String {
    if (abs(value - value.toInt()) < 0.001) return value.toInt().toString()
    return when {
        abs(value - 0.25) < 0.001 -> "¼"
        abs(value - 0.5) < 0.001 -> "½"
        abs(value - 0.75) < 0.001 -> "¾"
        abs(value - 1.5) < 0.001 -> "1½"
        abs(value - 2.5) < 0.001 -> "2½"
        abs(value - 3.5) < 0.001 -> "3½"
        else -> "%.2f".format(value)
    }
}

private fun noteName(midi: Int): String {
    val names = listOf("до", "до♯", "ре", "ре♯", "ми", "фа", "фа♯", "соль", "соль♯", "ля", "ля♯", "си")
    return names[Math.floorMod(midi, 12)]
}

private fun diatonicStaffStep(midi: Int): Int {
    val octave = Math.floorDiv(midi, 12) - 5
    val pc = Math.floorMod(midi, 12)
    val diatonic = when (pc) {
        0, 1 -> 0
        2, 3 -> 1
        4 -> 2
        5, 6 -> 3
        7, 8 -> 4
        9, 10 -> 5
        else -> 6
    }
    return octave * 7 + diatonic
}

private fun masteryLabel(value: Double): String = when {
    value < 0.44 -> "нужна опора"
    value < 0.62 -> "развивается"
    value < 0.80 -> "уверенно"
    else -> "очень уверенно"
}
