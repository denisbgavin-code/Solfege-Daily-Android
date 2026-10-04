package ru.solfege.daily;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PitchDetectorTest {
    private static final int SAMPLE_RATE = 44100;
    private static final int WINDOW = 4096;

    private short[] sine(double hz) {
        short[] data = new short[WINDOW];
        double amplitude = 12000.0;
        for (int i = 0; i < data.length; i++) {
            double envelope = Math.min(1.0, i / 180.0);
            data[i] = (short) Math.round(amplitude * envelope * Math.sin(2.0 * Math.PI * hz * i / SAMPLE_RATE));
        }
        return data;
    }

    @Test
    public void detectsCommonChildVoiceAndReferencePitches() {
        double[] frequencies = {196.0, 220.0, 261.63, 329.63, 440.0, 523.25};
        for (double expected : frequencies) {
            short[] data = sine(expected);
            double actual = PitchDetector.estimatePitch(data, data.length);
            assertTrue("pitch must be detected for " + expected, actual > 0);
            double relativeError = Math.abs(actual - expected) / expected;
            assertTrue("relative error too high for " + expected + ": " + actual, relativeError < 0.025);
        }
    }

    @Test
    public void rejectsSilence() {
        short[] data = new short[WINDOW];
        assertEquals(-1.0, PitchDetector.estimatePitch(data, data.length), 0.0);
    }
}
