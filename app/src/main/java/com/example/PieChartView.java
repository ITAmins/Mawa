package com.example;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/* loaded from: classes5.dex */
public class PieChartView extends View {
    private final Map<String, Integer> categoryColors;
    private final Paint paintArc;
    private final Paint paintBg;
    private final Paint paintBorder;
    private final Paint paintCenter;
    private final Paint paintGlow;
    private final Paint paintText;
    private final RectF rectF;
    private int selectedSliceIndex;
    private final List<PieSlice> slices;

    /* loaded from: classes5.dex */
    public static class PieSlice {
        public int color;
        public String name;
        public float startAngle;
        public float sweepAngle;
        public double value;

        public PieSlice(String name, double value, int color) {
            this.name = name;
            this.value = value;
            this.color = color;
        }
    }

    public PieChartView(Context context) {
        super(context);
        this.paintArc = new Paint(1);
        this.paintCenter = new Paint(1);
        this.paintText = new Paint(1);
        this.paintBg = new Paint(1);
        this.paintBorder = new Paint(1);
        this.paintGlow = new Paint(1);
        this.rectF = new RectF();
        this.slices = new ArrayList();
        this.categoryColors = new HashMap();
        this.selectedSliceIndex = -1;
        init();
    }

    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.paintArc = new Paint(1);
        this.paintCenter = new Paint(1);
        this.paintText = new Paint(1);
        this.paintBg = new Paint(1);
        this.paintBorder = new Paint(1);
        this.paintGlow = new Paint(1);
        this.rectF = new RectF();
        this.slices = new ArrayList();
        this.categoryColors = new HashMap();
        this.selectedSliceIndex = -1;
        init();
    }

    public PieChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.paintArc = new Paint(1);
        this.paintCenter = new Paint(1);
        this.paintText = new Paint(1);
        this.paintBg = new Paint(1);
        this.paintBorder = new Paint(1);
        this.paintGlow = new Paint(1);
        this.rectF = new RectF();
        this.slices = new ArrayList();
        this.categoryColors = new HashMap();
        this.selectedSliceIndex = -1;
        init();
    }

    private void init() {
        this.categoryColors.put("🛍️ বাজার", Integer.valueOf(Color.parseColor("#FF007F")));
        this.categoryColors.put("🏠 ভাড়া", Integer.valueOf(Color.parseColor("#00F0FF")));
        this.categoryColors.put("🚌 পরিবহন", Integer.valueOf(Color.parseColor("#BD00FF")));
        this.categoryColors.put("💊 ওষুধ", Integer.valueOf(Color.parseColor("#FF5F00")));
        this.categoryColors.put("🏦 ব্যাংক", Integer.valueOf(Color.parseColor("#00FF66")));
        this.categoryColors.put("🌾 কাঁচামাল", Integer.valueOf(Color.parseColor("#FFF000")));
        this.categoryColors.put("⚙️ অন্যান্য", Integer.valueOf(Color.parseColor("#8F9CAE")));
        this.paintCenter.setColor(Color.parseColor("#0B0E14"));
        this.paintText.setFakeBoldText(true);
        this.paintText.setTextAlign(Paint.Align.CENTER);
        this.paintBg.setColor(Color.parseColor("#0B0E14"));
        this.paintBg.setStyle(Paint.Style.FILL);
        this.paintBorder.setColor(Color.parseColor("#1E293B"));
        this.paintBorder.setStrokeWidth(3.0f);
        this.paintBorder.setStyle(Paint.Style.STROKE);
        this.paintGlow.setStyle(Paint.Style.STROKE);
        this.paintGlow.setStrokeWidth(8.0f);
    }

    public void setExpenses(List<ExpenseModel> expenses) {
        int color;
        String matchedCat;
        this.slices.clear();
        this.selectedSliceIndex = -1;
        Map<String, Double> totals = new HashMap<>();
        for (String cat : this.categoryColors.keySet()) {
            totals.put(cat, Double.valueOf(0.0d));
        }
        for (ExpenseModel exp : expenses) {
            String name = exp.getName().trim();
            String nameLower = name.toLowerCase();
            if (nameLower.contains("বাজার") || nameLower.contains("চাল") || nameLower.contains("আটা") || nameLower.contains("ডাল") || nameLower.contains("তেল")) {
                matchedCat = "🛍️ বাজার";
            } else if (nameLower.contains("ভাড়া") || nameLower.contains("ভড়া") || nameLower.contains("মেস") || nameLower.contains("দোকান") || nameLower.contains("বাড়ি")) {
                matchedCat = "🏠 ভাড়া";
            } else if (nameLower.contains("পরিবহন") || nameLower.contains("বাস") || nameLower.contains("রিকশা") || nameLower.contains("ভ্যান") || nameLower.contains("যাতায়াত") || nameLower.contains("গাড়ি")) {
                matchedCat = "🚌 পরিবহন";
            } else if (nameLower.contains("ওষুধ") || nameLower.contains("ঔষধ") || nameLower.contains("ডাক্তার") || nameLower.contains("মেডিকেল") || nameLower.contains("হাসপাতাল")) {
                matchedCat = "💊 ওষুধ";
            } else if (nameLower.contains("ব্যাংক") || nameLower.contains("রকেট") || nameLower.contains("বিকাশ") || nameLower.contains("নগদ") || nameLower.contains("সার্ভিস") || nameLower.contains("ট্যাক্স")) {
                matchedCat = "🏦 ব্যাংক";
            } else if (nameLower.contains("কাঁচামাল") || nameLower.contains("সবজি") || nameLower.contains("ফল") || nameLower.contains("মাছ") || nameLower.contains("মাংস") || nameLower.contains("ডিম")) {
                matchedCat = "🌾 কাঁচামাল";
            } else {
                matchedCat = name;
            }
            if (!totals.containsKey(matchedCat)) {
                totals.put(matchedCat, Double.valueOf(0.0d));
            }
            totals.put(matchedCat, Double.valueOf(totals.get(matchedCat).doubleValue() + exp.getAmount()));
        }
        double grandTotal = 0.0d;
        Iterator<Double> it = totals.values().iterator();
        while (it.hasNext()) {
            double val = it.next().doubleValue();
            grandTotal += val;
        }
        if (grandTotal > 0.0d) {
            float currentAngle = -90.0f;
            for (Map.Entry<String, Double> entry : totals.entrySet()) {
                if (entry.getValue().doubleValue() > 0.0d) {
                    float sweep = (float) ((entry.getValue().doubleValue() / grandTotal) * 360.0d);
                    if (this.categoryColors.containsKey(entry.getKey())) {
                        color = this.categoryColors.get(entry.getKey()).intValue();
                    } else {
                        color = generateColorForName(entry.getKey());
                    }
                    PieSlice slice = new PieSlice(entry.getKey(), entry.getValue().doubleValue(), color);
                    slice.startAngle = currentAngle;
                    slice.sweepAngle = sweep;
                    this.slices.add(slice);
                    currentAngle += sweep;
                }
            }
        }
        invalidate();
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent event) {
        if (this.slices.isEmpty()) {
            return super.onTouchEvent(event);
        }
        if (event.getAction() == 0 || event.getAction() == 2) {
            float tx = event.getX();
            float ty = event.getY();
            float centerX = getWidth() / 2.0f;
            float centerY = getHeight() / 2.0f;
            double distance = Math.sqrt(Math.pow(tx - centerX, 2.0d) + Math.pow(ty - centerY, 2.0d));
            int size = Math.min(getWidth(), getHeight());
            int radius = (size / 2) - 40;
            if (distance <= radius && distance >= radius * 0.25f) {
                double angle = Math.toDegrees(Math.atan2(ty - centerY, tx - centerX));
                double d = 360.0d;
                if (angle < 0.0d) {
                    angle += 360.0d;
                }
                int oldIndex = this.selectedSliceIndex;
                this.selectedSliceIndex = -1;
                int i = 0;
                while (true) {
                    if (i >= this.slices.size()) {
                        break;
                    }
                    PieSlice slice = this.slices.get(i);
                    double d2 = d;
                    float start = slice.startAngle;
                    while (start < 0.0f) {
                        start += 360.0f;
                    }
                    float f = slice.sweepAngle + start;
                    float tx2 = tx;
                    float ty2 = ty;
                    double relAngle = angle - start;
                    while (relAngle < 0.0d) {
                        relAngle += d2;
                    }
                    if (relAngle > slice.sweepAngle) {
                        i++;
                        d = d2;
                        tx = tx2;
                        ty = ty2;
                    } else {
                        this.selectedSliceIndex = i;
                        break;
                    }
                }
                if (this.selectedSliceIndex != oldIndex) {
                    performClick();
                    invalidate();
                    return true;
                }
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override // android.view.View
    public boolean performClick() {
        return super.performClick();
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        float f;
        int i;
        Canvas canvas2 = canvas;
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) {
            return;
        }
        RectF outerBg = new RectF(4.0f, 4.0f, width - 4, height - 4);
        canvas2.drawRoundRect(outerBg, dpToPx(16), dpToPx(16), this.paintBg);
        this.paintBorder.setColor(Color.parseColor("#1E293B"));
        canvas2.drawRoundRect(outerBg, dpToPx(16), dpToPx(16), this.paintBorder);
        int size = Math.min(width, height);
        int radius = (size / 2) - 40;
        float f2 = 2.0f;
        this.rectF.set((width / 2.0f) - radius, (height / 2.0f) - radius, (width / 2.0f) + radius, (height / 2.0f) + radius);
        boolean isEmpty = this.slices.isEmpty();
        Paint paint = this.paintArc;
        if (isEmpty) {
            paint.setColor(Color.parseColor("#151B26"));
            this.paintArc.setStyle(Paint.Style.FILL);
            canvas2.drawCircle(width / 2.0f, height / 2.0f, radius, this.paintArc);
            this.paintBorder.setColor(Color.parseColor("#334155"));
            canvas2.drawCircle(width / 2.0f, height / 2.0f, radius, this.paintBorder);
            this.paintText.setTextSize(radius * 0.13f);
            this.paintText.setColor(Color.parseColor("#64748B"));
            canvas2.drawText("কোনো রেকর্ড নেই", width / 2.0f, (height / 2.0f) + 6.0f, this.paintText);
            return;
        }
        paint.setStyle(Paint.Style.FILL);
        int i2 = 0;
        while (i2 < this.slices.size()) {
            PieSlice slice = this.slices.get(i2);
            this.paintArc.setColor(slice.color);
            if (i2 == this.selectedSliceIndex) {
                canvas2.save();
                float midAngleStr = slice.startAngle + (slice.sweepAngle / f2);
                double rad = Math.toRadians(midAngleStr);
                f = f2;
                i = i2;
                float dx = (float) (18.0f * Math.cos(rad));
                float dy = (float) (18.0f * Math.sin(rad));
                canvas2.translate(dx, dy);
                this.paintGlow.setColor(slice.color);
                this.paintGlow.setAlpha(60);
                RectF rectF = this.rectF;
                float dy2 = slice.startAngle;
                canvas2.drawArc(rectF, dy2, slice.sweepAngle, true, this.paintGlow);
                this.paintArc.setAlpha(255);
                canvas2 = canvas;
                canvas2.drawArc(this.rectF, slice.startAngle, slice.sweepAngle, true, this.paintArc);
                canvas.restore();
            } else {
                f = f2;
                i = i2;
                this.paintArc.setAlpha(this.selectedSliceIndex >= 0 ? 120 : 255);
                canvas2 = canvas;
                canvas2.drawArc(this.rectF, slice.startAngle, slice.sweepAngle, true, this.paintArc);
            }
            i2 = i + 1;
            f2 = f;
        }
        float f3 = f2;
        this.paintCenter.setColor(Color.parseColor("#0B0E14"));
        canvas2.drawCircle(width / f3, height / f3, radius * 0.58f, this.paintCenter);
        this.paintBorder.setColor(Color.parseColor("#1E293B"));
        canvas2.drawCircle(width / f3, height / f3, radius * 0.58f, this.paintBorder);
        float f4 = 0.14f;
        float f5 = 0.11f;
        if (this.selectedSliceIndex >= 0 && this.selectedSliceIndex < this.slices.size()) {
            PieSlice sel = this.slices.get(this.selectedSliceIndex);
            this.paintText.setTextSize(radius * 0.11f);
            this.paintText.setColor(Color.parseColor("#00F0FF"));
            canvas2.drawText(sel.name, width / f3, (height / f3) - (radius * 0.14f), this.paintText);
            this.paintText.setTextSize(radius * 0.2f);
            this.paintText.setColor(Color.parseColor("#FF007F"));
            canvas2.drawText("৳" + formatCompact(sel.value), width / f3, (height / f3) + (radius * 0.08f), this.paintText);
            this.paintText.setTextSize(radius * 0.08f);
            this.paintText.setColor(Color.parseColor("#8F9CAE"));
            double total = 0.0d;
            for (PieSlice s : this.slices) {
                total += s.value;
            }
            String percentage = String.format("%.1f%%", Double.valueOf((sel.value / total) * 100.0d));
            canvas2.drawText(percentage, width / f3, (height / f3) + (radius * 0.25f), this.paintText);
            return;
        }
        double totalExpense = 0.0d;
        for (PieSlice s2 : this.slices) {
            totalExpense += s2.value;
            f5 = f5;
            f4 = f4;
        }
        this.paintText.setTextSize(radius * f5);
        this.paintText.setColor(Color.parseColor("#8F9CAE"));
        canvas2.drawText("মোট খরচ", width / f3, (height / f3) - (radius * 0.1f), this.paintText);
        this.paintText.setTextSize(radius * 0.18f);
        this.paintText.setColor(Color.parseColor("#00FF66"));
        canvas2.drawText("৳" + formatCompact(totalExpense), width / f3, (height / f3) + (radius * f4), this.paintText);
        this.paintText.setTextSize(radius * 0.075f);
        this.paintText.setColor(Color.parseColor("#8F9CAE"));
        canvas2.drawText("খাতে চাপ দিয়ে দেখুন", width / f3, (height / f3) + (radius * 0.32f), this.paintText);
    }

    private int generateColorForName(String name) {
        if (name == null || name.isEmpty()) {
            return Color.parseColor("#8F9CAE");
        }
        int hash = name.hashCode();
        float hue = Math.abs(hash % 360);
        return Color.HSVToColor(new float[]{hue, 0.85f, 0.55f});
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private String formatCompact(double val) {
        if (val < 1000.0d) {
            return String.format("%.0f", Double.valueOf(val));
        }
        if (val < 100000.0d) {
            return String.format("%.1fK", Double.valueOf(val / 1000.0d));
        }
        return String.format("%.1fL", Double.valueOf(val / 100000.0d));
    }
}
