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
import java.text.SimpleDateFormat;
import java.util.Date;
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
        paint.setColor(Color.parseColor("#1E293B"));
        paint.setTextSize(14);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("হিসাবের সারসংক্ষেপ", 45, y + 25, paint);

        paint.setTextSize(12);
        paint.setTypeface(Typeface.DEFAULT);
        int summaryY = y + 50;

        drawSummaryLine(canvas, paint, "মোট খরচের যোগফল:", "৳ " + formatBengaliNumber(totalExpenses), 45, summaryY, pageWidth - 45);
        summaryY += 20;
        drawSummaryLine(canvas, paint, "বর্তমান ক্যাশ / টাকা:", "৳ " + formatBengaliNumber(availableCash), 45, summaryY, pageWidth - 45);
        summaryY += 20;
        drawSummaryLine(canvas, paint, "মোট ক্যাশ যোগফল:", "৳ " + formatBengaliNumber(totalSale), 45, summaryY, pageWidth - 45);
        summaryY += 20;
        drawSummaryLine(canvas, paint, "সাবেক ক্যাশ বাদ:", "- ৳ " + formatBengaliNumber(sabekCash), 45, summaryY, pageWidth - 45);
        summaryY += 25;

        paint.setColor(Color.parseColor("#16A34A"));
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(14);
        drawSummaryLine(canvas, paint, "দৈনিক নিট বেচা (বিক্রি):", "৳ " + formatBengaliNumber(dailySale), 45, summaryY, pageWidth - 45);

        paint.setColor(Color.parseColor("#94A3B8"));
        paint.setTextSize(10);
        paint.setTypeface(Typeface.DEFAULT);
        canvas.drawText("তৈরি করেছে: মাওয়া স্টোর ডিজিটাল অ্যাপ", 30, pageHeight - 30, paint);

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

    public static File exportBakiReportToPdf(Context context, List<BakiModel> bakiList, double totalDue) {
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

        // Header
        paint.setColor(Color.parseColor("#EA580C"));
        canvas.drawRect(0, 0, pageWidth, 90, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(22);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("মাওয়া স্টোর - বাকির খাতা ও খতিয়ান", 30, 42, paint);

        paint.setTextSize(13);
        paint.setTypeface(Typeface.DEFAULT);
        String dateHeader = "রিপোর্ট তৈরির তারিখ: " + new SimpleDateFormat("dd/MM/yyyy, hh:mm a", Locale.getDefault()).format(new Date());
        canvas.drawText(dateHeader, 30, 68, paint);

        int y = 120;
        paint.setColor(Color.parseColor("#1E293B"));
        paint.setTextSize(15);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("খরিদ্দারদের বকেয়া তালিকা:", 30, y, paint);

        y += 25;
        paint.setTextSize(11);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setColor(Color.parseColor("#475569"));
        canvas.drawText("নং", 35, y, paint);
        canvas.drawText("খরিদ্দারের নাম ও ফোন", 65, y, paint);
        canvas.drawText("বিবরণ / মেয়াদ", 260, y, paint);
        canvas.drawText("বকেয়া (৳)", pageWidth - 90, y, paint);

        paint.setColor(Color.parseColor("#FED7AA"));
        paint.setStrokeWidth(1);
        canvas.drawLine(30, y + 6, pageWidth - 30, y + 6, paint);
        y += 20;

        paint.setColor(Color.parseColor("#334155"));
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(11);

        if (bakiList != null && !bakiList.isEmpty()) {
            int serial = 1;
            for (BakiModel item : bakiList) {
                if (y > pageHeight - 140) {
                    break;
                }
                String sNum = toBengaliDigits(String.valueOf(serial++));
                String namePhone = item.getCustomerName();
                if (item.getPhone() != null && !item.getPhone().trim().isEmpty()) {
                    namePhone += " (" + item.getPhone() + ")";
                }
                String details = item.getDetails() != null ? item.getDetails() : "-";
                if (item.getDueDate() != null && !item.getDueDate().trim().isEmpty()) {
                    details += " | মেয়াদ: " + item.getDueDate();
                }
                String amount = "৳ " + formatBengaliNumber(item.getAmount());

                canvas.drawText(sNum, 35, y, paint);
                canvas.drawText(namePhone.length() > 28 ? namePhone.substring(0, 26) + ".." : namePhone, 65, y, paint);
                canvas.drawText(details.length() > 30 ? details.substring(0, 28) + ".." : details, 260, y, paint);
                
                paint.setColor(Color.parseColor("#DC2626"));
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                canvas.drawText(amount, pageWidth - 90, y, paint);

                paint.setColor(Color.parseColor("#F1F5F9"));
                paint.setTypeface(Typeface.DEFAULT);
                canvas.drawLine(30, y + 6, pageWidth - 30, y + 6, paint);
                paint.setColor(Color.parseColor("#334155"));

                y += 22;
            }
        } else {
            canvas.drawText("কোন বাকি হিসাব পাওয়া যায়নি", 65, y, paint);
            y += 25;
        }

        // Summary Card
        y = Math.max(y + 20, pageHeight - 110);
        paint.setColor(Color.parseColor("#FFF7ED"));
        canvas.drawRoundRect(30, y, pageWidth - 30, y + 60, 10, 10, paint);
        paint.setColor(Color.parseColor("#FFEDD5"));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        canvas.drawRoundRect(30, y, pageWidth - 30, y + 60, 10, 10, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#C2410C"));
        paint.setTextSize(14);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("মোট বকেয়া পাওনা (" + toBengaliDigits(String.valueOf(bakiList != null ? bakiList.size() : 0)) + " জন গ্রাহক):", 45, y + 36, paint);
        canvas.drawText("৳ " + formatBengaliNumber(totalDue), pageWidth - 140, y + 36, paint);

        paint.setColor(Color.parseColor("#94A3B8"));
        paint.setTextSize(10);
        paint.setTypeface(Typeface.DEFAULT);
        canvas.drawText("মাওয়া স্টোর ডিজিটাল অ্যাপ দ্বারা তৈরিকৃত", 30, pageHeight - 20, paint);

        document.finishPage(page);

        File outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (outputDir == null) {
            outputDir = context.getFilesDir();
        }
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        File outputFile = new File(outputDir, "MawaStore_Baki_Khata_" + System.currentTimeMillis() + ".pdf");

        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            document.writeTo(out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            document.close();
        }

        return outputFile;
    }

    public static File exportCustomerLedgerToPdf(Context context, BakiModel customer) {
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

        // Header
        paint.setColor(Color.parseColor("#EA580C"));
        canvas.drawRect(0, 0, pageWidth, 90, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(22);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("মাওয়া স্টোর - গ্রাহক খতিয়ান স্লিপ", 30, 42, paint);

        paint.setTextSize(13);
        paint.setTypeface(Typeface.DEFAULT);
        String dateHeader = "তারিখ: " + new SimpleDateFormat("dd/MM/yyyy, hh:mm a", Locale.getDefault()).format(new Date());
        canvas.drawText(dateHeader, 30, 68, paint);

        // Customer Info Card
        int y = 120;
        paint.setColor(Color.parseColor("#FFF7ED"));
        canvas.drawRoundRect(30, y, pageWidth - 30, y + 70, 10, 10, paint);
        paint.setColor(Color.parseColor("#FED7AA"));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        canvas.drawRoundRect(30, y, pageWidth - 30, y + 70, 10, 10, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.parseColor("#1E293B"));
        paint.setTextSize(15);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("খরিদ্দারের নাম: " + customer.getCustomerName(), 45, y + 28, paint);

        paint.setTextSize(12);
        paint.setTypeface(Typeface.DEFAULT);
        paint.setColor(Color.parseColor("#475569"));
        String ph = customer.getPhone() != null && !customer.getPhone().isEmpty() ? customer.getPhone() : "উল্লেখ নেই";
        canvas.drawText("মোবাইল: " + ph, 45, y + 52, paint);

        paint.setColor(Color.parseColor("#DC2626"));
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(15);
        canvas.drawText("বর্তমান বকেয়া: ৳ " + formatBengaliNumber(customer.getAmount()), pageWidth - 210, y + 42, paint);

        y += 100;
        paint.setColor(Color.parseColor("#1E293B"));
        paint.setTextSize(14);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("লেনদেনের বিস্তারিত খতিয়ান ইতিহাস:", 30, y, paint);

        y += 25;
        paint.setTextSize(11);
        paint.setColor(Color.parseColor("#475569"));
        canvas.drawText("তারিখ ও সময়", 35, y, paint);
        canvas.drawText("লেনদেন ধরণ", 150, y, paint);
        canvas.drawText("বিবরণ / নোট", 240, y, paint);
        canvas.drawText("পরিমাণ (৳)", 400, y, paint);
        canvas.drawText("অবশিষ্ট বকেয়া", pageWidth - 100, y, paint);

        paint.setColor(Color.parseColor("#E2E8F0"));
        paint.setStrokeWidth(1);
        canvas.drawLine(30, y + 6, pageWidth - 30, y + 6, paint);
        y += 20;

        List<BakiTransaction> list = customer.getTransactions();
        if (list != null && !list.isEmpty()) {
            for (BakiTransaction tx : list) {
                if (y > pageHeight - 80) break;
                paint.setTextSize(11);
                paint.setTypeface(Typeface.DEFAULT);
                paint.setColor(Color.parseColor("#334155"));

                String dateTime = tx.getDate() + (tx.getTime() != null ? " " + tx.getTime() : "");
                canvas.drawText(dateTime, 35, y, paint);

                boolean isPay = "JOMA".equalsIgnoreCase(tx.getType());
                paint.setColor(isPay ? Color.parseColor("#059669") : Color.parseColor("#DC2626"));
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                canvas.drawText(isPay ? "টাকা জমা" : "বাকি গ্রহণ", 150, y, paint);

                paint.setColor(Color.parseColor("#334155"));
                paint.setTypeface(Typeface.DEFAULT);
                String note = tx.getNote() != null ? tx.getNote() : "-";
                canvas.drawText(note.length() > 22 ? note.substring(0, 20) + ".." : note, 240, y, paint);

                paint.setColor(isPay ? Color.parseColor("#059669") : Color.parseColor("#DC2626"));
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                canvas.drawText((isPay ? "- ৳ " : "+ ৳ ") + formatBengaliNumber(tx.getAmount()), 400, y, paint);

                paint.setColor(Color.parseColor("#1E293B"));
                canvas.drawText("৳ " + formatBengaliNumber(tx.getBalanceAfter()), pageWidth - 100, y, paint);

                paint.setColor(Color.parseColor("#F1F5F9"));
                canvas.drawLine(30, y + 6, pageWidth - 30, y + 6, paint);
                y += 22;
            }
        } else {
            paint.setTextSize(11);
            paint.setColor(Color.parseColor("#64748B"));
            canvas.drawText("খাতায় প্রাথমিক বাকি হিসাব: ৳ " + formatBengaliNumber(customer.getAmount()) + " (তারিখ: " + customer.getDate() + ")", 35, y, paint);
            y += 25;
        }

        paint.setColor(Color.parseColor("#94A3B8"));
        paint.setTextSize(10);
        paint.setTypeface(Typeface.DEFAULT);
        canvas.drawText("মাওয়া স্টোর ডিজিটাল অ্যাপ দ্বারা তৈরিকৃত ভাউচার", 30, pageHeight - 20, paint);

        document.finishPage(page);

        File outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (outputDir == null) {
            outputDir = context.getFilesDir();
        }
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        String cleanName = customer.getCustomerName().replaceAll("[^a-zA-Z0-9\\p{L}]", "_");
        File outputFile = new File(outputDir, "Ledger_" + cleanName + "_" + System.currentTimeMillis() + ".pdf");

        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            document.writeTo(out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            document.close();
        }

        return outputFile;
    }

    private static void drawSummaryLine(Canvas canvas, Paint paint, String label, String value, int x1, int y, int x2) {
        canvas.drawText(label, x1, y, paint);
        float valueWidth = paint.measureText(value);
        canvas.drawText(value, x2 - valueWidth, y, paint);
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
        char[] banglaDigits = {'০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯'};
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
