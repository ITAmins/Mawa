package com.example;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* loaded from: classes5.dex */
public class StorageManager {
    private static final String KEY_ACTIVE_DATES = "key_active_dates";
    private static final String KEY_AVAILABLE_CASH = "key_available_cash";
    private static final String KEY_BAKI_RECORDS = "key_baki_records";
    private static final String KEY_DAILY_SALE = "key_daily_sale";
    private static final String KEY_EXPENSES = "key_expenses";
    private static final String KEY_FORDI_RECORDS = "key_fordi_records";
    private static final String KEY_SABEK_CASH = "key_sabek_cash";
    public static final String KEY_ESTIMATED_GROSS_MARGIN = "key_estimated_gross_margin";
    private static final String PREF_NAME = "DailyCashBookPrefs";
    private static StorageManager instance;
    private final Gson gson = new Gson();
    private final SharedPreferences sharedPreferences;

    private StorageManager(Context context) {
        this.sharedPreferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, 0);
    }

    public static synchronized StorageManager getInstance(Context context) {
        StorageManager storageManager;
        synchronized (StorageManager.class) {
            if (instance == null) {
                instance = new StorageManager(context);
            }
            storageManager = instance;
        }
        return storageManager;
    }

    public void saveExpenses(String date, List<ExpenseModel> expenses) {
        String json = this.gson.toJson(expenses);
        this.sharedPreferences.edit().putString("key_expenses_" + date, json).apply();
    }

    public List<ExpenseModel> loadExpenses(String date) {
        String json = this.sharedPreferences.getString("key_expenses_" + date, null);
        if (json == null) {
            json = this.sharedPreferences.getString(KEY_EXPENSES, null);
            if (json != null) {
                this.sharedPreferences.edit().putString("key_expenses_" + date, json).apply();
            } else {
                return new ArrayList();
            }
        }
        Type type = new TypeToken<List<ExpenseModel>>() { // from class: com.example.StorageManager.1
        }.getType();
        List<ExpenseModel> expenses = (List) this.gson.fromJson(json, type);
        return expenses != null ? expenses : new ArrayList();
    }

    public void saveDailySale(double sale) {
        this.sharedPreferences.edit().putFloat(KEY_DAILY_SALE, (float) sale).apply();
    }

    public double loadDailySale() {
        return this.sharedPreferences.getFloat(KEY_DAILY_SALE, 0.0f);
    }

    public void saveCashSale(String date, double sale) {
        this.sharedPreferences.edit().putFloat("key_cash_sale_" + date, (float) sale).apply();
    }

    public double loadCashSale(String date) {
        if (this.sharedPreferences.contains("key_cash_sale_" + date)) {
            return this.sharedPreferences.getFloat("key_cash_sale_" + date, 0.0f);
        }
        return 0.0d;
    }

    public double getEstimatedGrossMarginRate() {
        return this.sharedPreferences.getFloat(KEY_ESTIMATED_GROSS_MARGIN, 0.20f);
    }

    public void saveEstimatedGrossMarginRate(double rate) {
        float val = (float) Math.max(0.01, Math.min(1.0, rate));
        this.sharedPreferences.edit().putFloat(KEY_ESTIMATED_GROSS_MARGIN, val).apply();
    }

    public void saveAvailableCash(String date, double cash) {
        this.sharedPreferences.edit().putFloat("key_available_cash_" + date, (float) cash).apply();
    }

    public double loadAvailableCash(String date) {
        boolean contains = this.sharedPreferences.contains("key_available_cash_" + date);
        SharedPreferences sharedPreferences = this.sharedPreferences;
        if (contains) {
            return sharedPreferences.getFloat("key_available_cash_" + date, 0.0f);
        }
        if (sharedPreferences.contains(KEY_AVAILABLE_CASH)) {
            float legacy = this.sharedPreferences.getFloat(KEY_AVAILABLE_CASH, 0.0f);
            saveAvailableCash(date, legacy);
            return legacy;
        }
        return 0.0d;
    }

    public void saveSabekCash(String date, double cash) {
        this.sharedPreferences.edit().putFloat("key_sabek_cash_" + date, (float) cash).apply();
    }

    public double loadSabekCash(String date) {
        boolean contains = this.sharedPreferences.contains("key_sabek_cash_" + date);
        SharedPreferences sharedPreferences = this.sharedPreferences;
        if (contains) {
            return sharedPreferences.getFloat("key_sabek_cash_" + date, 0.0f);
        }
        if (sharedPreferences.contains(KEY_SABEK_CASH)) {
            float legacy = this.sharedPreferences.getFloat(KEY_SABEK_CASH, 0.0f);
            saveSabekCash(date, legacy);
            return legacy;
        }
        return 0.0d;
    }

    public void saveActiveDate(String date) {
        List<String> dates = getActiveDates();
        if (!dates.contains(date)) {
            dates.add(date);
            String json = this.gson.toJson(dates);
            this.sharedPreferences.edit().putString(KEY_ACTIVE_DATES, json).apply();
        }
    }

    public List<String> getActiveDates() {
        String json = this.sharedPreferences.getString(KEY_ACTIVE_DATES, null);
        if (json == null) {
            return new ArrayList();
        }
        Type type = new TypeToken<List<String>>() { // from class: com.example.StorageManager.2
        }.getType();
        List<String> dates = (List) this.gson.fromJson(json, type);
        return dates != null ? dates : new ArrayList();
    }

    public void removeActiveDate(String date) {
        List<String> dates = getActiveDates();
        if (dates.contains(date)) {
            dates.remove(date);
            String json = this.gson.toJson(dates);
            this.sharedPreferences.edit().putString(KEY_ACTIVE_DATES, json).apply();
        }
    }

    public void saveBakiRecords(List<BakiModel> bakiList) {
        String json = this.gson.toJson(bakiList);
        this.sharedPreferences.edit().putString(KEY_BAKI_RECORDS, json).apply();
    }

    public List<BakiModel> loadBakiRecords() {
        String json = this.sharedPreferences.getString(KEY_BAKI_RECORDS, null);
        if (json == null) {
            return new ArrayList();
        }
        Type type = new TypeToken<List<BakiModel>>() { // from class: com.example.StorageManager.3
        }.getType();
        List<BakiModel> bakiList = (List) this.gson.fromJson(json, type);
        return bakiList != null ? bakiList : new ArrayList();
    }

    public void saveFordiRecords(List<FordiModel> fordiList) {
        String json = this.gson.toJson(fordiList);
        this.sharedPreferences.edit().putString(KEY_FORDI_RECORDS, json).apply();
    }

    public List<FordiModel> loadFordiRecords() {
        String json = this.sharedPreferences.getString(KEY_FORDI_RECORDS, null);
        if (json == null) {
            return new ArrayList();
        }
        Type type = new TypeToken<List<FordiModel>>() { // from class: com.example.StorageManager.4
        }.getType();
        List<FordiModel> fordiList = (List) this.gson.fromJson(json, type);
        return fordiList != null ? fordiList : new ArrayList();
    }

    public Map<String, Object> exportAllData() {
        Map<String, Object> allData = new HashMap<>();
        List<String> dates = getActiveDates();
        allData.put(KEY_ACTIVE_DATES, dates);
        for (String date : dates) {
            allData.put("key_expenses_" + date, loadExpenses(date));
            allData.put("key_available_cash_" + date, Double.valueOf(loadAvailableCash(date)));
            allData.put("key_sabek_cash_" + date, Double.valueOf(loadSabekCash(date)));
            allData.put("key_cash_sale_" + date, Double.valueOf(loadCashSale(date)));
        }
        allData.put(KEY_BAKI_RECORDS, loadBakiRecords());
        allData.put(KEY_FORDI_RECORDS, loadFordiRecords());
        allData.put(KEY_PRODUCT_MEMORY, loadProductMemory());
        allData.put(KEY_PRODUCT_SUGGESTIONS, getCustomProductSuggestions());
        allData.put(KEY_ESTIMATED_GROSS_MARGIN, Double.valueOf(getEstimatedGrossMarginRate()));
        return allData;
    }

    public void importAllData(Map<String, Object> data) {
        if (data == null) {
            return;
        }
        SharedPreferences.Editor editor = this.sharedPreferences.edit();
        editor.clear();
        try {
            if (data.containsKey(KEY_ESTIMATED_GROSS_MARGIN)) {
                Object marginObj = data.get(KEY_ESTIMATED_GROSS_MARGIN);
                if (marginObj instanceof Number) {
                    editor.putFloat(KEY_ESTIMATED_GROSS_MARGIN, ((Number) marginObj).floatValue());
                }
            }
            if (data.containsKey(KEY_PRODUCT_MEMORY)) {
                editor.putString(KEY_PRODUCT_MEMORY, this.gson.toJson(data.get(KEY_PRODUCT_MEMORY)));
            }
            if (data.containsKey(KEY_PRODUCT_SUGGESTIONS)) {
                editor.putString(KEY_PRODUCT_SUGGESTIONS, this.gson.toJson(data.get(KEY_PRODUCT_SUGGESTIONS)));
            }
            if (data.containsKey(KEY_BAKI_RECORDS)) {
                editor.putString(KEY_BAKI_RECORDS, this.gson.toJson(data.get(KEY_BAKI_RECORDS)));
            }
            if (data.containsKey(KEY_FORDI_RECORDS)) {
                editor.putString(KEY_FORDI_RECORDS, this.gson.toJson(data.get(KEY_FORDI_RECORDS)));
            }
            if (data.containsKey(KEY_ACTIVE_DATES)) {
                Object datesObj = data.get(KEY_ACTIVE_DATES);
                String datesJson = this.gson.toJson(datesObj);
                editor.putString(KEY_ACTIVE_DATES, datesJson);
                Type listType = new TypeToken<List<String>>() { // from class: com.example.StorageManager.5
                }.getType();
                List<String> dates = (List) this.gson.fromJson(datesJson, listType);
                if (dates != null) {
                    for (String date : dates) {
                        String expKey = "key_expenses_" + date;
                        if (data.containsKey(expKey)) {
                            editor.putString(expKey, this.gson.toJson(data.get(expKey)));
                        }
                        String availKey = "key_available_cash_" + date;
                        if (data.containsKey(availKey)) {
                            Object val = data.get(availKey);
                            if (val instanceof Number) {
                                editor.putFloat(availKey, ((Number) val).floatValue());
                            }
                        }
                        String sabekKey = "key_sabek_cash_" + date;
                        if (data.containsKey(sabekKey)) {
                            Object val2 = data.get(sabekKey);
                            if (val2 instanceof Number) {
                                editor.putFloat(sabekKey, ((Number) val2).floatValue());
                            }
                        }
                        String cashSaleKey = "key_cash_sale_" + date;
                        if (data.containsKey(cashSaleKey)) {
                            Object val3 = data.get(cashSaleKey);
                            if (val3 instanceof Number) {
                                editor.putFloat(cashSaleKey, ((Number) val3).floatValue());
                            }
                        }
                    }
                }
            }
            editor.apply();
        } catch (Exception e) {
        }
    }

    public static final String KEY_PRODUCT_MEMORY = "key_product_memory";
    public static final String KEY_PRODUCT_SUGGESTIONS = "key_product_suggestions";

    public List<ProductModel> loadProductMemory() {
        String json = this.sharedPreferences.getString(KEY_PRODUCT_MEMORY, null);
        List<ProductModel> products = null;
        if (json != null) {
            try {
                Type type = new TypeToken<List<ProductModel>>() {}.getType();
                products = this.gson.fromJson(json, type);
            } catch (Exception ignored) {}
        }
        if (products == null || products.isEmpty()) {
            products = seedDefaultProductMemory();
            saveProductMemory(products);
        }
        return products;
    }

    public void saveProductMemory(List<ProductModel> products) {
        if (products == null) return;
        String json = this.gson.toJson(products);
        this.sharedPreferences.edit().putString(KEY_PRODUCT_MEMORY, json).apply();
    }

    public ProductModel findProductByName(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        String target = ProductModel.normalizeName(name).toLowerCase();
        List<ProductModel> list = loadProductMemory();
        for (ProductModel p : list) {
            if (p.getName() != null && ProductModel.normalizeName(p.getName()).equalsIgnoreCase(target)) {
                return p;
            }
        }
        return null;
    }

    public ProductModel saveOrUpdateProduct(ProductModel product) {
        if (product == null || product.getName() == null || product.getName().trim().isEmpty()) {
            return product;
        }
        List<ProductModel> list = loadProductMemory();
        String target = ProductModel.normalizeName(product.getName()).toLowerCase();
        int foundIndex = -1;
        for (int i = 0; i < list.size(); i++) {
            ProductModel existing = list.get(i);
            if (existing.getName() != null && ProductModel.normalizeName(existing.getName()).equalsIgnoreCase(target)) {
                foundIndex = i;
                break;
            }
        }
        if (foundIndex != -1) {
            ProductModel existing = list.get(foundIndex);
            if (product.getLastPurchasePrice() > 0) {
                existing.setLastPurchasePrice(product.getLastPurchasePrice());
            }
            if (product.getSellingPrice() > 0) {
                existing.setSellingPrice(product.getSellingPrice());
            }
            if (product.getUnit() != null && !product.getUnit().isEmpty()) {
                existing.setUnit(product.getUnit());
            }
            if (product.getCategory() != null && !product.getCategory().isEmpty()) {
                existing.setCategory(product.getCategory());
            }
            existing.setUpdatedAt(System.currentTimeMillis());
            list.set(foundIndex, existing);
            saveProductMemory(list);
            saveProductSuggestion(existing.getName());
            return existing;
        } else {
            product.setName(ProductModel.normalizeName(product.getName()));
            list.add(0, product);
            saveProductMemory(list);
            saveProductSuggestion(product.getName());
            return product;
        }
    }

    public List<ProductModel> searchProductMemory(String query) {
        List<ProductModel> all = loadProductMemory();
        if (query == null || query.trim().isEmpty()) {
            return all;
        }
        String q = query.trim().toLowerCase();
        List<ProductModel> exactMatches = new ArrayList<>();
        List<ProductModel> prefixMatches = new ArrayList<>();
        List<ProductModel> containsMatches = new ArrayList<>();

        for (ProductModel p : all) {
            String name = p.getName() != null ? p.getName().toLowerCase() : "";
            if (name.equals(q)) {
                exactMatches.add(p);
            } else if (name.startsWith(q)) {
                prefixMatches.add(p);
            } else if (name.contains(q)) {
                containsMatches.add(p);
            }
        }
        List<ProductModel> results = new ArrayList<>();
        results.addAll(exactMatches);
        results.addAll(prefixMatches);
        results.addAll(containsMatches);
        return results;
    }

    private List<ProductModel> seedDefaultProductMemory() {
        List<ProductModel> list = new ArrayList<>();
        list.add(new ProductModel(null, "চিনি", ProductModel.UNIT_KG, 130.0, 140.0, "চিনি"));
        list.add(new ProductModel(null, "সয়াবিন তেল", ProductModel.UNIT_LITER, 185.0, 195.0, "তেল"));
        list.add(new ProductModel(null, "মসুর ডাল", ProductModel.UNIT_KG, 125.0, 140.0, "চাল/ডাল"));
        list.add(new ProductModel(null, "মিনিকেট চাল", ProductModel.UNIT_KG, 72.0, 80.0, "চাল/ডাল"));
        list.add(new ProductModel(null, "নাজিরশাইল চাল", ProductModel.UNIT_KG, 78.0, 85.0, "চাল/ডাল"));
        list.add(new ProductModel(null, "আটা", ProductModel.UNIT_KG, 50.0, 58.0, "চাল/ডাল"));
        list.add(new ProductModel(null, "ময়দা", ProductModel.UNIT_KG, 65.0, 75.0, "চাল/ডাল"));
        list.add(new ProductModel(null, "লবণ", ProductModel.UNIT_KG, 35.0, 42.0, "মসলা"));
        list.add(new ProductModel(null, "ডিম", ProductModel.UNIT_DOZEN, 145.0, 160.0, "অন্যান্য"));
        list.add(new ProductModel(null, "পেঁয়াজ", ProductModel.UNIT_KG, 85.0, 95.0, "মসলা"));
        list.add(new ProductModel(null, "আলু", ProductModel.UNIT_KG, 50.0, 60.0, "অন্যান্য"));
        list.add(new ProductModel(null, "রসুন", ProductModel.UNIT_KG, 210.0, 230.0, "মসলা"));
        list.add(new ProductModel(null, "আদা", ProductModel.UNIT_KG, 240.0, 260.0, "মসলা"));
        list.add(new ProductModel(null, "চা পাতা", ProductModel.UNIT_PACKET, 95.0, 110.0, "পানীয়"));
        list.add(new ProductModel(null, "লাক্স সাবান", ProductModel.UNIT_PIECE, 58.0, 65.0, "পরিষ্কারক"));
        list.add(new ProductModel(null, "হুইল ডিটারজেন্ট ১ কেজি", ProductModel.UNIT_PACKET, 120.0, 135.0, "পরিষ্কারক"));
        list.add(new ProductModel(null, "টোস্ট বিস্কুট", ProductModel.UNIT_PACKET, 45.0, 55.0, "বিস্কুট"));
        list.add(new ProductModel(null, "চানাচুর", ProductModel.UNIT_PACKET, 20.0, 25.0, "বিস্কুট"));
        return list;
    }

    public List<String> getCustomProductSuggestions() {
        String json = this.sharedPreferences.getString(KEY_PRODUCT_SUGGESTIONS, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<String>>() {}.getType();
        List<String> list = this.gson.fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }

    public void saveProductSuggestion(String name) {
        if (name == null || name.trim().isEmpty()) return;
        String cleanName = name.trim();
        List<String> list = getCustomProductSuggestions();
        for (String existing : list) {
            if (existing.equalsIgnoreCase(cleanName)) {
                return;
            }
        }
        list.add(0, cleanName);
        this.sharedPreferences.edit().putString(KEY_PRODUCT_SUGGESTIONS, this.gson.toJson(list)).apply();
    }

    public void scanAndSaveAllHistoricalProducts() {
        List<String> activeDates = getActiveDates();
        List<String> customList = getCustomProductSuggestions();
        boolean changed = false;
        for (String d : activeDates) {
            List<ExpenseModel> expList = loadExpenses(d);
            if (expList != null) {
                for (ExpenseModel exp : expList) {
                    if (exp != null && exp.getName() != null && !exp.getName().trim().isEmpty()) {
                        String name = exp.getName().trim();
                        boolean found = false;
                        for (String item : customList) {
                            if (item.equalsIgnoreCase(name)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            customList.add(name);
                            changed = true;
                        }
                    }
                }
            }
        }
        if (changed) {
            this.sharedPreferences.edit().putString(KEY_PRODUCT_SUGGESTIONS, this.gson.toJson(customList)).apply();
        }
    }

    public List<String> getAllProductSuggestionsWithDefaults() {
        List<String> result = new ArrayList<>();
        
        // 1. First add user's custom and previously entered items
        List<String> custom = getCustomProductSuggestions();
        for (String item : custom) {
            if (!result.contains(item)) {
                result.add(item);
            }
        }

        // 2. Add common default items for grocery & daily store
        String[] defaults = {
            "চাল", "মিনিকেট চাল", "নাজিরশাইল চাল", "পোলাও চাল", "আটা", "ময়দা", "সুজি",
            "সয়াবিন তেল", "সরিষার তেল", "চিনি", "লবণ", "ডাল", "মসুর ডাল", "মুগ ডাল", "খেসারি ডাল",
            "ডিম", "দুধ", "গরুর দুধ", "গুঁড়ো দুধ", "পেঁয়াজ", "আলু", "রসুন", "আদা", "কাঁচামরিচ", "শুকনামরিচ",
            "হলুদ", "মরিচের গুঁড়ো", "ধনিয়া", "জিরা", "গরম মসলা", "তেজপাতা", "এলাচ", "দারুচিনি",
            "বিস্কুট", "টোস্ট বিস্কুট", "চানাচুর", "চিপস", "চকলেট", "কেক", "পাউরুটি", "বনরুটি",
            "চা পাতা", "কফি", "সাবান", "লাক্স সাবান", "লাইফবয়", "কাপড় ধোয়ার সাবান", "ডিটারজেন্ট", "হুইল পাউডার",
            "শ্যাম্পু", "টুথপেস্ট", "টুথব্রাশ", "কোল্ড ড্রিংকস", "সেভেন আপ", "কোকাকোলা", "স্প্রাইট", "জুস", "আইসক্রিম", "মিনারেল ওয়াটার",
            "খাবার স্যালাইন", "সিগারেট", "বেনসন", "গোল্ডলিফ", "ডার্বি", "ম্যাচ", "মশার কয়েল", "টিস্যু পেপার", "পলিথিন", "বস্তা", "কার্টুন",
            "বাজার", "কাঁচামাল", "শাক-সবজি", "মাছ", "মাংস", "মুরগি", "গরুর মাংস",
            "পরিবহন", "রিকশা ভাড়া", "ভ্যান ভাড়া", "গাড়ি ভাড়া", "দোকান ভাড়া", "বিদ্যুৎ বিল",
            "কর্মচারীর বেতন", "নাস্তা খরচ", "চা-নাস্তা", "বিকাশ খরচ", "নগদ খরচ", "ব্যাংক খরচ", "অন্যান্য খরচ"
        };

        for (String def : defaults) {
            if (!result.contains(def)) {
                result.add(def);
            }
        }

        return result;
    }

    public double getPreviousDayClosingCash(String currentDate) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.US);
            java.util.Date cur = sdf.parse(currentDate);
            if (cur == null) return 0.0;

            // 1. Try yesterday
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(cur);
            cal.add(java.util.Calendar.DATE, -1);
            String yesterday = sdf.format(cal.getTime());
            double yesterdayCash = loadAvailableCash(yesterday);
            if (yesterdayCash > 0) {
                return yesterdayCash;
            }

            // 2. Find most recent past active date
            List<String> activeDates = getActiveDates();
            String bestPastDate = null;
            long bestDiff = Long.MAX_VALUE;
            for (String dStr : activeDates) {
                try {
                    java.util.Date d = sdf.parse(dStr);
                    if (d != null && d.before(cur)) {
                        long diff = cur.getTime() - d.getTime();
                        if (diff < bestDiff) {
                            double cash = loadAvailableCash(dStr);
                            if (cash > 0) {
                                bestDiff = diff;
                                bestPastDate = dStr;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }

            if (bestPastDate != null) {
                return loadAvailableCash(bestPastDate);
            }
        } catch (Exception ignored) {}
        return 0.0;
    }

    public void clearAll() {
        this.sharedPreferences.edit().clear().apply();
    }
}
