package com.example.easylex.ui.charts;

/**
 * =====================================================================
 * LineChartView — גרף קו מינימלי, בלי ספריית תרשימים חיצונית (שלב 2,
 * InstitutionalStatsFragment, "המסע שלי")
 * =====================================================================
 *
 * למה custom View ולא MPAndroidChart?
 * -------------------------------------
 * הפרויקט לא כולל כרגע שום ספריית charting (נבדק ב-app/build.gradle.kts) —
 * לפי ההנחיה המפורשת "אל תוסיף ספרייה חדשה בלי לבדוק", זהו View פשוט
 * שמצייר קו + נקודות + ציר X על Canvas רגיל.
 * =====================================================================
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LineChartView extends View {

    private List<Integer> values = new ArrayList<>();
    private List<String> labels = new ArrayList<>();

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public LineChartView(Context context) { super(context); init(); }
    public LineChartView(Context context, @Nullable AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        linePaint.setColor(Color.parseColor("#00BFA5"));
        linePaint.setStrokeWidth(dp(3));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        dotPaint.setColor(Color.parseColor("#00897B"));
        dotPaint.setStyle(Paint.Style.FILL);

        axisPaint.setColor(Color.parseColor("#DDDDDD"));
        axisPaint.setStrokeWidth(dp(1));

        labelPaint.setColor(Color.parseColor("#AAAAAA"));
        labelPaint.setTextSize(dp(10));
        labelPaint.setTextAlign(Paint.Align.CENTER);
    }

    /** setData — ערכים מצטברים (Y) + תוויות ציר X (למשל "שבוע 1" .. "שבוע 8"). */
    public void setData(List<Integer> newValues, List<String> newLabels) {
        this.values = newValues != null ? newValues : new ArrayList<>();
        this.labels = newLabels != null ? newLabels : new ArrayList<>();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        int paddingBottom = dp(20); // מקום לתוויות ציר X
        int paddingTop = dp(10);
        int paddingSide = dp(16);

        // ציר תחתון
        canvas.drawLine(paddingSide, h - paddingBottom, w - paddingSide, h - paddingBottom, axisPaint);

        if (values.isEmpty()) return;

        int maxVal = Collections.max(values);
        if (maxVal <= 0) maxVal = 1;
        int n = values.size();
        float usableW = w - 2f * paddingSide;
        float usableH = h - paddingBottom - paddingTop;
        float stepX = n > 1 ? usableW / (n - 1) : 0;

        Path path = new Path();
        float[] xs = new float[n], ys = new float[n];
        for (int i = 0; i < n; i++) {
            float x = paddingSide + stepX * i;
            float y = (h - paddingBottom) - (values.get(i) / (float) maxVal) * usableH;
            xs[i] = x; ys[i] = y;
            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        canvas.drawPath(path, linePaint);

        for (int i = 0; i < n; i++) {
            canvas.drawCircle(xs[i], ys[i], dp(4), dotPaint);
            if (i < labels.size()) {
                canvas.drawText(labels.get(i), xs[i], h - dp(4), labelPaint);
            }
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
