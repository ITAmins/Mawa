package com.example;

import android.content.Context;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Core Accounting Service for MAWA Daily Accounting App.
 *
 * Implements strict accounting separation:
 * 1. CASH POSITION != ESTIMATED PROFIT
 * 2. Total Sales = Cash Sales + Credit Sales (Baki)
 * 3. Baki Collection (Joma) = Cash Inflow, NOT a new sale
 * 4. Purchase = Resale Goods / Inventory (decreases Cash, increases Stock, NOT an immediate profit expense)
 * 5. Operating Expense = Business Expense (decreases Cash, decreases Net Profit)
 * 6. Configurable Estimated Gross Margin Rate (e.g. 20%)
 * 7. 100% backward compatible with existing StorageManager, BakiModel, ExpenseModel, and GoogleSheetsSyncManager.
 */
public class AccountingService {

    public enum TransactionType {
        OPENING_CASH,       // সাবেক ক্যাশ (সকালের শুরুর ক্যাশ)
        CASH_SALE,          // নগদ বিক্রি
        CREDIT_SALE,        // বাকি বিক্রি (খরিদ্দারের বাকি নেওয়া)
        BAKI_PAYMENT,       // বাকি আদায় / জমা (খরিদ্দারের টাকা পরিশোধ)
        PURCHASE,           // পণ্য ক্রয় / মাল কেনা (দোকানের বিক্রির জন্য কেনা স্টক)
        OPERATING_EXPENSE,  // দোকান পরিচালনা খরচ (ভাড়া, বিদ্যুৎ, বেতন, চা-নাস্তা ইত্যাদি)
        ADJUSTMENT          // ক্যাশ সমন্বয় / ভুল সংশোধন
    }

    public static class DailyAccountingSummary {
        public String dateKey;                     // তারিখ (dd-MM-yyyy)
        public double openingCash;                 // সাবেক ক্যাশ (৳)
        public double cashSales;                   // নগদ বিক্রি (৳)
        public double creditSales;                 // বাকি বিক্রি (৳)
        public double totalSales;                  // মোট বিক্রি = নগদ বিক্রি + বাকি বিক্রি (৳)
        public double bakiCollection;              // বকেয়া আদায় / জমা (৳)
        public double totalPurchases;              // পণ্য ক্রয় / মাল কেনা (৳)
        public double totalOperatingExpenses;      // দোকান খরচ (৳)
        public double totalLegacyExpenses;         // সাধারণ/পূর্ববর্তী খরচ (৳)
        public double totalCashOutflow;            // সর্বমোট ক্যাশ ব্যয় (Purchases + OpEx + Legacy)
        public double adjustments;                 // সমন্বয় (+/-)
        public double expectedClosingCash;         // প্রত্যাশিত সমাপনী ক্যাশ / ক্যাশ পজিশন
        public double actualAvailableCash;         // প্রকৃত গোনা ক্যাশ (হাতে থাকা ক্যাশ)
        public double cashDiscrepancy;             // ক্যাশ অমিল / শর্ট বা বাড়তি
        public double estimatedStockAddition;      // আনুমানিক স্টক বৃদ্ধি (পণ্য ক্রয়)
        
        public double estimatedGrossMarginRate;    // আনুমানিক মোট মুনাফার হার (যেমন: 0.20 বা ২০%)
        public double estimatedGrossProfit;        // আনুমানিক মোট লাভ = মোট বিক্রি × মার্জিন
        public double estimatedNetProfit;          // আনুমানিক নিট লাভ = মোট লাভ - দোকান খরচ
    }

    private static AccountingService instance;
    private final StorageManager storageManager;
    private final Context context;

    private AccountingService(Context context) {
        this.context = context.getApplicationContext();
        this.storageManager = StorageManager.getInstance(context);
    }

    public static synchronized AccountingService getInstance(Context context) {
        if (instance == null) {
            instance = new AccountingService(context);
        }
        return instance;
    }

    // ==========================================
    // CONFIGURABLE ESTIMATED GROSS MARGIN
    // ==========================================

    public double getEstimatedGrossMarginRate() {
        return this.storageManager.getEstimatedGrossMarginRate();
    }

    public void setEstimatedGrossMarginRate(double rate) {
        this.storageManager.saveEstimatedGrossMarginRate(rate);
    }

    // ==========================================
    // DATE NORMALIZATION & MATCHING
    // ==========================================

    public static String normalizeDateKey(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(new Date());
        }
        String clean = rawDate.trim().replace("/", "-").replace(".", "-");
        String[] parts = clean.split("-");
        if (parts.length == 3) {
            try {
                int p0 = Integer.parseInt(parts[0]);
                int p1 = Integer.parseInt(parts[1]);
                int p2 = Integer.parseInt(parts[2]);

                if (parts[0].length() == 4) {
                    // yyyy-MM-dd -> dd-MM-yyyy
                    return String.format(Locale.US, "%02d-%02d-%04d", p2, p1, p0);
                } else {
                    // dd-MM-yyyy
                    return String.format(Locale.US, "%02d-%02d-%04d", p0, p1, p2);
                }
            } catch (Exception ignored) {
            }
        }
        return clean;
    }

    public static boolean isSameDate(String date1, String date2) {
        if (date1 == null || date2 == null) return false;
        return normalizeDateKey(date1).equals(normalizeDateKey(date2));
    }

    // ==========================================
    // CORE ACCOUNTING CALCULATIONS
    // ==========================================

    public DailyAccountingSummary calculateDailySummary(String dateKey) {
        String normalizedKey = normalizeDateKey(dateKey);
        DailyAccountingSummary summary = new DailyAccountingSummary();
        summary.dateKey = normalizedKey;

        // 1. Opening cash
        summary.openingCash = this.storageManager.loadSabekCash(normalizedKey);

        // 2. Available cash (actual counted cash)
        summary.actualAvailableCash = this.storageManager.loadAvailableCash(normalizedKey);

        // 3. Expenses breakdown (Purchases vs Operating Expenses vs Legacy)
        List<ExpenseModel> expenses = this.storageManager.loadExpenses(normalizedKey);
        double purchases = 0.0;
        double operating = 0.0;
        double legacy = 0.0;

        Set<String> seenExpenseIds = new HashSet<>();
        if (expenses != null) {
            for (ExpenseModel exp : expenses) {
                if (exp == null) continue;
                String expId = exp.getId();
                if (expId != null && !expId.isEmpty()) {
                    if (seenExpenseIds.contains(expId)) {
                        continue; // Prevent accidental duplicate calculation
                    }
                    seenExpenseIds.add(expId);
                }

                double amt = Math.max(0.0, exp.getAmount());
                if (exp.isPurchase()) {
                    purchases += amt;
                } else if (exp.isOperatingExpense()) {
                    operating += amt;
                } else {
                    legacy += amt;
                }
            }
        }
        summary.totalPurchases = purchases;
        summary.totalOperatingExpenses = operating;
        summary.totalLegacyExpenses = legacy;
        summary.totalCashOutflow = purchases + operating + legacy;
        summary.estimatedStockAddition = purchases;

        // 4. Baki Records (Credit Sales & Baki Collections on this date)
        List<BakiModel> bakiList = this.storageManager.loadBakiRecords();
        double creditSales = 0.0;
        double bakiCollection = 0.0;

        Set<String> seenTxIds = new HashSet<>();
        if (bakiList != null) {
            for (BakiModel customer : bakiList) {
                if (customer == null || customer.getTransactions() == null) continue;
                for (BakiTransaction tx : customer.getTransactions()) {
                    if (tx == null) continue;
                    String txId = tx.getId();
                    if (txId != null && !txId.isEmpty()) {
                        if (seenTxIds.contains(txId)) {
                            continue; // Deduplicate
                        }
                        seenTxIds.add(txId);
                    }

                    if (isSameDate(tx.getDate(), normalizedKey)) {
                        double amt = Math.max(0.0, tx.getAmount());
                        if ("BAKI".equalsIgnoreCase(tx.getType())) {
                            creditSales += amt;
                        } else if ("JOMA".equalsIgnoreCase(tx.getType())) {
                            bakiCollection += amt;
                        }
                    }
                }
            }
        }
        summary.creditSales = creditSales;
        summary.bakiCollection = bakiCollection;

        // 5. Cash Sales
        // In daily grocery shop accounting:
        // Cash in drawer at closing = Opening Cash + Cash Sales + Baki Collections - Total Expenses
        // Therefore, Cash Sales = Available Cash + Total Expenses - Opening Cash - Baki Collections
        double explicitCashSale = this.storageManager.loadCashSale(normalizedKey);
        if (explicitCashSale > 0.0) {
            summary.cashSales = explicitCashSale;
        } else {
            double derivedCashSale = (summary.actualAvailableCash + summary.totalCashOutflow) - summary.openingCash - summary.bakiCollection;
            summary.cashSales = Math.max(0.0, derivedCashSale);
        }

        // 6. Total Sales = Cash Sales + Credit Sales (Baki)
        summary.totalSales = summary.cashSales + summary.creditSales;

        // 7. Expected Closing Cash (Cash Position)
        // Expected Cash = Opening Cash + Cash Sales + Baki Collections - Total Expenses +/- Adjustments
        summary.expectedClosingCash = summary.openingCash + summary.cashSales + summary.bakiCollection - summary.totalCashOutflow;
        summary.cashDiscrepancy = summary.actualAvailableCash - summary.expectedClosingCash;

        // 8. Estimated Profit Calculation
        summary.estimatedGrossMarginRate = getEstimatedGrossMarginRate();
        summary.estimatedGrossProfit = summary.totalSales * summary.estimatedGrossMarginRate;
        // Operating expenses are deducted from gross profit; purchases are resale inventory (not profit expenses)
        summary.estimatedNetProfit = summary.estimatedGrossProfit - summary.totalOperatingExpenses;

        return summary;
    }

    // ==========================================
    // REUSABLE ACCESSOR METHODS
    // ==========================================

    public double calculateExpectedClosingCash(String dateKey) {
        return calculateDailySummary(dateKey).expectedClosingCash;
    }

    public double calculateTotalSales(String dateKey) {
        return calculateDailySummary(dateKey).totalSales;
    }

    public double calculateCashSales(String dateKey) {
        return calculateDailySummary(dateKey).cashSales;
    }

    public double calculateCreditSales(String dateKey) {
        return calculateDailySummary(dateKey).creditSales;
    }

    public double calculateBakiCollection(String dateKey) {
        return calculateDailySummary(dateKey).bakiCollection;
    }

    public double calculatePurchaseTotal(String dateKey) {
        return calculateDailySummary(dateKey).totalPurchases;
    }

    public double calculateOperatingExpenseTotal(String dateKey) {
        return calculateDailySummary(dateKey).totalOperatingExpenses;
    }

    public double calculateLegacyExpenseTotal(String dateKey) {
        return calculateDailySummary(dateKey).totalLegacyExpenses;
    }

    public double calculateEstimatedGrossProfit(String dateKey) {
        return calculateDailySummary(dateKey).estimatedGrossProfit;
    }

    public double calculateEstimatedNetProfit(String dateKey) {
        return calculateDailySummary(dateKey).estimatedNetProfit;
    }

    public double calculateStockAddition(String dateKey) {
        return calculateDailySummary(dateKey).estimatedStockAddition;
    }

    // ==========================================
    // SAFE TRANSACTION MANAGEMENT & RECORDING
    // ==========================================

    /**
     * Records a new customer credit sale (BAKI)
     */
    public synchronized void recordCreditSale(String customerName, String phone, double amount, String date, String time, String note, String dueDate) {
        if (customerName == null || customerName.trim().isEmpty() || amount <= 0.0) {
            return;
        }
        String cleanName = customerName.trim();
        String cleanDate = (date != null && !date.trim().isEmpty()) ? date.trim() : new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(new Date());
        String cleanTime = (time != null && !time.trim().isEmpty()) ? time.trim() : new SimpleDateFormat("hh:mm a", Locale.US).format(new Date());

        List<BakiModel> bakiList = this.storageManager.loadBakiRecords();
        BakiModel targetCustomer = null;
        for (BakiModel b : bakiList) {
            if (b.getCustomerName() != null && b.getCustomerName().trim().equalsIgnoreCase(cleanName)) {
                targetCustomer = b;
                break;
            }
        }

        if (targetCustomer != null) {
            double newTotal = targetCustomer.getAmount() + amount;
            targetCustomer.setAmount(newTotal);
            if (phone != null && !phone.trim().isEmpty()) targetCustomer.setPhone(phone.trim());
            if (dueDate != null && !dueDate.trim().isEmpty()) targetCustomer.setDueDate(dueDate.trim());
            if (note != null && !note.trim().isEmpty()) targetCustomer.setDetails(note.trim());
            targetCustomer.setDate(cleanDate);

            BakiTransaction tx = new BakiTransaction(UUID.randomUUID().toString(), cleanDate, cleanTime, "BAKI", amount, (note != null && !note.isEmpty()) ? note : "বাকি যোগ", newTotal);
            targetCustomer.addTransaction(tx);
        } else {
            String id = UUID.randomUUID().toString();
            BakiModel record = new BakiModel(id, cleanName, phone != null ? phone.trim() : "", amount, cleanDate, dueDate != null ? dueDate.trim() : "", note != null ? note.trim() : "");
            BakiTransaction tx = new BakiTransaction(UUID.randomUUID().toString(), cleanDate, cleanTime, "BAKI", amount, (note != null && !note.isEmpty()) ? note : "নতুন বাকি শুরু", amount);
            record.addTransaction(tx);
            bakiList.add(0, record);
        }

        this.storageManager.saveBakiRecords(bakiList);
        this.storageManager.saveActiveDate(normalizeDateKey(cleanDate));
    }

    /**
     * Records a customer debt payment (JOMA)
     */
    public synchronized void recordBakiPayment(String customerIdOrName, double amount, String date, String time, String note, boolean addToDailyCash) {
        if (customerIdOrName == null || customerIdOrName.trim().isEmpty() || amount <= 0.0) {
            return;
        }
        String cleanDate = (date != null && !date.trim().isEmpty()) ? date.trim() : new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(new Date());
        String cleanTime = (time != null && !time.trim().isEmpty()) ? time.trim() : new SimpleDateFormat("hh:mm a", Locale.US).format(new Date());

        List<BakiModel> bakiList = this.storageManager.loadBakiRecords();
        BakiModel target = null;
        for (BakiModel b : bakiList) {
            if (b.getId() != null && b.getId().equals(customerIdOrName)) {
                target = b;
                break;
            }
            if (b.getCustomerName() != null && b.getCustomerName().trim().equalsIgnoreCase(customerIdOrName.trim())) {
                target = b;
                break;
            }
        }

        if (target != null) {
            double newAmt = Math.max(0.0, target.getAmount() - amount);
            target.setAmount(newAmt);
            target.setDate(cleanDate);

            BakiTransaction tx = new BakiTransaction(UUID.randomUUID().toString(), cleanDate, cleanTime, "JOMA", amount, (note != null && !note.isEmpty()) ? note : "টাকা জমা", newAmt);
            target.addTransaction(tx);
            this.storageManager.saveBakiRecords(bakiList);

            if (addToDailyCash) {
                String normalizedDate = normalizeDateKey(cleanDate);
                double currentCash = this.storageManager.loadAvailableCash(normalizedDate);
                this.storageManager.saveAvailableCash(normalizedDate, currentCash + amount);
                this.storageManager.saveActiveDate(normalizedDate);
            }
        }
    }

    /**
     * Records a purchase (resale goods / inventory)
     */
    public synchronized boolean recordPurchase(String itemName, double amount, String date, String time) {
        if (itemName == null || itemName.trim().isEmpty() || amount <= 0.0) {
            return false;
        }
        String cleanDate = normalizeDateKey(date);
        String cleanTime = (time != null && !time.trim().isEmpty()) ? time.trim() : new SimpleDateFormat("hh:mm a", Locale.US).format(new Date());

        ExpenseModel expense = new ExpenseModel(
                UUID.randomUUID().toString(),
                itemName.trim(),
                amount,
                cleanDate,
                cleanTime,
                ExpenseModel.TYPE_PURCHASE
        );

        List<ExpenseModel> list = this.storageManager.loadExpenses(cleanDate);
        if (list == null) list = new ArrayList<>();
        list.add(0, expense);
        this.storageManager.saveExpenses(cleanDate, list);
        this.storageManager.saveProductSuggestion(itemName.trim());
        this.storageManager.saveActiveDate(cleanDate);
        return true;
    }

    /**
     * Records an operating expense (rent, electric bill, salary, food/tea, etc.)
     */
    public synchronized boolean recordOperatingExpense(String expenseName, double amount, String date, String time) {
        if (expenseName == null || expenseName.trim().isEmpty() || amount <= 0.0) {
            return false;
        }
        String cleanDate = normalizeDateKey(date);
        String cleanTime = (time != null && !time.trim().isEmpty()) ? time.trim() : new SimpleDateFormat("hh:mm a", Locale.US).format(new Date());

        ExpenseModel expense = new ExpenseModel(
                UUID.randomUUID().toString(),
                expenseName.trim(),
                amount,
                cleanDate,
                cleanTime,
                ExpenseModel.TYPE_OPERATING_EXPENSE
        );

        List<ExpenseModel> list = this.storageManager.loadExpenses(cleanDate);
        if (list == null) list = new ArrayList<>();
        list.add(0, expense);
        this.storageManager.saveExpenses(cleanDate, list);
        this.storageManager.saveProductSuggestion(expenseName.trim());
        this.storageManager.saveActiveDate(cleanDate);
        return true;
    }

    /**
     * Posts actual purchases from a Smart Fordi directly into daily accounting.
     * 1. Creates a PURCHASE transaction (stock addition, cash outflow, non-profit expense)
     * 2. Updates Product Memory purchase rates & selling prices
     * 3. Marks Fordi as POSTED with expense reference to prevent double posting.
     */
    public synchronized boolean postFordiPurchaseToDailyAccounting(FordiModel fordi, String targetDateKey) {
        if (fordi == null || fordi.isPostedToAccounting()) {
            return false;
        }
        double actualTotal = fordi.getActualTotal();
        if (actualTotal <= 0.0) {
            return false;
        }

        String cleanDate = normalizeDateKey(targetDateKey != null ? targetDateKey : fordi.getDate());
        String cleanTime = new SimpleDateFormat("hh:mm a", Locale.US).format(new Date());

        // 1. Build memo of bought items and update product memory
        StringBuilder memoBuilder = new StringBuilder();
        int boughtCount = 0;
        for (FordiItemModel item : fordi.getItems()) {
            if (!FordiItemModel.STATUS_NOT_BOUGHT.equals(item.getStatus()) && item.getActualQuantity() > 0) {
                boughtCount++;
                if (memoBuilder.length() > 0) memoBuilder.append(", ");
                memoBuilder.append(item.getProductName())
                        .append(" ")
                        .append(PdfExporter.formatBengaliNumber(item.getActualQuantity()))
                        .append(ProductModel.getBengaliUnitLabel(item.getUnit()))
                        .append(" (৳")
                        .append(PdfExporter.formatBengaliNumber(item.getActualTotal()))
                        .append(")");

                // Update product memory
                ProductModel product = storageManager.findProductByName(item.getProductName());
                if (product == null) {
                    product = new ProductModel(
                            item.getProductId(),
                            item.getProductName(),
                            item.getUnit(),
                            item.getActualPurchaseRate(),
                            item.getSellingRate(),
                            "বাজার ফর্দ"
                    );
                } else {
                    product.recordNewPurchase(item.getActualPurchaseRate(), item.getActualQuantity(), cleanDate);
                    if (item.getSellingRate() > 0) {
                        product.setSellingPrice(item.getSellingRate());
                    }
                    if (item.getUnit() != null) {
                        product.setUnit(item.getUnit());
                    }
                }
                storageManager.saveOrUpdateProduct(product);
            }
        }

        if (boughtCount == 0) {
            return false;
        }

        // 2. Create and record purchase expense
        String expenseTitle = fordi.getTitle() + " (ফর্দ কেনা)";
        ExpenseModel expense = new ExpenseModel(
                UUID.randomUUID().toString(),
                expenseTitle,
                actualTotal,
                cleanDate,
                cleanTime,
                ExpenseModel.TYPE_PURCHASE
        );
        expense.setNote(memoBuilder.toString());

        List<ExpenseModel> list = this.storageManager.loadExpenses(cleanDate);
        if (list == null) list = new ArrayList<>();
        list.add(0, expense);
        this.storageManager.saveExpenses(cleanDate, list);
        this.storageManager.saveActiveDate(cleanDate);

        // 3. Update Fordi model status
        fordi.setPostedToAccounting(true);
        fordi.setPostedExpenseId(expense.getId());
        fordi.setPostedAmount(actualTotal);
        fordi.setPostedDate(cleanDate);
        fordi.setStatus(FordiModel.STATUS_POSTED);

        // Save updated fordi in list
        List<FordiModel> allFordi = this.storageManager.loadFordiRecords();
        for (int i = 0; i < allFordi.size(); i++) {
            if (allFordi.get(i).getId().equals(fordi.getId())) {
                allFordi.set(i, fordi);
                break;
            }
        }
        this.storageManager.saveFordiRecords(allFordi);

        return true;
    }
}
