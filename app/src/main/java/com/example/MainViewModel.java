package com.example;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/* loaded from: classes5.dex */
public class MainViewModel extends AndroidViewModel {
    private final Calendar activeCalendar;
    private final MutableLiveData<String> activeDateString;
    private final MutableLiveData<String> activeDayOfWeek;
    private final MutableLiveData<Double> availableCash;
    private final MutableLiveData<Double> calculationResult;
    private final MutableLiveData<Double> dailySale;
    private final MutableLiveData<List<ExpenseModel>> expenses;
    private final MutableLiveData<Double> sabekCash;
    private final StorageManager storageManager;
    private final AccountingService accountingService;
    private final MutableLiveData<Double> totalExpenses;
    private final MutableLiveData<Double> totalSale;

    // Advanced Accounting LiveData
    private final MutableLiveData<AccountingService.DailyAccountingSummary> dailySummary;
    private final MutableLiveData<Double> cashSales;
    private final MutableLiveData<Double> creditSales;
    private final MutableLiveData<Double> bakiCollection;
    private final MutableLiveData<Double> totalPurchases;
    private final MutableLiveData<Double> totalOperatingExpenses;
    private final MutableLiveData<Double> expectedClosingCash;
    private final MutableLiveData<Double> estimatedGrossProfit;
    private final MutableLiveData<Double> estimatedNetProfit;

    /* loaded from: classes5.dex */
    public static class DaySummary {
        public double availableCash;
        public double computedSale;
        public String dateKey;
        public double expenses;
        public double margin;
        public double sabek;
        public double purchases;
        public double operatingExpenses;
        public double creditSales;
        public double bakiCollection;
        public double estimatedProfit;
    }

    public MainViewModel(Application application) {
        super(application);
        this.expenses = new MutableLiveData<>(new ArrayList());
        Double valueOf = Double.valueOf(0.0d);
        this.sabekCash = new MutableLiveData<>(valueOf);
        this.availableCash = new MutableLiveData<>(valueOf);
        this.dailySale = new MutableLiveData<>(valueOf);
        this.totalExpenses = new MutableLiveData<>(valueOf);
        this.totalSale = new MutableLiveData<>(valueOf);
        this.calculationResult = new MutableLiveData<>(valueOf);

        this.dailySummary = new MutableLiveData<>(null);
        this.cashSales = new MutableLiveData<>(valueOf);
        this.creditSales = new MutableLiveData<>(valueOf);
        this.bakiCollection = new MutableLiveData<>(valueOf);
        this.totalPurchases = new MutableLiveData<>(valueOf);
        this.totalOperatingExpenses = new MutableLiveData<>(valueOf);
        this.expectedClosingCash = new MutableLiveData<>(valueOf);
        this.estimatedGrossProfit = new MutableLiveData<>(valueOf);
        this.estimatedNetProfit = new MutableLiveData<>(valueOf);

        this.activeCalendar = Calendar.getInstance();
        this.activeDateString = new MutableLiveData<>("");
        this.activeDayOfWeek = new MutableLiveData<>("");
        this.storageManager = StorageManager.getInstance(application);
        this.accountingService = AccountingService.getInstance(application);
        updateActiveDateState();
    }

    public LiveData<String> getActiveDateString() {
        return this.activeDateString;
    }

    public LiveData<String> getActiveDayOfWeek() {
        return this.activeDayOfWeek;
    }

    public LiveData<AccountingService.DailyAccountingSummary> getDailySummary() {
        return this.dailySummary;
    }

    public LiveData<Double> getCashSales() {
        return this.cashSales;
    }

    public LiveData<Double> getCreditSales() {
        return this.creditSales;
    }

    public LiveData<Double> getBakiCollection() {
        return this.bakiCollection;
    }

    public LiveData<Double> getTotalPurchases() {
        return this.totalPurchases;
    }

    public LiveData<Double> getTotalOperatingExpenses() {
        return this.totalOperatingExpenses;
    }

    public LiveData<Double> getExpectedClosingCash() {
        return this.expectedClosingCash;
    }

    public LiveData<Double> getEstimatedGrossProfit() {
        return this.estimatedGrossProfit;
    }

    public LiveData<Double> getEstimatedNetProfit() {
        return this.estimatedNetProfit;
    }

    public AccountingService getAccountingService() {
        return this.accountingService;
    }

    public void moveToPreviousDay() {
        this.activeCalendar.add(6, -1);
        updateActiveDateState();
    }

    public void moveToNextDay() {
        this.activeCalendar.add(6, 1);
        updateActiveDateState();
    }

    public void selectDate(int year, int month, int dayOfMonth) {
        this.activeCalendar.set(1, year);
        this.activeCalendar.set(2, month);
        this.activeCalendar.set(5, dayOfMonth);
        updateActiveDateState();
    }

    private void updateActiveDateState() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
        this.activeDateString.setValue(dateFormat.format(this.activeCalendar.getTime()));
        this.activeDayOfWeek.setValue(calculateBengaliDayOfWeek(this.activeCalendar));
        loadSavedData();
    }

    private String calculateBengaliDayOfWeek(Calendar cal) {
        int day = cal.get(7);
        switch (day) {
            case 1:
                return "রবিবার";
            case 2:
                return "সোমবার";
            case 3:
                return "মঙ্গলবার";
            case 4:
                return "বুধবার";
            case 5:
                return "বৃহস্পতিবার";
            case 6:
                return "শুক্রবার";
            case 7:
                return "শনিবার";
            default:
                return "দৈনিক দিন";
        }
    }

    public String getActiveDateKey() {
        if (this.activeDateString.getValue() == null || this.activeDateString.getValue().isEmpty()) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
            return dateFormat.format(Calendar.getInstance().getTime());
        }
        return this.activeDateString.getValue();
    }

    public LiveData<List<ExpenseModel>> getExpenses() {
        return this.expenses;
    }

    public LiveData<Double> getSabekCash() {
        return this.sabekCash;
    }

    public LiveData<Double> getDailySale() {
        return this.dailySale;
    }

    public LiveData<Double> getAvailableCash() {
        return this.availableCash;
    }

    public LiveData<Double> getTotalExpenses() {
        return this.totalExpenses;
    }

    public LiveData<Double> getTotalSale() {
        return this.totalSale;
    }

    public LiveData<Double> getCalculationResult() {
        return this.calculationResult;
    }

    public void loadSavedData() {
        String dateKey = getActiveDateKey();
        List<ExpenseModel> savedExpenses = this.storageManager.loadExpenses(dateKey);
        this.expenses.setValue(savedExpenses);
        double savedSabekCash = this.storageManager.loadSabekCash(dateKey);
        this.sabekCash.setValue(Double.valueOf(savedSabekCash));
        double savedAvailableCash = this.storageManager.loadAvailableCash(dateKey);
        this.availableCash.setValue(Double.valueOf(savedAvailableCash));
        calculateTotals();
    }

    private void registerActiveDate() {
        this.storageManager.saveActiveDate(getActiveDateKey());
    }

    public void setSabekCash(double cash) {
        this.sabekCash.setValue(Double.valueOf(cash));
        this.storageManager.saveSabekCash(getActiveDateKey(), cash);
        registerActiveDate();
        calculateTotals();
    }

    public void setAvailableCash(double cash) {
        this.availableCash.setValue(Double.valueOf(cash));
        this.storageManager.saveAvailableCash(getActiveDateKey(), cash);
        registerActiveDate();
        calculateTotals();
    }

    public double getSuggestedSabekCash() {
        return this.storageManager.getPreviousDayClosingCash(getActiveDateKey());
    }

    public boolean addExpense(String name, double amount) {
        if (name != null && !name.trim().isEmpty()) {
            if (amount > 0.0d) {
                new SimpleDateFormat("dd-MM-yyyy", Locale.US);
                SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.US);
                String dateStr = getActiveDateKey();
                String timeStr = timeFormat.format(Calendar.getInstance().getTime());
                ExpenseModel newExpense = new ExpenseModel(UUID.randomUUID().toString(), name.trim(), amount, dateStr, timeStr);
                List<ExpenseModel> currentExpenses = this.expenses.getValue();
                if (currentExpenses == null) {
                    currentExpenses = new ArrayList();
                }
                currentExpenses.add(0, newExpense);
                this.expenses.setValue(currentExpenses);
                this.storageManager.saveExpenses(getActiveDateKey(), currentExpenses);
                this.storageManager.saveProductSuggestion(name.trim());
                registerActiveDate();
                calculateTotals();
                return true;
            }
        }
        return false;
    }

    public boolean updateExpense(String id, String newName, double newAmount) {
        if (id == null || newName == null || newName.trim().isEmpty() || newAmount <= 0.0d) {
            return false;
        }
        List<ExpenseModel> currentExpenses = this.expenses.getValue();
        if (currentExpenses == null) {
            return false;
        }
        boolean updated = false;
        for (ExpenseModel exp : currentExpenses) {
            if (exp.getId().equals(id)) {
                exp.setName(newName.trim());
                exp.setAmount(newAmount);
                updated = true;
                break;
            }
        }
        if (updated) {
            this.expenses.setValue(currentExpenses);
            this.storageManager.saveExpenses(getActiveDateKey(), currentExpenses);
            this.storageManager.saveProductSuggestion(newName.trim());
            calculateTotals();
            return true;
        }
        return false;
    }

    public void deleteExpense(String id) {
        List<ExpenseModel> currentExpenses = this.expenses.getValue();
        if (currentExpenses == null) {
            return;
        }
        ExpenseModel toRemove = null;
        Iterator<ExpenseModel> it = currentExpenses.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            ExpenseModel exp = it.next();
            if (exp.getId().equals(id)) {
                toRemove = exp;
                break;
            }
        }
        if (toRemove != null) {
            currentExpenses.remove(toRemove);
            this.expenses.setValue(currentExpenses);
            this.storageManager.saveExpenses(getActiveDateKey(), currentExpenses);
            calculateTotals();
        }
    }

    public void clearAllData() {
        String dateKey = getActiveDateKey();
        this.storageManager.saveExpenses(dateKey, new ArrayList());
        StorageManager storageManager = this.storageManager;
        Double valueOf = Double.valueOf(0.0d);
        storageManager.saveSabekCash(dateKey, 0.0d);
        this.storageManager.saveAvailableCash(dateKey, 0.0d);
        this.expenses.setValue(new ArrayList());
        this.sabekCash.setValue(valueOf);
        this.availableCash.setValue(valueOf);
        calculateTotals();
    }

    private void calculateTotals() {
        String dateKey = getActiveDateKey();
        AccountingService.DailyAccountingSummary summary = this.accountingService.calculateDailySummary(dateKey);
        this.dailySummary.setValue(summary);

        this.totalExpenses.setValue(Double.valueOf(summary.totalCashOutflow));
        this.cashSales.setValue(Double.valueOf(summary.cashSales));
        this.creditSales.setValue(Double.valueOf(summary.creditSales));
        this.totalSale.setValue(Double.valueOf(summary.totalSales));
        this.dailySale.setValue(Double.valueOf(summary.totalSales));
        this.bakiCollection.setValue(Double.valueOf(summary.bakiCollection));
        this.totalPurchases.setValue(Double.valueOf(summary.totalPurchases));
        this.totalOperatingExpenses.setValue(Double.valueOf(summary.totalOperatingExpenses));
        this.expectedClosingCash.setValue(Double.valueOf(summary.expectedClosingCash));
        this.estimatedGrossProfit.setValue(Double.valueOf(summary.estimatedGrossProfit));
        this.estimatedNetProfit.setValue(Double.valueOf(summary.estimatedNetProfit));

        // Keep calculationResult populated for backward compatibility with existing result views
        this.calculationResult.setValue(Double.valueOf(summary.estimatedNetProfit));
    }

    public String getBengaliDayOfWeek() {
        return calculateBengaliDayOfWeek(this.activeCalendar);
    }

    public String getCurrentFormattedDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
        return dateFormat.format(this.activeCalendar.getTime());
    }

    public String generateRuledNotebookReport() {
        String day = getBengaliDayOfWeek();
        String date = getCurrentFormattedDate();
        AccountingService.DailyAccountingSummary summary = this.accountingService.calculateDailySummary(date);

        StringBuilder report = new StringBuilder();
        report.append("তারিখ: ").append(date).append(" (").append(day).append(")\n");
        report.append("=============================\n");
        report.append("💵 ক্যাশ ও বিক্রি হিসাব\n");
        report.append("-----------------------------\n");
        report.append("সাবেক ক্যাশ ......... ৳ ").append(PdfExporter.formatBengaliNumber(summary.openingCash)).append("\n");
        report.append("নগদ বিক্রি ........... ৳ ").append(PdfExporter.formatBengaliNumber(summary.cashSales)).append("\n");
        report.append("বাকি আদায় (জমা) ..... ৳ ").append(PdfExporter.formatBengaliNumber(summary.bakiCollection)).append("\n");
        report.append("বাকি বিক্রি (বকেয়া) ... ৳ ").append(PdfExporter.formatBengaliNumber(summary.creditSales)).append("\n");
        report.append("-----------------------------\n");
        report.append("মোট বিক্রি: ৳ ").append(PdfExporter.formatBengaliNumber(summary.totalSales)).append("\n\n");

        report.append("🛍️ খরচ ও মাল কেনা বিবরণ:\n");
        report.append("-----------------------------\n");
        List<ExpenseModel> currentExpenses = this.expenses.getValue();
        if (currentExpenses == null || currentExpenses.isEmpty()) {
            report.append("(কোনো খরচ/মাল কেনা যোগ করা হয়নি)\n");
        } else {
            for (ExpenseModel exp : currentExpenses) {
                String prefix = exp.isPurchase() ? "📦 " : "🏢 ";
                report.append(prefix).append(exp.getName()).append(" ........ ৳ ").append(PdfExporter.formatBengaliNumber(exp.getAmount())).append("\n");
            }
        }
        report.append("-----------------------------\n");
        report.append("পণ্য ক্রয় (স্টক): ৳ ").append(PdfExporter.formatBengaliNumber(summary.totalPurchases)).append("\n");
        report.append("দোকান খরচ: ৳ ").append(PdfExporter.formatBengaliNumber(summary.totalOperatingExpenses + summary.totalLegacyExpenses)).append("\n");
        report.append("মোট ক্যাশ ব্যয়: ৳ ").append(PdfExporter.formatBengaliNumber(summary.totalCashOutflow)).append("\n\n");

        report.append("=============================\n");
        report.append("📈 মুনাফা বিশ্লেষণ (আনুমানিক)\n");
        report.append("-----------------------------\n");
        report.append("আনুমানিক মোট লাভ (").append(PdfExporter.formatBengaliNumber((int) Math.round(summary.estimatedGrossMarginRate * 100))).append("%): ৳ ").append(PdfExporter.formatBengaliNumber(summary.estimatedGrossProfit)).append("\n");
        report.append("দোকান পরিচালনা খরচ: -৳ ").append(PdfExporter.formatBengaliNumber(summary.totalOperatingExpenses)).append("\n");
        report.append("-----------------------------\n");
        if (summary.estimatedNetProfit > 0.0d) {
            report.append("🟢 আনুমানিক নিট লাভ: ৳ ").append(PdfExporter.formatBengaliNumber(summary.estimatedNetProfit)).append("\n");
        } else if (summary.estimatedNetProfit < 0.0d) {
            report.append("🔴 আনুমানিক ঘাটতি: ৳ ").append(PdfExporter.formatBengaliNumber(Math.abs(summary.estimatedNetProfit))).append("\n");
        } else {
            report.append("✅ হিসাব সমান সমান\n");
        }
        report.append("-----------------------------\n");
        report.append("💵 প্রত্যাশিত সমাপনী ক্যাশ: ৳ ").append(PdfExporter.formatBengaliNumber(summary.expectedClosingCash)).append("\n");
        report.append("গোনা সমাপনী ক্যাশ: ৳ ").append(PdfExporter.formatBengaliNumber(summary.actualAvailableCash)).append("\n");
        if (summary.cashDiscrepancy != 0.0) {
            if (summary.cashDiscrepancy > 0) {
                report.append("⚠️ ক্যাশ বাড়তি: +৳ ").append(PdfExporter.formatBengaliNumber(summary.cashDiscrepancy)).append("\n");
            } else {
                report.append("⚠️ ক্যাশ কম/শর্ট: -৳ ").append(PdfExporter.formatBengaliNumber(Math.abs(summary.cashDiscrepancy))).append("\n");
            }
        }
        return report.toString();
    }

    public List<DaySummary> getHistoricalSummaries() {
        List<String> dates = this.storageManager.getActiveDates();
        List<DaySummary> summaries = new ArrayList<>();
        for (String d : dates) {
            AccountingService.DailyAccountingSummary s = this.accountingService.calculateDailySummary(d);
            if (s.totalCashOutflow > 0.0d || s.openingCash > 0.0d || s.actualAvailableCash > 0.0d || s.totalSales > 0.0d) {
                DaySummary sum = new DaySummary();
                sum.dateKey = d;
                sum.expenses = s.totalCashOutflow;
                sum.sabek = s.openingCash;
                sum.availableCash = s.actualAvailableCash;
                sum.computedSale = s.totalSales;
                sum.margin = s.estimatedNetProfit;
                sum.purchases = s.totalPurchases;
                sum.operatingExpenses = s.totalOperatingExpenses;
                sum.creditSales = s.creditSales;
                sum.bakiCollection = s.bakiCollection;
                sum.estimatedProfit = s.estimatedNetProfit;
                summaries.add(sum);
            }
        }
        summaries.sort((o1, o2) -> {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
                Date d1 = sdf.parse(o1.dateKey);
                Date d2 = sdf.parse(o2.dateKey);
                if (d1 != null && d2 != null) {
                    return d2.compareTo(d1);
                }
            } catch (Exception ignored) {
            }
            return o2.dateKey.compareTo(o1.dateKey);
        });
        return summaries;
    }
}
