package ru.solfege.daily;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

class PianoView extends View {
    interface Listener {
        void onNote(int midi);
    }

    private Listener listener;
    private final Paint white = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint black = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int pressedMidi = -1;

    PianoView(Context context) {
        super(context);
        white.setColor(Color.WHITE);
        black.setColor(Color.rgb(35, 38, 46));
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(dp(1));
        border.setColor(Color.rgb(90, 95, 105));
        label.setColor(Color.rgb(75, 80, 90));
        label.setTextAlign(Paint.Align.CENTER);
        label.setTextSize(dp(11));
        setFocusable(true);
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, resolveSize((int) dp(170), heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth() / 15f;
        String[] names = {"до","ре","ми","фа","соль","ля","си","до","ре","ми","фа","соль","ля","си","до"};
        int[] whiteMidi = {60,62,64,65,67,69,71,72,74,76,77,79,81,83,84};

        for (int i = 0; i < 15; i++) {
            RectF r = new RectF(i * w, 0, (i + 1) * w, getHeight());
            Paint p = white;
            if (pressedMidi == whiteMidi[i]) {
                p = new Paint(white);
                p.setColor(Color.rgb(222, 232, 255));
            }
            canvas.drawRect(r, p);
            canvas.drawRect(r, border);
            canvas.drawText(names[i], r.centerX(), getHeight() - dp(12), label);
        }

        int[] blackAfterWhite = {0,1,3,4,5,7,8,10,11,12};
        int[] blackMidi = {61,63,66,68,70,73,75,78,80,82};
        float bw = w * 0.58f;
        float bh = getHeight() * 0.61f;

        for (int i = 0; i < blackAfterWhite.length; i++) {
            float cx = (blackAfterWhite[i] + 1) * w;
            RectF r = new RectF(cx - bw / 2f, 0, cx + bw / 2f, bh);
            Paint p = black;
            if (pressedMidi == blackMidi[i]) {
                p = new Paint(black);
                p.setColor(Color.rgb(90, 110, 160));
            }
            canvas.drawRoundRect(r, dp(2), dp(2), p);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int midi = hitTest(event.getX(), event.getY());
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            pressedMidi = midi;
            invalidate();
            if (listener != null && midi > 0) listener.onNote(midi);
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            pressedMidi = -1;
            invalidate();
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (midi != pressedMidi && midi > 0) {
                pressedMidi = midi;
                invalidate();
            }
            return true;
        }
        return true;
    }

    private int hitTest(float x, float y) {
        float w = getWidth() / 15f;
        int[] blackAfterWhite = {0,1,3,4,5,7,8,10,11,12};
        int[] blackMidi = {61,63,66,68,70,73,75,78,80,82};
        float bw = w * 0.58f;
        float bh = getHeight() * 0.61f;

        if (y <= bh) {
            for (int i = 0; i < blackAfterWhite.length; i++) {
                float cx = (blackAfterWhite[i] + 1) * w;
                if (x >= cx - bw / 2f && x <= cx + bw / 2f) return blackMidi[i];
            }
        }

        int index = Math.max(0, Math.min(14, (int) (x / w)));
        int[] whiteMidi = {60,62,64,65,67,69,71,72,74,76,77,79,81,83,84};
        return whiteMidi[index];
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}

class MelodyEntryView extends View {
    interface Listener {
        void onComplete(double score);
        void onChanged(int entered, int total);
    }

    private Course.Note[] target = new Course.Note[0];
    private int limit = 4;
    private final List<Integer> entered = new ArrayList<>();
    private Listener listener;

    private final Paint staff = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint note = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint muted = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);

    MelodyEntryView(Context context) {
        super(context);
        staff.setColor(Color.rgb(80, 85, 95));
        staff.setStrokeWidth(dp(1.2f));
        note.setColor(Color.rgb(42, 76, 160));
        muted.setColor(Color.rgb(220, 224, 234));
        text.setColor(Color.rgb(70, 75, 88));
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(dp(12));
    }

    void setTarget(Course.Note[] notes, int limit) {
        this.target = notes == null ? new Course.Note[0] : notes;
        this.limit = Math.min(Math.max(1, limit), this.target.length);
        entered.clear();
        invalidate();
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    void addMidi(int midi) {
        if (entered.size() >= limit) return;
        entered.add(midi);
        invalidate();
        if (listener != null) listener.onChanged(entered.size(), limit);

        if (entered.size() == limit && listener != null) {
            int correct = 0;
            for (int i = 0; i < limit; i++) {
                if (entered.get(i) == target[i].midi) correct++;
            }
            listener.onComplete(correct / (double) limit);
        }
    }

    void undo() {
        if (!entered.isEmpty()) entered.remove(entered.size() - 1);
        invalidate();
        if (listener != null) listener.onChanged(entered.size(), limit);
    }

    void clear() {
        entered.clear();
        invalidate();
        if (listener != null) listener.onChanged(0, limit);
    }

    String enteredNames() {
        StringBuilder sb = new StringBuilder();
        for (int midi : entered) {
            if (sb.length() > 0) sb.append(" – ");
            sb.append(noteName(midi));
        }
        return sb.toString();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), resolveSize((int) dp(160), heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float left = dp(16);
        float right = getWidth() - dp(16);
        float top = dp(34);
        float gap = dp(15);
        float bottom = top + gap * 4;

        for (int i = 0; i < 5; i++) {
            float y = top + i * gap;
            canvas.drawLine(left, y, right, y, staff);
        }

        float usable = right - left;
        float dx = usable / Math.max(1, limit);
        for (int i = 0; i < limit; i++) {
            float x = left + dx * (i + 0.5f);
            if (i >= entered.size()) {
                canvas.drawCircle(x, bottom + dp(34), dp(4), muted);
                continue;
            }

            int midi = entered.get(i);
            int step = staffStepForMidi(midi);
            float y = bottom - (step - 2) * gap / 2f;
            drawLedger(canvas, x, y, step, bottom, gap);
            canvas.save();
            canvas.rotate(-18, x, y);
            canvas.drawOval(new RectF(x - dp(7), y - dp(5), x + dp(7), y + dp(5)), note);
            canvas.restore();
            canvas.drawLine(x + dp(6), y, x + dp(6), y - dp(28), note);
            canvas.drawText(noteName(midi), x, bottom + dp(36), text);
        }
    }

    private void drawLedger(Canvas canvas, float x, float y, int step, float bottom, float gap) {
        if (step <= 0) {
            for (int s = 0; s >= step; s -= 2) {
                float ly = bottom - (s - 2) * gap / 2f;
                canvas.drawLine(x - dp(11), ly, x + dp(11), ly, staff);
            }
        }
        if (step >= 12) {
            for (int s = 12; s <= step; s += 2) {
                float ly = bottom - (s - 2) * gap / 2f;
                canvas.drawLine(x - dp(11), ly, x + dp(11), ly, staff);
            }
        }
    }

    private static int staffStepForMidi(int midi) {
        int octave = Math.floorDiv(midi, 12) - 5; // C4 octave offset is 0 for MIDI 60.
        int pc = Math.floorMod(midi, 12);
        int diatonic;
        switch (pc) {
            case 0:
            case 1: diatonic = 0; break;
            case 2:
            case 3: diatonic = 1; break;
            case 4: diatonic = 2; break;
            case 5:
            case 6: diatonic = 3; break;
            case 7:
            case 8: diatonic = 4; break;
            case 9:
            case 10: diatonic = 5; break;
            default: diatonic = 6; break;
        }
        return octave * 7 + diatonic;
    }

    private static String noteName(int midi) {
        String[] names = {"до","до♯","ре","ре♯","ми","фа","фа♯","соль","соль♯","ля","ля♯","си"};
        return names[Math.floorMod(midi, 12)];
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}

class StaffTapView extends View {
    interface Listener {
        void onComplete(double score);
        void onChanged(int entered, int total);
    }

    private Course.Note[] target = new Course.Note[0];
    private int limit = 4;
    private final List<Entry> entries = new ArrayList<>();
    private String accidental = "";
    private Listener listener;

    private final Paint staff = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint note = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ghost = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);

    StaffTapView(Context context) {
        super(context);
        staff.setColor(Color.rgb(72, 78, 90));
        staff.setStrokeWidth(dp(1.2f));
        note.setColor(Color.rgb(45, 78, 165));
        ghost.setColor(Color.rgb(218, 224, 238));
        text.setColor(Color.rgb(70, 75, 88));
        text.setTextSize(dp(17));
        text.setTextAlign(Paint.Align.RIGHT);
    }

    void setTarget(Course.Note[] target, int limit) {
        this.target = target == null ? new Course.Note[0] : target;
        this.limit = Math.min(Math.max(1, limit), this.target.length);
        entries.clear();
        invalidate();
    }

    void setAccidental(String accidental) {
        this.accidental = accidental == null ? "" : accidental;
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    void undo() {
        if (!entries.isEmpty()) entries.remove(entries.size() - 1);
        invalidate();
        if (listener != null) listener.onChanged(entries.size(), limit);
    }

    void clear() {
        entries.clear();
        invalidate();
        if (listener != null) listener.onChanged(0, limit);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), resolveSize((int) dp(185), heightMeasureSpec));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN || entries.size() >= limit) return true;

        float top = dp(45);
        float gap = dp(16);
        float bottom = top + gap * 4;
        int step = Math.round(2 + (bottom - event.getY()) / (gap / 2f));
        step = Math.max(-2, Math.min(14, step));

        entries.add(new Entry(step, accidental));
        invalidate();
        if (listener != null) listener.onChanged(entries.size(), limit);

        if (entries.size() == limit && listener != null) {
            int correct = 0;
            for (int i = 0; i < limit; i++) {
                Entry e = entries.get(i);
                Course.Note t = target[i];
                boolean stepCorrect = e.staffStep == t.staffStep;
                boolean accidentalCorrect = normalize(e.accidental).equals(normalize(t.accidental));
                if (stepCorrect && accidentalCorrect) correct++;
            }
            listener.onComplete(correct / (double) limit);
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float left = dp(16);
        float right = getWidth() - dp(16);
        float top = dp(45);
        float gap = dp(16);
        float bottom = top + gap * 4;

        for (int i = 0; i < 5; i++) {
            float y = top + i * gap;
            canvas.drawLine(left, y, right, y, staff);
        }

        float dx = (right - left) / Math.max(1, limit);
        for (int i = 0; i < limit; i++) {
            float x = left + dx * (i + 0.5f);
            if (i >= entries.size()) {
                canvas.drawCircle(x, bottom + dp(39), dp(4), ghost);
                continue;
            }

            Entry e = entries.get(i);
            float y = bottom - (e.staffStep - 2) * gap / 2f;
            drawLedger(canvas, x, e.staffStep, bottom, gap);
            canvas.save();
            canvas.rotate(-18, x, y);
            canvas.drawOval(new RectF(x - dp(7), y - dp(5), x + dp(7), y + dp(5)), note);
            canvas.restore();
            canvas.drawLine(x + dp(6), y, x + dp(6), y - dp(29), note);
            if (!e.accidental.isEmpty()) {
                canvas.drawText(e.accidental, x - dp(10), y + dp(6), text);
            }
        }
    }

    private void drawLedger(Canvas canvas, float x, int step, float bottom, float gap) {
        if (step <= 0) {
            for (int s = 0; s >= step; s -= 2) {
                float ly = bottom - (s - 2) * gap / 2f;
                canvas.drawLine(x - dp(11), ly, x + dp(11), ly, staff);
            }
        }
        if (step >= 12) {
            for (int s = 12; s <= step; s += 2) {
                float ly = bottom - (s - 2) * gap / 2f;
                canvas.drawLine(x - dp(11), ly, x + dp(11), ly, staff);
            }
        }
    }

    private static String normalize(String a) {
        if (a == null) return "";
        if ("#".equals(a)) return "♯";
        if ("b".equalsIgnoreCase(a)) return "♭";
        return a;
    }

    private static final class Entry {
        final int staffStep;
        final String accidental;

        Entry(int staffStep, String accidental) {
            this.staffStep = staffStep;
            this.accidental = accidental == null ? "" : accidental;
        }
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}

class MeasureBuilderView extends View {
    interface Listener {
        void onChanged(double total, double target, boolean exact);
    }

    private double targetBeats = 4.0;
    private double[] allowed = {1.0, 0.5};
    private final List<Double> placed = new ArrayList<>();
    private Listener listener;

    private int dragging = -1;
    private float dragX;
    private float dragY;

    private final Paint measure = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chip = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chipActive = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint muted = new Paint(Paint.ANTI_ALIAS_FLAG);

    MeasureBuilderView(Context context) {
        super(context);
        measure.setColor(Color.rgb(55, 62, 75));
        measure.setStrokeWidth(dp(2));
        measure.setStyle(Paint.Style.STROKE);

        chip.setColor(Color.rgb(230, 236, 252));
        chipActive.setColor(Color.rgb(182, 201, 247));

        text.setColor(Color.rgb(40, 55, 90));
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(dp(15));

        muted.setColor(Color.rgb(112, 118, 130));
        muted.setTextAlign(Paint.Align.CENTER);
        muted.setTextSize(dp(12));

        setFocusable(true);
    }

    void configure(double targetBeats, double[] allowed) {
        this.targetBeats = targetBeats;
        this.allowed = allowed == null || allowed.length == 0 ? new double[]{1.0} : allowed.clone();
        placed.clear();
        invalidate();
        notifyChanged();
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    void undo() {
        if (!placed.isEmpty()) placed.remove(placed.size() - 1);
        invalidate();
        notifyChanged();
    }

    void clear() {
        placed.clear();
        invalidate();
        notifyChanged();
    }

    double total() {
        double t = 0;
        for (double d : placed) t += d;
        return t;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), resolveSize((int) dp(250), heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float left = dp(16);
        float right = getWidth() - dp(16);
        float measureTop = dp(28);
        float measureBottom = dp(116);

        canvas.drawRect(left, measureTop, right, measureBottom, measure);

        double total = total();
        float x = left + dp(8);
        float usable = right - left - dp(16);
        for (double d : placed) {
            float width = (float) (usable * d / targetBeats);
            width = Math.max(dp(28), width);
            RectF r = new RectF(x, measureTop + dp(14), Math.min(right - dp(4), x + width - dp(4)), measureBottom - dp(14));
            canvas.drawRoundRect(r, dp(9), dp(9), chip);
            canvas.drawText(symbol(d), r.centerX(), r.centerY() + dp(6), text);
            x += width;
        }

        String totalLabel = format(total) + " / " + format(targetBeats) + " долей";
        canvas.drawText(totalLabel, (left + right) / 2f, measureBottom + dp(24), muted);

        float paletteTop = dp(168);
        float chipW = (right - left - dp(10) * (allowed.length - 1)) / allowed.length;
        for (int i = 0; i < allowed.length; i++) {
            float l = left + i * (chipW + dp(10));
            RectF r = new RectF(l, paletteTop, l + chipW, paletteTop + dp(54));
            canvas.drawRoundRect(r, dp(12), dp(12), dragging == i ? chipActive : chip);
            canvas.drawText(symbol(allowed[i]), r.centerX(), r.centerY() + dp(5), text);
        }
        canvas.drawText("перетащи или нажми", (left + right) / 2f, getHeight() - dp(8), muted);

        if (dragging >= 0) {
            RectF ghost = new RectF(dragX - dp(36), dragY - dp(25), dragX + dp(36), dragY + dp(25));
            canvas.drawRoundRect(ghost, dp(12), dp(12), chipActive);
            canvas.drawText(symbol(allowed[dragging]), ghost.centerX(), ghost.centerY() + dp(5), text);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int hit = hitPalette(event.getX(), event.getY());

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (hit >= 0) {
                dragging = hit;
                dragX = event.getX();
                dragY = event.getY();
                invalidate();
                return true;
            }
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (dragging >= 0) {
                dragX = event.getX();
                dragY = event.getY();
                invalidate();
                return true;
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            if (dragging >= 0) {
                int selected = dragging;
                boolean droppedInMeasure = event.getY() <= dp(145);
                boolean tapPalette = hit == selected;
                dragging = -1;
                if (droppedInMeasure || tapPalette) addDuration(allowed[selected]);
                invalidate();
                return true;
            }
        } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
            dragging = -1;
            invalidate();
            return true;
        }
        return true;
    }

    private void addDuration(double duration) {
        double next = total() + duration;
        if (next <= targetBeats + 0.001) {
            placed.add(duration);
            notifyChanged();
        }
    }

    private void notifyChanged() {
        if (listener != null) {
            double total = total();
            listener.onChanged(total, targetBeats, Math.abs(total - targetBeats) < 0.001);
        }
    }

    private int hitPalette(float x, float y) {
        float left = dp(16);
        float right = getWidth() - dp(16);
        float paletteTop = dp(168);
        if (y < paletteTop || y > paletteTop + dp(60)) return -1;

        float chipW = (right - left - dp(10) * (allowed.length - 1)) / allowed.length;
        for (int i = 0; i < allowed.length; i++) {
            float l = left + i * (chipW + dp(10));
            if (x >= l && x <= l + chipW) return i;
        }
        return -1;
    }

    private static String symbol(double d) {
        if (d >= 3.0) return "3 доли";
        if (d >= 2.0) return "𝅗𝅥";
        if (d >= 1.0) return "♩";
        if (d >= 0.5) return "♪";
        return "♬";
    }

    private static String format(double d) {
        if (Math.abs(d - Math.round(d)) < 0.001) return Integer.toString((int) Math.round(d));
        return String.format(java.util.Locale.ROOT, "%.2f", d).replace(".50", "½").replace(".25", "¼").replace(".75", "¾");
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}

class JourneyMapView extends View {
    private int completedLessons = 0;
    private final Paint path = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint done = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint current = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint locked = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);

    JourneyMapView(Context context) {
        super(context);
        path.setColor(Color.rgb(205, 211, 225));
        path.setStrokeWidth(dp(4));
        path.setStyle(Paint.Style.STROKE);

        done.setColor(Color.rgb(59, 126, 86));
        current.setColor(Color.rgb(54, 89, 181));
        locked.setColor(Color.rgb(214, 219, 229));

        text.setColor(Color.rgb(70, 75, 90));
        text.setTextSize(dp(10));
        text.setTextAlign(Paint.Align.CENTER);
    }

    void setCompletedLessons(int completedLessons) {
        this.completedLessons = completedLessons;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), resolveSize((int) dp(720), heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int cols = 4;
        float left = dp(30);
        float right = getWidth() - dp(30);
        float top = dp(30);
        float rowH = dp(78);
        float colW = (right - left) / (cols - 1f);

        float prevX = 0, prevY = 0;
        for (int i = 0; i < Course.TOTAL_WEEKS; i++) {
            int row = i / cols;
            int colRaw = i % cols;
            int col = row % 2 == 0 ? colRaw : cols - 1 - colRaw;
            float x = left + col * colW;
            float y = top + row * rowH;

            if (i > 0) canvas.drawLine(prevX, prevY, x, y, path);

            int week = i + 1;
            int completedWeek = completedLessons / 7;
            boolean isDone = week <= completedWeek;
            boolean isCurrent = week == completedWeek + 1;

            Paint p = isDone ? done : isCurrent ? current : locked;
            canvas.drawCircle(x, y, dp(16), p);
            Paint number = new Paint(text);
            number.setColor(isDone || isCurrent ? Color.WHITE : Color.rgb(100, 106, 120));
            number.setTextSize(dp(10));
            canvas.drawText(Integer.toString(week), x, y + dp(4), number);

            prevX = x;
            prevY = y;
        }
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
