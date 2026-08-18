package com.example;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Locale;

public class PdfExporter {

    public static File exportToPdf(Context context, List<ExpenseModel> expenses, double totalExpenses, double dailySale, double availableCash, double totalSale, double sabekCash, double result, String dateStr, String dayOfWeek) {
        PdfDocument document = new PdfDocument();
        int pageWidth = 595;
        int pageHeight = 842;

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        paint.setColor(Color.WHITE);
        canvas.drawRect(0, 0, pageWidth, pageHeight, paint);

        paint.setColor(Color.parseColor("#2563EB"));
        canvas.drawRect(0, 0, pageWidth, 90, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(22);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("মাওয়া স্টোর - দৈনিক ক্যাশ হিসাব", 30, 42, paint);

        paint.setTextSize(13);
        paint.setTypeface(Typeface.DEFAULT);
        String dateHeader = "তারিখ: " + (dateStr != null ? dateStr : "") + " (" + (dayOfWeek != null ? dayOfWeek : "") + ")";
        canvas.drawText(dateHeader, 30, 68, paint);

        int y = 120;
        paint.setColor(Color.parseColor("#1E293B"));
        paint.setTextSize(16);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("খরচের তালিকা:", 30, y, paint);

        y += 25;
        paint.setTextSize(12);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setColor(Color.parseColor("#475569"));
        canvas.drawText("নং", 35, y, paint);
        canvas.drawText("বিবরণ", 70, y, paint);
        canvas.drawText("টাকা (৳)", pageWidth - 90, y, paint);

        paint.setColor(Color.parseColor("#E2E8F0"));
        paint.setStrokeWidth(1);
        canvas.drawLine(30, y + 6, pageWidth - 30, y + 6, paint);
        y += 20;

        paint.setColor(Color.parseColor("#334155"));
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(12);

        if (expenses != null && !expenses.isEmpty()) {
            int serial = 1;
            for (ExpenseModel exp : expenses) {
                if (y > pageHeight - 220) {
                    break;
                }
                String sNum = toBengaliDigits(String.valueOf(serial++));
                String name = exp.getName() != null ? exp.getName() : "";
                String amount = "৳ " + formatBengaliNumber(exp.getAmount());

                canvas.drawText(sNum, 35, y, paint);
                canvas.drawText(name, 70, y, paint);
                canvas.drawText(amount, pageWidth - 90, y, paint);

                paint.setColor(Color.parseColor("#F1F5F9"));
                canvas.drawLine(30, y + 6, pageWidth - 30, y + 6, paint);
                paint.setColor(Color.parseColor("#334155"));

                y += 22;
            }
        } else {
            canvas.drawText("কোন খরচ লিপিবদ্ধ নেই", 70, y, paint);
            y += 25;
        }

        y = Math.max(y + 20, 520);

        paint.setColor(Color.parseColor("#F8FAFC"));
        canvas.drawRoundRect(30, y, pageWidth - 30, y + 170, 12, 12, paint);
        paint.setColor(Color.parseColor("#CBD5E1"));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        canvas.drawRoundRect(30, y, pageWidth - 30, y + 170, 12, 12, paint);
        paint.setStyle(Paint.Style.FILL);

        int sumY = y + 25;
        paint.setTextSize(13);
        paint.setColor(Color.parseColor("#1E293B"));
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("সারসংক্ষেপ খতিয়ান", 45, sumY, paint);

        sumY += 24;
        paint.setTextSize(12);
        paint.setTypeface(Typeface.DEFAULT);
        paint.setColor(Color.parseColor("#334155"));
        canvas.drawText("মোট খরচ:", 45, sumY, paint);
        canvas.drawText("৳ " + formatBengaliNumber(totalExpenses), pageWidth - 140, sumY, paint);

        sumY += 20;
        canvas.drawText("আজকের বেচা:", 45, sumY, paint);
        canvas.drawText("৳ " + formatBengaliNumber(dailySale), pageWidth - 140, sumY, paint);

        sumY += 20;
        canvas.drawText("আছে (হাতে ক্যাশ):", 45, sumY, paint);
        canvas.drawText("৳ " + formatBengaliNumber(availableCash), pageWidth - 140, sumY, paint);

        sumY += 20;
        canvas.drawText("সাবেক ক্যাশ:", 45, sumY, paint);
        canvas.drawText("৳ " + formatBengaliNumber(sabekCash), pageWidth - 140, sumY, paint);

        sumY += 20;
        canvas.drawText("মোট বেচা:", 45, sumY, paint);
        canvas.drawText("৳ " + formatBengaliNumber(totalSale), pageWidth - 140, sumY, paint);

        sumY += 26;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        if (result >= 0) {
            paint.setColor(Color.parseColor("#059669"));
            canvas.drawText("🟢 ফলাফল (লাভ):", 45, sumY, paint);
        } else {
            paint.setColor(Color.parseColor("#DC2626"));
            canvas.drawText("🔴 ফলাফল (ঘাটতি):", 45, sumY, paint);
        }
        canvas.drawText("৳ " + formatBengaliNumber(Math.abs(result)), pageWidth - 140, sumY, paint);

        paint.setColor(Color.parseColor("#94A3B8"));
        paint.setTextSize(10);
        paint.setTypeface(Typeface.DEFAULT);
        canvas.drawText("মাওয়া স্টোর ডিজিটাল খাতা অ্যাপ দ্বারা প্রস্তুতকৃত", 30, pageHeight - 30, paint);

        document.finishPage(page);

        File outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (outputDir == null) {
            outputDir = context.getFilesDir();
        }
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        String cleanDate = dateStr != null ? dateStr.replace("/", "-") : "report";
        File outputFile = new File(outputDir, "MawaStore_Report_" + cleanDate + ".pdf");

        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            document.writeTo(out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            document.close();
        }

        return outputFile;
    }

    public static String toBengaliDigits(String input) {
        if (input == null) return "";
        char[] banglaDigits = {'০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯'};
        StringBuilder banglaSb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c >= '0' && c <= '9') {
                banglaSb.append(banglaDigits[c - '0']);
            } else {
                banglaSb.append(c);
            }
        }
        return banglaSb.toString();
    }

    public static String formatBengaliNumber(double number) {
        String inputStr;
        if (number == ((long) number)) {
            inputStr = String.format(Locale.US, "%d", Long.valueOf((long) number));
        } else {
            inputStr = String.format(Locale.US, "%.2f", Double.valueOf(number));
        }
        String formatted = formatWithIndianStyle(inputStr);
        StringBuilder banglaSb = new StringBuilder();
        char[] banglaDigits = {2534, 2535, 2536, 2537, 2538, 2539, 2540, 2541, 2542, 2543};
        for (int i = 0; i < formatted.length(); i++) {
            char c = formatted.charAt(i);
            if (c >= '0' && c <= '9') {
                banglaSb.append(banglaDigits[c - '0']);
            } else {
                banglaSb.append(c);
            }
        }
        return banglaSb.toString();
    }

    private static String formatWithIndianStyle(String numStr) {
        if (numStr == null || numStr.isEmpty()) {
            return "0";
        }
        String integerPart = numStr;
        String decimalPart = "";
        int dotIndex = numStr.indexOf(46);
        if (dotIndex != -1) {
            integerPart = numStr.substring(0, dotIndex);
            decimalPart = numStr.substring(dotIndex);
        }
        if (integerPart.length() <= 3) {
            return integerPart + decimalPart;
        }
        String lastThree = integerPart.substring(integerPart.length() - 3);
        String remaining = integerPart.substring(0, integerPart.length() - 3);
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = remaining.length() - 1; i >= 0; i--) {
            sb.append(remaining.charAt(i));
            count++;
            if (count == 2 && i != 0) {
                sb.append(',');
                count = 0;
            }
        }
        return sb.reverse().toString() + "," + lastThree + decimalPart;
    }
}
