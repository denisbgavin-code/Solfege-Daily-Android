package ru.solfege.daily;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int TOTAL_LESSONS = 238;
    private static final int DAYS_PER_WEEK = 7;
    private static final String PREFS = "solfege_progress";

    private static final String[] THEMES = {
            "Метр и ритм: пульс",
            "Четверти и восьмые",
            "Размер 2/4 и такт",
            "Размер 3/4, гамма, половинная",
            "Мотив и музыкальная фраза",
            "До мажор: тоника и устойчивость",
            "Разрешение и вводные звуки",
            "Дирижирование 2/4 и паузы",
            "Строение мажорной гаммы",
            "Знаки альтерации",
            "Ре мажор",
            "Транспонирование",
            "Дирижирование 3/4",
            "Половинная с точкой",
            "Затакт",
            "Опевание тоники",
            "Опевание III и V ступеней",
            "Соль мажор",
            "Соль мажор: чтение и слух",
            "Размер 4/4 и целая нота",
            "Главные ступени лада",
            "Фа мажор",
            "Фа мажор: чтение и слух",
            "Шестнадцатые: четыре звука на долю",
            "Шестнадцатые в рисунках",
            "Интервал: расстояние между звуками",
            "Прима и октава",
            "Квинта",
            "Секунды",
            "Терции, канон и секвенция",
            "Си-бемоль мажор",
            "Си-бемоль мажор: практика",
            "Одноимённые тональности",
            "Итоговая музыкальная экспедиция"
    };

    private static final String[] FOCUS = {
            "Удерживать ровный внутренний пульс и различать сильную/слабую долю.",
            "Слышать и воспроизводить четверти и пары восьмых.",
            "Чувствовать две доли в такте и понимать функцию тактовой черты.",
            "Чувствовать трёхдольность и соотносить длительность с движением.",
            "Слышать короткую музыкальную мысль и место естественного дыхания.",
            "Узнавать тонику и устойчивые I–III–V ступени в До мажоре.",
            "Слышать тяготение неустойчивых ступеней к устойчивым.",
            "Соединять двудольный жест, пульс и паузы.",
            "Освоить слуховой образ мажорной гаммы до формулы тонов и полутонов.",
            "Слышать повышение и понижение звука на полутон.",
            "Закрепить звуковой образ Ре мажора и его ключевые знаки.",
            "Переносить знакомый мотив на другую высоту, сохраняя отношения.",
            "Соединять трёхдольный метр с дирижёрским жестом.",
            "Удерживать звук или движение на три четвертных доли.",
            "Чувствовать начало до первой сильной доли.",
            "Узнавать соседнее движение вокруг тоники и возвращение к опоре.",
            "Переносить модель опевания на III и V устойчивые ступени.",
            "Закрепить звуковой образ Соль мажора и фа-диез.",
            "Связать слух, ступени и нотные названия в Соль мажоре.",
            "Чувствовать четыре доли и длительность целой ноты.",
            "Слышать I, IV и V как опорные ступени лада.",
            "Закрепить звуковой образ Фа мажора и си-бемоль.",
            "Связать слух, ступени и нотные названия в Фа мажоре.",
            "Ровно делить одну долю на четыре части без ускорения.",
            "Сочетать четверти, восьмые и шестнадцатые в одном пульсе.",
            "Понимать интервал как расстояние между двумя звуками.",
            "Различать повтор звука и октавное перенесение.",
            "Слышать и строить чистую квинту.",
            "Различать шаг на тон и полутон в контексте секунды.",
            "Слышать терцовый ход и узнавать повтор модели на новой ступени.",
            "Познакомиться со звуковым образом Си-бемоль мажора.",
            "Применять материал Си-бемоль мажора в чтении, слухе и импровизации.",
            "Сравнивать мажор и минор с общей тоникой без заучивания ярлыков.",
            "Связать слух, ритм, пение, теорию и короткое творчество."
    };

    private static final String[] ZOLINA = {
            "Ритм: пульс и доля",
            "Ритм: длительности",
            "Ритм: такт и размер",
            "Ритм + начало ладовых представлений",
            "Лад: фраза и мелодическое движение",
            "Тоника, ступени, устойчивость",
            "Неустойчивые ступени и вводные звуки",
            "Ритм: размер и паузы",
            "Строение мажорной гаммы",
            "Ключевые знаки и альтерация",
            "Тональности: Ре мажор",
            "Тональности: перенос модели",
            "Ритм: 3/4",
            "Ритм: длительности",
            "Ритм: затакт",
            "Лад: мелодические обороты",
            "Лад: мелодические обороты",
            "Тональности: Соль мажор",
            "Тональности: Соль мажор",
            "Ритм: 4/4",
            "Лад: опорные ступени",
            "Тональности: Фа мажор",
            "Тональности: Фа мажор",
            "Ритм: шестнадцатые",
            "Ритм: смешанные рисунки",
            "Интервалы",
            "Интервалы",
            "Интервалы",
            "Интервалы",
            "Интервалы + секвенция",
            "Расширение тонального материала",
            "Повторение и закрепление",
            "Сопоставление ладов",
            "Итоговое повторение"
    };

    private SharedPreferences prefs;
    private int currentLessonId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        int completed = prefs.getInt("completed", 0);
        currentLessonId = Math.min(TOTAL_LESSONS, completed + 1);
        render();
    }

    private void render() {
        Lesson lesson = lessonFor(currentLessonId);
        boolean teacher = prefs.getBoolean("teacher", false);
        int completed = prefs.getInt("completed", 0);
        int xp = prefs.getInt("xp", 0);
        int streak = prefs.getInt("streak", 0);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(18), dp(16), dp(28));
        root.setBackgroundColor(Color.rgb(245, 247, 251));
        scroll.addView(root);

        TextView appTitle = text("Сольфеджио: игра каждый день", 24, true);
        root.addView(appTitle);

        TextView stats = text("Прогресс: " + completed + "/" + TOTAL_LESSONS +
                "   •   XP " + xp + "   •   серия " + streak + " дн.", 14, false);
        stats.setTextColor(Color.DKGRAY);
        root.addView(stats, marginParams(0, 4, 0, 12));

        LinearLayout topButtons = new LinearLayout(this);
        topButtons.setOrientation(LinearLayout.HORIZONTAL);
        Button map = button("Карта курса");
        map.setOnClickListener(v -> showCourseMap());
        topButtons.addView(map, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Switch teacherSwitch = new Switch(this);
        teacherSwitch.setText("Преподаватель");
        teacherSwitch.setTextSize(13);
        teacherSwitch.setPadding(dp(10), 0, 0, 0);
        teacherSwitch.setChecked(teacher);
        teacherSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("teacher", isChecked).apply();
            Toast.makeText(this, isChecked ? "Просмотр всех уроков включён" : "Последовательное открытие уроков", Toast.LENGTH_SHORT).show();
            render();
        });
        topButtons.addView(teacherSwitch, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(topButtons, marginParams(0, 0, 0, 12));

        LinearLayout lessonCard = card();
        lessonCard.addView(text("Урок " + lesson.id + " из " + TOTAL_LESSONS +
                "  •  неделя " + lesson.week + ", день " + lesson.day, 13, true));
        TextView theme = text(lesson.theme, 22, true);
        theme.setTextColor(Color.rgb(34, 70, 155));
        lessonCard.addView(theme, marginParams(0, 4, 0, 4));
        lessonCard.addView(text(dayMode(lesson.day), 16, true));
        lessonCard.addView(text(lesson.objective, 15, false), marginParams(0, 6, 0, 8));
        lessonCard.addView(text("Связь с рабочей тетрадью: " + ZOLINA[lesson.week - 1], 13, false));
        root.addView(lessonCard, marginParams(0, 0, 0, 12));

        if (!lesson.reviewText.isEmpty()) {
            LinearLayout review = card();
            review.addView(text("Вспомни без подсказки", 17, true));
            review.addView(text(lesson.reviewText, 14, false), marginParams(0, 6, 0, 0));
            root.addView(review, marginParams(0, 0, 0, 12));
        }

        LinearLayout experience = card();
        experience.addView(text("1. Услышь и почувствуй", 18, true));
        experience.addView(text(lesson.move, 15, false), marginParams(0, 6, 0, 4));
        experience.addView(text(lesson.sing, 15, false), marginParams(0, 0, 0, 8));

        Button listen = button("▶ Слушать пример");
        listen.setOnClickListener(v -> playMelody(lesson));
        experience.addView(listen);

        Button rhythm = button("👏 Эхо-ритм");
        rhythm.setOnClickListener(v -> showRhythmTrainer(lesson));
        experience.addView(rhythm, marginParams(0, 6, 0, 0));
        root.addView(experience, marginParams(0, 0, 0, 12));

        LinearLayout symbols = card();
        symbols.addView(text("2. Назови и свяжи со знаком", 18, true));
        symbols.addView(text(lesson.notation, 15, false), marginParams(0, 6, 0, 8));

        Button notes = button("Показать нотные названия");
        notes.setOnClickListener(v -> showNoteNames(lesson));
        symbols.addView(notes);

        Button quiz = button("🎯 Музыкальный детектив");
        quiz.setOnClickListener(v -> showQuiz(lesson));
        symbols.addView(quiz, marginParams(0, 6, 0, 0));
        root.addView(symbols, marginParams(0, 0, 0, 12));

        LinearLayout creative = card();
        creative.addView(text("3. Примени творчески", 18, true));
        creative.addView(text(lesson.creative, 15, false), marginParams(0, 6, 0, 6));
        creative.addView(text("Домашняя мини-игра: один раз послушай, один раз повтори, один раз вспомни без подсказки. 5–10 минут.", 14, false));
        root.addView(creative, marginParams(0, 0, 0, 12));

        Button complete = button(teacher ? "Режим преподавателя: урок просмотрен" : "✓ Завершить урок и открыть следующий");
        complete.setEnabled(teacher || currentLessonId <= completed + 1);
        complete.setOnClickListener(v -> completeLesson());
        root.addView(complete, marginParams(0, 0, 0, 8));

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        Button prev = button("← Назад");
        prev.setEnabled(currentLessonId > 1);
        prev.setOnClickListener(v -> {
            currentLessonId--;
            render();
        });
        nav.addView(prev, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button next = button("Вперёд →");
        boolean nextAllowed = currentLessonId < TOTAL_LESSONS &&
                (teacher || currentLessonId + 1 <= completed + 1);
        next.setEnabled(nextAllowed);
        next.setOnClickListener(v -> {
            currentLessonId++;
            render();
        });
        nav.addView(next, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(nav);

        TextView parent = text("Для взрослого: не сообщайте правильный ответ сразу. Спросите: «Что ты услышал?», «Как можно проверить?». Ошибка — повод уменьшить задачу, а не ускоряться.", 13, false);
        parent.setTextColor(Color.DKGRAY);
        root.addView(parent, marginParams(0, 14, 0, 0));

        setContentView(scroll);
    }

    private void completeLesson() {
        boolean teacher = prefs.getBoolean("teacher", false);
        if (teacher) {
            Toast.makeText(this, "Режим преподавателя: прогресс ученика не изменён", Toast.LENGTH_SHORT).show();
            if (currentLessonId < TOTAL_LESSONS) {
                currentLessonId++;
                render();
            }
            return;
        }

        int completed = prefs.getInt("completed", 0);
        if (currentLessonId == completed + 1) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("completed", currentLessonId);
            editor.putInt("xp", prefs.getInt("xp", 0) + 20);
            updateStreak(editor);
            editor.apply();
            Toast.makeText(this, "Урок завершён. +20 XP", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Повторение завершено", Toast.LENGTH_SHORT).show();
        }

        if (currentLessonId < TOTAL_LESSONS) currentLessonId++;
        render();
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
                    // keep current streak
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

    private void showCourseMap() {
        Dialog dialog = new Dialog(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));
        root.setBackgroundColor(Color.WHITE);
        root.addView(text("Карта 34 недель", 22, true));

        ScrollView sv = new ScrollView(this);
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        int completed = prefs.getInt("completed", 0);
        boolean teacher = prefs.getBoolean("teacher", false);

        for (int w = 1; w <= 34; w++) {
            int firstLesson = (w - 1) * 7 + 1;
            boolean open = teacher || firstLesson <= completed + 1;
            Button b = button((open ? "" : "🔒 ") + "Неделя " + w + ". " + THEMES[w - 1]);
            b.setEnabled(open);
            final int target = firstLesson;
            b.setOnClickListener(v -> {
                currentLessonId = target;
                dialog.dismiss();
                render();
            });
            list.addView(b, marginParams(0, 4, 0, 4));
        }
        sv.addView(list);
        root.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        Button close = button("Закрыть");
        close.setOnClickListener(v -> dialog.dismiss());
        root.addView(close, marginParams(0, 8, 0, 0));

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    private void showQuiz(Lesson lesson) {
        Quiz q = quizForWeek(lesson.week);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Музыкальный детектив")
                .setMessage(q.question)
                .setNegativeButton("Закрыть", null)
                .create();
        dialog.setOnShowListener(d -> {
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(dp(20), dp(10), dp(20), dp(8));
            for (int i = 0; i < q.options.length; i++) {
                int index = i;
                Button option = button(q.options[i]);
                option.setOnClickListener(v -> {
                    if (index == q.correct) {
                        Toast.makeText(this, "Верно. Объясни ответ своими словами.", Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(this, "Пока нет. Послушай или проделай движение ещё раз и проверь.", Toast.LENGTH_LONG).show();
                    }
                });
                box.addView(option, marginParams(0, 4, 0, 4));
            }
            dialog.setView(box);
        });
        dialog.show();
    }

    private void showNoteNames(Lesson lesson) {
        StringBuilder sb = new StringBuilder();
        String[] names = useFlatNames(lesson.week)
                ? new String[]{"до", "ре♭", "ре", "ми♭", "ми", "фа", "соль♭", "соль", "ля♭", "ля", "си♭", "си"}
                : new String[]{"до", "до♯", "ре", "ре♯", "ми", "фа", "фа♯", "соль", "соль♯", "ля", "ля♯", "си"};
        for (int i = 0; i < lesson.notes.length; i++) {
            if (i > 0) sb.append(" — ");
            sb.append(names[Math.floorMod(lesson.notes[i], 12)]);
        }
        new AlertDialog.Builder(this)
                .setTitle("Проверь после слуховой попытки")
                .setMessage(sb + "\n\nСначала пропой последовательность ещё раз без подсказки. Затем сравни.")
                .setPositiveButton("Готово", null)
                .show();
    }

    private boolean useFlatNames(int week) {
        return week == 22 || week == 23 || week == 31 || week == 32 || week == 33;
    }

    private void showRhythmTrainer(Lesson lesson) {
        Dialog dialog = new Dialog(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(20));
        root.setBackgroundColor(Color.WHITE);

        root.addView(text("Эхо-ритм", 22, true));
        TextView instruction = text("1) Нажми «Послушать ритм». 2) Повтори его кнопкой «ХЛОП». Темп можно выбрать свой — важны отношения длительностей.", 15, false);
        root.addView(instruction, marginParams(0, 6, 0, 12));

        Button demo = button("▶ Послушать ритм");
        demo.setOnClickListener(v -> playRhythmDemo(lesson));
        root.addView(demo);

        TextView status = text("Нужно касаний: " + (lesson.rhythm.length + 1), 14, true);
        status.setGravity(Gravity.CENTER);
        root.addView(status, marginParams(0, 12, 0, 8));

        Button tap = button("ХЛОП");
        tap.setTextSize(24);
        root.addView(tap);

        List<Long> taps = new ArrayList<>();
        tap.setOnClickListener(v -> {
            taps.add(System.currentTimeMillis());
            int need = lesson.rhythm.length + 1;
            status.setText("Касаний: " + taps.size() + "/" + need);
            if (taps.size() == need) {
                double error = rhythmError(taps, lesson.rhythm);
                if (error < 0.18) {
                    status.setText("Очень ровно. Ритмические отношения сохранены.");
                } else if (error < 0.32) {
                    status.setText("Почти получилось. Повтори медленнее, сохраняя пульс.");
                } else {
                    status.setText("Сейчас рисунок изменился. Послушай ещё раз и уменьши темп.");
                }
                taps.clear();
            }
        });

        Button close = button("Закрыть");
        close.setOnClickListener(v -> dialog.dismiss());
        root.addView(close, marginParams(0, 12, 0, 0));

        dialog.setContentView(root);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private double rhythmError(List<Long> taps, double[] expected) {
        double actualSum = 0;
        double expectedSum = 0;
        double[] actual = new double[expected.length];
        for (int i = 0; i < expected.length; i++) {
            actual[i] = taps.get(i + 1) - taps.get(i);
            actualSum += actual[i];
            expectedSum += expected[i];
        }
        double scale = actualSum / Math.max(0.001, expectedSum);
        double err = 0;
        for (int i = 0; i < expected.length; i++) {
            double target = expected[i] * scale;
            err += Math.abs(actual[i] - target) / Math.max(1.0, target);
        }
        return err / expected.length;
    }

    private void playRhythmDemo(Lesson lesson) {
        new Thread(() -> {
            long beatMs = 700;
            for (int i = 0; i <= lesson.rhythm.length; i++) {
                playToneBlocking(1200.0, 55);
                if (i < lesson.rhythm.length) {
                    try {
                        Thread.sleep((long) (beatMs * lesson.rhythm[i]));
                    } catch (InterruptedException ignored) {
                        return;
                    }
                }
            }
        }).start();
    }

    private void playMelody(Lesson lesson) {
        new Thread(() -> {
            for (int midi : lesson.notes) {
                double hz = 440.0 * Math.pow(2.0, (midi - 69) / 12.0);
                playToneBlocking(hz, 330);
                try {
                    Thread.sleep(70);
                } catch (InterruptedException ignored) {
                    return;
                }
            }
        }).start();
    }

    private void playToneBlocking(double frequency, int durationMs) {
        int sampleRate = 44100;
        int samples = Math.max(1, durationMs * sampleRate / 1000);
        short[] data = new short[samples];
        for (int i = 0; i < samples; i++) {
            double envelope = Math.min(1.0, Math.min(i / 300.0, (samples - i) / 500.0));
            data[i] = (short) (Math.sin(2.0 * Math.PI * frequency * i / sampleRate) * 9000 * envelope);
        }

        AudioTrack track = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setBufferSizeInBytes(data.length * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build();
        track.write(data, 0, data.length);
        track.play();
        try {
            Thread.sleep(durationMs + 20L);
        } catch (InterruptedException ignored) {
        }
        track.stop();
        track.release();
    }

    private Lesson lessonFor(int id) {
        int week = ((id - 1) / DAYS_PER_WEEK) + 1;
        int day = ((id - 1) % DAYS_PER_WEEK) + 1;
        int[] notes = melodyFor(week, day);
        double[] rhythm = rhythmFor(week, day);

        String move;
        switch (day) {
            case 1:
                move = "Шагай 8 ровных долей. Первую долю каждого предполагаемого такта отмечай чуть яснее, но не громче.";
                break;
            case 2:
                move = "Покажи пульс ладонями на коленях, а ритм — отдельными хлопками. Два слоя не должны спорить друг с другом.";
                break;
            case 3:
                move = "После прослушивания нарисуй рукой контур мелодии: выше, ниже или на месте. Названия нот пока не нужны.";
                break;
            case 4:
                move = "Переноси вес с ноги на ногу по долям и проговаривай ритм нейтральным слогом «та».";
                break;
            case 5:
                move = "Сделай тихий дирижёрский жест и удерживай внутренний пульс 10 секунд без звучания.";
                break;
            case 6:
                move = "Сначала вспомни вчерашний материал без подсказки. Затем сравни его с сегодняшним примером.";
                break;
            default:
                move = "Выбери движение недели, которое лучше всего помогало слышать метр, и выполни его один раз точно.";
                break;
        }

        String sing;
        if (day <= 2) {
            sing = "После второго прослушивания повтори мелодию голосом на удобном слоге. Не стремись сразу назвать ноты.";
        } else if (day <= 5) {
            sing = "Спой пример сначала на слоге, затем с названиями нот или ступеней, если они уже знакомы.";
        } else {
            sing = "Попробуй начать пение по памяти до нажатия «Слушать», затем проверь себя эталоном.";
        }

        String notation;
        switch (day) {
            case 1:
                notation = "Определи на слух, где мелодия идёт вверх, вниз и где повторяет звук. Затем открой подсказку с названиями.";
                break;
            case 2:
                notation = "Найди самый высокий и самый низкий звук примера. Только после этого сверяй нотные названия.";
                break;
            case 3:
                notation = "Проговори последовательность нот в ритме без пения, затем снова спой её.";
                break;
            case 4:
                notation = "Представь тактовые черты и отметь, на какие звуки приходятся сильные доли.";
                break;
            case 5:
                notation = "Запиши в тетради первые 3–5 звуков услышанной модели и проверь по подсказке.";
                break;
            case 6:
                notation = "Закрой подсказки и восстанови последовательность из памяти. Проверяй только после полной попытки.";
                break;
            default:
                notation = "Выбери один фрагмент недели и запиши его в тетради: высота + ритм. Исправляй только после сравнения.";
                break;
        }

        String creative;
        switch (day) {
            case 1:
                creative = "Придумай короткий ответ из 3–5 звуков, который заканчивается устойчиво.";
                break;
            case 2:
                creative = "Измени только ритм услышанного мотива, сохранив его направление.";
                break;
            case 3:
                creative = "Сочини «музыкальное имя» из четырёх звуков и повтори его дважды одинаково.";
                break;
            case 4:
                creative = "Сделай один музыкальный вопрос и один ответ. Ответ должен звучать завершённее.";
                break;
            case 5:
                creative = "Возьми два элемента недели и соедини их в мини-фразу на 4–8 долей.";
                break;
            case 6:
                creative = "Измени один параметр: высоту начала, ритм или направление. Остальное сохрани.";
                break;
            default:
                creative = "Выбери одно трудное задание недели и исполни один удачный вариант без остановок и самоисправлений.";
                break;
        }

        StringBuilder review = new StringBuilder();
        int[] deltas = {1, 7, 21, 42};
        String[] labels = {"вчера", "неделю назад", "три недели назад", "шесть недель назад"};
        for (int i = 0; i < deltas.length; i++) {
            int prev = id - deltas[i];
            if (prev >= 1) {
                if (review.length() > 0) review.append("\n");
                review.append("• ").append(labels[i]).append(": урок ").append(prev)
                        .append(" — вспомни один признак или короткий фрагмент.");
            }
        }

        return new Lesson(
                id, week, day, THEMES[week - 1],
                FOCUS[week - 1] + " Сегодня задача — " + dayMode(day).toLowerCase(Locale.ROOT) + ".",
                move, sing, notation, creative, notes, rhythm, review.toString()
        );
    }

    private String dayMode(int day) {
        switch (day) {
            case 1: return "Слушай и двигайся";
            case 2: return "Эхо и голос";
            case 3: return "Свяжи звук со знаком";
            case 4: return "Ритм в движении";
            case 5: return "Музыкальная лаборатория";
            case 6: return "Смешанная тренировка";
            default: return "Спокойный контроль без оценки";
        }
    }

    private int[] melodyFor(int week, int day) {
        if (week == 10) return vary(new int[]{60, 61, 60, 59, 60}, day);
        if (week == 27) return vary(new int[]{60, 60, 60, 72}, day);
        if (week == 28) return vary(new int[]{60, 67, 62, 69, 65, 72}, day);
        if (week == 29) return vary(new int[]{60, 61, 60, 62, 64, 65}, day);
        if (week == 30) return vary(new int[]{60, 64, 62, 65, 64, 67}, day);
        if (week == 33) return vary(new int[]{60, 64, 67, 60, 63, 67, 60}, day);

        int root = tonicForWeek(week);
        int[][] degreePatterns = {
                {0, 1, 2, 1, 0},
                {0, 2, 4, 2, 0},
                {0, 1, 2, 3, 4},
                {4, 3, 2, 1, 0},
                {0, 2, 1, 3, 2, 0},
                {4, 2, 3, 1, 0},
                {0, 1, 2, 4, 2, 1, 0}
        };
        int[] major = {0, 2, 4, 5, 7, 9, 11, 12};
        int[] d = degreePatterns[day - 1];
        int[] result = new int[d.length];
        for (int i = 0; i < d.length; i++) result[i] = root + major[d[i]];
        return result;
    }

    private int tonicForWeek(int week) {
        if (week == 11 || week == 12) return 62; // D
        if (week == 18 || week == 19) return 67; // G
        if (week == 22 || week == 23) return 65; // F
        if (week == 31 || week == 32) return 70; // Bb
        return 60; // C
    }

    private int[] vary(int[] base, int day) {
        int[] out = base.clone();
        if (day == 6) {
            for (int i = 0; i < out.length / 2; i++) {
                int t = out[i];
                out[i] = out[out.length - 1 - i];
                out[out.length - 1 - i] = t;
            }
        } else if (day == 5 && out.length > 2) {
            int first = out[0];
            System.arraycopy(out, 1, out, 0, out.length - 1);
            out[out.length - 1] = first;
        }
        return out;
    }

    private double[] rhythmFor(int week, int day) {
        double[] base;
        if (week == 1) base = new double[]{1, 1, 1, 1};
        else if (week == 2) base = new double[]{1, 0.5, 0.5, 1};
        else if (week == 3) base = new double[]{1, 1, 0.5, 0.5};
        else if (week == 4 || week == 13) base = new double[]{1, 1, 1};
        else if (week == 14) base = new double[]{3, 1};
        else if (week == 15) base = new double[]{0.5, 1, 1, 1};
        else if (week == 20) base = new double[]{1, 1, 1, 1};
        else if (week == 24) base = new double[]{0.25, 0.25, 0.25, 0.25, 1};
        else if (week == 25) base = new double[]{1, 0.5, 0.25, 0.25, 1};
        else base = new double[]{1, 1, 0.5, 0.5};

        if (day == 6) {
            double[] rev = new double[base.length];
            for (int i = 0; i < base.length; i++) rev[i] = base[base.length - 1 - i];
            return rev;
        }
        return base;
    }

    private Quiz quizForWeek(int week) {
        switch (week) {
            case 1:
                return new Quiz("Что помогает музыке идти ровно, как шаги?", new String[]{"Пульс", "Ключевой знак", "Интервал"}, 0);
            case 2:
                return new Quiz("Сколько восьмых обычно помещается в одну четверть?", new String[]{"Одна", "Две", "Четыре"}, 1);
            case 3:
                return new Quiz("Что показывает верхняя цифра размера?", new String[]{"Число долей в такте", "Название ноты", "Высоту тоники"}, 0);
            case 4:
            case 13:
                return new Quiz("Сколько долей в размере 3/4?", new String[]{"Две", "Три", "Четыре"}, 1);
            case 5:
                return new Quiz("Что такое музыкальная фраза в этом курсе?", new String[]{"Короткая связная музыкальная мысль", "Любая одна нота", "Только громкий звук"}, 0);
            case 6:
                return new Quiz("Как называется главный устойчивый звук тональности?", new String[]{"Тоника", "Затакт", "Пауза"}, 0);
            case 7:
                return new Quiz("Что обычно хочется сделать с неустойчивым звуком?", new String[]{"Разрешить в устойчивый", "Обязательно ускорить", "Сделать тише"}, 0);
            case 8:
                return new Quiz("Что обозначает пауза?", new String[]{"Молчание определённой длительности", "Повышение звука", "Повтор такта"}, 0);
            case 9:
                return new Quiz("Где в мажорной гамме находятся полутоны?", new String[]{"III–IV и VII–I", "I–II и V–VI", "II–III и IV–V"}, 0);
            case 10:
                return new Quiz("Как диез изменяет звук?", new String[]{"Повышает на полутон", "Понижает на полутон", "Удлиняет вдвое"}, 0);
            case 11:
                return new Quiz("Какие ключевые знаки в Ре мажоре?", new String[]{"Фа♯ и до♯", "Только си♭", "Только фа♯"}, 0);
            case 12:
                return new Quiz("Что сохраняется при транспонировании мотива?", new String[]{"Отношения звуков и ритм", "Абсолютная высота", "Только первая нота"}, 0);
            case 14:
                return new Quiz("Сколько четвертей длится половинная с точкой?", new String[]{"Две", "Три", "Четыре"}, 1);
            case 15:
                return new Quiz("Что такое затакт?", new String[]{"Начало до первой полной сильной доли", "Последний такт", "Любая пауза"}, 0);
            case 16:
            case 17:
                return new Quiz("Что происходит при опевании устойчивой ступени?", new String[]{"Соседние звуки окружают её и возвращаются", "Все звуки одинаковые", "Темп ускоряется"}, 0);
            case 18:
            case 19:
                return new Quiz("Какой ключевой знак в Соль мажоре?", new String[]{"Фа♯", "Си♭", "До♯"}, 0);
            case 20:
                return new Quiz("Сколько четвертных долей в размере 4/4?", new String[]{"Две", "Три", "Четыре"}, 2);
            case 21:
                return new Quiz("Какие ступени называют главными?", new String[]{"I, IV, V", "II, III, VI", "I, II, VII"}, 0);
            case 22:
            case 23:
                return new Quiz("Какой ключевой знак в Фа мажоре?", new String[]{"Си♭", "Фа♯", "До♯"}, 0);
            case 24:
            case 25:
                return new Quiz("Сколько шестнадцатых помещается в одну четверть?", new String[]{"Две", "Три", "Четыре"}, 2);
            case 26:
                return new Quiz("Как называется расстояние между двумя звуками?", new String[]{"Интервал", "Такт", "Регистр"}, 0);
            case 27:
                return new Quiz("Какой интервал охватывает восемь ступеней?", new String[]{"Октава", "Квинта", "Секунда"}, 0);
            case 28:
                return new Quiz("Сколько ступеней охватывает квинта?", new String[]{"Три", "Пять", "Восемь"}, 1);
            case 29:
                return new Quiz("Какой интервал между соседними ступенями?", new String[]{"Секунда", "Терция", "Квинта"}, 0);
            case 30:
                return new Quiz("Сколько ступеней охватывает терция?", new String[]{"Две", "Три", "Пять"}, 1);
            case 31:
            case 32:
                return new Quiz("Какие знаки у Си-бемоль мажора?", new String[]{"Си♭ и ми♭", "Фа♯ и до♯", "Только си♭"}, 0);
            case 33:
                return new Quiz("Что общего у одноимённых мажора и минора?", new String[]{"Одна и та же тоника", "Всегда одинаковые знаки", "Одинаковая III ступень"}, 0);
            default:
                return new Quiz("Что важно в итоговом повторении?", new String[]{"Связать слух, пение, ритм и запись", "Только выучить определения", "Только играть быстро"}, 0);
        }
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackgroundColor(Color.WHITE);
        return card;
    }

    private TextView text(String value, int sp, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(value);
        tv.setTextSize(sp);
        tv.setTextColor(Color.rgb(28, 31, 38));
        tv.setLineSpacing(0, 1.12f);
        if (bold) tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return tv;
    }

    private Button button(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setAllCaps(false);
        b.setTextSize(15);
        return b;
    }

    private LinearLayout.LayoutParams marginParams(int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class Lesson {
        final int id;
        final int week;
        final int day;
        final String theme;
        final String objective;
        final String move;
        final String sing;
        final String notation;
        final String creative;
        final int[] notes;
        final double[] rhythm;
        final String reviewText;

        Lesson(int id, int week, int day, String theme, String objective,
               String move, String sing, String notation, String creative,
               int[] notes, double[] rhythm, String reviewText) {
            this.id = id;
            this.week = week;
            this.day = day;
            this.theme = theme;
            this.objective = objective;
            this.move = move;
            this.sing = sing;
            this.notation = notation;
            this.creative = creative;
            this.notes = notes;
            this.rhythm = rhythm;
            this.reviewText = reviewText;
        }
    }

    private static class Quiz {
        final String question;
        final String[] options;
        final int correct;

        Quiz(String question, String[] options, int correct) {
            this.question = question;
            this.options = options;
            this.correct = correct;
        }
    }
}
