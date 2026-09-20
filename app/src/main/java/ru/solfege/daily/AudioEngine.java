package ru.solfege.daily;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public final class AudioEngine {
    private static final int SAMPLE_RATE = 44100;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicInteger generation = new AtomicInteger();
    private volatile AudioTrack activeTrack;

    public interface Completion {
        void onFinished();
    }

    public void playEarGame(Course.EarGame game, Completion completion) {
        stop();
        final int token = generation.incrementAndGet();
        executor.execute(() -> {
            ByteArrayOutputStream pcm = new ByteArrayOutputStream();

            if (game.rhythmMode) {
                appendRhythm(pcm, game.rhythmA, game.meter);
                if (game.rhythmB != null) {
                    appendSilence(pcm, game.longGap ? 1500 : 650);
                    appendRhythm(pcm, game.rhythmB, game.meter);
                }
            } else {
                appendMelody(pcm, game.melodyA, 390, 80);
                if (game.melodyB != null) {
                    appendSilence(pcm, game.longGap ? 1600 : 650);
                    appendMelody(pcm, game.melodyB, 390, 80);
                }
            }

            playBytes(pcm.toByteArray(), token);
            if (generation.get() == token && completion != null) {
                main.post(completion::onFinished);
            }
        });
    }

    public void playMelody(Course.Note[] notes, Completion completion) {
        int[] midi = new int[notes.length];
        for (int i = 0; i < notes.length; i++) midi[i] = notes[i].midi;
        playMidi(midi, completion);
    }

    public void playMidi(int[] midi, Completion completion) {
        stop();
        final int token = generation.incrementAndGet();
        executor.execute(() -> {
            ByteArrayOutputStream pcm = new ByteArrayOutputStream();
            appendMelody(pcm, midi, 420, 75);
            playBytes(pcm.toByteArray(), token);
            if (generation.get() == token && completion != null) {
                main.post(completion::onFinished);
            }
        });
    }

    public void playHomeNote(int midi, Completion completion) {
        stop();
        final int token = generation.incrementAndGet();
        executor.execute(() -> {
            ByteArrayOutputStream pcm = new ByteArrayOutputStream();
            appendTone(pcm, midiToHz(midi), 900, 0.58);
            playBytes(pcm.toByteArray(), token);
            if (generation.get() == token && completion != null) {
                main.post(completion::onFinished);
            }
        });
    }

    public void playRhythm(double[] durations, int meter, Completion completion) {
        stop();
        final int token = generation.incrementAndGet();
        executor.execute(() -> {
            ByteArrayOutputStream pcm = new ByteArrayOutputStream();
            appendRhythm(pcm, durations, meter);
            playBytes(pcm.toByteArray(), token);
            if (generation.get() == token && completion != null) {
                main.post(completion::onFinished);
            }
        });
    }

    public void playPulse(int beats, Completion completion) {
        double[] durations = new double[beats];
        for (int i = 0; i < beats; i++) durations[i] = 1.0;
        playRhythm(durations, 0, completion);
    }

    public boolean mediaVolumeIsZero(Context context) {
        AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return manager != null && manager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0;
    }

    public synchronized void stop() {
        generation.incrementAndGet();
        AudioTrack track = activeTrack;
        activeTrack = null;
        if (track != null) {
            try {
                track.pause();
                track.flush();
                track.stop();
            } catch (Exception ignored) {
            }
            try {
                track.release();
            } catch (Exception ignored) {
            }
        }
    }

    public void close() {
        stop();
        executor.shutdownNow();
    }

    private void playBytes(byte[] bytes, int token) {
        if (bytes.length == 0 || generation.get() != token) return;

        int min = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );
        int buffer = Math.max(min, 16384);

        AudioTrack track = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setBufferSizeInBytes(buffer)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();

        synchronized (this) {
            if (generation.get() != token) {
                track.release();
                return;
            }
            activeTrack = track;
        }

        try {
            track.play();
            int offset = 0;
            while (offset < bytes.length && generation.get() == token) {
                int count = Math.min(buffer, bytes.length - offset);
                int written = track.write(bytes, offset, count, AudioTrack.WRITE_BLOCKING);
                if (written <= 0) break;
                offset += written;
            }
            if (generation.get() == token) {
                int remainingMs = Math.max(30, buffer * 1000 / (SAMPLE_RATE * 2));
                try {
                    Thread.sleep(remainingMs);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        } finally {
            synchronized (this) {
                if (activeTrack == track) activeTrack = null;
            }
            try {
                track.stop();
            } catch (Exception ignored) {
            }
            try {
                track.release();
            } catch (Exception ignored) {
            }
        }
    }

    private static void appendMelody(ByteArrayOutputStream out, int[] midi, int toneMs, int gapMs) {
        if (midi == null) return;
        for (int note : midi) {
            appendTone(out, midiToHz(note), toneMs, 0.52);
            appendSilence(out, gapMs);
        }
    }

    private static void appendRhythm(ByteArrayOutputStream out, double[] durations, int meter) {
        if (durations == null) return;
        final int beatMs = 620;
        for (int i = 0; i < durations.length; i++) {
            boolean strong = meter > 0 && i % meter == 0;
            appendTone(out, strong ? 1320.0 : 880.0, 55, strong ? 0.75 : 0.48);
            int total = (int) Math.round(beatMs * durations[i]);
            appendSilence(out, Math.max(25, total - 55));
        }
    }

    private static void appendTone(ByteArrayOutputStream out, double hz, int ms, double gain) {
        int samples = Math.max(1, SAMPLE_RATE * ms / 1000);
        ByteBuffer buffer = ByteBuffer.allocate(samples * 2).order(ByteOrder.LITTLE_ENDIAN);
        int attack = Math.max(1, SAMPLE_RATE * 8 / 1000);
        int release = Math.max(1, SAMPLE_RATE * 35 / 1000);

        for (int i = 0; i < samples; i++) {
            double env = 1.0;
            if (i < attack) env = i / (double) attack;
            int left = samples - i - 1;
            if (left < release) env = Math.min(env, left / (double) release);

            double base = Math.sin(2.0 * Math.PI * hz * i / SAMPLE_RATE);
            double second = 0.16 * Math.sin(2.0 * Math.PI * hz * 2.0 * i / SAMPLE_RATE);
            short sample = (short) Math.round((base + second) * gain * env * 16000.0);
            buffer.putShort(sample);
        }
        byte[] bytes = buffer.array();
        out.write(bytes, 0, bytes.length);
    }

    private static void appendSilence(ByteArrayOutputStream out, int ms) {
        int bytes = Math.max(0, SAMPLE_RATE * ms / 1000 * 2);
        byte[] silence = new byte[bytes];
        out.write(silence, 0, silence.length);
    }

    private static double midiToHz(int midi) {
        return 440.0 * Math.pow(2.0, (midi - 69) / 12.0);
    }
}
