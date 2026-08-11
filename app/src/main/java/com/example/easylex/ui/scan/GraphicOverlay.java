package com.example.easylex.ui.scan;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

/**
 * =====================================================================
 * GraphicOverlay — שכבת הציור של מסך הסריקה
 * =====================================================================
 *
 * מה זה עושה?
 * -----------
 * GraphicOverlay היא View מותאמת אישית שמצויירת מעל תמונת הצילום.
 * היא מציגה מלבנים צבעוניים עגולי-פינות מסביב לכל מילה מזוהה:
 *   ● ירוק — המילה כבר קיימת במאגר של המשתמש
 *   ● אדום  — המילה חדשה, ניתן להוסיפה
 *
 * כיצד עובדת ההמרה?
 * -------------------
 * ה-OCR מחזיר קואורדינטות לפי גודל התמונה המקורית.
 * ה-View מוצגת על המסך בגודל שונה.
 * getScaledRect() ממיר את הקואורדינטות של התמונה לקואורדינטות המסך
 * לפי יחסי scaleX = viewWidth / imageWidth.
 *
 * מחלקות שמשתמשות בה:
 * ---------------------
 * ScanFragment — קורא ל-setWordBoxes() אחרי ניתוח OCR.
 */
public class GraphicOverlay extends View {

    public static class WordBox {
        public Rect rect;
        public boolean existsInDb;
        public String text;
        public String pos; // חלק הדיבר שחולץ מהסוגריים — ריק אם לא קיים

        public WordBox(Rect rect, boolean existsInDb, String text, String pos) {
            this.rect = rect;
            this.existsInDb = existsInDb;
            this.text = text;
            this.pos = pos;
        }
    }

    private static final String TAG = "GraphicOverlay";

    // אופסיות 50% (0x80) — גבוהה מספיק לנראות, נמוכה מספיק לא להסתיר טקסט
    // #80A5E0A5: ירוק רך — מילה קיימת במאגר
    private static final int COLOR_EXISTS = Color.argb(0x80, 0xA5, 0xE0, 0xA5);
    // #80FF7070: אדום רך — מילה לא קיימת במאגר (ניתן להוסיף)
    private static final int COLOR_ADD    = Color.argb(0x80, 0xFF, 0x70, 0x70);

    private final List<WordBox> wordBoxes = new ArrayList<>();
    // Paint בסגנון FILL עם anti-aliasing לפינות חלקות
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int imageWidth, imageHeight;

    public GraphicOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        fillPaint.setStyle(Paint.Style.FILL);
    }

    public void setImageSourceInfo(int width, int height) {
        this.imageWidth = width;
        this.imageHeight = height;
        postInvalidate();
    }

    public void setWordBoxes(List<WordBox> boxes) {
        wordBoxes.clear();
        wordBoxes.addAll(boxes);
        // נקודת בדיקה: האם ה-extraction ב-ScanFragment יצר תיבות בכלל?
        Log.d(TAG, "setWordBoxes: received " + boxes.size() + " box(es). imageSize=" + imageWidth + "x" + imageHeight);
        invalidate();
    }

    public List<WordBox> getWordBoxes() { return wordBoxes; }

    public RectF getScaledRect(Rect originalRect) {
        if (imageWidth == 0 || imageHeight == 0) return new RectF();
        float scaleX = (float) getWidth() / imageWidth;
        float scaleY = (float) getHeight() / imageHeight;
        return new RectF(originalRect.left * scaleX, originalRect.top * scaleY,
                originalRect.right * scaleX, originalRect.bottom * scaleY);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (imageWidth == 0 || imageHeight == 0) {
            Log.d(TAG, "onDraw: skipped — imageSize not set (" + imageWidth + "x" + imageHeight + ")");
            return;
        }
        for (WordBox box : wordBoxes) {
            // הגנה מפני Rect ריק
            if (box.rect == null || box.rect.isEmpty()) {
                Log.d(TAG, "onDraw: skipping box '" + box.text + "' — rect is null or empty");
                continue;
            }
            // getScaledRect ממפה את ה-Rect של ה-OCR לקואורדינטות המסך
            RectF scaled = getScaledRect(box.rect);
            Log.d(TAG, "Drawing box: '" + box.text + "' at " + scaled.toShortString()
                    + " | viewSize=" + getWidth() + "x" + getHeight()
                    + " | exists=" + box.existsInDb);
            // רדיוס פינות = 40% מגובה התיבה — נותן מראה pill עגול אך לא סגלגל מדי
            float cornerRadius = scaled.height() * 0.4f;
            fillPaint.setColor(box.existsInDb ? COLOR_EXISTS : COLOR_ADD);
            canvas.drawRoundRect(scaled, cornerRadius, cornerRadius, fillPaint);
        }
    }
}