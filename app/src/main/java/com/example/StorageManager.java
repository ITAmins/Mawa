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
        }
        allData.put(KEY_BAKI_RECORDS, loadBakiRecords());
        allData.put(KEY_FORDI_RECORDS, loadFordiRecords());
        allData.put(KEY_PRODUCT_SUGGESTIONS, getCustomProductSuggestions());
        return allData;
    }

    public void importAllData(Map<String, Object> data) {
        if (data == null) {
            return;
        }
        SharedPreferences.Editor editor = this.sharedPreferences.edit();
        editor.clear();
        try {
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
                    }
                }
            }
            editor.apply();
        } catch (Exception e) {
        }
    }

    public static final String KEY_PRODUCT_SUGGESTIONS = "key_product_suggestions";

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
