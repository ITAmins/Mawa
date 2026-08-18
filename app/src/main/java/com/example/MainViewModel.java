package com.example;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.MainViewModel;
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
    private final MutableLiveData<Double> totalExpenses;
    private final MutableLiveData<Double> totalSale;

    /* loaded from: classes5.dex */
    public static class DaySummary {
        public double availableCash;
        public double computedSale;
        public String dateKey;
        public double expenses;
        public double margin;
        public double sabek;
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
        this.activeCalendar = Calendar.getInstance();
        this.activeDateString = new MutableLiveData<>("");
        this.activeDayOfWeek = new MutableLiveData<>("");
        this.storageManager = StorageManager.getInstance(application);
        updateActiveDateState();
    }

    public LiveData<String> getActiveDateString() {
        return this.activeDateString;
    }

    public LiveData<String> getActiveDayOfWeek() {
        return this.activeDayOfWeek;
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
        double totalExp = 0.0d;
        List<ExpenseModel> currentExpenses = this.expenses.getValue();
        if (currentExpenses != null) {
            for (ExpenseModel exp : currentExpenses) {
                totalExp += exp.getAmount();
            }
        }
        this.totalExpenses.setValue(Double.valueOf(totalExp));
        double sabek = this.sabekCash.getValue() != null ? this.sabekCash.getValue().doubleValue() : 0.0d;
        double cash = this.availableCash.getValue() != null ? this.availableCash.getValue().doubleValue() : 0.0d;
        double computedSale = (cash + totalExp) - sabek;
        this.dailySale.setValue(Double.valueOf(computedSale));
        this.totalSale.setValue(Double.valueOf(computedSale));
        double result = computedSale - totalExp;
        this.calculationResult.setValue(Double.valueOf(result));
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
        StringBuilder report = new StringBuilder();
        report.append("তারিখ: ").append(date).append(" (").append(day).append(")\n");
        report.append("-----------------------------\n");
        report.append("খরচের বিবরণ:\n");
        List<ExpenseModel> currentExpenses = this.expenses.getValue();
        if (currentExpenses == null || currentExpenses.isEmpty()) {
            report.append("(কোনো খরচ যোগ করা হয়নি)\n");
        } else {
            for (ExpenseModel exp : currentExpenses) {
                report.append(exp.getName()).append(" ............ ৳ ").append(PdfExporter.formatBengaliNumber(exp.getAmount())).append("\n");
            }
        }
        double totalExp = this.totalExpenses.getValue() != null ? this.totalExpenses.getValue().doubleValue() : 0.0d;
        report.append("-----------------------------\n");
        report.append("মোট খরচ: ৳ ").append(PdfExporter.formatBengaliNumber(totalExp)).append("\n\n");
        double sale = this.dailySale.getValue() != null ? this.dailySale.getValue().doubleValue() : 0.0d;
        double sabek = this.sabekCash.getValue() != null ? this.sabekCash.getValue().doubleValue() : 0.0d;
        double totalSl = this.totalSale.getValue() != null ? this.totalSale.getValue().doubleValue() : 0.0d;
        report.append("আজকের বেচা ......... ৳ ").append(PdfExporter.formatBengaliNumber(sale)).append("\n");
        report.append("সাবেক (আছে) ......... ৳ ").append(PdfExporter.formatBengaliNumber(sabek)).append("\n");
        report.append("-----------------------------\n");
        report.append("মোট বেচা: ৳ ").append(PdfExporter.formatBengaliNumber(totalSl)).append("\n");
        report.append("-----------------------------\n");
        double result = this.calculationResult.getValue() != null ? this.calculationResult.getValue().doubleValue() : 0.0d;
        if (result > 0.0d) {
            report.append("🟢 লাভ হয়েছে: ৳ ").append(PdfExporter.formatBengaliNumber(result)).append("\n");
        } else if (result != 0.0d) {
            report.append("🔴 ঘাটতি হয়েছে: ৳ ").append(PdfExporter.formatBengaliNumber(Math.abs(result))).append("\n");
        } else {
            report.append("✅ হিসাব সমান সমান\n");
        }
        return report.toString();
    }

    public List<DaySummary> getHistoricalSummaries() {
        MainViewModel mainViewModel = this;
        List<String> dates = mainViewModel.storageManager.getActiveDates();
        List<DaySummary> summaries = new ArrayList<>();
        for (String d : dates) {
            double expTotal = 0.0d;
            List<ExpenseModel> list = mainViewModel.storageManager.loadExpenses(d);
            for (ExpenseModel e : list) {
                expTotal += e.getAmount();
            }
            double sabek = mainViewModel.storageManager.loadSabekCash(d);
            double cash = mainViewModel.storageManager.loadAvailableCash(d);
            double sale = (cash + expTotal) - sabek;
            double margin = sale - expTotal;
            if (expTotal > 0.0d || sabek > 0.0d || cash > 0.0d) {
                DaySummary sum = new DaySummary();
                sum.dateKey = d;
                sum.expenses = expTotal;
                sum.sabek = sabek;
                sum.availableCash = cash;
                sum.computedSale = sale;
                sum.margin = margin;
                summaries.add(sum);
            }
            mainViewModel = this;
        }
        summaries.sort(new Comparator() { // from class: com.example.MainViewModel$$ExternalSyntheticLambda0
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return MainViewModel.lambda$getHistoricalSummaries$0((MainViewModel.DaySummary) obj, (MainViewModel.DaySummary) obj2);
            }
        });
        return summaries;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ int lambda$getHistoricalSummaries$0(DaySummary o1, DaySummary o2) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
            Date d1 = sdf.parse(o1.dateKey);
            Date d2 = sdf.parse(o2.dateKey);
            if (d1 != null && d2 != null) {
                return d2.compareTo(d1);
            }
        } catch (Exception e) {
        }
        return o2.dateKey.compareTo(o1.dateKey);
    }
}
