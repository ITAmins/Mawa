package com.example;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Cloud Sync Engine for MAWA Accounting System.
 * Connects to Supabase Database (user_backups & mawa_cloud_records)
 * Full offline-first design:
 * - Saves locally first
 * - Queues pending items
 * - Pushes & merges safely with RLS protection (user_id = auth.uid())
 * - Conflict resolution by latest updated_at
 * - Zero data loss: never clears local or cloud data without explicit sync logic.
 */
public class MawaSyncManager {
    private static final String TAG = "MawaSyncManager";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    public enum SyncStatus {
        NOT_SIGNED_IN("লগইন নেই", "#94A3B8"),
        OFFLINE("অফলাইন", "#EAB308"),
        SYNCING("সিঙ্ক হচ্ছে...", "#3B82F6"),
        SYNCED("ক্লাউডে সংরক্ষিত", "#10B981"),
        SYNC_FAILED("সিঙ্ক ব্যর্থ", "#EF4444");

        private final String labelBengali;
        private final String colorHex;

        SyncStatus(String labelBengali, String colorHex) {
            this.labelBengali = labelBengali;
            this.colorHex = colorHex;
        }

        public String getLabelBengali() {
            return labelBengali;
        }

        public String getColorHex() {
            return colorHex;
        }
    }

    public interface SyncListener {
        void onSyncStatusChanged(SyncStatus status, String message);
        void onSyncCompleted(boolean success, String summary);
    }

    public interface SyncCallback {
        void onSyncStarted();
        void onSyncSuccess(String message);
        void onSyncFailed(String error);
    }

    private static MawaSyncManager instance;
    private final Context context;
    private final StorageManager storageManager;
    private final SupabaseAuthManager authManager;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final ExecutorService executor;
    private final Handler mainHandler;

    private SyncStatus currentStatus = SyncStatus.NOT_SIGNED_IN;
    private String lastSyncTimeFormatted = "কখনো নয়";
    private final List<SyncListener> listeners = new ArrayList<>();

    private MawaSyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.storageManager = StorageManager.getInstance(context);
        this.authManager = SupabaseAuthManager.getInstance(context);
        this.httpClient = new OkHttpClient.Builder().build();
        this.gson = new Gson();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());

        updateInitialStatus();
    }

    public static synchronized MawaSyncManager getInstance(Context context) {
        if (instance == null) {
            instance = new MawaSyncManager(context);
        }
        return instance;
    }

    public void addListener(SyncListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            listener.onSyncStatusChanged(currentStatus, getStatusMessage());
        }
    }

    public void removeListener(SyncListener listener) {
        listeners.remove(listener);
    }

    public SyncStatus getCurrentStatus() {
        return currentStatus;
    }

    public boolean isSyncing() {
        return currentStatus == SyncStatus.SYNCING;
    }

    public String getLastSyncTimeFormatted() {
        return lastSyncTimeFormatted;
    }

    public String getStatusMessage() {
        if (!authManager.isAuthenticated()) {
            return "ক্লাউড ব্যাকআপের জন্য লগইন করুন";
        }
        if (!authManager.isNetworkAvailable()) {
            return "অফলাইন মোড (ইন্টারনেট পেলে স্বয়ংক্রিয় সিঙ্ক হবে)";
        }
        if (currentStatus == SyncStatus.SYNCED) {
            return "সর্বশেষ সিঙ্ক: " + lastSyncTimeFormatted;
        }
        return currentStatus.getLabelBengali();
    }

    private void updateInitialStatus() {
        if (!authManager.isAuthenticated()) {
            setStatus(SyncStatus.NOT_SIGNED_IN, "লগইন করা নেই");
        } else if (!authManager.isNetworkAvailable()) {
            setStatus(SyncStatus.OFFLINE, "অফলাইন");
        } else {
            setStatus(SyncStatus.SYNCED, "প্রস্তুত");
        }
    }

    private void setStatus(SyncStatus status, String msg) {
        this.currentStatus = status;
        mainHandler.post(() -> {
            for (SyncListener l : new ArrayList<>(listeners)) {
                l.onSyncStatusChanged(status, msg);
            }
        });
    }

    public void syncAsync(SyncCallback callback) {
        if (callback != null) {
            mainHandler.post(callback::onSyncStarted);
        }
        triggerSync(new SyncListener() {
            @Override
            public void onSyncStatusChanged(SyncStatus status, String message) {
            }

            @Override
            public void onSyncCompleted(boolean success, String summary) {
                if (callback != null) {
                    if (success) {
                        callback.onSyncSuccess(summary);
                    } else {
                        callback.onSyncFailed(summary);
                    }
                }
            }
        });
    }

    /**
     * Trigger full two-way cloud sync (Local <-> Cloud)
     */
    public void triggerSync(SyncListener callback) {
        if (callback != null) {
            addListener(callback);
        }

        if (!authManager.isAuthenticated()) {
            setStatus(SyncStatus.NOT_SIGNED_IN, "অনুগ্রহ করে প্রথমে লগইন করুন।");
            if (callback != null) callback.onSyncCompleted(false, "লগইন করা নেই।");
            return;
        }

        if (!authManager.isNetworkAvailable()) {
            setStatus(SyncStatus.OFFLINE, "ইন্টারনেট সংযোগ নেই। সংযোগ পেলে স্বয়ংক্রিয় সিঙ্ক হবে।");
            if (callback != null) callback.onSyncCompleted(false, "ইন্টারনেট সংযোগ নেই।");
            return;
        }

        setStatus(SyncStatus.SYNCING, "ক্লাউড সিঙ্ক হচ্ছে...");

        executor.execute(() -> {
            try {
                String userId = authManager.getUserId();
                String accessToken = authManager.getAccessToken();
                String supabaseUrl = authManager.getSupabaseUrl();
                String supabaseKey = authManager.getSupabaseKey();

                if (userId == null || accessToken == null) {
                    setStatus(SyncStatus.NOT_SIGNED_IN, "সেশন পাওয়া যায়নি।");
                    return;
                }

                // 1. First, pull and merge remote snapshot if any exists
                pullAndMergeCloudData(supabaseUrl, supabaseKey, accessToken, userId);

                // 2. Push full merged local snapshot to cloud user_backups table
                boolean pushBackupSuccess = pushUserBackup(supabaseUrl, supabaseKey, accessToken, userId);

                // 3. Push granular records to mawa_cloud_records table
                pushGranularRecords(supabaseUrl, supabaseKey, accessToken, userId);

                if (pushBackupSuccess) {
                    lastSyncTimeFormatted = new SimpleDateFormat("hh:mm a, dd MMM", Locale.US).format(new Date());
                    setStatus(SyncStatus.SYNCED, "সর্বশেষ সিঙ্ক: " + lastSyncTimeFormatted);
                    mainHandler.post(() -> {
                        for (SyncListener l : new ArrayList<>(listeners)) {
                            l.onSyncCompleted(true, "ক্লাউড সিঙ্ক সফলভাবে সম্পন্ন হয়েছে!");
                        }
                    });
                } else {
                    setStatus(SyncStatus.SYNC_FAILED, "সিঙ্ক ব্যর্থ হয়েছে।");
                    mainHandler.post(() -> {
                        for (SyncListener l : new ArrayList<>(listeners)) {
                            l.onSyncCompleted(false, "সিঙ্ক ব্যর্থ হয়েছে।");
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Sync error: ", e);
                setStatus(SyncStatus.SYNC_FAILED, "সিঙ্ক ত্রুটি: " + e.getMessage());
                mainHandler.post(() -> {
                    for (SyncListener l : new ArrayList<>(listeners)) {
                        l.onSyncCompleted(false, "ত্রুটি: " + e.getMessage());
                    }
                });
            }
        });
    }

    /**
     * Pull remote cloud backup and safely merge with local data.
     */
    private void pullAndMergeCloudData(String url, String apikey, String token, String userId) {
        try {
            String endpoint = url + "/rest/v1/user_backups?user_id=eq." + userId + "&order=updated_at.desc&limit=1";
            Request request = new Request.Builder()
                    .url(endpoint)
                    .addHeader("apikey", apikey)
                    .addHeader("Authorization", "Bearer " + token)
                    .get()
                    .build();

            Response response = httpClient.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                String respStr = response.body().string();
                JsonArray arr = JsonParser.parseString(respStr).getAsJsonArray();
                if (arr != null && arr.size() > 0) {
                    JsonObject latestBackup = arr.get(0).getAsJsonObject();
                    if (latestBackup.has("backup_data")) {
                        JsonObject cloudData = latestBackup.getAsJsonObject("backup_data");
                        mergeCloudDataIntoLocal(cloudData);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error pulling remote backup: ", e);
        }
    }

    /**
     * Merge cloud data safely into local StorageManager without deleting existing records
     */
    private void mergeCloudDataIntoLocal(JsonObject cloudData) {
        if (cloudData == null) return;

        try {
            // 1. Merge Products
            if (cloudData.has("products")) {
                JsonArray cloudProducts = cloudData.getAsJsonArray("products");
                List<ProductModel> localProducts = storageManager.loadProductMemory();
                Map<String, ProductModel> map = new HashMap<>();
                for (ProductModel p : localProducts) {
                    map.put(p.getName().toLowerCase(), p);
                }
                for (JsonElement elem : cloudProducts) {
                    try {
                        ProductModel cp = gson.fromJson(elem, ProductModel.class);
                        if (cp != null && cp.getName() != null) {
                            String k = cp.getName().toLowerCase();
                            if (!map.containsKey(k) || map.get(k).getUpdatedAt() < cp.getUpdatedAt()) {
                                map.put(k, cp);
                            }
                        }
                    } catch (Exception ignored) {}
                }
                storageManager.saveProductMemory(new ArrayList<>(map.values()));
            }

            // 2. Merge Baki Records
            if (cloudData.has("baki_records")) {
                JsonArray cloudBaki = cloudData.getAsJsonArray("baki_records");
                List<BakiModel> localBaki = storageManager.loadBakiRecords();
                Map<String, BakiModel> bakiMap = new HashMap<>();
                for (BakiModel b : localBaki) {
                    bakiMap.put(b.getId(), b);
                }
                for (JsonElement elem : cloudBaki) {
                    try {
                        BakiModel cb = gson.fromJson(elem, BakiModel.class);
                        if (cb != null && cb.getId() != null) {
                            if (!bakiMap.containsKey(cb.getId()) || bakiMap.get(cb.getId()).getUpdatedAt() < cb.getUpdatedAt()) {
                                bakiMap.put(cb.getId(), cb);
                            }
                        }
                    } catch (Exception ignored) {}
                }
                storageManager.saveBakiRecords(new ArrayList<>(bakiMap.values()));
            }

            // 3. Merge Fordi Records
            if (cloudData.has("fordi_records")) {
                JsonArray cloudFordi = cloudData.getAsJsonArray("fordi_records");
                List<FordiModel> localFordi = storageManager.loadFordiRecords();
                Map<String, FordiModel> fordiMap = new HashMap<>();
                for (FordiModel f : localFordi) {
                    fordiMap.put(f.getId(), f);
                }
                for (JsonElement elem : cloudFordi) {
                    try {
                        FordiModel cf = gson.fromJson(elem, FordiModel.class);
                        if (cf != null && cf.getId() != null) {
                            if (!fordiMap.containsKey(cf.getId()) || fordiMap.get(cf.getId()).getUpdatedAt() < cf.getUpdatedAt()) {
                                fordiMap.put(cf.getId(), cf);
                            }
                        }
                    } catch (Exception ignored) {}
                }
                storageManager.saveFordiRecords(new ArrayList<>(fordiMap.values()));
            }

            // 4. Merge Expenses for active dates
            if (cloudData.has("expenses_by_date")) {
                JsonObject expByDate = cloudData.getAsJsonObject("expenses_by_date");
                for (String dateKey : expByDate.keySet()) {
                    storageManager.saveActiveDate(dateKey);
                    JsonArray dateExps = expByDate.getAsJsonArray(dateKey);
                    List<ExpenseModel> localList = storageManager.loadExpenses(dateKey);
                    Map<String, ExpenseModel> expMap = new HashMap<>();
                    for (ExpenseModel e : localList) {
                        if (e.getId() != null) expMap.put(e.getId(), e);
                    }
                    for (JsonElement el : dateExps) {
                        try {
                            ExpenseModel ce = gson.fromJson(el, ExpenseModel.class);
                            if (ce != null && ce.getId() != null) {
                                if (!expMap.containsKey(ce.getId()) || expMap.get(ce.getId()).getUpdatedAt() < ce.getUpdatedAt()) {
                                    expMap.put(ce.getId(), ce);
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                    storageManager.saveExpenses(dateKey, new ArrayList<>(expMap.values()));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error merging cloud data: ", e);
        }
    }

    /**
     * Push full local backup snapshot to user_backups table
     */
    private boolean pushUserBackup(String url, String apikey, String token, String userId) {
        try {
            JsonObject backupData = new JsonObject();
            backupData.add("products", gson.toJsonTree(storageManager.loadProductMemory()));
            backupData.add("baki_records", gson.toJsonTree(storageManager.loadBakiRecords()));
            backupData.add("fordi_records", gson.toJsonTree(storageManager.loadFordiRecords()));

            JsonObject expensesByDate = new JsonObject();
            List<String> activeDates = storageManager.getActiveDates();
            for (String d : activeDates) {
                expensesByDate.add(d, gson.toJsonTree(storageManager.loadExpenses(d)));
            }
            backupData.add("expenses_by_date", expensesByDate);
            backupData.addProperty("estimated_gross_margin", storageManager.getEstimatedGrossMarginRate());

            JsonObject row = new JsonObject();
            row.addProperty("user_id", userId);
            String userEmail = authManager.getUserEmail();
            if (userEmail != null) {
                row.addProperty("email", userEmail);
            }
            row.add("backup_data", backupData);
            row.addProperty("updated_at", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(new Date()));

            RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, row.toString());
            String endpoint = url + "/rest/v1/user_backups";

            Request request = new Request.Builder()
                    .url(endpoint)
                    .addHeader("apikey", apikey)
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "resolution=merge-duplicates")
                    .post(body)
                    .build();

            Response response = httpClient.newCall(request).execute();
            return response.isSuccessful() || response.code() == 201 || response.code() == 200 || response.code() == 204;
        } catch (Exception e) {
            Log.e(TAG, "Error pushing user backup: ", e);
            return false;
        }
    }

    /**
     * Push individual granular records to mawa_cloud_records table
     */
    private void pushGranularRecords(String url, String apikey, String token, String userId) {
        try {
            JsonArray records = new JsonArray();
            String nowIso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(new Date());

            // 1. Push Baki customers
            List<BakiModel> bakiList = storageManager.loadBakiRecords();
            for (BakiModel b : bakiList) {
                JsonObject r = new JsonObject();
                r.addProperty("user_id", userId);
                r.addProperty("domain", "business");
                r.addProperty("entity_type", "BAKI");
                r.addProperty("entity_id", b.getId());
                r.add("data", gson.toJsonTree(b));
                r.addProperty("updated_at", nowIso);
                records.add(r);
            }

            // 2. Push Fordi sheets
            List<FordiModel> fordiList = storageManager.loadFordiRecords();
            for (FordiModel f : fordiList) {
                JsonObject r = new JsonObject();
                r.addProperty("user_id", userId);
                r.addProperty("domain", "business");
                r.addProperty("entity_type", "FORDI");
                r.addProperty("entity_id", f.getId());
                r.add("data", gson.toJsonTree(f));
                r.addProperty("updated_at", nowIso);
                records.add(r);
            }

            // 3. Push Products
            List<ProductModel> products = storageManager.loadProductMemory();
            for (ProductModel p : products) {
                JsonObject r = new JsonObject();
                r.addProperty("user_id", userId);
                r.addProperty("domain", "business");
                r.addProperty("entity_type", "PRODUCT");
                r.addProperty("entity_id", p.getId());
                r.add("data", gson.toJsonTree(p));
                r.addProperty("updated_at", nowIso);
                records.add(r);
            }

            if (records.size() > 0) {
                RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, records.toString());
                String endpoint = url + "/rest/v1/mawa_cloud_records";

                Request request = new Request.Builder()
                        .url(endpoint)
                        .addHeader("apikey", apikey)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "resolution=merge-duplicates")
                        .post(body)
                        .build();

                httpClient.newCall(request).execute();
            }
        } catch (Exception e) {
            Log.w(TAG, "Optional granular records push failed: " + e.getMessage());
        }
    }
}
