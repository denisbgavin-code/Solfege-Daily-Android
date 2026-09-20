package ru.solfege.daily;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Course {
    public static final int TOTAL_WEEKS = 34;
    public static final int DAYS_PER_WEEK = 7;
    public static final int TOTAL_LESSONS = TOTAL_WEEKS * DAYS_PER_WEEK;

    public static final String[] WEEK_TITLES = {
            "Ровный пульс",
            "Музыкальное эхо",
            "Длинный и короткий",
            "Два шага: размер 2/4",
            "Три шага: размер 3/4",
            "Звук-дом: тоника",
            "Опоры: 1–3–5",
            "Звук тянется домой",
            "До мажор на нотном стане",
            "Пауза и внутренний слух",
            "Читаем ритм 2/4",
            "Три доли и половинная с точкой",
            "Фраза и музыкальное дыхание",
            "Мажорная лестница",
            "Полутон, диез и бемоль",
            "Соль мажор",
            "Фа мажор",
            "Ре мажор",
            "Один рисунок — разные высоты",
            "Четыре доли: размер 4/4",
            "Затакт",
            "Четыре быстрых: шестнадцатые",
            "Смешиваем ритмы",
            "Главные опоры: I, IV, V",
            "Интервал — расстояние",
            "Секунда",
            "Терция",
            "Квинта",
            "Октава",
            "Секвенция и канон",
            "Си-бемоль мажор",
            "Си-бемоль: практика",
            "Один дом — разное настроение",
            "Большая музыкальная миссия"
    };

    public static final String[] WEEK_GOALS = {
            "Слышать ровные доли и различать выше, ниже и тот же звук.",
            "Точно повторять короткие звуковые и ритмические модели.",
            "Слышать одну долгую долю и деление доли на две равные части.",
            "Чувствовать сильную долю через каждые две доли.",
            "Чувствовать сильную долю через каждые три доли.",
            "Слышать тонику как точку покоя — «дом» мелодии.",
            "Узнавать устойчивые ступени 1, 3 и 5 на слух и голосом.",
            "Слышать тяготение соседних ступеней к устойчивым.",
            "Связать знакомое звучание До мажора с нотами до–ре–ми–фа–соль–ля–си.",
            "Сохранять музыку во внутреннем слухе во время короткой паузы.",
            "Слышать, хлопать и читать короткие ритмы в размере 2/4.",
            "Удерживать трёхдольность и длительность в три четверти.",
            "Слышать начало и окончание короткой музыкальной мысли.",
            "Пропевать мажорную гамму и понимать расположение тонов и полутонов.",
            "Слышать движение на полутон и понимать, зачем нужны ♯ и ♭.",
            "Слышать и петь Соль мажор; узнавать фа-диез.",
            "Слышать и петь Фа мажор; узнавать си-бемоль.",
            "Слышать и петь Ре мажор; узнавать фа-диез и до-диез.",
            "Переносить знакомый рисунок от другой высоты, сохраняя его устройство.",
            "Чувствовать четыре доли и целую длительность.",
            "Слышать начало до первой сильной доли.",
            "Ровно делить одну долю на четыре части.",
            "Переключаться между четвертями, восьмыми и шестнадцатыми без потери пульса.",
            "Слышать ступени I, IV и V как главные опоры лада.",
            "Сравнивать расстояние между двумя звуками.",
            "Узнавать шаг на соседнюю ступень — секунду.",
            "Узнавать движение через одну ступень — терцию.",
            "Узнавать широкое устойчивое звучание квинты.",
            "Узнавать тот же звук в другом регистре — октаву.",
            "Повторять модель от новой ступени и удерживать свой голос в простом каноне.",
            "Освоить звуковой образ Си-бемоль мажора и два бемоля.",
            "Применять Си-бемоль мажор в слухе, пении, ритме и чтении.",
            "Сравнивать мажор и минор с одной тоникой по звучанию, а не по ярлыку.",
            "Соединить слух, пульс, пение, чтение, интервалы и творчество."
    };

    private static final KeySpec C_MAJOR = new KeySpec(
            "До мажор", 60, 0,
            new int[]{0,2,4,5,7,9,11,12},
            new String[]{"до","ре","ми","фа","соль","ля","си","до"},
            new String[]{"","","","","","","",""}
    );
    private static final KeySpec G_MAJOR = new KeySpec(
            "Соль мажор", 67, 4,
            new int[]{0,2,4,5,7,9,11,12},
            new String[]{"соль","ля","си","до","ре","ми","фа♯","соль"},
            new String[]{"","","","","","","♯",""}
    );
    private static final KeySpec F_MAJOR = new KeySpec(
            "Фа мажор", 65, 3,
            new int[]{0,2,4,5,7,9,11,12},
            new String[]{"фа","соль","ля","си♭","до","ре","ми","фа"},
            new String[]{"","","","♭","","","",""}
    );
    private static final KeySpec D_MAJOR = new KeySpec(
            "Ре мажор", 62, 1,
            new int[]{0,2,4,5,7,9,11,12},
            new String[]{"ре","ми","фа♯","соль","ля","си","до♯","ре"},
            new String[]{"","","♯","","","","♯",""}
    );
    private static final KeySpec BB_MAJOR = new KeySpec(
            "Си-бемоль мажор", 70, 6,
            new int[]{0,2,4,5,7,9,11,12},
            new String[]{"си♭","до","ре","ми♭","фа","соль","ля","си♭"},
            new String[]{"♭","","","♭","","","","♭"}
    );
    private static final KeySpec C_MINOR = new KeySpec(
            "До минор", 60, 0,
            new int[]{0,2,3,5,7,8,10,12},
            new String[]{"до","ре","ми♭","фа","соль","ля♭","си♭","до"},
            new String[]{"","","♭","","","♭","♭",""}
    );

    private static final int[][] DEGREE_PATTERNS = {
            {0,1,2,1,0},
            {0,2,4,2,0},
            {0,1,2,3,2,1,0},
            {4,3,2,1,0},
            {0,2,1,3,2,0},
            {0,4,3,2,1,0},
            {0,1,2,4,2,1,0}
    };

    private Course() {}

    public static Lesson lesson(int id) {
        int safeId = Math.max(1, Math.min(TOTAL_LESSONS, id));
        int week = ((safeId - 1) / DAYS_PER_WEEK) + 1;
        int day = ((safeId - 1) % DAYS_PER_WEEK) + 1;
        KeySpec key = keyFor(week, day);
        Note[] melody = melodyFor(week, day, key);
        double[] rhythm = rhythmFor(week, day);
        EarGame ear = earGameFor(week, day, key, melody);
        String move = movementFor(week, day);
        String sing = singingFor(week, day, key, melody);
        String create = creativeFor(week, day, key);
        String rhythmLabel = rhythmLabel(rhythm, week);
        String review = reviewFor(safeId);
        boolean showStaff = week >= 9;

        return new Lesson(
                safeId,
                week,
                day,
                "День " + day + ". " + dayTitle(day),
                WEEK_TITLES[week - 1],
                WEEK_GOALS[week - 1],
                ear,
                move,
                sing,
                create,
                melody,
                rhythm,
                rhythmLabel,
                key.name,
                showStaff,
                review
        );
    }

    public static String dayTitle(int day) {
        switch (day) {
            case 1: return "Сначала услышь";
            case 2: return "Повтори точно";
            case 3: return "Узнай знакомое";
            case 4: return "Разберись с новым";
            case 5: return "Увидь музыку";
            case 6: return "Придумай своё";
            default: return "Вспомни без подсказки";
        }
    }

    public static String adultSummary() {
        return "Курс построен не по одному учебнику. Он соединяет несколько сильных педагогических идей:\n\n"
                + "• сначала звук и действие, потом термин и нотный знак — aural-before-symbol (Conversational Solfege, Gordon, Kodály);\n"
                + "• движение и телесный пульс — Dalcroze и Orff-Schulwerk;\n"
                + "• регулярное пение и внутреннее слышание — Kodály и Music Learning Theory;\n"
                + "• импровизация и музыкальный ответ ребёнка — Orff и Feierabend;\n"
                + "• короткое извлечение из памяти через интервалы времени — retrieval/spacing;\n"
                + "• фиксированные названия нот используются для чтения, а номера ступеней — для функции внутри тональности.\n\n"
                + "Темы первого года российских ДМШ учтены как контроль покрытия: размеры 2/4, 3/4, 4/4, основные длительности, "
                + "До/Соль/Фа/Ре/Си-бемоль мажор, ступени, устойчивость, гамма, затакт, шестнадцатые, интервалы и простые мелодические обороты. "
                + "Но образовательная траектория определяется развитием слухового действия, а не порядком страниц конкретного пособия.";
    }

    private static KeySpec keyFor(int week, int day) {
        if (week == 16) return G_MAJOR;
        if (week == 17) return F_MAJOR;
        if (week == 18) return D_MAJOR;
        if (week == 31 || week == 32) return BB_MAJOR;
        if (week == 33 && day % 2 == 0) return C_MINOR;
        if (week == 34) {
            switch ((day - 1) % 5) {
                case 1: return G_MAJOR;
                case 2: return F_MAJOR;
                case 3: return D_MAJOR;
                case 4: return BB_MAJOR;
                default: return C_MAJOR;
            }
        }
        return C_MAJOR;
    }

    private static Note[] melodyFor(int week, int day, KeySpec key) {
        if (week == 1) {
            return new Note[]{
                    rawNote(60,0,"до",""),
                    rawNote(day % 2 == 0 ? 64 : 67, day % 2 == 0 ? 2 : 4, day % 2 == 0 ? "ми" : "соль","")
            };
        }
        if (week == 2) {
            int[] p = day % 2 == 0 ? new int[]{0,2,0,2} : new int[]{0,1,2,1};
            return fromDegrees(key, p);
        }
        if (week == 6) {
            int[] p = day % 2 == 0 ? new int[]{0,2,4,1} : new int[]{0,2,4,0};
            return fromDegrees(key, p);
        }
        if (week == 7) {
            return fromDegrees(key, new int[]{0,2,4,2,0});
        }
        if (week == 8) {
            int[][] p = {
                    {1,0},{6,7},{3,2},{5,4},{1,2,0},{6,5,4},{3,2,1,0}
            };
            return fromDegrees(key, p[day - 1]);
        }
        if (week == 15) {
            if (day % 2 == 1) {
                return new Note[]{
                        rawNote(60,0,"до",""),
                        rawNote(61,0,"до♯","♯"),
                        rawNote(62,1,"ре","")
                };
            } else {
                return new Note[]{
                        rawNote(64,2,"ми",""),
                        rawNote(63,2,"ми♭","♭"),
                        rawNote(62,1,"ре","")
                };
            }
        }
        if (week == 25) {
            int span = Math.min(7, day);
            return fromDegrees(C_MAJOR, new int[]{0, span});
        }
        if (week == 26) {
            return fromDegrees(key, day % 2 == 0 ? new int[]{2,1,0} : new int[]{0,1,2});
        }
        if (week == 27) {
            return fromDegrees(key, day % 2 == 0 ? new int[]{4,2,0} : new int[]{0,2,4});
        }
        if (week == 28) {
            return fromDegrees(key, day % 2 == 0 ? new int[]{4,0,4} : new int[]{0,4,0});
        }
        if (week == 29) {
            return fromDegrees(key, day % 2 == 0 ? new int[]{7,0,7} : new int[]{0,7,0});
        }
        if (week == 30) {
            int[] base = day <= 3 ? new int[]{0,1,2} : new int[]{1,2,3};
            Note[] a = fromDegrees(key, base);
            Note[] b = fromDegrees(key, new int[]{base[0]+1, base[1]+1, base[2]+1});
            return concat(a, b);
        }
        if (week == 33) {
            int[] p = DEGREE_PATTERNS[(day - 1) % DEGREE_PATTERNS.length];
            return fromDegrees(key, p);
        }

        int[] p = DEGREE_PATTERNS[(day - 1) % DEGREE_PATTERNS.length];
        return fromDegrees(key, p);
    }

    private static EarGame earGameFor(int week, int day, KeySpec key, Note[] melody) {
        if (week == 1) {
            boolean up = melody[melody.length - 1].midi > melody[0].midi;
            return EarGame.melody(
                    "Нажми «Слушать». Услышишь два звука. Второй звук выше или ниже первого?",
                    midi(melody),
                    null,
                    new String[]{"выше", "ниже"},
                    up ? 0 : 1,
                    "Представь два этажа: второй звук оказался " + (up ? "выше" : "ниже") + "."
            );
        }

        if (week == 2) {
            Note[] second = day % 3 == 0
                    ? fromDegrees(key, new int[]{0,2,1,2})
                    : cloneNotes(melody);
            boolean same = sameMidi(melody, second);
            return EarGame.melody(
                    "Сейчас прозвучат два коротких рисунка. Они одинаковые или разные?",
                    midi(melody),
                    midi(second),
                    new String[]{"одинаковые", "разные"},
                    same ? 0 : 1,
                    same ? "Да. Рисунок повторился точно." : "Да. Во втором рисунке одна нота изменилась."
            );
        }

        if (week == 3 || week == 11) {
            double[] a = day % 2 == 0 ? new double[]{0.5,0.5,1} : new double[]{1,0.5,0.5};
            return EarGame.rhythm(
                    "Слушай щелчки. Где были два быстрых звука: в начале или в конце?",
                    a,
                    null,
                    new String[]{"в начале", "в конце"},
                    day % 2 == 0 ? 0 : 1,
                    "Две восьмые делят одну долю на две равные части."
            );
        }

        if (week == 4 || week == 5 || week == 20) {
            int meter = week == 4 ? 2 : (week == 5 ? 3 : 4);
            double[] accented = accentedMeter(meter, 3);
            return EarGame.rhythm(
                    "Слушай сильный щелчок и тихие щелчки после него. Через сколько долей возвращается сильный?",
                    accented,
                    null,
                    meter == 2 ? new String[]{"через 2", "через 3", "через 4"} :
                            meter == 3 ? new String[]{"через 2", "через 3", "через 4"} :
                                    new String[]{"через 2", "через 3", "через 4"},
                    meter - 2,
                    "Это размер " + meter + "/4: в такте " + meter + " доли."
            ).withMeter(meter);
        }

        if (week == 6 || week == 8 || week == 24) {
            boolean home = melody[melody.length - 1].midi % 12 == key.rootMidi % 12;
            return EarGame.melody(
                    "Послушай конец мелодии. Он остановился «дома» или будто просит продолжения?",
                    midi(melody),
                    null,
                    new String[]{"дома", "просит продолжения"},
                    home ? 0 : 1,
                    home ? "Последний звук — тоника. На ней легко остановиться." : "Последний звук не тоника, поэтому хочется продолжения."
            );
        }

        if (week == 7) {
            int lastDegree = day % 3 == 0 ? 1 : (day % 3 == 1 ? 0 : 4);
            Note[] test = fromDegrees(key, new int[]{0,2,lastDegree});
            boolean stable = lastDegree == 0 || lastDegree == 2 || lastDegree == 4;
            return EarGame.melody(
                    "Последний звук устойчивый или неустойчивый?",
                    midi(test),
                    null,
                    new String[]{"устойчивый", "неустойчивый"},
                    stable ? 0 : 1,
                    stable ? "Да. Это одна из опор: 1, 3 или 5." : "Да. Этот звук тянется к ближайшей опоре."
            );
        }

        if (week == 10) {
            int[] a = midi(melody);
            int[] b = a.clone();
            if (b.length > 2 && day % 2 == 0) b[b.length - 2] += 2;
            boolean same = Arrays.equals(a, b);
            return EarGame.melody(
                    "Первый рисунок прозвучит, потом будет тишина, затем второй. После паузы рисунок сохранился?",
                    a,
                    b,
                    new String[]{"да, тот же", "нет, изменился"},
                    same ? 0 : 1,
                    "Тишина тоже часть задания: музыку можно удерживать внутри."
            ).withLongGap(true);
        }

        if (week == 12) {
            return EarGame.rhythm(
                    "Слушай. Один звук длится три доли. Как называется такая длительность?",
                    new double[]{3,1},
                    null,
                    new String[]{"половинная", "половинная с точкой", "целая"},
                    1,
                    "Половинная с точкой длится три четвертных доли."
            );
        }

        if (week == 13) {
            Note[] first = fromDegrees(key, new int[]{0,1,2,1});
            Note[] second = fromDegrees(key, new int[]{0,1,2,0});
            boolean closed = day % 2 == 0;
            return EarGame.melody(
                    "Какая фраза звучит завершённее — первая или вторая?",
                    midi(first),
                    midi(second),
                    new String[]{"первая", "вторая"},
                    1,
                    "Вторая заканчивается на тонике, поэтому ощущается завершённее."
            );
        }

        if (week == 14 || week == 16 || week == 17 || week == 18 || week == 31 || week == 32 || week == 34) {
            int[] scale = midi(fromDegrees(key, new int[]{0,1,2,3,4,5,6,7}));
            return EarGame.melody(
                    "Мелодия идёт по ступеням вверх или скачками?",
                    scale,
                    null,
                    new String[]{"по соседним ступеням", "скачками"},
                    0,
                    "Гамма идёт по соседним ступеням от тоники до тоники."
            );
        }

        if (week == 15) {
            return EarGame.melody(
                    "Между соседними звуками здесь очень маленький шаг. Это тон или полутон?",
                    midi(melody),
                    null,
                    new String[]{"тон", "полутон"},
                    1,
                    "Полутон — самый маленький шаг в привычной клавиатуре между соседними клавишами."
            );
        }

        if (week == 19) {
            Note[] second = transposeDiatonic(melody, 2);
            return EarGame.melody(
                    "Два рисунка начинаются с разных звуков. Их форма сохранилась?",
                    midi(melody),
                    midi(second),
                    new String[]{"да", "нет"},
                    0,
                    "Да. Это один и тот же рисунок, перенесённый выше."
            );
        }

        if (week == 21) {
            return EarGame.rhythm(
                    "Первый короткий звук прозвучит ДО сильной доли. Как называется такое начало?",
                    new double[]{0.5,1,1,1},
                    null,
                    new String[]{"затакт", "пауза", "октава"},
                    0,
                    "Затакт начинается до первой полной сильной доли."
            );
        }

        if (week == 22 || week == 23) {
            double[] pattern = week == 22
                    ? new double[]{0.25,0.25,0.25,0.25,1}
                    : new double[]{1,0.5,0.5,0.25,0.25,0.25,0.25};
            return EarGame.rhythm(
                    "Сколько быстрых шестнадцатых помещается в одну четвертную долю?",
                    pattern,
                    null,
                    new String[]{"2", "3", "4"},
                    2,
                    "Четыре шестнадцатых занимают столько же времени, сколько одна четверть."
            );
        }

        if (week >= 25 && week <= 29) {
            String interval = week == 25 ? "интервал"
                    : week == 26 ? "секунда"
                    : week == 27 ? "терция"
                    : week == 28 ? "квинта" : "октава";
            int correct = week == 25 ? 0 : week - 26;
            String[] opts;
            if (week == 25) {
                opts = new String[]{"расстояние между звуками", "скорость музыки", "сильная доля"};
                correct = 0;
            } else {
                opts = new String[]{"секунда","терция","квинта","октава"};
                correct = week == 26 ? 0 : week == 27 ? 1 : week == 28 ? 2 : 3;
            }
            return EarGame.melody(
                    week == 25 ? "Послушай два звука. Что в музыке называют интервалом?"
                            : "Послушай расстояние между первым и вторым звуком. Как называется этот интервал?",
                    midi(melody),
                    null,
                    opts,
                    correct,
                    week == 25 ? "Интервал — расстояние по высоте между двумя звуками." : "Это " + interval + "."
            );
        }

        if (week == 30) {
            return EarGame.melody(
                    "Вторая половина рисунка повторяет форму первой, но начинается выше. Это похоже на что?",
                    midi(melody),
                    null,
                    new String[]{"секвенцию", "паузу", "затакт"},
                    0,
                    "Секвенция повторяет один рисунок от другой высоты."
            );
        }

        if (week == 33) {
            boolean minor = key == C_MINOR;
            return EarGame.melody(
                    "Обе мелодии могут начинаться от до. Эта звучит светлее или темнее?",
                    midi(melody),
                    null,
                    new String[]{"светлее", "темнее"},
                    minor ? 1 : 0,
                    "Мы не ставим оценку настроению. Важно услышать, что при той же тонике изменилось устройство лада."
            );
        }

        return EarGame.melody(
                "Послушай мелодию. Она заканчивается на звуке «дом»?",
                midi(melody),
                null,
                new String[]{"да", "нет"},
                melody[melody.length - 1].midi % 12 == key.rootMidi % 12 ? 0 : 1,
                "Проверь конец ещё раз на слух."
        );
    }

    private static String movementFor(int week, int day) {
        if (week == 1) return "Встань или сядь ровно. Нажми «Пульс» и сделай 8 одинаковых шагов или касаний ладонью. Не ускоряйся.";
        if (week == 2) return "Сначала четыре ровных касания коленей, потом повтори ритм хлопками. Пульс внутри должен остаться ровным.";
        if (week == 3 || week == 11) return "На четверть сделай один широкий жест. На две восьмые — два маленьких жеста за то же время.";
        if (week == 4) return "Считай движением: СИЛЬНО–тихо, СИЛЬНО–тихо. Сделай четыре такта.";
        if (week == 5 || week == 12) return "Считай движением: СИЛЬНО–тихо–тихо. Сделай четыре такта.";
        if (week == 20) return "Считай: СИЛЬНО–тихо–средне–тихо. Не делай сильную долю резким ударом.";
        if (week == 21) return "Сделай маленький подготовительный жест перед первой сильной долей, затем шагни на сильную.";
        if (week == 22 || week == 23) return "Держи ногой ровную долю, а пальцами дели одну долю на четыре одинаковых быстрых касания.";
        if (week >= 25 && week <= 29) return "Покажи расстояние руками: близко для маленького интервала, шире для большого. Потом послушай ещё раз.";
        return day % 2 == 0
                ? "Покажи рукой линию мелодии: рука идёт выше, ниже или остаётся на месте вместе со звуком."
                : "Отмечай ровный пульс ладонью, пока звучит мелодия. Не хлопай сам ритм — только доли.";
    }

    private static String singingFor(int week, int day, KeySpec key, Note[] melody) {
        if (week <= 2) {
            return "Нажми «Слушать мелодию». Потом повтори её голосом на слог «лу». Если неудобно высоко — пой тише, а не сильнее.";
        }
        if (week <= 5) {
            return "Сначала пропой на «лу», затем простучи пульс и спой ещё раз. Голос и пульс должны закончить вместе.";
        }
        if (week == 6) {
            return "После мелодии спой только последний звук и задержи его на две секунды. Это наш звук-дом — тоника.";
        }
        if (week == 7 || week == 8 || week == 24) {
            return "Спой рисунок сначала на «лу», затем номерами ступеней. Номер 1 — тоника. Не называй ноту, пока не услышал функцию.";
        }
        if (week >= 9) {
            return "1) Послушай. 2) Спой на «лу». 3) Только потом спой названиями нот. Тональность сегодня: " + key.name + ".";
        }
        return "Послушай и повтори голосом.";
    }

    private static String creativeFor(int week, int day, KeySpec key) {
        if (week == 1) return "Сделай голосом два звука: сначала низкий, потом высокий. Затем наоборот.";
        if (week == 2) return "Придумай своё эхо из 3 звуков. Повтори его второй раз точно так же.";
        if (week == 3) return "Собери 4 доли из четвертей и пар восьмых. Главное правило: общий пульс не меняется.";
        if (week == 4) return "Придумай ритм на два такта 2/4 и прохлопай его два раза одинаково.";
        if (week == 5) return "Придумай ритм на два такта 3/4. Первая доля каждого такта должна хорошо чувствоваться.";
        if (week == 6) return "Спой короткий вопрос из 2–4 звуков и ответь так, чтобы ответ закончился на тонике.";
        if (week == 7 || week == 8) return "Придумай короткий рисунок из ступеней 1, 2, 3 и 5. Закончи на 1.";
        if (week == 10) return "Спой три звука, затем сделай две секунды полной тишины и попробуй продолжить с той же высоты.";
        if (week == 13) return "Спой музыкальный вопрос и ответ. Сделай маленькое дыхание между ними.";
        if (week == 14) return "Выбери любые 4 соседние ступени гаммы и спой их вверх, затем обратно.";
        if (week == 15) return "Спой один звук, затем попробуй очень маленький шаг вверх. Сравни с эталоном кнопкой.";
        if (week == 19) return "Придумай рисунок из трёх ступеней и повтори его, начав на одну ступень выше.";
        if (week == 21) return "Придумай один короткий затактовый звук и продолжи на сильную долю.";
        if (week == 22 || week == 23) return "Сделай одну долю из четырёх быстрых звуков и одну долю из одного длинного. Поменяй их местами.";
        if (week >= 25 && week <= 29) return "Спой первый звук. Второй выбери сам так, чтобы получилось расстояние недели. Затем послушай эталон.";
        if (week == 30) return "Придумай рисунок из трёх звуков и повтори его чуть выше — получится маленькая секвенция.";
        if (week == 33) return "Спой одну короткую фразу от до в мажоре, затем похожую — в миноре. Сравни ощущение без слов «весело/грустно».";
        if (week == 34) return "Сочини фразу на 4–8 долей: ровный пульс, понятный конец и хотя бы один шаг и один скачок.";
        return "Придумай ответ из 3–5 звуков в " + key.name + " и закончи на тонике.";
    }

    private static double[] rhythmFor(int week, int day) {
        if (week == 1) return new double[]{1,1,1,1};
        if (week == 2) return day % 2 == 0 ? new double[]{1,0.5,0.5,1} : new double[]{0.5,0.5,1,1};
        if (week == 3) return day % 2 == 0 ? new double[]{1,0.5,0.5,1} : new double[]{0.5,0.5,1,0.5,0.5};
        if (week == 4 || week == 11) return new double[]{1,1,0.5,0.5};
        if (week == 5) return new double[]{1,1,1};
        if (week == 12) return day % 2 == 0 ? new double[]{3,1} : new double[]{1,3};
        if (week == 20) return new double[]{1,1,1,1};
        if (week == 21) return new double[]{0.5,1,1,1};
        if (week == 22) return new double[]{0.25,0.25,0.25,0.25,1};
        if (week == 23) return day % 2 == 0
                ? new double[]{1,0.5,0.5,0.25,0.25,0.25,0.25}
                : new double[]{0.25,0.25,0.5,1,0.5,0.5};
        if (day == 7) return new double[]{1,0.5,0.5,1};
        return day % 2 == 0 ? new double[]{1,0.5,0.5,1} : new double[]{0.5,0.5,1,1};
    }

    private static String rhythmLabel(double[] rhythm, int week) {
        List<String> labels = new ArrayList<>();
        for (double d : rhythm) {
            if (d >= 3) labels.add("ТА-а-а");
            else if (d >= 2) labels.add("ТА-а");
            else if (d >= 1) labels.add("ТА");
            else if (d >= 0.5) labels.add("ти");
            else labels.add("та-ка");
        }
        return join(labels, "  ");
    }

    private static String reviewFor(int id) {
        List<String> pieces = new ArrayList<>();
        if (id > 1) pieces.add("вчера: вспомни одно действие до подсказки");
        if (id > 7) pieces.add("неделю назад: вспомни один звук или ритм");
        if (id > 21) pieces.add("три недели назад: назови одно правило своими словами");
        return join(pieces, " • ");
    }

    private static Note[] fromDegrees(KeySpec key, int[] degrees) {
        Note[] notes = new Note[degrees.length];
        for (int i = 0; i < degrees.length; i++) {
            int degree = degrees[i];
            int octave = Math.floorDiv(degree, 7);
            int normalized = Math.floorMod(degree, 7);
            int scaleIndex = normalized + (octave > 0 ? 7 : 0);
            if (degree >= 7) {
                notes[i] = new Note(
                        key.rootMidi + 12 * octave + key.semitones[normalized],
                        key.rootStaffStep + degree,
                        key.labels[normalized] + (octave > 0 && normalized == 0 ? "↑" : ""),
                        key.accidentals[normalized]
                );
            } else {
                notes[i] = new Note(
                        key.rootMidi + key.semitones[scaleIndex],
                        key.rootStaffStep + degree,
                        key.labels[scaleIndex],
                        key.accidentals[scaleIndex]
                );
            }
        }
        return notes;
    }

    private static Note rawNote(int midi, int staffStep, String label, String accidental) {
        return new Note(midi, staffStep, label, accidental);
    }

    private static Note[] concat(Note[] a, Note[] b) {
        Note[] out = new Note[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private static Note[] cloneNotes(Note[] source) {
        Note[] out = new Note[source.length];
        for (int i = 0; i < source.length; i++) {
            Note n = source[i];
            out[i] = new Note(n.midi, n.staffStep, n.label, n.accidental);
        }
        return out;
    }

    private static boolean sameMidi(Note[] a, Note[] b) {
        return Arrays.equals(midi(a), midi(b));
    }

    private static int[] midi(Note[] notes) {
        int[] out = new int[notes.length];
        for (int i = 0; i < notes.length; i++) out[i] = notes[i].midi;
        return out;
    }

    private static Note[] transposeDiatonic(Note[] notes, int semitones) {
        Note[] out = new Note[notes.length];
        for (int i = 0; i < notes.length; i++) {
            out[i] = new Note(notes[i].midi + semitones, notes[i].staffStep + 1, notes[i].label, notes[i].accidental);
        }
        return out;
    }

    private static double[] accentedMeter(int meter, int bars) {
        double[] out = new double[meter * bars];
        Arrays.fill(out, 1.0);
        return out;
    }

    private static String join(List<String> list, String sep) {
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            if (sb.length() > 0) sb.append(sep);
            sb.append(s);
        }
        return sb.toString();
    }

    public static final class Lesson {
        public final int id;
        public final int week;
        public final int day;
        public final String dayTitle;
        public final String weekTitle;
        public final String goal;
        public final EarGame earGame;
        public final String movement;
        public final String singing;
        public final String creative;
        public final Note[] melody;
        public final double[] rhythm;
        public final String rhythmLabel;
        public final String keyName;
        public final boolean showStaff;
        public final String review;

        Lesson(int id, int week, int day, String dayTitle, String weekTitle, String goal,
               EarGame earGame, String movement, String singing, String creative,
               Note[] melody, double[] rhythm, String rhythmLabel, String keyName,
               boolean showStaff, String review) {
            this.id = id;
            this.week = week;
            this.day = day;
            this.dayTitle = dayTitle;
            this.weekTitle = weekTitle;
            this.goal = goal;
            this.earGame = earGame;
            this.movement = movement;
            this.singing = singing;
            this.creative = creative;
            this.melody = melody;
            this.rhythm = rhythm;
            this.rhythmLabel = rhythmLabel;
            this.keyName = keyName;
            this.showStaff = showStaff;
            this.review = review;
        }
    }

    public static final class Note {
        public final int midi;
        public final int staffStep; // C4 = 0, D4 = 1, ...
        public final String label;
        public final String accidental;

        public Note(int midi, int staffStep, String label, String accidental) {
            this.midi = midi;
            this.staffStep = staffStep;
            this.label = label;
            this.accidental = accidental;
        }
    }

    public static final class EarGame {
        public final String prompt;
        public final int[] melodyA;
        public final int[] melodyB;
        public final double[] rhythmA;
        public final double[] rhythmB;
        public final String[] options;
        public final int correct;
        public final String explanation;
        public final boolean rhythmMode;
        public int meter = 0;
        public boolean longGap = false;

        private EarGame(String prompt, int[] melodyA, int[] melodyB, double[] rhythmA,
                        double[] rhythmB, String[] options, int correct, String explanation,
                        boolean rhythmMode) {
            this.prompt = prompt;
            this.melodyA = melodyA;
            this.melodyB = melodyB;
            this.rhythmA = rhythmA;
            this.rhythmB = rhythmB;
            this.options = options;
            this.correct = correct;
            this.explanation = explanation;
            this.rhythmMode = rhythmMode;
        }

        public static EarGame melody(String prompt, int[] a, int[] b, String[] options, int correct, String explanation) {
            return new EarGame(prompt, a, b, null, null, options, correct, explanation, false);
        }

        public static EarGame rhythm(String prompt, double[] a, double[] b, String[] options, int correct, String explanation) {
            return new EarGame(prompt, null, null, a, b, options, correct, explanation, true);
        }

        public EarGame withMeter(int meter) {
            this.meter = meter;
            return this;
        }

        public EarGame withLongGap(boolean longGap) {
            this.longGap = longGap;
            return this;
        }
    }

    private static final class KeySpec {
        final String name;
        final int rootMidi;
        final int rootStaffStep;
        final int[] semitones;
        final String[] labels;
        final String[] accidentals;

        KeySpec(String name, int rootMidi, int rootStaffStep, int[] semitones,
                String[] labels, String[] accidentals) {
            this.name = name;
            this.rootMidi = rootMidi;
            this.rootStaffStep = rootStaffStep;
            this.semitones = semitones;
            this.labels = labels;
            this.accidentals = accidentals;
        }
    }
}
