package com.example.easylex.ui.charts;

/**
 * DonutChartView — טבעת התקדמות פשוטה (נכבשו/סה"כ) על Canvas רגיל,
 * ללא ספריית charting חיצונית (ר' LineChartView לפירוט הסיבה).
 * שימוש: InstitutionalStatsFragment, "המשימות שלי" — טבעת אחת לכל משימה.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class DonutChartView extends View {

    private int mastered = 0;
    private int total = 0;

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();

    public DonutChartView(Context context) { super(context); init(); }
    public DonutChartView(Context context, @Nullable AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        float strokeWidth = dp(10);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(strokeWidth);
        trackPaint.setColor(Color.parseColor("#EEEEEE"));

        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokeWidth);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setColor(Color.parseColor("#00BFA5"));

        textPaint.setColor(Color.parseColor("#333333"));
        textPaint.setTextSize(dp(13));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
    }

    public void setProgress(int mastered, int total) {
        this.mastered = mastered;
        this.total = total;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int size = Math.min(getWidth(), getHeight());
        float strokeHalf = trackPaint.getStrokeWidth() / 2f;
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        oval.set(cx - size / 2f + strokeHalf, cy - size / 2f + strokeHalf,
                cx + size / 2f - strokeHalf, cy + size / 2f - strokeHalf);

        canvas.drawOval(oval, trackPaint);

        float pct = total > 0 ? mastered / (float) total : 0f;
        canvas.drawArc(oval, -90, 360 * pct, false, progressPaint);

        String label = mastered + "/" + total;
        float textY = cy - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(label, cx, textY, textPaint);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
