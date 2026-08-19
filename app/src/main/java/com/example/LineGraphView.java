package com.example;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.example.MainViewModel;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes5.dex */
public class LineGraphView extends View {
    private float[] cachedXCoords;
    private List<MainViewModel.DaySummary> historyData;
    private final Paint paintBg;
    private final Paint paintBorder;
    private final Paint paintExpensesFill;
    private final Paint paintExpensesLine;
    private final Paint paintGrid;
    private final Paint paintHud;
    private final Paint paintLabelPoint;
    private final Paint paintSalesFill;
    private final Paint paintSalesLine;
    private final Paint paintText;
    private int selectedIndex;

    public LineGraphView(Context context) {
        super(context);
        this.paintGrid = new Paint(1);
        this.paintSalesLine = new Paint(1);
        this.paintExpensesLine = new Paint(1);
        this.paintSalesFill = new Paint(1);
        this.paintExpensesFill = new Paint(1);
        this.paintText = new Paint(1);
        this.paintLabelPoint = new Paint(1);
        this.paintBg = new Paint(1);
        this.paintBorder = new Paint(1);
        this.paintHud = new Paint(1);
        this.historyData = new ArrayList();
        this.selectedIndex = -1;
        init();
    }

    public LineGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.paintGrid = new Paint(1);
        this.paintSalesLine = new Paint(1);
        this.paintExpensesLine = new Paint(1);
        this.paintSalesFill = new Paint(1);
        this.paintExpensesFill = new Paint(1);
        this.paintText = new Paint(1);
        this.paintLabelPoint = new Paint(1);
        this.paintBg = new Paint(1);
        this.paintBorder = new Paint(1);
        this.paintHud = new Paint(1);
        this.historyData = new ArrayList();
        this.selectedIndex = -1;
        init();
    }

    public LineGraphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.paintGrid = new Paint(1);
        this.paintSalesLine = new Paint(1);
        this.paintExpensesLine = new Paint(1);
        this.paintSalesFill = new Paint(1);
        this.paintExpensesFill = new Paint(1);
        this.paintText = new Paint(1);
        this.paintLabelPoint = new Paint(1);
        this.paintBg = new Paint(1);
        this.paintBorder = new Paint(1);
        this.paintHud = new Paint(1);
        this.historyData = new ArrayList();
        this.selectedIndex = -1;
        init();
    }

    private void init() {
        this.paintGrid.setColor(Color.parseColor("#E2E8F0"));
        this.paintGrid.setStrokeWidth(1.5f);
        this.paintGrid.setStyle(Paint.Style.STROKE);
        this.paintGrid.setPathEffect(new DashPathEffect(new float[]{8.0f, 8.0f}, 0.0f));
        this.paintSalesLine.setColor(Color.parseColor("#2563EB"));
        this.paintSalesLine.setStrokeWidth(5.0f);
        this.paintSalesLine.setStyle(Paint.Style.STROKE);
        this.paintSalesLine.setStrokeCap(Paint.Cap.ROUND);
        this.paintExpensesLine.setColor(Color.parseColor("#EF4444"));
        this.paintExpensesLine.setStrokeWidth(5.0f);
        this.paintExpensesLine.setStyle(Paint.Style.STROKE);
        this.paintExpensesLine.setStrokeCap(Paint.Cap.ROUND);
        this.paintText.setFakeBoldText(true);
        this.paintLabelPoint.setStyle(Paint.Style.FILL);
        this.paintBg.setColor(Color.parseColor("#FFFFFF"));
        this.paintBg.setStyle(Paint.Style.FILL);
        this.paintBorder.setColor(Color.parseColor("#E2E8F0"));
        this.paintBorder.setStrokeWidth(2.0f);
        this.paintBorder.setStyle(Paint.Style.STROKE);
        this.paintHud.setStyle(Paint.Style.FILL);
    }

    public void setData(List<MainViewModel.DaySummary> summaries) {
        this.historyData = new ArrayList();
        this.selectedIndex = -1;
        if (summaries != null && !summaries.isEmpty()) {
            int count = Math.min(summaries.size(), 7);
            for (int i = count - 1; i >= 0; i--) {
                this.historyData.add(summaries.get(i));
            }
        }
        invalidate();
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent event) {
        if (this.historyData.isEmpty() || this.cachedXCoords == null) {
            return super.onTouchEvent(event);
        }
        if (event.getAction() == 0 || event.getAction() == 2) {
            float tx = event.getX();
            int oldIndex = this.selectedIndex;
            int bestIndex = 0;
            float minDistance = Float.MAX_VALUE;
            for (int i = 0; i < this.cachedXCoords.length; i++) {
                float dist = Math.abs(this.cachedXCoords[i] - tx);
                if (dist < minDistance) {
                    minDistance = dist;
                    bestIndex = i;
                }
            }
            if (getParent() != null) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
            this.selectedIndex = bestIndex;
            if (this.selectedIndex != oldIndex) {
                performClick();
                invalidate();
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override // android.view.View
    public boolean performClick() {
        return super.performClick();
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        int chartHeight;
        int height;
        Path salesFillPath;
        int i;
        super.onDraw(canvas);
        int width = getWidth();
        int height2 = getHeight();
        if (width != 0 && height2 != 0) {
            RectF roundRect = new RectF(4.0f, 4.0f, width - 4, height2 - 4);
            canvas.drawRoundRect(roundRect, dpToPx(16.0f), dpToPx(16.0f), this.paintBg);
            this.paintBorder.setColor(Color.parseColor("#E2E8F0"));
            canvas.drawRoundRect(roundRect, dpToPx(16.0f), dpToPx(16.0f), this.paintBorder);
            int paddingLeft = dpToPx(40.0f);
            int paddingRight = dpToPx(16.0f);
            int paddingTop = dpToPx(24.0f);
            int paddingBottom = dpToPx(24.0f);
            int chartWidth = (width - paddingLeft) - paddingRight;
            int chartHeight2 = (height2 - paddingTop) - paddingBottom;
            if (this.historyData == null) {
                chartHeight = width;
                height = height2;
            } else {
                if (!this.historyData.isEmpty()) {
                    double maxVal = 1000.0d;
                    for (MainViewModel.DaySummary sum : this.historyData) {
                        if (sum.computedSale > maxVal) {
                            maxVal = sum.computedSale;
                        }
                        if (sum.expenses > maxVal) {
                            maxVal = sum.expenses;
                        }
                    }
                    double maxVal2 = maxVal * 1.25d;
                    int pointsCount = this.historyData.size();
                    float stepX = pointsCount > 1 ? chartWidth / (pointsCount - 1) : chartWidth;
                    canvas.drawLine(paddingLeft, paddingTop, paddingLeft + chartWidth, paddingTop, this.paintGrid);
                    canvas.drawLine(paddingLeft, (chartHeight2 / 2.0f) + paddingTop, paddingLeft + chartWidth, (chartHeight2 / 2.0f) + paddingTop, this.paintGrid);
                    canvas.drawLine(paddingLeft, paddingTop + chartHeight2, paddingLeft + chartWidth, paddingTop + chartHeight2, this.paintGrid);
                    this.paintText.setTextAlign(Paint.Align.LEFT);
                    this.paintText.setTextSize(dpToPx(9.0f));
                    this.paintText.setColor(Color.parseColor("#94A3B8"));
                    canvas.drawText("৳" + formatCompact(maxVal2), paddingLeft + dpToPx(4.0f), paddingTop - dpToPx(4.0f), this.paintText);
                    canvas.drawText("৳" + formatCompact(maxVal2 / 2.0d), dpToPx(4.0f) + paddingLeft, (paddingTop + (chartHeight2 / 2.0f)) - dpToPx(4.0f), this.paintText);
                    canvas.drawText("৳0", dpToPx(4.0f) + paddingLeft, (paddingTop + chartHeight2) - dpToPx(4.0f), this.paintText);
                    Path salesPath = new Path();
                    Path expensesPath = new Path();
                    Path salesFillPath2 = new Path();
                    Path expensesFillPath = new Path();
                    float[] xCoords = new float[pointsCount];
                    float[] ySales = new float[pointsCount];
                    float[] yExp = new float[pointsCount];
                    int i2 = 0;
                    while (i2 < pointsCount) {
                        float[] ySales2 = ySales;
                        MainViewModel.DaySummary sum2 = this.historyData.get(i2);
                        float[] yExp2 = yExp;
                        xCoords[i2] = paddingLeft + (i2 * stepX);
                        int width2 = width;
                        int height3 = height2;
                        ySales2[i2] = (float) ((paddingTop + chartHeight2) - (chartHeight2 * (sum2.computedSale / maxVal2)));
                        yExp2[i2] = (float) ((paddingTop + chartHeight2) - (chartHeight2 * (sum2.expenses / maxVal2)));
                        if (i2 == 0) {
                            salesPath.moveTo(xCoords[i2], ySales2[i2]);
                            expensesPath.moveTo(xCoords[i2], yExp2[i2]);
                            salesFillPath2.moveTo(xCoords[i2], paddingTop + chartHeight2);
                            salesFillPath2.lineTo(xCoords[i2], ySales2[i2]);
                            expensesFillPath.moveTo(xCoords[i2], paddingTop + chartHeight2);
                            expensesFillPath.lineTo(xCoords[i2], yExp2[i2]);
                        } else {
                            salesPath.lineTo(xCoords[i2], ySales2[i2]);
                            expensesPath.lineTo(xCoords[i2], yExp2[i2]);
                            salesFillPath2.lineTo(xCoords[i2], ySales2[i2]);
                            expensesFillPath.lineTo(xCoords[i2], yExp2[i2]);
                        }
                        if (i2 == pointsCount - 1) {
                            salesFillPath2.lineTo(xCoords[i2], paddingTop + chartHeight2);
                            salesFillPath2.close();
                            expensesFillPath.lineTo(xCoords[i2], paddingTop + chartHeight2);
                            expensesFillPath.close();
                        }
                        i2++;
                        ySales = ySales2;
                        yExp = yExp2;
                        width = width2;
                        height2 = height3;
                    }
                    int width3 = width;
                    float[] ySales3 = ySales;
                    float[] yExp3 = yExp;
                    this.cachedXCoords = xCoords;
                    this.paintSalesFill.setShader(new LinearGradient(0.0f, paddingTop, 0.0f, paddingTop + chartHeight2, Color.parseColor("#302563EB"), Color.parseColor("#002563EB"), Shader.TileMode.CLAMP));
                    canvas.drawPath(salesFillPath2, this.paintSalesFill);
                    this.paintExpensesFill.setShader(new LinearGradient(0.0f, paddingTop, 0.0f, paddingTop + chartHeight2, Color.parseColor("#20EF4444"), Color.parseColor("#00EF4444"), Shader.TileMode.CLAMP));
                    canvas.drawPath(expensesFillPath, this.paintExpensesFill);
                    canvas.drawPath(salesPath, this.paintSalesLine);
                    canvas.drawPath(expensesPath, this.paintExpensesLine);
                    int i3 = 0;
                    while (i3 < pointsCount) {
                        MainViewModel.DaySummary sum3 = this.historyData.get(i3);
                        Path expensesPath2 = expensesPath;
                        if (i3 != this.selectedIndex) {
                            salesFillPath = salesFillPath2;
                            i = i3;
                        } else {
                            this.paintLabelPoint.setColor(Color.parseColor("#402563EB"));
                            float f = xCoords[i3];
                            float f2 = ySales3[i3];
                            salesFillPath = salesFillPath2;
                            i = i3;
                            int i4 = dpToPx(8.0f);
                            canvas.drawCircle(f, f2, i4, this.paintLabelPoint);
                            this.paintLabelPoint.setColor(Color.parseColor("#40EF4444"));
                            canvas.drawCircle(xCoords[i], yExp3[i], dpToPx(8.0f), this.paintLabelPoint);
                        }
                        this.paintLabelPoint.setColor(Color.parseColor("#2563EB"));
                        canvas.drawCircle(xCoords[i], ySales3[i], dpToPx(4.0f), this.paintLabelPoint);
                        this.paintLabelPoint.setColor(Color.parseColor("#EF4444"));
                        canvas.drawCircle(xCoords[i], yExp3[i], dpToPx(4.0f), this.paintLabelPoint);
                        this.paintText.setTextAlign(Paint.Align.CENTER);
                        this.paintText.setTextSize(dpToPx(9.0f));
                        this.paintText.setColor(Color.parseColor("#64748B"));
                        String label = sum3.dateKey;
                        if (label.length() >= 5) {
                            label = label.substring(0, 5);
                        }
                        canvas.drawText(label, xCoords[i], paddingTop + chartHeight2 + dpToPx(15.0f), this.paintText);
                        i3 = i + 1;
                        expensesPath = expensesPath2;
                        salesFillPath2 = salesFillPath;
                    }
                    if (this.selectedIndex >= 0 && this.selectedIndex < pointsCount) {
                        MainViewModel.DaySummary sel = this.historyData.get(this.selectedIndex);
                        float selX = xCoords[this.selectedIndex];
                        this.paintGrid.setColor(Color.parseColor("#402563EB"));
                        this.paintGrid.setStrokeWidth(2.0f);
                        canvas.drawLine(selX, paddingTop, selX, paddingTop + chartHeight2, this.paintGrid);
                        float hudWidth = dpToPx(130.0f);
                        float hudHeight = dpToPx(60.0f);
                        float hudLeft = selX - (hudWidth / 2.0f);
                        if (hudLeft < paddingLeft) {
                            hudLeft = paddingLeft + dpToPx(4.0f);
                        }
                        if (hudLeft + hudWidth > width3) {
                            hudLeft = (width3 - hudWidth) - dpToPx(4.0f);
                        }
                        float hudTop = paddingTop - dpToPx(8.0f);
                        if (hudTop < 10.0f) {
                            hudTop = 10.0f;
                        }
                        float hudHeight2 = hudLeft + hudWidth;
                        RectF hudRect = new RectF(hudLeft, hudTop, hudHeight2, hudTop + hudHeight);
                        this.paintHud.setColor(Color.parseColor("#FFFFFF"));
                        float hudTop2 = hudTop;
                        canvas.drawRoundRect(hudRect, dpToPx(10.0f), dpToPx(10.0f), this.paintHud);
                        this.paintBorder.setColor(Color.parseColor("#2563EB"));
                        this.paintBorder.setStrokeWidth(1.5f);
                        canvas.drawRoundRect(hudRect, dpToPx(10.0f), dpToPx(10.0f), this.paintBorder);
                        float textStartX = dpToPx(8.0f) + hudLeft;
                        this.paintText.setTextAlign(Paint.Align.LEFT);
                        this.paintText.setTextSize(dpToPx(8.5f));
                        this.paintText.setColor(Color.parseColor("#64748B"));
                        canvas.drawText("তারিখ: " + sel.dateKey, textStartX, hudTop2 + dpToPx(14.0f), this.paintText);
                        this.paintText.setColor(Color.parseColor("#2563EB"));
                        canvas.drawText("● বেচা: ৳" + formatCompact(sel.computedSale), textStartX, hudTop2 + dpToPx(28.0f), this.paintText);
                        this.paintText.setColor(Color.parseColor("#EF4444"));
                        canvas.drawText("● খরচ: ৳" + formatCompact(sel.expenses), textStartX, hudTop2 + dpToPx(42.0f), this.paintText);
                        double netProfit = sel.computedSale - sel.expenses;
                        this.paintText.setColor(Color.parseColor(netProfit >= 0.0d ? "#059669" : "#EF4444"));
                        canvas.drawText("● লাভ: ৳" + formatCompact(netProfit), textStartX, hudTop2 + dpToPx(54.0f), this.paintText);
                        return;
                    }
                    this.paintText.setTextAlign(Paint.Align.CENTER);
                    this.paintText.setTextSize(dpToPx(9.0f));
                    this.paintText.setColor(Color.parseColor("#94A3B8"));
                    canvas.drawText("পয়েন্টে আঙুল ছুঁয়ে স্পর্শ করুন", width3 / 2.0f, paddingTop - dpToPx(6.0f), this.paintText);
                    return;
                }
                chartHeight = width;
                height = height2;
            }
            this.paintText.setTextAlign(Paint.Align.CENTER);
            this.paintText.setTextSize(dpToPx(14.0f));
            this.paintText.setColor(Color.parseColor("#94A3B8"));
            canvas.drawText("নিবন্ধিত দিনের ডাটা পর্যাপ্ত নয়", chartHeight / 2.0f, height / 2.0f, this.paintText);
        }
    }

    private int dpToPx(float dp) {
        return (int) (getResources().getDisplayMetrics().density * dp);
    }

    private String formatCompact(double val) {
        if (Math.abs(val) < 1000.0d) {
            return String.format("%.0f", Double.valueOf(val));
        }
        if (Math.abs(val) < 100000.0d) {
            return String.format("%.1fk", Double.valueOf(val / 1000.0d));
        }
        return String.format("%.1fL", Double.valueOf(val / 100000.0d));
    }
}
