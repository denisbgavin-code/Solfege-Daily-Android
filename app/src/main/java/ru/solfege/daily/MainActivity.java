package ru.solfege.daily;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
    private static final String PREFS = "solfege_progress_v2";
    private static final int REQ_MIC = 7001;

    private static final int SCREEN_HOME = 0;
    private static final int SCREEN_LESSON = 1;
    private static final int SCREEN_LAB = 2;
    private static final int SCREEN_PROGRESS = 3;
    private static final int SCREEN_ADULT = 4;

    private static final int ACCENT = Color.rgb(47, 82, 171);
    private static final int ACCENT_LIGHT = Color.rgb(229, 235, 251);
    private static final int BG = Color.rgb(247, 249, 253);
    private static final int TEXT = Color.rgb(28, 31, 38);
    private static final int MUTED = Color.rgb(92, 99, 113);
    private static final int GOOD = Color.rgb(35, 119, 77);
    private static final int WARN = Color.rgb(174, 95, 26);
    private static final int LINE = Color.rgb(226, 230, 238);

    private SharedPreferences prefs;
    private AudioEngine audio;
    private SkillTracker skills;
    private PitchDetector pitchDetector;
    private TextToSpeech tts;
    private boolean ttsReady = false;

    private int screen = SCREEN_HOME;
    private int lessonId;
    private int station = -1;
    private final boolean[] stationDone = new boolean[6];
    private boolean reviewMode = false;
    private SkillTracker.Skill reviewSkill = null;
    private Button navActionButton;

    private final List<Long> rhythmTaps = new ArrayList<>();
    private boolean rhythmRecording = false;
    private int earWrongAnswers = 0;

    private int pendingMicTarget = 60;
    private TextView pitchStatus;
    private ProgressBar pitchLevel;
    private boolean pitchMatchRecorded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        audio = new AudioEngine();
        skills = new SkillTracker(prefs);
        pitchDetector = new PitchDetector();
        tts = new TextToSpeech(this, this);

        int completed = prefs.getInt("completed", 0);
        lessonId = Math.min(Course.TOTAL_LESSONS, completed + 1);

        int partialLesson = prefs.getInt("partialLesson", -1);
        int partialStep = prefs.getInt("partialStep", -1);
        if (partialLesson == lessonId && partialStep >= 0 && partialStep < 6) {
            station = partialStep;
            for (int i = 0; i < station; i++) stationDone[i] = true;
        }

        render();
    }

    @Override
    protected void onPause() {
        audio.stop();
        pitchDetector.stop();
        if (tts != null) tts.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (audio != null) audio.close();
        if (pitchDetector != null) pitchDetector.stop();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(new Locale("ru", "RU"));
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED;
            tts.setSpeechRate(0.92f);
        }
    }

    private void render() {
        rhythmRecording = false;
        rhythmTaps.clear();
        earWrongAnswers = 0;
        navActionButton = null;

        switch (screen) {
            case SCREEN_LESSON:
                renderLesson();
                break;
            case SCREEN_LAB:
                renderLab();
                break;
            case SCREEN_PROGRESS:
                renderProgress();
                break;
            case SCREEN_ADULT:
                renderAdult();
                break;
            default:
                renderHome();
                break;
        }
    }

    private void renderHome() {
        int completed = prefs.getInt("completed", 0);
        int currentId = Math.min(Course.TOTAL_LESSONS, completed + 1);
        lessonId = currentId;
        Course.Lesson lesson = Course.lesson(currentId);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = page();
        scroll.addView(root);

        root.addView(text("Музыкальная экспедиция", 28, true));
        TextView subtitle = text("Сольфеджио каждый день • слух → действие → знак → творчество", 14, false);
        subtitle.setTextColor(MUTED);
        root.addView(subtitle, margins(0, 2, 0, 16));

        LinearLayout today = card();
        today.addView(kicker("СЕГОДНЯ"));
        today.addView(text("Неделя " + lesson.week + " • день " + lesson.day, 14, true), margins(0, 8, 0, 2));
        today.addView(text(lesson.weekTitle, 24, true));
        today.addView(text(lesson.dayTitle, 17, true), margins(0, 2, 0, 5));

        TextView goal = text(lesson.goal, 15, false);
        goal.setTextColor(MUTED);
        today.addView(goal, margins(0, 0, 0, 13));

        ProgressBar total = progressBar(Course.TOTAL_LESSONS, completed);
        today.addView(total, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8)));

        TextView summary = text(
                completed + " из " + Course.TOTAL_LESSONS + " уроков • "
                        + prefs.getInt("streak", 0) + " дн. подряд",
                13,
                false
        );
        summary.setTextColor(MUTED);
        today.addView(summary, margins(0, 7, 0, 11));

        Button start = primaryButton(station >= 0 ? "Продолжить сегодняшнюю миссию" : "Начать сегодняшнюю миссию");
        start.setOnClickListener(v -> startDailyLesson());
        today.addView(start);
        root.addView(today);

        LinearLayout adaptive = card();
        adaptive.addView(kicker("УМНАЯ ТРЕНИРОВКА"));
        SkillTracker.Skill weak = skills.weakestSkill();
        adaptive.addView(text("Короткое повторение: " + weak.title, 19, true), margins(0, 8, 0, 4));

        TextView adaptiveText = text(skills.summaryLine(), 14, false);
        adaptiveText.setTextColor(MUTED);
        adaptive.addView(adaptiveText, margins(0, 0, 0, 9));

        Button review = secondaryButton("Тренировать слабое место");
        review.setOnClickListener(v -> startSmartReview());
        adaptive.addView(review);
        root.addView(adaptive, margins(0, 12, 0, 0));

        LinearLayout sound = card();
        sound.addView(text("Быстрая проверка звука", 17, true));
        TextView soundStatus = text("Музыкальные задания используют обычную громкость «Медиа».", 13, false);
        soundStatus.setTextColor(MUTED);
        sound.addView(soundStatus, margins(0, 4, 0, 8));

        Button test = secondaryButton("▶ Нота до");
        test.setOnClickListener(v -> {
            if (audio.mediaVolumeIsZero(this)) {
                soundStatus.setText("Громкость «Медиа» равна нулю. Увеличь её кнопками телефона.");
                soundStatus.setTextColor(WARN);
            } else {
                soundStatus.setText("Слушай…");
                soundStatus.setTextColor(MUTED);
                audio.playHomeNote(60, () -> {
                    soundStatus.setText("Если нота прозвучала — звук настроен.");
                    soundStatus.setTextColor(GOOD);
                });
            }
        });
        sound.addView(test);
        root.addView(sound, margins(0, 12, 0, 0));

        LinearLayout nav1 = row();
        Button lab = secondaryButton("Лаборатория");
        lab.setOnClickListener(v -> {
            screen = SCREEN_LAB;
            render();
        });
        nav1.addView(lab, weighted(1f));

        Button progress = secondaryButton("Мой прогресс");
        progress.setOnClickListener(v -> {
            screen = SCREEN_PROGRESS;
            render();
        });
        LinearLayout.LayoutParams pp = weighted(1f);
        pp.setMargins(dp(8), 0, 0, 0);
        nav1.addView(progress, pp);
        root.addView(nav1, margins(0, 12, 0, 0));

        LinearLayout nav2 = row();
        Button map = secondaryButton("Карта 34 недель");
        map.setOnClickListener(v -> showCourseMap());
        nav2.addView(map, weighted(1f));

        Button adult = secondaryButton("Для взрослого");
        adult.setOnClickListener(v -> {
            screen = SCREEN_ADULT;
            render();
        });
        LinearLayout.LayoutParams ap = weighted(1f);
        ap.setMargins(dp(8), 0, 0, 0);
        nav2.addView(adult, ap);
        root.addView(nav2, margins(0, 8, 0, 0));

        TextView selfContained = text(
                "Все задания выполняются внутри приложения. Для урока не нужны отдельный учебник, рабочая тетрадь или интернет.",
                13,
                false
        );
        selfContained.setTextColor(MUTED);
        root.addView(selfContained, margins(2, 14, 2, 0));

        setContentView(scroll);
    }

    private void startDailyLesson() {
        reviewMode = false;
        reviewSkill = null;
        screen = SCREEN_LESSON;

        int completed = prefs.getInt("completed", 0);
        lessonId = Math.min(Course.TOTAL_LESSONS, completed + 1);

        if (prefs.getInt("partialLesson", -1) == lessonId) {
            station = Math.max(0, Math.min(5, prefs.getInt("partialStep", 0)));
            for (int i = 0; i < station; i++) stationDone[i] = true;
        } else {
            resetStationState();
            station = 0;
        }
        render();
    }

    private void startSmartReview() {
        int completed = prefs.getInt("completed", 0);
        if (completed <= 0) {
            Toast.makeText(this, "Сначала пройди первый урок.", Toast.LENGTH_SHORT).show();
            return;
        }

        reviewMode = true;
        reviewSkill = skills.weakestSkill();
        lessonId = skills.smartReviewLesson(completed);
        station = stationForSkill(reviewSkill);
        resetStationState();
        screen = SCREEN_LESSON;
        render();
    }

    private int stationForSkill(SkillTracker.Skill skill) {
        switch (skill) {
            case PULSE:
            case RHYTHM:
                return 1;
            case PITCH:
            case TONALITY:
                return 2;
            case READING:
            case KEYS:
                return 3;
            case WRITING:
                return 4;
            case INTERVALS:
                return 0;
            case CREATIVITY:
            case MEMORY:
            default:
                return 5;
        }
    }

    private void renderLesson() {
        Course.Lesson lesson = Course.lesson(lessonId);
        boolean teacher = prefs.getBoolean("teacher", false);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = page();
        scroll.addView(root);

        LinearLayout top = row();
        Button exit = secondaryButton("← Домой");
        exit.setOnClickListener(v -> {
            stopLiveTools();
            if (!reviewMode) savePartial();
            screen = SCREEN_HOME;
            station = reviewMode ? -1 : station;
            reviewMode = false;
            reviewSkill = null;
            render();
        });
        top.addView(exit, weighted(1f));

        TextView counter = text(
                reviewMode ? "повторение" : (station + 1) + " / 6",
                14,
                true
        );
        counter.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        top.addView(counter, weighted(1f));
        root.addView(top);

        TextView eyebrow = text(
                reviewMode
                        ? "УМНАЯ ТРЕНИРОВКА • " + (reviewSkill == null ? "" : reviewSkill.title)
                        : "Неделя " + lesson.week + " • день " + lesson.day,
                12,
                true
        );
        eyebrow.setTextColor(ACCENT);
        root.addView(eyebrow, margins(2, 13, 0, 2));

        root.addView(text(lesson.weekTitle, 22, true));
        TextView day = text(lesson.dayTitle, 15, false);
        day.setTextColor(MUTED);
        root.addView(day, margins(0, 2, 0, 9));

        ProgressBar progress = progressBar(6, station + 1);
        root.addView(progress, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(7)));

        LinearLayout stationCard = card();
        root.addView(stationCard, margins(0, 12, 0, 10));

        switch (station) {
            case 0:
                renderEarStation(stationCard, lesson);
                break;
            case 1:
                renderRhythmStation(stationCard, lesson);
                break;
            case 2:
                renderVoiceStation(stationCard, lesson);
                break;
            case 3:
                renderReadingStation(stationCard, lesson);
                break;
            case 4:
                renderWritingStation(stationCard, lesson);
                break;
            default:
                renderCreativeStation(stationCard, lesson);
                break;
        }

        LinearLayout nav = row();

        Button back = secondaryButton("← Назад");
        back.setEnabled(!reviewMode && station > 0);
        back.setOnClickListener(v -> {
            stopLiveTools();
            station--;
            savePartial();
            render();
        });
        nav.addView(back, weighted(1f));

        if (reviewMode) {
            Button done = primaryButton("Завершить тренировку");
            done.setEnabled(stationDone[station] || teacher);
            done.setOnClickListener(v -> {
                stopLiveTools();
                reviewMode = false;
                reviewSkill = null;
                station = -1;
                screen = SCREEN_HOME;
                render();
            });
            LinearLayout.LayoutParams dp = weighted(1f);
            dp.setMargins(this.dp(8), 0, 0, 0);
            nav.addView(done, dp);
            navActionButton = done;
        } else if (station < 5) {
            Button next = primaryButton("Дальше →");
            next.setEnabled(stationDone[station] || teacher);
            next.setOnClickListener(v -> {
                stopLiveTools();
                stationDone[station] = true;
                station++;
                savePartial();
                render();
            });
            LinearLayout.LayoutParams np = weighted(1f);
            np.setMargins(dp(8), 0, 0, 0);
            nav.addView(next, np);
            navActionButton = next;
        } else {
            Button finish = primaryButton("Завершить миссию");
            finish.setEnabled(stationDone[5] || teacher);
            finish.setOnClickListener(v -> finishLesson());
            LinearLayout.LayoutParams fp = weighted(1f);
            fp.setMargins(dp(8), 0, 0, 0);
            nav.addView(finish, fp);
            navActionButton = finish;
        }

        root.addView(nav);
        setContentView(scroll);
    }

    private void renderEarStation(LinearLayout box, Course.Lesson lesson) {
        box.addView(stepTitle("1", "Ухо-детектив"));
        TextView instruction = bigInstruction(lesson.earGame.prompt);
        box.addView(instruction);

        addSpeakButton(box, lesson.earGame.prompt);

        String adaptive = skills.supportHint(lesson.week, 0);
        if (!adaptive.isEmpty()) box.addView(adaptiveHint(adaptive), margins(0, 8, 0, 0));

        TextView status = statusText("Сначала послушай до конца. Потом отвечай.");
        box.addView(status, margins(0, 9, 0, 9));

        Button play = primaryButton("▶ Слушать");
        play.setOnClickListener(v -> {
            if (!ensureAudio(status)) return;
            play.setEnabled(false);
            status.setText("Слушай…");
            status.setTextColor(MUTED);
            audio.playEarGame(lesson.earGame, () -> {
                play.setEnabled(true);
                status.setText("Теперь выбери ответ.");
                status.setTextColor(TEXT);
            });
        });
        box.addView(play);

        boolean[] recorded = {false};
        for (int i = 0; i < lesson.earGame.options.length; i++) {
            final int answer = i;
            Button option = secondaryButton(lesson.earGame.options[i]);
            option.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);

                if (answer == lesson.earGame.correct) {
                    status.setText("Верно. " + lesson.earGame.explanation);
                    status.setTextColor(GOOD);
                    if (!recorded[0]) {
                        skills.recordStation(lesson.week, 0, earWrongAnswers == 0 ? 1.0 : 0.78);
                        recorded[0] = true;
                    }
                    stationDone[0] = true;
                    savePartial();
                    unlockNavigation();
                } else {
                    earWrongAnswers++;
                    status.setText(
                            earWrongAnswers < 2
                                    ? "Пока не совпало. Послушай ещё раз и следи только за одним признаком."
                                    : "Ответ: «" + lesson.earGame.options[lesson.earGame.correct]
                                    + "». Послушай ещё раз и найди именно этот признак."
                    );
                    status.setTextColor(WARN);

                    if (earWrongAnswers >= 2) {
                        if (!recorded[0]) {
                            skills.recordStation(lesson.week, 0, 0.38);
                            recorded[0] = true;
                        }
                        stationDone[0] = true;
                        savePartial();
                        unlockNavigation();
                    }
                }
            });
            box.addView(option, margins(0, 6, 0, 0));
        }

        if (stationDone[0]) {
            box.addView(doneNote("Шаг пройден. Можно переслушать пример перед переходом дальше."), margins(0, 9, 0, 0));
        }
    }

    private void renderRhythmStation(LinearLayout box, Course.Lesson lesson) {
        box.addView(stepTitle("2", "Пульс и ритм"));
        box.addView(bigInstruction(lesson.movement));
        addSpeakButton(box, lesson.movement);

        String adaptive = skills.supportHint(lesson.week, 1);
        if (!adaptive.isEmpty()) box.addView(adaptiveHint(adaptive), margins(0, 8, 0, 0));

        TextView status = statusText("Сначала почувствуй ровные доли.");
        box.addView(status, margins(0, 9, 0, 8));

        Button pulse = secondaryButton("▶ 8 ровных долей");
        pulse.setOnClickListener(v -> {
            if (ensureAudio(status)) audio.playPulse(8, null);
        });
        box.addView(pulse);

        Button model = primaryButton("▶ Послушать ритм");
        model.setOnClickListener(v -> {
            if (!ensureAudio(status)) return;
            status.setText("Слушай и держи пульс внутри.");
            status.setTextColor(MUTED);
            audio.playRhythm(lesson.rhythm, meterForWeek(lesson.week), () -> {
                status.setText("Теперь повтори кнопкой «ХЛОП».");
                status.setTextColor(TEXT);
            });
        });
        box.addView(model, margins(0, 7, 0, 0));

        RhythmPatternView pattern = new RhythmPatternView(this);
        pattern.setDurations(lesson.rhythm);
        pattern.setVisibility(View.GONE);
        box.addView(pattern, margins(0, 8, 0, 0));

        Button begin = secondaryButton("Начать мой ответ");
        box.addView(begin, margins(0, 7, 0, 0));

        Button tap = primaryButton("ХЛОП");
        tap.setTextSize(23);
        tap.setEnabled(false);
        box.addView(tap, margins(0, 7, 0, 0));

        begin.setOnClickListener(v -> {
            rhythmTaps.clear();
            rhythmRecording = true;
            tap.setEnabled(true);
            status.setText("Сделай " + lesson.rhythm.length + " касаний. Не спеши.");
            status.setTextColor(TEXT);
        });

        tap.setOnClickListener(v -> {
            if (!rhythmRecording) return;
            v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            rhythmTaps.add(System.currentTimeMillis());
            status.setText("Касаний: " + rhythmTaps.size() + " из " + lesson.rhythm.length);

            if (rhythmTaps.size() >= lesson.rhythm.length) {
                rhythmRecording = false;
                tap.setEnabled(false);
                double error = rhythmError(rhythmTaps, lesson.rhythm);
                double result;

                if (error < 0.18) {
                    status.setText("Очень близко к образцу. Ритмические отношения сохранились.");
                    status.setTextColor(GOOD);
                    result = 1.0;
                } else if (error < 0.34) {
                    status.setText("Почти. Сравни с рисунком ниже и попробуй ещё раз по желанию.");
                    status.setTextColor(WARN);
                    result = 0.72;
                } else {
                    status.setText("Рисунок изменился. Посмотри на длительности ниже и повтори в более спокойном темпе.");
                    status.setTextColor(WARN);
                    result = 0.42;
                }

                pattern.setVisibility(View.VISIBLE);
                skills.recordStation(lesson.week, 1, result);
                stationDone[1] = true;
                savePartial();
                unlockNavigation();
            }
        });

        TextView label = text("После попытки: " + lesson.rhythmLabel, 13, true);
        label.setTextColor(ACCENT);
        box.addView(label, margins(0, 8, 0, 0));
    }

    private void renderVoiceStation(LinearLayout box, Course.Lesson lesson) {
        box.addView(stepTitle("3", "Голос и внутренний слух"));
        box.addView(bigInstruction(lesson.singing));
        addSpeakButton(box, lesson.singing);

        String adaptive = skills.supportHint(lesson.week, 2);
        if (!adaptive.isEmpty()) box.addView(adaptiveHint(adaptive), margins(0, 8, 0, 0));

        TextView status = statusText(
                "Можно выполнять без микрофона. Микрофон — только дополнительный помощник высоты и ничего не записывает."
        );
        box.addView(status, margins(0, 9, 0, 8));

        Button melody = primaryButton("▶ Слушать мелодию");
        melody.setOnClickListener(v -> {
            if (!ensureAudio(status)) return;
            melody.setEnabled(false);
            status.setText("Слушай. После окончания выдержи маленькую паузу и пой.");
            status.setTextColor(MUTED);
            audio.playMelody(lesson.melody, () -> {
                melody.setEnabled(true);
                status.setText("Теперь повтори голосом.");
                status.setTextColor(TEXT);
            });
        });
        box.addView(melody);

        Button first = secondaryButton("▶ Только первый звук");
        first.setOnClickListener(v -> {
            if (ensureAudio(status)) audio.playHomeNote(lesson.melody[0].midi, null);
        });
        box.addView(first, margins(0, 7, 0, 0));

        TextView micLabel = text("Помощник высоты", 16, true);
        micLabel.setTextColor(ACCENT);
        box.addView(micLabel, margins(0, 13, 0, 3));

        pitchStatus = statusText("Спой первый звук на удобной октаве.");
        box.addView(pitchStatus);

        pitchLevel = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        pitchLevel.setMax(100);
        pitchLevel.setProgress(0);
        pitchLevel.setProgressTintList(ColorStateList.valueOf(ACCENT));
        pitchLevel.setProgressBackgroundTintList(ColorStateList.valueOf(LINE));
        box.addView(pitchLevel, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(6)));

        LinearLayout micRow = row();
        Button mic = secondaryButton("🎤 Проверить");
        mic.setOnClickListener(v -> requestOrStartPitch(lesson.melody[0].midi));
        micRow.addView(mic, weighted(1f));

        Button stop = secondaryButton("Стоп");
        stop.setOnClickListener(v -> {
            pitchDetector.stop();
            if (pitchStatus != null) pitchStatus.setText("Проверка остановлена.");
        });
        LinearLayout.LayoutParams sp = weighted(1f);
        sp.setMargins(dp(8), 0, 0, 0);
        micRow.addView(stop, sp);
        box.addView(micRow, margins(0, 6, 0, 0));

        Button manual = primaryButton("Я спел и сравнил");
        manual.setOnClickListener(v -> {
            pitchDetector.stop();
            if (!pitchMatchRecorded) {
                skills.recordStation(lesson.week, 2, 0.66);
                pitchMatchRecorded = true;
            }
            stationDone[2] = true;
            status.setText("Готово. Сравни не только точность высоты, но и направление всей фразы.");
            status.setTextColor(GOOD);
            savePartial();
            unlockNavigation();
        });
        box.addView(manual, margins(0, 8, 0, 0));
    }

    private void renderReadingStation(LinearLayout box, Course.Lesson lesson) {
        box.addView(stepTitle("4", lesson.showStaff ? "Чтение нот" : "Карта мелодии"));

        String instruction = lesson.showStaff
                ? "Сначала послушай и попробуй представить рисунок. Потом открой ноты. Названия появятся только после твоей попытки."
                : "Сначала послушай и представь линию мелодии. Потом открой карту: выше, ниже или тот же уровень.";
        box.addView(bigInstruction(instruction));
        addSpeakButton(box, instruction);

        String adaptive = skills.supportHint(lesson.week, 3);
        if (!adaptive.isEmpty()) box.addView(adaptiveHint(adaptive), margins(0, 8, 0, 0));

        TextView status = statusText("Сначала — слух. Потом — картинка.");
        box.addView(status, margins(0, 9, 0, 8));

        Button listen = secondaryButton("▶ Послушать мелодию");
        listen.setOnClickListener(v -> {
            if (ensureAudio(status)) audio.playMelody(lesson.melody, null);
        });
        box.addView(listen);

        LinearLayout visual = column();
        visual.setVisibility(View.GONE);
        box.addView(visual, margins(0, 8, 0, 0));

        if (lesson.showStaff) {
            TextView key = text("Тональность: " + lesson.keyName, 13, true);
            key.setTextColor(ACCENT);
            visual.addView(key, margins(0, 0, 0, 5));

            StaffView staff = new StaffView(this);
            staff.setNotes(lesson.melody);
            staff.setShowLabels(false);
            visual.addView(staff);

            Button labels = secondaryButton("Показать названия нот");
            labels.setOnClickListener(v -> {
                staff.setShowLabels(true);
                labels.setText("Названия открыты");
                labels.setEnabled(false);
                skills.recordStation(lesson.week, 3, 0.72);
            });
            visual.addView(labels, margins(0, 3, 0, 0));
        } else {
            ContourView contour = new ContourView(this);
            contour.setNotes(lesson.melody);
            visual.addView(contour);

            TextView note = text("Каждая точка — звук. Чем выше точка, тем выше звук.", 13, false);
            note.setTextColor(MUTED);
            visual.addView(note);
        }

        RhythmPatternView rhythm = new RhythmPatternView(this);
        rhythm.setDurations(lesson.rhythm);
        visual.addView(rhythm, margins(0, 5, 0, 0));

        Button reveal = primaryButton("Открыть после попытки");
        reveal.setOnClickListener(v -> {
            boolean show = visual.getVisibility() != View.VISIBLE;
            visual.setVisibility(show ? View.VISIBLE : View.GONE);
            reveal.setText(show ? "Спрятать и вспомнить" : "Открыть снова");
            if (show) {
                if (!lesson.showStaff) skills.recordStation(lesson.week, 3, 0.70);
                stationDone[3] = true;
                savePartial();
                unlockNavigation();
            }
        });
        box.addView(reveal, margins(0, 8, 0, 0));

        if (lesson.showStaff) {
            TextView task = text(
                    "Прочитай первые 3–4 ноты вслух. Потом нажми «Показать названия нот» и проверь себя.",
                    14,
                    false
            );
            box.addView(task, margins(0, 9, 0, 0));
        }
    }

    private void renderWritingStation(LinearLayout box, Course.Lesson lesson) {
        boolean measure = shouldUseMeasureBuilder(lesson);

        box.addView(stepTitle("5", measure ? "Конструктор такта" : (lesson.showStaff ? "Музыкальный диктант" : "Ритмическая запись")));

        String instruction;
        if (measure) {
            instruction = "Собери один полный такт. Перетаскивай или нажимай длительности, пока сумма точно не заполнит размер.";
        } else {
            instruction = "Послушай первые звуки мелодии и введи их на клавиатуре. Сначала пробуй на слух, не по названиям.";
        }
        box.addView(bigInstruction(instruction));
        addSpeakButton(box, instruction);

        String adaptive = skills.supportHint(lesson.week, 4);
        if (!adaptive.isEmpty()) box.addView(adaptiveHint(adaptive), margins(0, 8, 0, 0));

        TextView status = statusText("Задание можно исправлять сколько угодно.");
        box.addView(status, margins(0, 9, 0, 8));

        if (measure) {
            renderMeasureBuilder(box, lesson, status);
        } else {
            renderMelodicDictation(box, lesson, status);
        }
    }

    private void renderMeasureBuilder(LinearLayout box, Course.Lesson lesson, TextView status) {
        double target = targetBeatsForLesson(lesson);
        double[] allowed = allowedDurationsForWeek(lesson.week);

        TextView meter = text("Нужно заполнить: " + formatBeat(target) + " долей", 15, true);
        meter.setTextColor(ACCENT);
        box.addView(meter);

        MeasureBuilderView builder = new MeasureBuilderView(this);
        builder.configure(target, allowed);
        box.addView(builder);

        builder.setListener((total, targetBeats, exact) -> {
            status.setText("Сейчас: " + formatBeat(total) + " из " + formatBeat(targetBeats) + " долей.");
            if (exact) {
                status.setText("Такт заполнен точно. Теперь прохлопай то, что собрал.");
                status.setTextColor(GOOD);
                skills.recordStation(lesson.week, 4, 1.0);
                stationDone[4] = true;
                savePartial();
                unlockNavigation();
            } else {
                status.setTextColor(TEXT);
            }
        });

        LinearLayout row = row();
        Button undo = secondaryButton("Отменить");
        undo.setOnClickListener(v -> builder.undo());
        row.addView(undo, weighted(1f));

        Button clear = secondaryButton("Очистить");
        clear.setOnClickListener(v -> builder.clear());
        LinearLayout.LayoutParams cp = weighted(1f);
        cp.setMargins(dp(8), 0, 0, 0);
        row.addView(clear, cp);
        box.addView(row, margins(0, 4, 0, 0));
    }

    private void renderMelodicDictation(LinearLayout box, Course.Lesson lesson, TextView status) {
        int count = Math.min(lesson.week < 16 ? 3 : 4, lesson.melody.length);
        int[] fragment = new int[count];
        for (int i = 0; i < count; i++) fragment[i] = lesson.melody[i].midi;

        Button play = primaryButton("▶ Слушать фрагмент");
        play.setOnClickListener(v -> {
            if (ensureAudio(status)) audio.playMidi(fragment, null);
        });
        box.addView(play);

        MelodyEntryView entry = new MelodyEntryView(this);
        entry.setTarget(lesson.melody, count);
        box.addView(entry, margins(0, 8, 0, 0));

        TextView entered = text("Введено: 0 из " + count, 13, false);
        entered.setTextColor(MUTED);
        box.addView(entered);

        PianoView piano = new PianoView(this);
        piano.setMinimumWidth(dp(720));
        piano.setListener(midi -> {
            audio.playHomeNote(midi, null);
            entry.addMidi(midi);
        });

        HorizontalScrollView horizontal = new HorizontalScrollView(this);
        horizontal.setHorizontalScrollBarEnabled(true);
        horizontal.addView(piano, new ViewGroup.LayoutParams(dp(720), dp(170)));
        box.addView(horizontal, margins(0, 7, 0, 0));

        boolean[] recorded = {false};
        entry.setListener(new MelodyEntryView.Listener() {
            @Override
            public void onComplete(double score) {
                if (score >= 0.999) {
                    status.setText("Точно. Фрагмент записан по слуху.");
                    status.setTextColor(GOOD);
                } else {
                    status.setText("Совпало " + (int) Math.round(score * count) + " из " + count
                            + ". Послушай снова, очисти и попробуй ещё раз. Шаг уже можно продолжить.");
                    status.setTextColor(WARN);
                }

                if (!recorded[0]) {
                    skills.recordStation(lesson.week, 4, Math.max(0.30, score));
                    recorded[0] = true;
                }
                stationDone[4] = true;
                savePartial();
                unlockNavigation();
            }

            @Override
            public void onChanged(int enteredCount, int total) {
                entered.setText("Введено: " + enteredCount + " из " + total);
            }
        });

        LinearLayout tools = row();
        Button undo = secondaryButton("← Убрать");
        undo.setOnClickListener(v -> entry.undo());
        tools.addView(undo, weighted(1f));

        Button clear = secondaryButton("Очистить");
        clear.setOnClickListener(v -> entry.clear());
        LinearLayout.LayoutParams cp = weighted(1f);
        cp.setMargins(dp(8), 0, 0, 0);
        tools.addView(clear, cp);
        box.addView(tools, margins(0, 6, 0, 0));

        if (lesson.showStaff && lesson.day % 3 == 0) {
            Button staffChallenge = secondaryButton("Дополнительно: расставить на нотном стане");
            staffChallenge.setOnClickListener(v -> showStaffWritingDialog(lesson, count));
            box.addView(staffChallenge, margins(0, 7, 0, 0));
        }
    }

    private void showStaffWritingDialog(Course.Lesson lesson, int count) {
        Dialog dialog = new Dialog(this);
        LinearLayout root = column();
        root.setPadding(dp(16), dp(16), dp(16), dp(16));
        root.setBackgroundColor(Color.WHITE);

        root.addView(text("Расставь ноты на стане", 22, true));
        root.addView(text("Слушай мелодию, затем нажимай на нужную высоту. Для изменённых звуков выбери ♯ или ♭ перед касанием.", 14, false), margins(0, 5, 0, 8));

        TextView status = statusText("Введено: 0 из " + count);
        root.addView(status);

        Button listen = secondaryButton("▶ Слушать");
        listen.setOnClickListener(v -> audio.playMelody(lesson.melody, null));
        root.addView(listen, margins(0, 6, 0, 0));

        StaffTapView staff = new StaffTapView(this);
        staff.setTarget(lesson.melody, count);
        root.addView(staff, margins(0, 6, 0, 0));

        LinearLayout acc = row();
        Button natural = secondaryButton("без знака");
        Button sharp = secondaryButton("♯");
        Button flat = secondaryButton("♭");
        natural.setOnClickListener(v -> {
            staff.setAccidental("");
            status.setText("Следующая нота: без дополнительного знака.");
        });
        sharp.setOnClickListener(v -> {
            staff.setAccidental("♯");
            status.setText("Следующая нота: с диезом.");
        });
        flat.setOnClickListener(v -> {
            staff.setAccidental("♭");
            status.setText("Следующая нота: с бемолем.");
        });
        acc.addView(natural, weighted(1f));
        LinearLayout.LayoutParams sap = weighted(1f);
        sap.setMargins(dp(6), 0, 0, 0);
        acc.addView(sharp, sap);
        LinearLayout.LayoutParams fap = weighted(1f);
        fap.setMargins(dp(6), 0, 0, 0);
        acc.addView(flat, fap);
        root.addView(acc);

        staff.setListener(new StaffTapView.Listener() {
            @Override
            public void onComplete(double score) {
                status.setText(score >= 0.999
                        ? "Все ноты на месте."
                        : "Совпало примерно " + Math.round(score * count) + " из " + count + ". Можно очистить и попробовать ещё.");
                status.setTextColor(score >= 0.999 ? GOOD : WARN);
                skills.record(SkillTracker.Skill.WRITING, Math.max(0.30, score));
            }

            @Override
            public void onChanged(int entered, int total) {
                if (entered < total) status.setText("Введено: " + entered + " из " + total);
            }
        });

        LinearLayout tools = row();
        Button undo = secondaryButton("Отменить");
        undo.setOnClickListener(v -> staff.undo());
        tools.addView(undo, weighted(1f));

        Button clear = secondaryButton("Очистить");
        clear.setOnClickListener(v -> staff.clear());
        LinearLayout.LayoutParams c = weighted(1f);
        c.setMargins(dp(8), 0, 0, 0);
        tools.addView(clear, c);
        root.addView(tools, margins(0, 6, 0, 0));

        Button close = primaryButton("Закрыть");
        close.setOnClickListener(v -> dialog.dismiss());
        root.addView(close, margins(0, 10, 0, 0));

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        dialog.setContentView(sv);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    private void renderCreativeStation(LinearLayout box, Course.Lesson lesson) {
        box.addView(stepTitle("6", "Твоя музыка"));
        box.addView(bigInstruction(lesson.creative));
        addSpeakButton(box, lesson.creative);

        String adaptive = skills.supportHint(lesson.week, 5);
        if (!adaptive.isEmpty()) box.addView(adaptiveHint(adaptive), margins(0, 8, 0, 0));

        TextView status = statusText("Здесь может быть несколько хороших ответов. Важно выполнить правило.");
        box.addView(status, margins(0, 9, 0, 8));

        Button start = secondaryButton("▶ Стартовый звук");
        start.setOnClickListener(v -> {
            if (ensureAudio(status)) audio.playHomeNote(lesson.melody[0].midi, null);
        });
        box.addView(start);

        if (lesson.day == 7 && !lesson.review.isEmpty()) {
            TextView retrieval = text("Вспомни без подсказки", 16, true);
            retrieval.setTextColor(ACCENT);
            box.addView(retrieval, margins(0, 12, 0, 3));

            TextView recall = text(lesson.review, 14, false);
            box.addView(recall);

            Button spoke = secondaryButton("Я вспомнил хотя бы один пункт");
            spoke.setOnClickListener(v -> skills.record(SkillTracker.Skill.MEMORY, 0.8));
            box.addView(spoke, margins(0, 6, 0, 0));
        }

        Button ready = primaryButton("Я придумал и исполнил");
        ready.setOnClickListener(v -> {
            skills.recordStation(lesson.week, 5, 0.82);
            stationDone[5] = true;
            status.setText("Готово. Хорошая проверка — можешь ли ты повторить свой вариант ещё раз примерно одинаково.");
            status.setTextColor(GOOD);
            savePartial();
            unlockNavigation();
        });
        box.addView(ready, margins(0, 8, 0, 0));
    }

    private void finishLesson() {
        stopLiveTools();
        boolean teacher = prefs.getBoolean("teacher", false);

        if (teacher) {
            Toast.makeText(this, "Режим преподавателя: прогресс ученика не изменён.", Toast.LENGTH_SHORT).show();
            if (lessonId < Course.TOTAL_LESSONS) lessonId++;
            resetStationState();
            station = -1;
            screen = SCREEN_HOME;
            render();
            return;
        }

        int completed = prefs.getInt("completed", 0);
        if (lessonId == completed + 1) {
            SharedPreferences.Editor e = prefs.edit();
            e.putInt("completed", lessonId);
            e.putInt("xp", prefs.getInt("xp", 0) + 10);
            e.remove("partialLesson");
            e.remove("partialStep");
            updateStreak(e);
            e.apply();
        }

        String next = lessonId >= Course.TOTAL_LESSONS
                ? "Годовая экспедиция пройдена. Теперь приложение будет особенно полезно как тренажёр слабых мест и музыкальная лаборатория."
                : "Сегодня ты работал со слухом, ритмом, голосом, чтением, записью и собственным музыкальным ответом. Часть материала вернётся позже в другой форме.";

        new AlertDialog.Builder(this)
                .setTitle(lessonId >= Course.TOTAL_LESSONS ? "Год завершён" : "Миссия завершена")
                .setMessage(next)
                .setPositiveButton("Готово", (d, which) -> {
                    if (lessonId < Course.TOTAL_LESSONS) lessonId++;
                    resetStationState();
                    station = -1;
                    screen = SCREEN_HOME;
                    render();
                })
                .show();
    }

    private void renderLab() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = page();
        scroll.addView(root);

        addTopBack(root, "Музыкальная лаборатория", SCREEN_HOME);
        TextView sub = text("Свободные тренажёры. Они не меняют порядок основного курса.", 14, false);
        sub.setTextColor(MUTED);
        root.addView(sub, margins(0, 2, 0, 12));

        LinearLayout pianoCard = card();
        pianoCard.addView(text("Две октавы", 20, true));
        TextView pianoStatus = statusText("Нажимай клавиши и слушай.");
        pianoCard.addView(pianoStatus, margins(0, 4, 0, 6));

        PianoView piano = new PianoView(this);
        piano.setMinimumWidth(dp(720));
        piano.setListener(midi -> {
            audio.playHomeNote(midi, null);
            pianoStatus.setText("Звук: " + PitchDetector.noteName(midi));
            skills.record(SkillTracker.Skill.PITCH, 0.58);
        });

        HorizontalScrollView pScroll = new HorizontalScrollView(this);
        pScroll.addView(piano, new ViewGroup.LayoutParams(dp(720), dp(170)));
        pianoCard.addView(pScroll);
        root.addView(pianoCard);

        LinearLayout interval = card();
        interval.addView(text("Интервальная лаборатория", 20, true));
        interval.addView(text("Слушай два звука подряд. Сравни расстояние.", 14, false), margins(0, 4, 0, 8));

        int[] semitones = {2,4,7,12};
        String[] names = {"секунда", "терция", "квинта", "октава"};
        for (int i = 0; i < names.length; i++) {
            final int st = semitones[i];
            Button b = secondaryButton("▶ " + names[i]);
            b.setOnClickListener(v -> {
                audio.playMidi(new int[]{60, 60 + st}, null);
                skills.record(SkillTracker.Skill.INTERVALS, 0.62);
            });
            interval.addView(b, margins(0, 5, 0, 0));
        }
        root.addView(interval, margins(0, 12, 0, 0));

        LinearLayout measure = card();
        measure.addView(text("Свободный конструктор такта", 20, true));
        TextView mStatus = statusText("Собери ровно 4 доли любым способом.");
        measure.addView(mStatus, margins(0, 4, 0, 5));

        MeasureBuilderView builder = new MeasureBuilderView(this);
        builder.configure(4.0, new double[]{2.0,1.0,0.5,0.25});
        builder.setListener((total, target, exact) -> {
            mStatus.setText(exact ? "Получился полный такт 4/4." : "Сейчас " + formatBeat(total) + " из 4 долей.");
            mStatus.setTextColor(exact ? GOOD : TEXT);
            if (exact) skills.record(SkillTracker.Skill.RHYTHM, 0.75);
        });
        measure.addView(builder);

        LinearLayout mTools = row();
        Button undo = secondaryButton("Отменить");
        undo.setOnClickListener(v -> builder.undo());
        mTools.addView(undo, weighted(1f));

        Button clear = secondaryButton("Очистить");
        clear.setOnClickListener(v -> builder.clear());
        LinearLayout.LayoutParams cl = weighted(1f);
        cl.setMargins(dp(8), 0, 0, 0);
        mTools.addView(clear, cl);
        measure.addView(mTools);
        root.addView(measure, margins(0, 12, 0, 0));

        LinearLayout pitch = card();
        pitch.addView(text("Зеркало высоты", 20, true));
        TextView pitchHelp = statusText("Выбери звук, послушай и попробуй спеть его на удобной октаве.");
        pitch.addView(pitchHelp, margins(0, 4, 0, 8));

        int[] targets = {60,62,64,65,67};
        String[] labels = {"до","ре","ми","фа","соль"};
        LinearLayout noteRow = row();
        for (int i = 0; i < targets.length; i++) {
            final int midi = targets[i];
            Button b = secondaryButton(labels[i]);
            b.setOnClickListener(v -> {
                pendingMicTarget = midi;
                audio.playHomeNote(midi, null);
                pitchHelp.setText("Выбран звук " + PitchDetector.noteName(midi) + ". Нажми «Проверить голос».");
            });
            noteRow.addView(b, weighted(1f));
        }
        pitch.addView(noteRow);

        pitchStatus = pitchHelp;
        pitchLevel = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        pitchLevel.setMax(100);
        pitchLevel.setProgressTintList(ColorStateList.valueOf(ACCENT));
        pitchLevel.setProgressBackgroundTintList(ColorStateList.valueOf(LINE));
        pitch.addView(pitchLevel, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(6)));

        Button check = primaryButton("🎤 Проверить голос");
        check.setOnClickListener(v -> requestOrStartPitch(pendingMicTarget));
        pitch.addView(check, margins(0, 7, 0, 0));

        Button stop = secondaryButton("Остановить микрофон");
        stop.setOnClickListener(v -> pitchDetector.stop());
        pitch.addView(stop, margins(0, 6, 0, 0));

        root.addView(pitch, margins(0, 12, 0, 0));

        setContentView(scroll);
    }

    private void renderProgress() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = page();
        scroll.addView(root);

        addTopBack(root, "Мой прогресс", SCREEN_HOME);

        int completed = prefs.getInt("completed", 0);
        int week = Math.min(34, completed / 7 + 1);

        LinearLayout overview = card();
        overview.addView(text("Экспедиция", 20, true));
        overview.addView(text(
                completed + " из " + Course.TOTAL_LESSONS + " уроков • сейчас неделя " + week,
                14,
                false
        ), margins(0, 4, 0, 8));

        JourneyMapView map = new JourneyMapView(this);
        map.setCompletedLessons(completed);
        overview.addView(map);
        root.addView(overview);

        LinearLayout skillsCard = card();
        skillsCard.addView(text("Карта навыков", 20, true));

        TextView explain = text(
                "Это не школьные оценки. Полосы показывают, насколько уверенно приложение видело этот навык в выполненных заданиях. Чем больше попыток, тем устойчивее оценка.",
                13,
                false
        );
        explain.setTextColor(MUTED);
        skillsCard.addView(explain, margins(0, 4, 0, 10));

        for (SkillTracker.Skill skill : SkillTracker.Skill.values()) {
            double score = skills.score(skill);
            int attempts = skills.attempts(skill);

            LinearLayout line = column();
            TextView label = text(
                    skill.title + " • " + skillWord(score, attempts) + " • попыток " + attempts,
                    14,
                    true
            );
            line.addView(label);

            ProgressBar bar = progressBar(100, (int) Math.round(score * 100));
            line.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(7)));
            skillsCard.addView(line, margins(0, 7, 0, 0));
        }

        TextView rec = text("Рекомендация: " + skills.summaryLine(), 14, true);
        rec.setTextColor(ACCENT);
        skillsCard.addView(rec, margins(0, 12, 0, 0));

        Button smart = primaryButton("Запустить умную тренировку");
        smart.setOnClickListener(v -> startSmartReview());
        skillsCard.addView(smart, margins(0, 9, 0, 0));

        root.addView(skillsCard, margins(0, 12, 0, 0));

        LinearLayout badges = card();
        badges.addView(text("Вехи", 20, true));
        badges.addView(text(badgeText(completed), 14, false), margins(0, 5, 0, 0));
        root.addView(badges, margins(0, 12, 0, 0));

        setContentView(scroll);
    }

    private void renderAdult() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = page();
        scroll.addView(root);

        addTopBack(root, "Для взрослого", SCREEN_HOME);

        LinearLayout principles = card();
        principles.addView(text("Как устроен курс", 20, true));
        principles.addView(text(Course.adultSummary(), 14, false), margins(0, 6, 0, 0));
        root.addView(principles);

        LinearLayout content = card();
        content.addView(text("Роль учебников", 20, true));
        content.addView(text(
                "Пособия Варламовой/Семченко и Золиной используются как ориентир покрытия программы первого года ДМШ, но не задают образовательную траекторию. "
                        + "Ребёнок не отсылается к странице или внешней тетради. Для каждого навыка внутри приложения создано самостоятельное упражнение: слуховое, ритмическое, вокальное, нотное или творческое. "
                        + "Защищённые авторским правом задания из изданий не копируются дословно; используются оригинальные функциональные эквиваленты.",
                14,
                false
        ), margins(0, 6, 0, 0));
        root.addView(content, margins(0, 12, 0, 0));

        LinearLayout privacy = card();
        privacy.addView(text("Микрофон и приватность", 20, true));
        privacy.addView(text(
                "Микрофон используется только по нажатию ребёнка для локального распознавания устойчивой высоты звука. Аудио не сохраняется и не отправляется в сеть. "
                        + "Распознавание служит подсказкой, а не экзаменом: ребёнок может пройти вокальное задание без микрофона.",
                14,
                false
        ), margins(0, 6, 0, 0));
        root.addView(privacy, margins(0, 12, 0, 0));

        LinearLayout teacherCard = card();
        teacherCard.addView(text("Режим преподавателя", 20, true));

        Switch teacher = new Switch(this);
        teacher.setText("Открыть весь курс для просмотра без изменения прогресса");
        teacher.setTextSize(14);
        teacher.setChecked(prefs.getBoolean("teacher", false));
        teacher.setOnCheckedChangeListener((buttonView, checked) -> {
            prefs.edit().putBoolean("teacher", checked).apply();
            Toast.makeText(this, checked ? "Весь курс открыт." : "Вернулась последовательная траектория.", Toast.LENGTH_SHORT).show();
        });
        teacherCard.addView(teacher, margins(0, 6, 0, 0));

        Button map = secondaryButton("Открыть карту курса");
        map.setOnClickListener(v -> showCourseMap());
        teacherCard.addView(map, margins(0, 8, 0, 0));
        root.addView(teacherCard, margins(0, 12, 0, 0));

        LinearLayout diagnostic = card();
        diagnostic.addView(text("Диагностика", 20, true));

        for (SkillTracker.Skill skill : skills.rankedWeakest()) {
            double score = skills.score(skill);
            int attempts = skills.attempts(skill);
            TextView line = text(
                    skill.title + ": " + Math.round(score * 100) + "% по внутренней шкале, попыток " + attempts,
                    13,
                    false
            );
            diagnostic.addView(line, margins(0, 4, 0, 0));
        }

        TextView caveat = text(
                "Проценты — адаптивная техническая оценка для выбора следующего упражнения, а не психометрический тест и не отметка успеваемости.",
                12,
                false
        );
        caveat.setTextColor(MUTED);
        diagnostic.addView(caveat, margins(0, 8, 0, 0));

        root.addView(diagnostic, margins(0, 12, 0, 0));

        LinearLayout danger = card();
        danger.addView(text("Управление данными", 20, true));
        Button reset = secondaryButton("Сбросить прогресс и диагностику");
        reset.setOnClickListener(v -> confirmReset());
        danger.addView(reset, margins(0, 7, 0, 0));
        root.addView(danger, margins(0, 12, 0, 0));

        setContentView(scroll);
    }

    private void showCourseMap() {
        Dialog dialog = new Dialog(this);
        LinearLayout root = column();
        root.setPadding(dp(16), dp(16), dp(16), dp(18));
        root.setBackgroundColor(Color.WHITE);

        root.addView(text("Карта 34 недель", 24, true));
        TextView note = text("Темы открываются последовательно. В режиме преподавателя можно просмотреть весь год.", 13, false);
        note.setTextColor(MUTED);
        root.addView(note, margins(0, 3, 0, 10));

        ScrollView sv = new ScrollView(this);
        LinearLayout list = column();

        int completed = prefs.getInt("completed", 0);
        boolean teacher = prefs.getBoolean("teacher", false);

        for (int w = 1; w <= Course.TOTAL_WEEKS; w++) {
            int first = (w - 1) * 7 + 1;
            int last = first + 6;
            boolean finished = completed >= last;
            boolean current = completed >= first - 1 && completed < last;
            boolean open = teacher || first <= completed + 1 || finished;

            String prefix = finished ? "✓ " : current ? "→ " : open ? "" : "🔒 ";
            Button b = secondaryButton(prefix + "Неделя " + w + ". " + Course.WEEK_TITLES[w - 1]);
            b.setEnabled(open);

            final int targetLesson;
            if (teacher) {
                targetLesson = first;
            } else if (finished) {
                targetLesson = first;
            } else {
                targetLesson = Math.max(first, Math.min(last, completed + 1));
            }

            b.setOnClickListener(v -> {
                lessonId = targetLesson;
                resetStationState();
                station = 0;
                reviewMode = false;
                screen = SCREEN_LESSON;
                dialog.dismiss();
                render();
            });
            list.addView(b, margins(0, 4, 0, 4));
        }

        sv.addView(list);
        root.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        Button close = primaryButton("Закрыть");
        close.setOnClickListener(v -> dialog.dismiss());
        root.addView(close, margins(0, 10, 0, 0));

        dialog.setContentView(root);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
                .setTitle("Сбросить прогресс?")
                .setMessage("Будут удалены пройденные уроки, серия дней и адаптивная диагностика. Сам курс останется.")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Сбросить", (d, which) -> {
                    prefs.edit().clear().apply();
                    lessonId = 1;
                    station = -1;
                    resetStationState();
                    screen = SCREEN_HOME;
                    render();
                })
                .show();
    }

    private void requestOrStartPitch(int targetMidi) {
        pendingMicTarget = targetMidi;
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startPitchCoach(targetMidi);
        } else {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
        }
    }

    private void startPitchCoach(int targetMidi) {
        pitchMatchRecorded = false;
        if (pitchStatus != null) {
            pitchStatus.setText("Слушаю. Пой спокойный длинный звук…");
            pitchStatus.setTextColor(TEXT);
        }

        pitchDetector.start(targetMidi, new PitchDetector.Listener() {
            @Override
            public void onLevel(double rms) {
                if (pitchLevel != null) pitchLevel.setProgress((int) Math.round(rms * 100));
            }

            @Override
            public void onPitch(double hz, double midi, double centsFromTarget, boolean stableMatch) {
                if (pitchStatus == null) return;

                String direction;
                if (Math.abs(centsFromTarget) < 18) direction = "очень близко";
                else if (centsFromTarget > 0) direction = "чуть выше";
                else direction = "чуть ниже";

                pitchStatus.setText(
                        "Слышу: " + PitchDetector.noteName(midi)
                                + " • " + direction
                                + (stableMatch ? " • совпало" : "")
                );

                if (stableMatch) {
                    pitchStatus.setTextColor(GOOD);
                    if (!pitchMatchRecorded) {
                        skills.record(SkillTracker.Skill.PITCH, 1.0);
                        pitchMatchRecorded = true;
                    }
                    if (screen == SCREEN_LESSON && station == 2) {
                        stationDone[2] = true;
                        savePartial();
                        unlockNavigation();
                    }
                } else {
                    pitchStatus.setTextColor(TEXT);
                }
            }

            @Override
            public void onError(String message) {
                if (pitchStatus != null) {
                    pitchStatus.setText(message + " Вокальное задание всё равно можно выполнить без микрофона.");
                    pitchStatus.setTextColor(WARN);
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_MIC) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startPitchCoach(pendingMicTarget);
            } else if (pitchStatus != null) {
                pitchStatus.setText("Микрофон не разрешён. Это не мешает пройти урок: используй кнопку «Я спел и сравнил».");
                pitchStatus.setTextColor(MUTED);
            }
        }
    }

    private void addSpeakButton(LinearLayout parent, String phrase) {
        Button speak = secondaryButton("🔊 Прочитать задание");
        speak.setOnClickListener(v -> speak(phrase));
        parent.addView(speak, margins(0, 7, 0, 0));
    }

    private void speak(String phrase) {
        if (!ttsReady || tts == null) {
            Toast.makeText(this, "Русский голосовой движок на устройстве недоступен.", Toast.LENGTH_SHORT).show();
            return;
        }
        tts.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "solfege_instruction");
    }

    private boolean ensureAudio(TextView status) {
        if (audio.mediaVolumeIsZero(this)) {
            status.setText("Громкость «Медиа» равна нулю. Увеличь её кнопками телефона.");
            status.setTextColor(WARN);
            return false;
        }
        return true;
    }

    private boolean shouldUseMeasureBuilder(Course.Lesson lesson) {
        if (lesson.week < 9) return true;
        if (lesson.week == 11 || lesson.week == 12 || lesson.week == 20 || lesson.week == 21 || lesson.week == 22 || lesson.week == 23) return true;
        return lesson.day % 2 == 0;
    }

    private double targetBeatsForLesson(Course.Lesson lesson) {
        if (lesson.week == 4 || lesson.week == 11) return 2.0;
        if (lesson.week == 5 || lesson.week == 12) return 3.0;
        if (lesson.week == 20) return 4.0;
        return lesson.week < 5 ? 2.0 : 4.0;
    }

    private double[] allowedDurationsForWeek(int week) {
        if (week <= 2) return new double[]{1.0};
        if (week <= 10) return new double[]{2.0,1.0,0.5};
        if (week <= 21) return new double[]{2.0,1.0,0.5};
        return new double[]{2.0,1.0,0.5,0.25};
    }

    private int meterForWeek(int week) {
        if (week == 4 || week == 11) return 2;
        if (week == 5 || week == 12) return 3;
        if (week == 20) return 4;
        return 0;
    }

    private double rhythmError(List<Long> taps, double[] expected) {
        if (taps.size() < 2 || expected.length < 2) return 0.0;

        int intervals = Math.min(taps.size() - 1, expected.length - 1);
        double actualSum = 0;
        double expectedSum = 0;
        double[] actual = new double[intervals];

        for (int i = 0; i < intervals; i++) {
            actual[i] = taps.get(i + 1) - taps.get(i);
            actualSum += actual[i];
            expectedSum += expected[i];
        }

        if (actualSum <= 0 || expectedSum <= 0) return 1.0;
        double scale = actualSum / expectedSum;
        double error = 0;

        for (int i = 0; i < intervals; i++) {
            double target = expected[i] * scale;
            error += Math.abs(actual[i] - target) / Math.max(1.0, target);
        }

        return error / intervals;
    }

    private void savePartial() {
        if (reviewMode || prefs.getBoolean("teacher", false)) return;
        int completed = prefs.getInt("completed", 0);
        if (lessonId != completed + 1) return;

        prefs.edit()
                .putInt("partialLesson", lessonId)
                .putInt("partialStep", Math.max(0, Math.min(5, station)))
                .apply();
    }

    private void updateStreak(SharedPreferences.Editor editor) {
        LocalDate today = LocalDate.now();
        String lastRaw = prefs.getString("lastDate", "");
        int streak = prefs.getInt("streak", 0);

        if (lastRaw.isEmpty()) {
            streak = 1;
        } else {
            try {
                LocalDate last = LocalDate.parse(lastRaw);
                if (today.equals(last)) {
                    // Keep current streak.
                } else if (today.minusDays(1).equals(last)) {
                    streak++;
                } else {
                    streak = 1;
                }
            } catch (Exception e) {
                streak = 1;
            }
        }

        editor.putInt("streak", streak);
        editor.putString("lastDate", today.toString());
    }

    private void stopLiveTools() {
        rhythmRecording = false;
        audio.stop();
        pitchDetector.stop();
        if (tts != null) tts.stop();
    }

    private void resetStationState() {
        for (int i = 0; i < stationDone.length; i++) stationDone[i] = false;
        earWrongAnswers = 0;
        pitchMatchRecorded = false;
        rhythmTaps.clear();
    }

    private void unlockNavigation() {
        if (navActionButton != null) navActionButton.setEnabled(true);
    }

    private void addTopBack(LinearLayout root, String title, int targetScreen) {
        LinearLayout top = row();
        Button back = secondaryButton("← Назад");
        back.setOnClickListener(v -> {
            stopLiveTools();
            screen = targetScreen;
            render();
        });
        top.addView(back, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView h = text(title, 24, true);
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        hp.setMargins(dp(10), 0, 0, 0);
        top.addView(h, hp);
        root.addView(top);
    }

    private String skillWord(double score, int attempts) {
        if (attempts == 0) return "ещё нет данных";
        if (score < 0.45) return "нужно больше опоры";
        if (score < 0.62) return "развивается";
        if (score < 0.80) return "уверенно";
        return "очень уверенно";
    }

    private String badgeText(int completed) {
        List<String> badges = new ArrayList<>();
        if (completed >= 7) badges.add("• Неделя слуха — пройдена первая неделя.");
        if (completed >= 35) badges.add("• Чувство метра — пройдены первые пять недель.");
        if (completed >= 70) badges.add("• Внутренний слух — десять недель практики.");
        if (completed >= 119) badges.add("• Тональный путешественник — половина курса.");
        if (completed >= 175) badges.add("• Исследователь интервалов.");
        if (completed >= Course.TOTAL_LESSONS) badges.add("• Годовая экспедиция завершена.");
        if (badges.isEmpty()) badges.add("Первая веха откроется после первой недели.");

        StringBuilder sb = new StringBuilder();
        for (String badge : badges) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(badge);
        }
        return sb.toString();
    }

    private String formatBeat(double value) {
        if (Math.abs(value - Math.round(value)) < 0.001) return Integer.toString((int) Math.round(value));
        if (Math.abs(value * 2 - Math.round(value * 2)) < 0.001) {
            int whole = (int) Math.floor(value);
            return whole == 0 ? "½" : whole + "½";
        }
        if (Math.abs(value * 4 - Math.round(value * 4)) < 0.001) {
            int whole = (int) Math.floor(value);
            double fraction = value - whole;
            String f = Math.abs(fraction - 0.25) < 0.001 ? "¼" : "¾";
            return whole == 0 ? f : whole + f;
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private LinearLayout page() {
        LinearLayout root = column();
        root.setPadding(dp(16), dp(18), dp(16), dp(28));
        root.setBackgroundColor(BG);
        return root;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }

    private LinearLayout card() {
        LinearLayout c = column();
        c.setPadding(dp(16), dp(15), dp(16), dp(15));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), LINE);

        c.setBackground(bg);
        c.setElevation(dp(1));
        return c;
    }

    private TextView kicker(String value) {
        TextView t = text(value, 12, true);
        t.setTextColor(ACCENT);
        return t;
    }

    private TextView stepTitle(String number, String value) {
        TextView t = text(number + ". " + value, 20, true);
        t.setTextColor(ACCENT);
        return t;
    }

    private TextView bigInstruction(String value) {
        TextView t = text(value, 18, true);
        t.setLineSpacing(dp(2), 1.12f);
        return t;
    }

    private TextView adaptiveHint(String value) {
        TextView t = text(value, 13, false);
        t.setTextColor(Color.rgb(70, 86, 126));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(241, 244, 253));
        bg.setCornerRadius(dp(12));
        t.setBackground(bg);
        t.setPadding(dp(10), dp(8), dp(10), dp(8));
        return t;
    }

    private TextView statusText(String value) {
        TextView t = text(value, 14, false);
        t.setTextColor(MUTED);
        return t;
    }

    private TextView doneNote(String value) {
        TextView t = text(value, 13, false);
        t.setTextColor(GOOD);
        return t;
    }

    private TextView text(String value, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(TEXT);
        t.setLineSpacing(0, 1.12f);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button primaryButton(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setTextColor(Color.WHITE);
        b.setMinHeight(dp(52));
        b.setBackgroundTintList(ColorStateList.valueOf(ACCENT));
        return b;
    }

    private Button secondaryButton(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setTextColor(Color.rgb(43, 61, 105));
        b.setMinHeight(dp(47));
        b.setBackgroundTintList(ColorStateList.valueOf(ACCENT_LIGHT));
        return b;
    }

    private ProgressBar progressBar(int max, int value) {
        ProgressBar p = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        p.setMax(max);
        p.setProgress(Math.max(0, Math.min(max, value)));
        p.setProgressTintList(ColorStateList.valueOf(ACCENT));
        p.setProgressBackgroundTintList(ColorStateList.valueOf(LINE));
        return p;
    }

    private LinearLayout.LayoutParams margins(int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }

    private LinearLayout.LayoutParams weighted(float weight) {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
