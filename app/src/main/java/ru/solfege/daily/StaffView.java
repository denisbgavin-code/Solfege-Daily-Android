package ru.solfege.daily;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

public class StaffView extends View {
    private Course.Note[] notes = new Course.Note[0];
    private final Paint staff = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint notePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);

    public StaffView(Context context) {
        super(context);
        staff.setColor(Color.rgb(65, 70, 80));
        staff.setStrokeWidth(dp(1.3f));

        notePaint.setColor(Color.rgb(35, 64, 130));
        notePaint.setStyle(Paint.Style.FILL);

        text.setColor(Color.rgb(45, 50, 60));
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(dp(13));
    }

    public void setNotes(Course.Note[] notes) {
        this.notes = notes == null ? new Course.Note[0] : notes;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int desired = dpInt(190);
        int height = resolveSize(desired, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() <= 0) return;

        float left = dp(16);
        float right = getWidth() - dp(16);
        float top = dp(48);
        float gap = dp(16);
        float bottomLine = top + gap * 4;

        for (int i = 0; i < 5; i++) {
            float y = top + i * gap;
            canvas.drawLine(left, y, right, y, staff);
        }

        if (notes.length == 0) return;

        float usable = right - left - dp(16);
        float stepX = usable / Math.max(1, notes.length);
        float rx = dp(7);
        float ry = dp(5);

        for (int i = 0; i < notes.length; i++) {
            Course.Note n = notes[i];
            float x = left + dp(8) + stepX * (i + 0.5f);
            float y = bottomLine - (n.staffStep - 2) * (gap / 2f);

            // Ledger lines below/above staff.
            if (n.staffStep <= 0) {
                for (int s = 0; s >= n.staffStep; s -= 2) {
                    if (s <= 0) {
                        float ly = bottomLine - (s - 2) * (gap / 2f);
                        canvas.drawLine(x - dp(10), ly, x + dp(10), ly, staff);
                    }
                }
            }
            if (n.staffStep >= 12) {
                for (int s = 12; s <= n.staffStep; s += 2) {
                    float ly = bottomLine - (s - 2) * (gap / 2f);
                    canvas.drawLine(x - dp(10), ly, x + dp(10), ly, staff);
                }
            }

            canvas.save();
            canvas.rotate(-18, x, y);
            canvas.drawOval(new RectF(x - rx, y - ry, x + rx, y + ry), notePaint);
            canvas.restore();

            canvas.drawLine(x + rx - dp(1), y, x + rx - dp(1), y - dp(32), notePaint);

            if (n.accidental != null && !n.accidental.isEmpty()) {
                Paint acc = new Paint(text);
                acc.setTextSize(dp(20));
                acc.setTextAlign(Paint.Align.RIGHT);
                canvas.drawText(n.accidental, x - dp(10), y + dp(6), acc);
            }

            canvas.drawText(n.label, x, bottomLine + dp(42), text);
        }
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    private int dpInt(int v) {
        return Math.round(dp(v));
    }
}

class ContourView extends View {
    private Course.Note[] notes = new Course.Note[0];
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);

    ContourView(Context context) {
        super(context);
        line.setColor(Color.rgb(92, 120, 190));
        line.setStrokeWidth(dp(3));
        line.setStyle(Paint.Style.STROKE);

        dot.setColor(Color.rgb(35, 64, 130));
        dot.setStyle(Paint.Style.FILL);
    }

    void setNotes(Course.Note[] notes) {
        this.notes = notes == null ? new Course.Note[0] : notes;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), resolveSize(dpInt(150), heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (notes.length == 0) return;
        int min = notes[0].midi;
        int max = notes[0].midi;
        for (Course.Note n : notes) {
            min = Math.min(min, n.midi);
            max = Math.max(max, n.midi);
        }
        if (min == max) max = min + 1;

        float left = dp(24);
        float right = getWidth() - dp(24);
        float top = dp(22);
        float bottom = getHeight() - dp(24);
        float dx = notes.length == 1 ? 0 : (right - left) / (notes.length - 1f);

        float prevX = 0, prevY = 0;
        for (int i = 0; i < notes.length; i++) {
            float x = left + dx * i;
            float normalized = (notes[i].midi - min) / (float) (max - min);
            float y = bottom - normalized * (bottom - top);
            if (i > 0) canvas.drawLine(prevX, prevY, x, y, line);
            canvas.drawCircle(x, y, dp(7), dot);
            prevX = x;
            prevY = y;
        }
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    private int dpInt(int v) {
        return Math.round(dp(v));
    }
}

class RhythmPatternView extends View {
    private double[] durations = new double[0];
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);

    RhythmPatternView(Context context) {
        super(context);
        fill.setColor(Color.rgb(226, 234, 253));
        fill.setStyle(Paint.Style.FILL);

        outline.setColor(Color.rgb(72, 98, 160));
        outline.setStyle(Paint.Style.STROKE);
        outline.setStrokeWidth(dp(1.5f));

        label.setColor(Color.rgb(35, 50, 80));
        label.setTextAlign(Paint.Align.CENTER);
        label.setTextSize(dp(12));
    }

    void setDurations(double[] durations) {
        this.durations = durations == null ? new double[0] : durations;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), resolveSize(dpInt(92), heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (durations.length == 0) return;

        double total = 0;
        for (double d : durations) total += d;
        float left = dp(12);
        float right = getWidth() - dp(12);
        float width = right - left;
        float y = dp(20);
        float h = dp(46);
        float x = left;

        for (double d : durations) {
            float w = (float) (width * d / total);
            RectF rect = new RectF(x + dp(2), y, x + w - dp(2), y + h);
            canvas.drawRoundRect(rect, dp(8), dp(8), fill);
            canvas.drawRoundRect(rect, dp(8), dp(8), outline);
            String txt = d >= 2.9 ? "3 доли" : d >= 1.9 ? "2 доли" : d >= 0.9 ? "1" : d >= 0.49 ? "½" : "¼";
            canvas.drawText(txt, rect.centerX(), rect.centerY() + dp(4), label);
            x += w;
        }
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    private int dpInt(int v) {
        return Math.round(dp(v));
    }
}
