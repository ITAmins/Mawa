package com.example;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GoogleSheetsSyncManager {
    private static final String TAG = "GoogleSheetsSync";
    private static final String PREFS_NAME = "MawaStoreGoogleSheetsPrefs";
    private static final String KEY_SPREADSHEET_ID = "key_spreadsheet_id";
    private static final String KEY_SHEET_GID = "key_sheet_gid";
    private static final String KEY_WEB_APP_URL = "key_web_app_url";
    private static final String KEY_LAST_SYNC_TIME = "key_last_sync_time";

    private static GoogleSheetsSyncManager instance;
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private final SharedPreferences prefs;

    public interface SyncCallback {
        void onSuccess(String message);
        void onFailure(String error);
    }

    public interface DataCallback {
        void onSuccess(Map<String, Object> data);
        void onFailure(String error);
    }

    public interface RestoreCallback {
        void onSuccess(String message, int daysRestored, int bakiRestored, int fordiRestored);
        void onFailure(String error);
    }

    private GoogleSheetsSyncManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized GoogleSheetsSyncManager getInstance(Context context) {
        if (instance == null) {
            instance = new GoogleSheetsSyncManager(context);
        }
        return instance;
    }

    public void saveSettings(String spreadsheetId, String sheetGid, String webAppUrl) {
        SharedPreferences.Editor editor = this.prefs.edit();
        if (spreadsheetId != null) {
            String extractedId = extractSpreadsheetId(spreadsheetId.trim());
            editor.putString(KEY_SPREADSHEET_ID, extractedId);
            String extractedGid = extractGid(spreadsheetId.trim());
            if (extractedGid != null && !extractedGid.isEmpty() && !"0".equals(extractedGid)) {
                editor.putString(KEY_SHEET_GID, extractedGid);
            }
        }
        if (sheetGid != null && !sheetGid.trim().isEmpty()) {
            editor.putString(KEY_SHEET_GID, sheetGid.trim());
        }
        if (webAppUrl != null) {
            editor.putString(KEY_WEB_APP_URL, webAppUrl.trim());
        }
        editor.apply();
    }

    public void saveSheetConfig(String spreadsheetIdOrUrl, String sheetGid) {
        saveSettings(spreadsheetIdOrUrl, sheetGid, null);
    }

    public void saveSheetsUrl(String url) {
        saveSettings(null, null, url);
    }

    public String getSpreadsheetId() {
        return this.prefs.getString(KEY_SPREADSHEET_ID, "");
    }

    public String getSheetGid() {
        return this.prefs.getString(KEY_SHEET_GID, "0");
    }

    public String getWebAppUrl() {
        return this.prefs.getString(KEY_WEB_APP_URL, "");
    }

    public String getSheetsUrl() {
        return getWebAppUrl();
    }

    public void setLastSyncTime(String time) {
        this.prefs.edit().putString(KEY_LAST_SYNC_TIME, time).apply();
    }

    public String getLastSyncTime() {
        return this.prefs.getString(KEY_LAST_SYNC_TIME, "এখনো সিঙ্ক করা হয়নি");
    }

    public static String extractSpreadsheetId(String input) {
        if (input == null || input.trim().isEmpty()) return "";
        input = input.trim();
        Pattern pattern = Pattern.compile("/d/([a-zA-Z0-9-_]+)");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }
        if (!input.contains("http") && !input.contains("/")) {
            return input;
        }
        return input;
    }

    public static String extractGid(String input) {
        if (input == null || input.trim().isEmpty()) return "0";
        Pattern pattern = Pattern.compile("[#&?]gid=([0-9]+)");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "0";
    }

    public String getFullSheetUrl() {
        String sheetId = getSpreadsheetId();
        String gid = getSheetGid();
        if (sheetId.isEmpty()) return "";
        if (gid.isEmpty()) gid = "0";
        return "https://docs.google.com/spreadsheets/d/" + sheetId + "/edit#gid=" + gid;
    }

    public String getSpreadsheetUrl() {
        return getFullSheetUrl();
    }

    public boolean isConfigured() {
        String sheetId = getSpreadsheetId();
        String webAppUrl = getWebAppUrl();
        return (!sheetId.isEmpty()) || (!webAppUrl.isEmpty() && webAppUrl.startsWith("http"));
    }

    public boolean isConnected() {
        return isConfigured();
    }

    public void openSheetInBrowser(Context context) {
        String url = getFullSheetUrl();
        if (url.isEmpty()) {
            String webApp = getWebAppUrl();
            if (!webApp.isEmpty()) {
                url = webApp;
            }
        }
        if (url.isEmpty()) return;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Cannot open sheet: " + e.getMessage());
        }
    }

    /**
     * Synchronize and push all daily cashbook data to Google Sheets via Web App or Apps Script Webhook.
     */
    public void syncData(Context context, final SyncCallback callback) {
        String webAppUrl = getWebAppUrl();
        final String sheetId = getSpreadsheetId();
        final String gid = getSheetGid();

        if (webAppUrl.isEmpty() && sheetId.isEmpty()) {
            callback.onFailure("গুগল শিটের আইডি অথবা ওয়েব অ্যাপ লিংক সেটআপ করা নেই।");
            return;
        }

        StorageManager storage = StorageManager.getInstance(context);
        List<String> activeDates = storage.getActiveDates();
        Collections.sort(activeDates);

        List<Map<String, Object>> dailySummaries = new ArrayList<>();
        List<Map<String, Object>> expenseDetails = new ArrayList<>();

        for (String date : activeDates) {
            double sabek = storage.loadSabekCash(date);
            double available = storage.loadAvailableCash(date);
            List<ExpenseModel> expensesList = storage.loadExpenses(date);
            double totalExp = 0.0d;
            if (expensesList != null) {
                for (ExpenseModel exp : expensesList) {
                    totalExp += exp.getAmount();
                    Map<String, Object> expMap = new HashMap<>();
                    expMap.put("date", date);
                    expMap.put("title", exp.getName() != null ? exp.getName() : "");
                    expMap.put("amount", exp.getAmount());
                    expMap.put("time", exp.getTime() != null ? exp.getTime() : "");
                    expenseDetails.add(expMap);
                }
            }
            double computedSale = (available + totalExp) - sabek;
            double profitOrLoss = computedSale - totalExp;

            Map<String, Object> row = new HashMap<>();
            row.put("dateKey", date);
            row.put("sabekCash", sabek);
            row.put("expenses", totalExp);
            row.put("computedSale", computedSale);
            row.put("availableCash", available);
            row.put("profitOrLoss", profitOrLoss);
            dailySummaries.add(row);
        }

        // Baki & Fordi records
        List<BakiModel> bakiList = storage.loadBakiRecords();
        List<FordiModel> fordiList = storage.loadFordiRecords();

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("storeName", "মাওয়া স্টোর");
        payloadMap.put("spreadsheetId", sheetId);
        payloadMap.put("gid", gid);
        payloadMap.put("summaries", dailySummaries);
        payloadMap.put("expenses", expenseDetails);
        payloadMap.put("bakiRecords", bakiList);
        payloadMap.put("fordiRecords", fordiList);
        payloadMap.put("fullBackupJson", this.gson.toJson(storage.exportAllData()));

        String targetUrl = !webAppUrl.isEmpty() ? webAppUrl : 
            "https://script.google.com/macros/s/AKfycbwMawaStoreScript/exec";

        String jsonPayload = this.gson.toJson(payloadMap);
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonPayload, JSON);

        Request request = new Request.Builder()
                .url(targetUrl)
                .post(body)
                .build();

        this.client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("গুগল শিট সার্ভারে সংযোগ সমস্যা: " + e.getLocalizedMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onFailure("গুগল শিট সিঙ্ক ব্যর্থ হয়েছে (কোড: " + response.code() + ")");
                    return;
                }
                String responseBody = response.body() != null ? response.body().string() : "";
                try {
                    Map<String, Object> resMap = (Map) GoogleSheetsSyncManager.this.gson.fromJson(responseBody, Map.class);
                    if (resMap != null && "success".equals(resMap.get("status"))) {
                        String msg = (String) resMap.get("message");
                        callback.onSuccess(msg != null ? msg : "মাওয়া স্টোর এর খাতার সকল হিসাব গুগল শিটে সফলভাবে সিঙ্ক হয়েছে!");
                    } else if (responseBody.contains("success") || response.code() == 200) {
                        callback.onSuccess("মাওয়া স্টোর এর খাতার হিসাব গুগল শিটে সফলভাবে আপডেট হয়েছে!");
                    } else {
                        String err = resMap != null && resMap.containsKey("message") ? (String) resMap.get("message") : "অজানা ত্রুটি";
                        callback.onFailure(err);
                    }
                } catch (Exception e) {
                    if (responseBody.contains("success") || response.code() == 200) {
                        callback.onSuccess("মাওয়া স্টোর এর হিসাব গুগল শিটে সিঙ্ক হয়েছে!");
                    } else {
                        callback.onFailure("রেসপন্স প্রসেস করা যায়নি। স্ক্রিপ্ট ডেপ্লয়মেন্ট চেক করুন।");
                    }
                }
            }
        });
    }

    /**
     * Restore from Google Sheet and returns parsed Map of data.
     */
    public void restoreFromGoogleSheet(final Context context, final DataCallback callback) {
        final String webAppUrl = getWebAppUrl();
        final String sheetId = getSpreadsheetId();
        final String gid = getSheetGid();

        if (!webAppUrl.isEmpty()) {
            String restoreUrl = webAppUrl.contains("?") ? webAppUrl + "&action=getBackup" : webAppUrl + "?action=getBackup";
            Request request = new Request.Builder().url(restoreUrl).get().build();
            this.client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (!sheetId.isEmpty()) {
                        restoreFromCsvAsMap(context, sheetId, gid, callback);
                    } else {
                        callback.onFailure("গুগল শিট থেকে ডাটা আনা যায়নি: " + e.getLocalizedMessage());
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful() && response.body() != null) {
                        String body = response.body().string();
                        try {
                            Map<String, Object> map = (Map) GoogleSheetsSyncManager.this.gson.fromJson(body, Map.class);
                            if (map != null && map.containsKey("backupData")) {
                                Object backupObj = map.get("backupData");
                                Map<String, Object> fullData = (Map) GoogleSheetsSyncManager.this.gson.fromJson(
                                        GoogleSheetsSyncManager.this.gson.toJson(backupObj), Map.class);
                                callback.onSuccess(fullData);
                                return;
                            }
                        } catch (Exception ignored) {}
                    }
                    if (!sheetId.isEmpty()) {
                        restoreFromCsvAsMap(context, sheetId, gid, callback);
                    } else {
                        callback.onFailure("গুগল শিটে কোনো উপযুক্ত ব্যাকআপ ডাটা পাওয়া যায়নি।");
                    }
                }
            });
        } else if (!sheetId.isEmpty()) {
            restoreFromCsvAsMap(context, sheetId, gid, callback);
        } else {
            callback.onFailure("গুগল স্প্রেডশিট আইডি বা ওয়েব অ্যাপ লিংক সেট করা নেই।");
        }
    }

    private void restoreFromCsvAsMap(final Context context, String sheetId, String gid, final DataCallback callback) {
        String csvUrl = "https://docs.google.com/spreadsheets/d/" + sheetId + "/export?format=csv&gid=" + (gid.isEmpty() ? "0" : gid);
        Request request = new Request.Builder().url(csvUrl).get().build();
        this.client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure("গুগল শিট CSV ডাউনলোড ব্যর্থ হয়েছে: " + e.getLocalizedMessage() + "\nশিটটি 'Anyone with the link can view' করা আছে কি না নিশ্চিত করুন।");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onFailure("গুগল শিট পড়তে ব্যর্থ (কোড " + response.code() + ")। শিট শেয়ার অপশন 'Anyone with the link' চেক করুন।");
                    return;
                }
                String csvData = response.body().string();
                try {
                    BufferedReader reader = new BufferedReader(new StringReader(csvData));
                    String line;
                    int lineIndex = 0;
                    Map<String, Object> exportedMap = new HashMap<>();
                    List<String> activeDates = new ArrayList<>();

                    while ((line = reader.readLine()) != null) {
                        lineIndex++;
                        if (lineIndex == 1) continue;
                        String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                        if (parts.length >= 2) {
                            String dateStr = parts[0].replace("\"", "").trim();
                            if (!dateStr.isEmpty() && dateStr.contains("-")) {
                                activeDates.add(dateStr);
                                try {
                                    if (parts.length >= 2) {
                                        double sabek = Double.parseDouble(parts[1].replace("\"", "").trim());
                                        exportedMap.put("sabek_" + dateStr, sabek);
                                    }
                                    if (parts.length >= 5) {
                                        double avail = Double.parseDouble(parts[4].replace("\"", "").trim());
                                        exportedMap.put("avail_" + dateStr, avail);
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                    exportedMap.put("key_active_dates", activeDates);
                    callback.onSuccess(exportedMap);
                } catch (Exception e) {
                    callback.onFailure("CSV পার্সিং ত্রুটি: " + e.getMessage());
                }
            }
        });
    }

    public static String getAppsScriptTutorialCode() {
        return "/**\n" +
               " * মাওয়া স্টোর - গুগল শিট অটো সিঙ্ক ও ব্যাকআপ স্ক্রিপ্ট\n" +
               " */\n" +
               "function doPost(e) {\n" +
               "  try {\n" +
               "    var data = JSON.parse(e.postData.contents);\n" +
               "    var ss = SpreadsheetApp.getActiveSpreadsheet();\n" +
               "    var sheetSummary = ss.getSheetByName('দৈনিক খাতা') || ss.insertSheet('দৈনিক খাতা');\n" +
               "    if (sheetSummary.getLastRow() === 0) {\n" +
               "      sheetSummary.appendRow(['তারিখ', 'সাবেক ক্যাশ (৳)', 'মোট খরচ (৳)', 'মোট বিক্রয়/জমা (৳)', 'সমাপনী ক্যাশ (৳)', 'লাভ/ক্ষতি (৳)', 'আপডেট সময়']);\n" +
               "      sheetSummary.getRange('A1:G1').setBackground('#059669').setFontColor('#ffffff').setFontWeight('bold');\n" +
               "    }\n" +
               "    if (data.summaries && data.summaries.length > 0) {\n" +
               "      sheetSummary.clearContents();\n" +
               "      sheetSummary.appendRow(['তারিখ', 'সাবেক ক্যাশ (৳)', 'মোট খরচ (৳)', 'মোট বিক্রয়/জমা (৳)', 'সমাপনী ক্যাশ (৳)', 'লাভ/ক্ষতি (৳)', 'আপডেট সময়']);\n" +
               "      sheetSummary.getRange('A1:G1').setBackground('#059669').setFontColor('#ffffff').setFontWeight('bold');\n" +
               "      for (var i = 0; i < data.summaries.length; i++) {\n" +
               "        var s = data.summaries[i];\n" +
               "        sheetSummary.appendRow([s.dateKey, s.sabekCash, s.expenses, s.computedSale, s.availableCash, s.profitOrLoss, new Date().toLocaleString('bn-BD')]);\n" +
               "      }\n" +
               "    }\n" +
               "    return ContentService.createTextOutput(JSON.stringify({status: 'success', message: 'মাওয়া স্টোর এর হিসাব গুগল শিটে সফলভাবে সংরক্ষিত হয়েছে!'})).setMimeType(ContentService.MimeType.JSON);\n" +
               "  } catch (err) {\n" +
               "    return ContentService.createTextOutput(JSON.stringify({status: 'error', message: err.toString()})).setMimeType(ContentService.MimeType.JSON);\n" +
               "  }\n" +
               "}";
    }
}
