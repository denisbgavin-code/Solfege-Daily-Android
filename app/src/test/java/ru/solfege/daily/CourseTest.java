package ru.solfege.daily;

import org.junit.Test;

import static org.junit.Assert.*;

public class CourseTest {
    @Test
    public void all238LessonsAreSelfContainedAndValid() {
        assertEquals(238, Course.TOTAL_LESSONS);

        for (int id = 1; id <= Course.TOTAL_LESSONS; id++) {
            Course.Lesson l = Course.lesson(id);

            assertEquals(id, l.id);
            assertTrue(l.week >= 1 && l.week <= 34);
            assertTrue(l.day >= 1 && l.day <= 7);
            assertNotNull(l.weekTitle);
            assertFalse(l.weekTitle.trim().isEmpty());
            assertNotNull(l.goal);
            assertFalse(l.goal.trim().isEmpty());

            assertNotNull(l.melody);
            assertTrue("lesson " + id + " melody", l.melody.length >= 2);
            for (Course.Note n : l.melody) {
                assertTrue(n.midi > 0);
                assertNotNull(n.label);
                assertFalse(n.label.trim().isEmpty());
                assertNotNull(n.accidental);
            }

            assertNotNull(l.rhythm);
            assertTrue("lesson " + id + " rhythm", l.rhythm.length >= 2);
            for (double d : l.rhythm) assertTrue(d > 0);

            assertNotNull(l.earGame);
            assertNotNull(l.earGame.prompt);
            assertNotNull(l.earGame.options);
            assertTrue(l.earGame.options.length >= 2);
            assertTrue(l.earGame.correct >= 0 && l.earGame.correct < l.earGame.options.length);
            assertNotNull(l.earGame.explanation);

            assertNotNull(l.movement);
            assertNotNull(l.singing);
            assertNotNull(l.creative);

            String childText = (l.earGame.prompt + " " + l.movement + " " + l.singing + " " + l.creative).toLowerCase();
            assertFalse("lesson " + id + " sends child to workbook", childText.contains("рабочая тетрад"));
            assertFalse("lesson " + id + " sends child to textbook", childText.contains("учебник"));
        }
    }

    @Test
    public void firstAndLastLessonsHaveCorrectCoordinates() {
        Course.Lesson first = Course.lesson(1);
        assertEquals(1, first.week);
        assertEquals(1, first.day);

        Course.Lesson last = Course.lesson(238);
        assertEquals(34, last.week);
        assertEquals(7, last.day);
    }
}
