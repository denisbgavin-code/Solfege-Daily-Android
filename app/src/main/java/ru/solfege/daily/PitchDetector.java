package ru.solfege.daily;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;

public final class PitchDetector {
    public interface Listener {
        void onLevel(double rms);
        void onPitch(double hz, double midi, double centsFromTarget, boolean stableMatch);
        void onError(String message);
    }

    private static final int SAMPLE_RATE = 44100;
    private static final int WINDOW = 4096;

    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile boolean running = false;
    private Thread worker;
    private AudioRecord recorder;

    public synchronized void start(int targetMidi, Listener listener) {
        stop();

        int min = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );
        int bufferSize = Math.max(min, WINDOW * 2);

        try {
            recorder = new AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
            );
        } catch (Exception e) {
            postError(listener, "Не удалось открыть микрофон.");
            return;
        }

        if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
            try {
                recorder.release();
            } catch (Exception ignored) {
            }
            recorder = null;
            postError(listener, "Микрофон недоступен.");
            return;
        }

        running = true;
        worker = new Thread(() -> runDetector(targetMidi, listener), "pitch-detector");
        worker.start();
    }

    public synchronized void stop() {
        running = false;

        AudioRecord r = recorder;
        recorder = null;
        if (r != null) {
            try {
                r.stop();
            } catch (Exception ignored) {
            }
            try {
                r.release();
            } catch (Exception ignored) {
            }
        }

        Thread w = worker;
        worker = null;
        if (w != null) {
            w.interrupt();
        }
    }

    private void runDetector(int targetMidi, Listener listener) {
        AudioRecord r = recorder;
        if (r == null) return;

        short[] buffer = new short[WINDOW];
        double[] history = new double[5];
        int historyCount = 0;

        try {
            r.startRecording();

            while (running) {
                int read = r.read(buffer, 0, buffer.length, AudioRecord.READ_BLOCKING);
                if (read < WINDOW / 2 || !running) continue;

                double rms = rms(buffer, read);
                double normalized = Math.min(1.0, rms / 4500.0);
                main.post(() -> listener.onLevel(normalized));

                if (rms < 350) continue;

                double hz = estimatePitch(buffer, read);
                if (hz < 65 || hz > 1100) continue;

                double midi = 69.0 + 12.0 * log2(hz / 440.0);
                double cents = (midi - targetMidi) * 100.0;

                history[historyCount % history.length] = midi;
                historyCount++;

                boolean stable = false;
                if (historyCount >= 3) {
                    int n = Math.min(history.length, historyCount);
                    double mean = 0;
                    for (int i = 0; i < n; i++) mean += history[i];
                    mean /= n;

                    double variance = 0;
                    for (int i = 0; i < n; i++) {
                        double d = history[i] - mean;
                        variance += d * d;
                    }
                    variance /= n;

                    stable = variance < 0.05 && Math.abs(mean - targetMidi) < 0.55;
                }

                final boolean match = stable;
                final double finalHz = hz;
                final double finalMidi = midi;
                final double finalCents = cents;
                main.post(() -> listener.onPitch(finalHz, finalMidi, finalCents, match));

                if (stable) {
                    // Give the child time to see success before another frame changes the label.
                    try {
                        Thread.sleep(280);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        } catch (SecurityException e) {
            postError(listener, "Нет разрешения на микрофон.");
        } catch (Exception e) {
            if (running) postError(listener, "Не удалось распознать звук микрофона.");
        } finally {
            synchronized (this) {
                if (recorder == r) recorder = null;
            }
            try {
                r.stop();
            } catch (Exception ignored) {
            }
            try {
                r.release();
            } catch (Exception ignored) {
            }
        }
    }

    private static double rms(short[] data, int length) {
        double sum = 0;
        for (int i = 0; i < length; i++) {
            double x = data[i];
            sum += x * x;
        }
        return Math.sqrt(sum / Math.max(1, length));
    }

    /**
     * Normalized autocorrelation. We downsample by two for speed and use a local-peak search.
     * This is intentionally used as coaching feedback, not as a high-stakes pass/fail assessment.
     */
    static double estimatePitch(short[] data, int length) {
        int step = 2;
        int n = length / step;
        if (n < 512) return -1;

        double mean = 0;
        for (int i = 0; i < n; i++) mean += data[i * step];
        mean /= n;

        double[] x = new double[n];
        double energy = 0;
        for (int i = 0; i < n; i++) {
            x[i] = data[i * step] - mean;
            energy += x[i] * x[i];
        }
        if (energy < 1e6) return -1;

        double effectiveRate = SAMPLE_RATE / (double) step;
        int minLag = (int) (effectiveRate / 1000.0);
        int maxLag = Math.min(n / 2, (int) (effectiveRate / 65.0));

        double best = -1;
        int bestLag = -1;

        double previous = -1;
        for (int lag = minLag; lag <= maxLag; lag++) {
            double sum = 0;
            double e1 = 0;
            double e2 = 0;
            int limit = n - lag;
            for (int i = 0; i < limit; i++) {
                double a = x[i];
                double b = x[i + lag];
                sum += a * b;
                e1 += a * a;
                e2 += b * b;
            }
            double corr = sum / Math.sqrt(Math.max(1e-12, e1 * e2));

            // Prefer first strong local maximum to reduce octave-halving errors.
            if (lag > minLag && previous > corr && previous > 0.72) {
                int candidate = lag - 1;
                double hz = effectiveRate / candidate;
                if (hz >= 65 && hz <= 1000) return hz;
            }

            if (corr > best) {
                best = corr;
                bestLag = lag;
            }
            previous = corr;
        }

        if (bestLag <= 0 || best < 0.50) return -1;
        return effectiveRate / bestLag;
    }

    public static String noteName(double midi) {
        String[] names = {"до", "до♯", "ре", "ре♯", "ми", "фа", "фа♯", "соль", "соль♯", "ля", "ля♯", "си"};
        int rounded = (int) Math.round(midi);
        return names[Math.floorMod(rounded, 12)];
    }

    private static double log2(double x) {
        return Math.log(x) / Math.log(2.0);
    }

    private void postError(Listener listener, String message) {
        main.post(() -> listener.onError(message));
    }
}
