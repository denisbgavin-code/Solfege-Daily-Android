package ru.solfege.daily;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class SkillTracker {
    public enum Skill {
        PULSE("Ровный пульс"),
        RHYTHM("Ритм"),
        PITCH("Высота звука"),
        TONALITY("Лад и ступени"),
        READING("Чтение нот"),
        WRITING("Запись музыки"),
        INTERVALS("Интервалы"),
        KEYS("Тональности"),
        MEMORY("Музыкальная память"),
        CREATIVITY("Музыкальное мышление");

        public final String title;

        Skill(String title) {
            this.title = title;
        }
    }

    private final SharedPreferences prefs;

    public SkillTracker(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public double score(Skill skill) {
        int attempts = attempts(skill);
        if (attempts == 0) return 0.50;
        return prefs.getFloat(keyScore(skill), 0.50f);
    }

    public int attempts(Skill skill) {
        return prefs.getInt(keyAttempts(skill), 0);
    }

    public void record(Skill skill, double result) {
        result = clamp(result, 0.0, 1.0);
        int attempts = attempts(skill);
        double old = score(skill);

        // Early attempts can move the estimate more quickly; later estimates are steadier.
        double alpha = attempts < 3 ? 0.38 : attempts < 8 ? 0.27 : 0.18;
        double updated = old * (1.0 - alpha) + result * alpha;

        prefs.edit()
                .putFloat(keyScore(skill), (float) updated)
                .putInt(keyAttempts(skill), attempts + 1)
                .apply();
    }

    public void recordStation(int week, int station, double result) {
        switch (station) {
            case 0:
                record(earSkillForWeek(week), result);
                record(MEMORY, Math.max(0.25, result * 0.9));
                break;
            case 1:
                record(RHYTHM, result);
                record(PULSE, Math.max(0.2, result));
                break;
            case 2:
                record(PITCH, result);
                if (week >= 6) record(TONALITY, result);
                break;
            case 3:
                record(READING, result);
                if (week >= 15) record(KEYS, result);
                break;
            case 4:
                record(WRITING, result);
                if (week >= 25 && week <= 30) record(INTERVALS, result);
                break;
            case 5:
                record(CREATIVITY, result);
                record(MEMORY, Math.max(0.35, result));
                break;
            default:
                break;
        }
    }

    public int adaptiveLevel(Skill skill) {
        int attempts = attempts(skill);
        double s = score(skill);
        if (attempts < 3) return 1;
        if (s < 0.48) return 0; // support
        if (s > 0.80 && attempts >= 5) return 2; // challenge
        return 1; // core
    }

    public Skill weakestSkill() {
        Skill weakest = Skill.PITCH;
        double best = Double.POSITIVE_INFINITY;
        for (Skill skill : Skill.values()) {
            if (attempts(skill) == 0) continue;
            double s = score(skill);
            if (s < best) {
                best = s;
                weakest = skill;
            }
        }
        return weakest;
    }

    public Skill strongestSkill() {
        Skill strongest = Skill.PULSE;
        double best = Double.NEGATIVE_INFINITY;
        for (Skill skill : Skill.values()) {
            if (attempts(skill) == 0) continue;
            double s = score(skill);
            if (s > best) {
                best = s;
                strongest = skill;
            }
        }
        return strongest;
    }

    public List<Skill> rankedWeakest() {
        List<Skill> list = new ArrayList<>(Arrays.asList(Skill.values()));
        list.sort(Comparator.comparingDouble(this::score));
        return list;
    }

    public int smartReviewLesson(int completedLessons) {
        Skill weak = weakestSkill();
        int maxWeek = Math.max(1, Math.min(Course.TOTAL_WEEKS, (completedLessons + 6) / 7));
        int[] candidates = candidateWeeks(weak);

        int selectedWeek = 1;
        for (int week : candidates) {
            if (week <= maxWeek) selectedWeek = week;
        }

        // Day 7 is designed as retrieval/review; if current week is early, use day 4.
        int day = selectedWeek == maxWeek && completedLessons < selectedWeek * 7 ? 4 : 7;
        int id = (selectedWeek - 1) * 7 + day;
        return Math.max(1, Math.min(Math.max(1, completedLessons), id));
    }

    public String supportHint(int week, int station) {
        Skill skill;
        switch (station) {
            case 0: skill = earSkillForWeek(week); break;
            case 1: skill = RHYTHM; break;
            case 2: skill = PITCH; break;
            case 3: skill = READING; break;
            case 4: skill = WRITING; break;
            default: skill = MEMORY;
        }

        int level = adaptiveLevel(skill);
        if (level == 0) {
            switch (station) {
                case 0:
                    return "Подсказка: перед ответом послушай пример дважды. Во второй раз следи только за одним признаком.";
                case 1:
                    return "Подсказка: сначала отбей только ровный пульс, потом добавь ритм.";
                case 2:
                    return "Подсказка: возьми только первый звук и пропой его спокойно. Потом добавь следующий.";
                case 3:
                    return "Подсказка: сначала найди направление — вверх или вниз. Названия нот оставь на потом.";
                case 4:
                    return "Подсказка: вводи по одному звуку и после каждого сравни направление с услышанным.";
                default:
                    return "Подсказка: уменьши задачу вдвое и выполни короткую версию.";
            }
        } else if (level == 2) {
            switch (station) {
                case 0:
                    return "Задание со звёздочкой: после ответа объясни, какой именно звук или ритмический признак помог решить.";
                case 1:
                    return "Задание со звёздочкой: повтори рисунок второй раз без нового прослушивания.";
                case 2:
                    return "Задание со звёздочкой: после пения начни тот же рисунок на другой удобной высоте.";
                case 3:
                    return "Задание со звёздочкой: закрой ноты и восстанови их по памяти.";
                case 4:
                    return "Задание со звёздочкой: после правильного диктанта транспонируй первый фрагмент на ступень выше.";
                default:
                    return "Задание со звёздочкой: придумай второй вариант по тому же правилу.";
            }
        }
        return "";
    }

    public String summaryLine() {
        Skill weak = weakestSkill();
        Skill strong = strongestSkill();
        return "Сейчас полезнее всего повторять: " + weak.title.toLowerCase(Locale.ROOT)
                + ". Самая уверенная область: " + strong.title.toLowerCase(Locale.ROOT) + ".";
    }

    public static Skill earSkillForWeek(int week) {
        if (week <= 2) return PITCH;
        if (week <= 5) return PULSE;
        if (week <= 10) return TONALITY;
        if (week <= 13) return RHYTHM;
        if (week <= 19) return KEYS;
        if (week <= 24) return RHYTHM;
        if (week <= 30) return INTERVALS;
        if (week <= 33) return KEYS;
        return MEMORY;
    }

    private int[] candidateWeeks(Skill skill) {
        switch (skill) {
            case PULSE: return new int[]{1,4,5,20};
            case RHYTHM: return new int[]{2,3,11,12,21,22,23};
            case PITCH: return new int[]{1,2,6,8,14};
            case TONALITY: return new int[]{6,7,8,14,24,33};
            case READING: return new int[]{9,11,14,16,17,18,31};
            case WRITING: return new int[]{9,11,16,17,18,26,27};
            case INTERVALS: return new int[]{25,26,27,28,29,30};
            case KEYS: return new int[]{15,16,17,18,31,32,33};
            case MEMORY: return new int[]{10,13,19,30,34};
            case CREATIVITY: return new int[]{2,6,13,19,30,34};
            default: return new int[]{1};
        }
    }

    private String keyScore(Skill skill) {
        return "skill_score_" + skill.name();
    }

    private String keyAttempts(Skill skill) {
        return "skill_attempts_" + skill.name();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
