package ru.solfege.daily;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

public class MainActivity extends Activity {
    private static final String PREFS = "solfege_progress_v2";
    private static final int ACCENT = Color.rgb(48, 84, 174);
    private static final int ACCENT_LIGHT = Color.rgb(230, 236, 252);
    private static final int TEXT = Color.rgb(28, 32, 40);
    private static final int MUTED = Color.rgb(91, 98, 112);
    private static final int BG = Color.rgb(246, 248, 252);
    private static final int GOOD = Color.rgb(34, 117, 74);
    private static final int WARN = Color.rgb(172, 92, 24);

    private SharedPreferences prefs;
    private AudioEngine audio;
    private int lessonId;
    private int step = -1;
    private final boolean[] stepDone = new boolean[5];
    private int wrongAnswers = 0;

    private final List<Long> rhythmTaps = new ArrayList<>();
    private boolean rhythmRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        audio = new AudioEngine();

        int completed = prefs.getInt("completed", 0);
        lessonId = Math.min(Course.TOTAL_LESSONS, completed + 1);
        if (prefs.getInt("partialLesson", -1) == lessonId) {
            int savedStep = prefs.getInt("partialStep", -1);
            if (savedStep >= 0 && savedStep < 5) step = savedStep;
        }
        render();
    }

    @Override
    protected void onDestroy() {
        if (audio != null) audio.close();
        super.onDestroy();
    }

    private void render() {
        rhythmRecording = false;
        rhythmTaps.clear();
        wrongAnswers = 0;
        if (step < 0) renderHome();
        else renderLesson();
    }

    private void renderHome() {
        Course.Lesson lesson = Course.lesson(lessonId);
        int completed = prefs.getInt("completed", 0);
        int streak = prefs.getInt("streak", 0);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = column();
        root.setPadding(dp(18), dp(20), dp(18), dp(28));
        scroll.addView(root);
        root.setBackgroundColor(BG);

        TextView title = text("Музыкальная экспедиция", 27, true);
        root.addView(title);

        TextView subtitle = text("Сольфеджио каждый день • 5–10 минут", 14, false);
        subtitle.setTextColor(MUTED);
        root.addView(subtitle, margins(0, 3, 0, 18));

        LinearLayout mission = card();
        TextView eyebrow = text("СЕГОДНЯШНЯЯ МИССИЯ", 12, true);
        eyebrow.setTextColor(ACCENT);
        mission.addView(eyebrow);

        mission.addView(text("Неделя " + lesson.week + " • день " + lesson.day, 14, true), margins(0, 8, 0, 2));
        mission.addView(text(lesson.weekTitle, 23, true), margins(0, 0, 0, 5));
        mission.addView(text(lesson.dayTitle, 17, true));
        TextView goal = text(lesson.goal, 15, false);
        goal.setTextColor(MUTED);
        mission.addView(goal, margins(0, 7, 0, 14));

        ProgressBar total = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        total.setMax(Course.TOTAL_LESSONS);
        total.setProgress(completed);
        total.setProgressTintList(ColorStateList.valueOf(ACCENT));
        total.setProgressBackgroundTintList(ColorStateList.valueOf(Color.rgb(222, 226, 235)));
        mission.addView(total, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8)));

        TextView progress = text(completed + " из " + Course.TOTAL_LESSONS + " уроков • серия " + streak + " дн.", 13, false);
        progress.setTextColor(MUTED);
        mission.addView(progress, margins(0, 7, 0, 10));

        Button start = primaryButton(completed >= lessonId ? "Повторить урок" : (step >= 0 ? "Продолжить" : "Начать"));
        start.setOnClickListener(v -> {
            step = Math.max(0, prefs.getInt("partialLesson", -1) == lessonId ? prefs.getInt("partialStep", 0) : 0);
            for (int i = 0; i < step; i++) stepDone[i] = true;
            render();
        });
        mission.addView(start);
        root.addView(mission);

        LinearLayout sound = card();
        sound.addView(text("Сначала проверь звук", 17, true));
        sound.addView(text("Музыкальные кнопки используют громкость «Медиа». Если ничего не слышно, увеличь её кнопками телефона.", 14, false), margins(0, 5, 0, 10));
        TextView soundStatus = text("", 13, false);
        sound.addView(soundStatus);
        Button test = secondaryButton("▶ Проверить звук: нота до");
        test.setOnClickListener(v -> {
            if (audio.mediaVolumeIsZero(this)) {
                soundStatus.setText("Громкость «Медиа» сейчас равна нулю.");
                soundStatus.setTextColor(WARN);
                return;
            }
            soundStatus.setText("Слушай…");
            soundStatus.setTextColor(MUTED);
            audio.playHomeNote(60, () -> {
                soundStatus.setText("Если услышал ноту — звук работает.");
                soundStatus.setTextColor(GOOD);
            });
        });
        sound.addView(test, margins(0, 8, 0, 0));
        root.addView(sound, margins(0, 12, 0, 0));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);

        Button map = secondaryButton("Карта курса");
        map.setOnClickListener(v -> showCourseMap());
        nav.addView(map, weightedButtonParams(1f));

        Button adult = secondaryButton("Для взрослого");
        adult.setOnClickListener(v -> showAdultInfo());
        LinearLayout.LayoutParams adultP = weightedButtonParams(1f);
        adultP.setMargins(dp(8), 0, 0, 0);
        nav.addView(adult, adultP);
        root.addView(nav, margins(0, 12, 0, 0));

        TextView note = text("В приложении есть весь материал для выполнения заданий. Рабочая тетрадь и учебник для урока не нужны.", 13, false);
        note.setTextColor(MUTED);
        root.addView(note, margins(0, 14, 2, 0));

        setContentView(scroll);
    }

    private void renderLesson() {
        Course.Lesson lesson = Course.lesson(lessonId);
        boolean teacher = prefs.getBoolean("teacher", false);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = column();
        root.setPadding(dp(16), dp(14), dp(16), dp(26));
        root.setBackgroundColor(BG);
        scroll.addView(root);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        Button exit = secondaryButton("← К миссии");
        exit.setOnClickListener(v -> {
            audio.stop();
            step = -1;
            render();
        });
        top.addView(exit, weightedButtonParams(1f));

        TextView count = text((step + 1) + " / 5", 14, true);
        count.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        top.addView(count, weightedButtonParams(1f));
        root.addView(top);

        TextView week = text("Неделя " + lesson.week + " • день " + lesson.day, 13, true);
        week.setTextColor(ACCENT);
        root.addView(week, margins(2, 14, 0, 2));
        root.addView(text(lesson.weekTitle, 21, true));
        TextView day = text(lesson.dayTitle, 15, false);
        day.setTextColor(MUTED);
        root.addView(day, margins(0, 3, 0, 10));

        ProgressBar p = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        p.setMax(5);
        p.setProgress(step + 1);
        p.setProgressTintList(ColorStateList.valueOf(ACCENT));
        p.setProgressBackgroundTintList(ColorStateList.valueOf(Color.rgb(222, 226, 235)));
        root.addView(p, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(7)));

        LinearLayout station = card();
        root.addView(station, margins(0, 13, 0, 10));

        switch (step) {
            case 0:
                renderEarStation(station, lesson);
                break;
            case 1:
                renderRhythmStation(station, lesson);
                break;
            case 2:
                renderVoiceStation(station, lesson);
                break;
            case 3:
                renderNotationStation(station, lesson);
                break;
            default:
                renderCreativeStation(station, lesson);
                break;
        }

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);

        Button back = secondaryButton("← Назад");
        back.setEnabled(step > 0);
        back.setOnClickListener(v -> {
            audio.stop();
            if (step > 0) step--;
            savePartial();
            render();
        });
        nav.addView(back, weightedButtonParams(1f));

        if (step < 4) {
            Button next = primaryButton("Дальше →");
            next.setEnabled(stepDone[step] || teacher);
            next.setOnClickListener(v -> {
                audio.stop();
                stepDone[step] = true;
                step++;
                savePartial();
                render();
            });
            LinearLayout.LayoutParams nextP = weightedButtonParams(1f);
            nextP.setMargins(dp(8), 0, 0, 0);
            nav.addView(next, nextP);
        } else {
            Button finish = primaryButton("Завершить миссию");
            finish.setEnabled(stepDone[4] || teacher);
            finish.setOnClickListener(v -> finishLesson());
            LinearLayout.LayoutParams finishP = weightedButtonParams(1f);
            finishP.setMargins(dp(8), 0, 0, 0);
            nav.addView(finish, finishP);
        }
        root.addView(nav);

        setContentView(scroll);
    }

    private void renderEarStation(LinearLayout box, Course.Lesson lesson) {
        box.addView(stepBadge("1", "Ухо-детектив"));
        box.addView(bigInstruction(lesson.earGame.prompt));

        TextView status = text("Сначала послушай. Отвечать до прослушивания не нужно.", 14, false);
        status.setTextColor(MUTED);
        box.addView(status, margins(0, 8, 0, 9));

        Button play = primaryButton("▶ Слушать");
        play.setOnClickListener(v -> {
            if (audio.mediaVolumeIsZero(this)) {
                status.setText("Увеличь громкость «Медиа» на телефоне.");
                status.setTextColor(WARN);
                return;
            }
            play.setEnabled(false);
            status.setText("Слушай до конца…");
            status.setTextColor(MUTED);
            audio.playEarGame(lesson.earGame, () -> {
                play.setEnabled(true);
                status.setText("Теперь выбери ответ.");
                status.setTextColor(TEXT);
            });
        });
        box.addView(play);

        LinearLayout answers = new LinearLayout(this);
        answers.setOrientation(LinearLayout.VERTICAL);
        box.addView(answers, margins(0, 10, 0, 0));

        for (int i = 0; i < lesson.earGame.options.length; i++) {
            final int answer = i;
            Button option = secondaryButton(lesson.earGame.options[i]);
            option.setOnClickListener(v -> {
                if (answer == lesson.earGame.correct) {
                    status.setText("Верно. " + lesson.earGame.explanation);
                    status.setTextColor(GOOD);
                    stepDone[0] = true;
                    savePartial();
                    renderLesson();
                } else {
                    wrongAnswers++;
                    status.setText(wrongAnswers < 2
                            ? "Не совпало. Послушай ещё раз и сравни."
                            : "Можно не угадывать. Послушай ещё раз медленнее и выбери то, что слышится ближе.");
                    status.setTextColor(WARN);
                    if (wrongAnswers >= 2) {
                        stepDone[0] = true; // ребёнок не блокируется из-за ошибки
                    }
                }
            });
            answers.addView(option, margins(0, 4, 0, 4));
        }

        if (stepDone[0]) {
            TextView hint = text("Шаг засчитан. Правильность важна, но ещё важнее — сравнить звук повторно.", 13, false);
            hint.setTextColor(MUTED);
            box.addView(hint, margins(0, 8, 0, 0));
        }
    }

    private void renderRhythmStation(LinearLayout box, Course.Lesson lesson) {
        box.addView(stepBadge("2", "Пульс и ритм"));
        box.addView(bigInstruction(lesson.movement));

        TextView status = text("Сначала найди ровный пульс.", 14, false);
        status.setTextColor(MUTED);
        box.addView(status, margins(0, 8, 0, 8));

        Button pulse = secondaryButton("▶ Пульс: 8 ровных долей");
        pulse.setOnClickListener(v -> audio.playPulse(8, null));
        box.addView(pulse);

        Button hear = primaryButton("▶ Послушать ритм");
        hear.setOnClickListener(v -> {
            status.setText("Слушай ритм и держи пульс внутри.");
            audio.playRhythm(lesson.rhythm, meterForWeek(lesson.week), () -> status.setText("Теперь повтори."));
        });
        box.addView(hear, margins(0, 7, 0, 0));

        Button begin = secondaryButton("Начать мой ответ");
        box.addView(begin, margins(0, 7, 0, 0));

        Button tap = primaryButton("ХЛОП");
        tap.setTextSize(24);
        tap.setEnabled(false);
        box.addView(tap, margins(0, 7, 0, 0));

        begin.setOnClickListener(v -> {
            rhythmTaps.clear();
            rhythmRecording = true;
            tap.setEnabled(true);
            status.setText("Сделай " + lesson.rhythm.length + " касаний. Первое касание — сразу.");
            status.setTextColor(TEXT);
        });

        tap.setOnClickListener(v -> {
            if (!rhythmRecording) return;
            rhythmTaps.add(System.currentTimeMillis());
            int need = lesson.rhythm.length;
            status.setText("Касаний: " + rhythmTaps.size() + " из " + need);
            if (rhythmTaps.size() >= need) {
                rhythmRecording = false;
                tap.setEnabled(false);
                double error = rhythmError(rhythmTaps, lesson.rhythm);
                if (error < 0.18) {
                    status.setText("Ритмические расстояния получились очень близко к образцу.");
                    status.setTextColor(GOOD);
                } else if (error < 0.34) {
                    status.setText("Почти. Попробуй ещё раз чуть медленнее, но не меняй отношения длительностей.");
                    status.setTextColor(WARN);
                } else {
                    status.setText("Рисунок заметно изменился. Это нормально: послушай снова и повтори в более медленном темпе.");
                    status.setTextColor(WARN);
                }
                stepDone[1] = true;
                savePartial();
            }
        });

        RhythmPatternView rhythmView = new RhythmPatternView(this);
        rhythmView.setDurations(lesson.rhythm);
        box.addView(rhythmView, margins(0, 10, 0, 0));

        TextView label = text("После попытки можно проговорить: " + lesson.rhythmLabel, 14, true);
        label.setTextColor(ACCENT);
        box.addView(label, margins(0, 2, 0, 0));
    }

    private void renderVoiceStation(LinearLayout box, Course.Lesson lesson) {
        box.addView(stepBadge("3", "Голос и внутренний слух"));
        box.addView(bigInstruction(lesson.singing));

        TextView status = text("Приложение не ставит оценку голосу. Сначала слушаем, затем повторяем.", 14, false);
        status.setTextColor(MUTED);
        box.addView(status, margins(0, 8, 0, 10));

        Button melody = primaryButton("▶ Слушать мелодию");
        melody.setOnClickListener(v -> {
            melody.setEnabled(false);
            status.setText("Слушай до конца. Потом выдержи две секунды тишины и начинай петь.");
            audio.playMelody(lesson.melody, () -> {
                melody.setEnabled(true);
                status.setText("Теперь попробуй спеть без записи.");
            });
        });
        box.addView(melody);

        Button startNote = secondaryButton("▶ Только первый звук");
        startNote.setOnClickListener(v -> audio.playHomeNote(lesson.melody[0].midi, null));
        box.addView(startNote, margins(0, 7, 0, 0));

        Button sang = secondaryButton("Я попробовал спеть");
        sang.setOnClickListener(v -> {
            stepDone[2] = true;
            status.setText("Готово. Ещё раз послушай мелодию и сравни только начало, направление и конец.");
            status.setTextColor(GOOD);
            savePartial();
        });
        box.addView(sang, margins(0, 7, 0, 0));
    }

    private void renderNotationStation(LinearLayout box, Course.Lesson lesson) {
        box.addView(stepBadge("4", lesson.showStaff ? "Увидь то, что уже слышал" : "Карта мелодии"));
        box.addView(bigInstruction(
                lesson.showStaff
                        ? "Сначала тихо вспомни мелодию. Потом нажми «Открыть ноты». Ноты появляются ПОСЛЕ слуховой попытки."
                        : "Сначала вспомни, куда шла мелодия. Потом открой её линию: вверх, вниз или на месте."
        ));

        LinearLayout visualHolder = new LinearLayout(this);
        visualHolder.setOrientation(LinearLayout.VERTICAL);
        visualHolder.setVisibility(View.GONE);
        box.addView(visualHolder, margins(0, 8, 0, 8));

        if (lesson.showStaff) {
            TextView key = text("Сегодня: " + lesson.keyName + ". Названия под нотами — фиксированные.", 13, true);
            key.setTextColor(ACCENT);
            visualHolder.addView(key, margins(0, 0, 0, 6));

            StaffView staff = new StaffView(this);
            staff.setNotes(lesson.melody);
            visualHolder.addView(staff);
        } else {
            ContourView contour = new ContourView(this);
            contour.setNotes(lesson.melody);
            visualHolder.addView(contour);
            TextView explanation = text("Каждая точка — один звук. Чем выше точка, тем выше звук.", 13, false);
            explanation.setTextColor(MUTED);
            visualHolder.addView(explanation);
        }

        RhythmPatternView rhythm = new RhythmPatternView(this);
        rhythm.setDurations(lesson.rhythm);
        visualHolder.addView(rhythm, margins(0, 5, 0, 0));

        Button reveal = primaryButton("Открыть");
        reveal.setOnClickListener(v -> {
            boolean nowVisible = visualHolder.getVisibility() != View.VISIBLE;
            visualHolder.setVisibility(nowVisible ? View.VISIBLE : View.GONE);
            reveal.setText(nowVisible ? "Спрятать и вспомнить" : "Открыть снова");
            if (nowVisible) {
                stepDone[3] = true;
                savePartial();
            }
        });
        box.addView(reveal);

        TextView task = text(
                lesson.showStaff
                        ? "Назови вслух первую и последнюю ноту. Потом проведи пальцем по направлению нот слева направо."
                        : "Покажи пальцем: где линия пошла вверх, где вниз, где повторила высоту.",
                14, false
        );
        box.addView(task, margins(0, 9, 0, 0));
    }

    private void renderCreativeStation(LinearLayout box, Course.Lesson lesson) {
        box.addView(stepBadge("5", "Твоя музыка"));
        box.addView(bigInstruction(lesson.creative));

        if (lesson.day == 7 && !lesson.review.isEmpty()) {
            TextView reviewTitle = text("Память без подсказки", 16, true);
            reviewTitle.setTextColor(ACCENT);
            box.addView(reviewTitle, margins(0, 12, 0, 3));
            box.addView(text(lesson.review, 14, false));
        }

        TextView status = text("Здесь нет единственного правильного варианта. Важно выполнить условие задания.", 14, false);
        status.setTextColor(MUTED);
        box.addView(status, margins(0, 10, 0, 8));

        Button start = secondaryButton("▶ Дать стартовый звук");
        start.setOnClickListener(v -> audio.playHomeNote(lesson.melody[0].midi, null));
        box.addView(start);

        Button ready = primaryButton("Я придумал и исполнил");
        ready.setOnClickListener(v -> {
            stepDone[4] = true;
            status.setText("Миссия готова к завершению. Если хочется — исполни свой вариант ещё один раз одинаково.");
            status.setTextColor(GOOD);
            savePartial();
            renderLesson();
        });
        box.addView(ready, margins(0, 8, 0, 0));
    }

    private void finishLesson() {
        boolean teacher = prefs.getBoolean("teacher", false);
        if (teacher) {
            Toast.makeText(this, "Режим преподавателя: прогресс ученика не изменён.", Toast.LENGTH_SHORT).show();
            if (lessonId < Course.TOTAL_LESSONS) lessonId++;
            resetSteps();
            step = -1;
            render();
            return;
        }

        int completed = prefs.getInt("completed", 0);
        if (lessonId == completed + 1) {
            SharedPreferences.Editor e = prefs.edit();
            e.putInt("completed", lessonId);
            e.putInt("xp", prefs.getInt("xp", 0) + 10);
            updateStreak(e);
            e.remove("partialLesson");
            e.remove("partialStep");
            e.apply();
        }

        new AlertDialog.Builder(this)
                .setTitle("Миссия завершена")
                .setMessage("Сегодня ты тренировал слух, ритм, голос, музыкальное чтение и собственный музыкальный ответ. Завтра часть материала вернётся в новой форме.")
                .setPositiveButton("Готово", (d, which) -> {
                    if (lessonId < Course.TOTAL_LESSONS) lessonId++;
                    resetSteps();
                    step = -1;
                    render();
                })
                .show();
    }

    private void savePartial() {
        if (prefs.getBoolean("teacher", false)) return;
        int completed = prefs.getInt("completed", 0);
        if (lessonId != completed + 1) return;
        prefs.edit()
                .putInt("partialLesson", lessonId)
                .putInt("partialStep", Math.max(0, Math.min(4, step)))
                .apply();
    }

    private void resetSteps() {
        for (int i = 0; i < stepDone.length; i++) stepDone[i] = false;
    }

    private void showCourseMap() {
        Dialog dialog = new Dialog(this);
        LinearLayout root = column();
        root.setPadding(dp(16), dp(16), dp(16), dp(18));
        root.setBackgroundColor(Color.WHITE);

        root.addView(text("Карта года", 24, true));
        TextView sub = text("34 недели. Учебники используются как ориентир покрытия тем, а не как сценарий курса.", 13, false);
        sub.setTextColor(MUTED);
        root.addView(sub, margins(0, 3, 0, 10));

        ScrollView sv = new ScrollView(this);
        LinearLayout list = column();
        int completed = prefs.getInt("completed", 0);
        boolean teacher = prefs.getBoolean("teacher", false);

        for (int w = 1; w <= Course.TOTAL_WEEKS; w++) {
            int first = (w - 1) * Course.DAYS_PER_WEEK + 1;
            int last = first + 6;
            boolean finished = completed >= last;
            boolean current = completed >= first - 1 && completed < last;
            boolean open = teacher || first <= completed + 1 || finished;

            String prefix = finished ? "✓ " : (current ? "→ " : (open ? "" : "🔒 "));
            Button b = secondaryButton(prefix + "Неделя " + w + ". " + Course.WEEK_TITLES[w - 1]);
            b.setEnabled(open);
            final int target = finished ? first : Math.max(first, Math.min(last, completed + 1));
            b.setOnClickListener(v -> {
                lessonId = target;
                resetSteps();
                step = -1;
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

    private void showAdultInfo() {
        ScrollView sv = new ScrollView(this);
        LinearLayout root = column();
        root.setPadding(dp(18), dp(16), dp(18), dp(18));
        root.setBackgroundColor(Color.WHITE);

        root.addView(text("Для взрослого", 24, true));
        root.addView(text(Course.adultSummary(), 14, false), margins(0, 8, 0, 12));

        TextView copyright = text(
                "Материал внутри приложения самостоятельный. Темы и общие музыкально-теоретические понятия сопоставлены с программой первого класса, "
                        + "но защищённые авторским правом упражнения из пособий не копируются. Вместо внешних ссылок ребёнок получает оригинальное эквивалентное задание прямо в приложении.",
                13, false
        );
        copyright.setTextColor(MUTED);
        root.addView(copyright);

        Switch teacher = new Switch(this);
        teacher.setText("Режим преподавателя: открыть весь курс для просмотра");
        teacher.setTextSize(14);
        teacher.setChecked(prefs.getBoolean("teacher", false));
        teacher.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("teacher", isChecked).apply();
            Toast.makeText(this, isChecked ? "Весь курс открыт для просмотра." : "Вернулась последовательная траектория.", Toast.LENGTH_SHORT).show();
        });
        root.addView(teacher, margins(0, 14, 0, 8));

        new AlertDialog.Builder(this)
                .setView(sv)
                .setPositiveButton("Закрыть", null)
                .show();
    }

    private double rhythmError(List<Long> taps, double[] expectedDurations) {
        if (taps.size() < 2 || expectedDurations.length < 2) return 0.0;
        int intervals = Math.min(taps.size() - 1, expectedDurations.length - 1);
        double actualSum = 0;
        double expectedSum = 0;
        double[] actual = new double[intervals];

        for (int i = 0; i < intervals; i++) {
            actual[i] = taps.get(i + 1) - taps.get(i);
            actualSum += actual[i];
            expectedSum += expectedDurations[i];
        }
        if (actualSum <= 0 || expectedSum <= 0) return 1.0;

        double scale = actualSum / expectedSum;
        double err = 0;
        for (int i = 0; i < intervals; i++) {
            double target = expectedDurations[i] * scale;
            err += Math.abs(actual[i] - target) / Math.max(1.0, target);
        }
        return err / intervals;
    }

    private int meterForWeek(int week) {
        if (week == 4 || week == 11) return 2;
        if (week == 5 || week == 12) return 3;
        if (week == 20) return 4;
        return 0;
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
                    // keep
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

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout card() {
        LinearLayout c = column();
        c.setPadding(dp(16), dp(15), dp(16), dp(15));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), Color.rgb(229, 232, 239));
        c.setBackground(bg);
        c.setElevation(dp(1));
        return c;
    }

    private TextView stepBadge(String number, String label) {
        TextView t = text(number + ". " + label, 19, true);
        t.setTextColor(ACCENT);
        return t;
    }

    private TextView bigInstruction(String value) {
        TextView t = text(value, 18, true);
        t.setLineSpacing(dp(2), 1.12f);
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
        b.setTextColor(Color.rgb(42, 61, 105));
        b.setMinHeight(dp(48));
        b.setBackgroundTintList(ColorStateList.valueOf(ACCENT_LIGHT));
        return b;
    }

    private LinearLayout.LayoutParams margins(int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }

    private LinearLayout.LayoutParams weightedButtonParams(float weight) {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
