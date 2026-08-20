package com.example;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
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
 * Supabase Authentication Manager for MAWA.
 * Handles Email/Password Signup, Login, Password Reset, Token Refresh, and Session Persistence.
 */
public class SupabaseAuthManager {
    private static final String TAG = "SupabaseAuthManager";
    private static final String PREFS_NAME = "MawaSupabaseAuthPrefs";
    private static final String KEY_ACCESS_TOKEN = "key_access_token";
    private static final String KEY_REFRESH_TOKEN = "key_refresh_token";
    private static final String KEY_USER_ID = "key_user_id";
    private static final String KEY_USER_EMAIL = "key_user_email";
    private static final String KEY_USER_NAME = "key_user_name";
    private static final String KEY_TOKEN_EXPIRES_AT = "key_token_expires_at";

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private static SupabaseAuthManager instance;
    private final Context context;
    private final SharedPreferences prefs;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final ExecutorService executor;
    private final Handler mainHandler;

    private final String supabaseUrl;
    private final String supabaseKey;

    public static class AuthSession {
        public String userId;
        public String email;
        public String accessToken;
        public String refreshToken;

        public AuthSession() {}

        public AuthSession(String userId, String email, String accessToken, String refreshToken) {
            this.userId = userId;
            this.email = email;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }

    public interface AuthCallback {
        void onSuccess(AuthSession session);
        void onFailure(String error);
    }

    public interface StateCallback {
        void onStateChanged(boolean isAuthenticated, String email, String name);
    }

    private SupabaseAuthManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.httpClient = new OkHttpClient.Builder().build();
        this.gson = new Gson();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());

        String url = BuildConfig.SUPABASE_URL;
        String key = BuildConfig.SUPABASE_KEY;

        if (url == null || url.trim().isEmpty() || url.contains("YOUR_")) {
            url = "https://pkpcfksbslbileordrqs.supabase.co";
        }
        if (key == null || key.trim().isEmpty()) {
            key = "";
        }

        this.supabaseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.supabaseKey = key;
    }

    public static synchronized SupabaseAuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new SupabaseAuthManager(context);
        }
        return instance;
    }

    public String getSupabaseUrl() {
        return supabaseUrl;
    }

    public String getSupabaseKey() {
        return supabaseKey;
    }

    public boolean isConfigured() {
        return supabaseUrl != null && !supabaseUrl.isEmpty() && supabaseKey != null && !supabaseKey.isEmpty();
    }

    public boolean isAuthenticated() {
        String token = getAccessToken();
        String userId = getUserId();
        return token != null && !token.isEmpty() && userId != null && !userId.isEmpty();
    }

    public boolean isLoggedIn() {
        return isAuthenticated();
    }

    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    public String getUserName() {
        String name = prefs.getString(KEY_USER_NAME, "");
        if (name == null || name.trim().isEmpty()) {
            String email = getUserEmail();
            if (email != null && email.contains("@")) {
                return email.substring(0, email.indexOf("@"));
            }
            return "মাওয়া গ্রাহক";
        }
        return name;
    }

    public boolean isNetworkAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return nc != null && (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Sign Up with Email and Password
     */
    public void signUp(String email, String password, String displayName, AuthCallback callback) {
        if (!isNetworkAvailable()) {
            if (callback != null) mainHandler.post(() -> callback.onFailure("ইন্টারনেট সংযোগ নেই। অনুগ্রহ করে ইন্টারনেট চালু করুন।"));
            return;
        }
        if (!isConfigured()) {
            if (callback != null) mainHandler.post(() -> callback.onFailure("Supabase কনফিগারেশন অনুপস্থিত।"));
            return;
        }

        executor.execute(() -> {
            try {
                JsonObject jsonBody = new JsonObject();
                jsonBody.addProperty("email", email.trim());
                jsonBody.addProperty("password", password);
                
                JsonObject dataObj = new JsonObject();
                dataObj.addProperty("display_name", (displayName != null && !displayName.trim().isEmpty()) ? displayName.trim() : "মাওয়া গ্রাহক");
                jsonBody.add("data", dataObj);

                RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, jsonBody.toString());
                String endpoint = supabaseUrl + "/auth/v1/signup";

                Request request = new Request.Builder()
                        .url(endpoint)
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Content-Type", "application/json")
                        .post(body)
                        .build();

                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onFailure("রেজিস্ট্রেশন ব্যর্থ: " + translateError(e.getMessage()));
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String respStr = response.body() != null ? response.body().string() : "";
                        if (response.isSuccessful()) {
                            try {
                                JsonObject obj = JsonParser.parseString(respStr).getAsJsonObject();
                                String accessToken = obj.has("access_token") ? obj.get("access_token").getAsString() : null;
                                String refreshToken = obj.has("refresh_token") ? obj.get("refresh_token").getAsString() : null;
                                
                                JsonObject userObj = obj.has("user") ? obj.getAsJsonObject("user") : obj;
                                String userId = userObj.has("id") ? userObj.get("id").getAsString() : "";
                                String resEmail = userObj.has("email") ? userObj.get("email").getAsString() : email;

                                if (accessToken != null && !accessToken.isEmpty()) {
                                    saveSession(accessToken, refreshToken, userId, resEmail, displayName);
                                }
                                AuthSession session = new AuthSession(userId, resEmail, accessToken, refreshToken);
                                mainHandler.post(() -> {
                                    if (callback != null) callback.onSuccess(session);
                                });
                            } catch (Exception e) {
                                AuthSession session = new AuthSession("", email, "", "");
                                mainHandler.post(() -> {
                                    if (callback != null) callback.onSuccess(session);
                                });
                            }
                        } else {
                            String err = extractErrorMessage(respStr);
                            mainHandler.post(() -> {
                                if (callback != null) callback.onFailure(translateError(err));
                            });
                        }
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onFailure("ত্রুটি: " + e.getMessage());
                });
            }
        });
    }

    public void signUpWithEmail(String email, String password, AuthCallback callback) {
        signUp(email, password, "", callback);
    }

    /**
     * Sign In with Email and Password
     */
    public void signIn(String email, String password, AuthCallback callback) {
        if (!isNetworkAvailable()) {
            if (callback != null) mainHandler.post(() -> callback.onFailure("ইন্টারনেট সংযোগ নেই। ইন্টারনেট সংযোগ চেক করুন।"));
            return;
        }
        if (!isConfigured()) {
            if (callback != null) mainHandler.post(() -> callback.onFailure("Supabase কনফিগারেশন অনুপস্থিত।"));
            return;
        }

        executor.execute(() -> {
            try {
                JsonObject jsonBody = new JsonObject();
                jsonBody.addProperty("email", email.trim());
                jsonBody.addProperty("password", password);

                RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, jsonBody.toString());
                String endpoint = supabaseUrl + "/auth/v1/token?grant_type=password";

                Request request = new Request.Builder()
                        .url(endpoint)
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Content-Type", "application/json")
                        .post(body)
                        .build();

                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onFailure("লগইন ব্যর্থ: " + translateError(e.getMessage()));
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String respStr = response.body() != null ? response.body().string() : "";
                        if (response.isSuccessful()) {
                            try {
                                JsonObject obj = JsonParser.parseString(respStr).getAsJsonObject();
                                String accessToken = obj.get("access_token").getAsString();
                                String refreshToken = obj.has("refresh_token") ? obj.get("refresh_token").getAsString() : "";
                                
                                JsonObject userObj = obj.has("user") ? obj.getAsJsonObject("user") : null;
                                String userId = userObj != null && userObj.has("id") ? userObj.get("id").getAsString() : "";
                                String resEmail = userObj != null && userObj.has("email") ? userObj.get("email").getAsString() : email;
                                
                                String displayName = "";
                                if (userObj != null && userObj.has("user_metadata")) {
                                    JsonObject meta = userObj.getAsJsonObject("user_metadata");
                                    if (meta.has("display_name")) {
                                        displayName = meta.get("display_name").getAsString();
                                    }
                                }

                                saveSession(accessToken, refreshToken, userId, resEmail, displayName);
                                AuthSession session = new AuthSession(userId, resEmail, accessToken, refreshToken);
                                mainHandler.post(() -> {
                                    if (callback != null) callback.onSuccess(session);
                                });
                            } catch (Exception e) {
                                mainHandler.post(() -> {
                                    if (callback != null) callback.onFailure("রেসপন্স প্রক্রিয়াকরণে ত্রুটি: " + e.getMessage());
                                });
                            }
                        } else {
                            String err = extractErrorMessage(respStr);
                            mainHandler.post(() -> {
                                if (callback != null) callback.onFailure(translateError(err));
                            });
                        }
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onFailure("ত্রুটি: " + e.getMessage());
                });
            }
        });
    }

    public void signInWithEmail(String email, String password, AuthCallback callback) {
        signIn(email, password, callback);
    }

    /**
     * Send Password Reset Email
     */
    public void resetPassword(String email, AuthCallback callback) {
        if (!isNetworkAvailable()) {
            if (callback != null) mainHandler.post(() -> callback.onFailure("ইন্টারনেট সংযোগ নেই।"));
            return;
        }

        executor.execute(() -> {
            try {
                JsonObject jsonBody = new JsonObject();
                jsonBody.addProperty("email", email.trim());

                RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, jsonBody.toString());
                String endpoint = supabaseUrl + "/auth/v1/recover";

                Request request = new Request.Builder()
                        .url(endpoint)
                        .addHeader("apikey", supabaseKey)
                        .addHeader("Content-Type", "application/json")
                        .post(body)
                        .build();

                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onFailure("রিকভারি ইমেইল পাঠানো ব্যর্থ: " + e.getMessage());
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String respStr = response.body() != null ? response.body().string() : "";
                        if (response.isSuccessful()) {
                            mainHandler.post(() -> {
                                if (callback != null) callback.onSuccess(new AuthSession("", email, "", ""));
                            });
                        } else {
                            String err = extractErrorMessage(respStr);
                            mainHandler.post(() -> {
                                if (callback != null) callback.onFailure(translateError(err));
                            });
                        }
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onFailure("ত্রুটি: " + e.getMessage());
                });
            }
        });
    }

    /**
     * Logout - Clears active session tokens only.
     * DOES NOT delete or clear local accounting database or SharedPreferences!
     */
    public void logout() {
        logout(null);
    }

    public void logout(AuthCallback callback) {
        prefs.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_USER_ID)
                .remove(KEY_USER_EMAIL)
                .remove(KEY_USER_NAME)
                .remove(KEY_TOKEN_EXPIRES_AT)
                .apply();

        if (callback != null) {
            mainHandler.post(() -> callback.onSuccess(new AuthSession("", "", "", "")));
        }
    }

    public void saveSession(String accessToken, String refreshToken, String userId, String email, String name) {
        SharedPreferences.Editor editor = prefs.edit();
        if (accessToken != null) editor.putString(KEY_ACCESS_TOKEN, accessToken);
        if (refreshToken != null) editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        if (userId != null) editor.putString(KEY_USER_ID, userId);
        if (email != null) editor.putString(KEY_USER_EMAIL, email);
        if (name != null && !name.isEmpty()) editor.putString(KEY_USER_NAME, name);
        editor.putLong(KEY_TOKEN_EXPIRES_AT, System.currentTimeMillis() + (3600 * 1000));
        editor.apply();
    }

    private String extractErrorMessage(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (obj.has("error_description")) return obj.get("error_description").getAsString();
            if (obj.has("message")) return obj.get("message").getAsString();
            if (obj.has("msg")) return obj.get("msg").getAsString();
            if (obj.has("error")) return obj.get("error").getAsString();
        } catch (Exception ignored) {}
        return json;
    }

    public static String translateError(String rawError) {
        if (rawError == null) return "অপ্রত্যাশিত ত্রুটি ঘটেছে।";
        String lower = rawError.toLowerCase();
        if (lower.contains("invalid login credentials") || lower.contains("invalid credentials") || lower.contains("invalid_grant")) {
            return "ইমেইল অথবা পাসওয়ার্ড সঠিক নয়। দয়া করে আবার চেষ্টা করুন।";
        }
        if (lower.contains("user already registered") || lower.contains("email address already in use")) {
            return "এই ইমেইল দিয়ে ইতিমধ্যে একটি অ্যাকাউন্ট তৈরি করা আছে। অনুগ্রহ করে লগইন করুন।";
        }
        if (lower.contains("password should be at least")) {
            return "পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে।";
        }
        if (lower.contains("unable to validate email") || lower.contains("invalid email")) {
            return "সঠিক ইমেইল এড্রেস প্রবেশ করান।";
        }
        if (lower.contains("rate limit") || lower.contains("too many requests")) {
            return "খুব বেশি চেষ্টা করা হয়েছে। কিছুক্ষণ পর আবার চেষ্টা করুন।";
        }
        if (lower.contains("network") || lower.contains("timeout") || lower.contains("connect")) {
            return "ইন্টারনেট সংযোগে সমস্যা হচ্ছে। আপনার ডাটা বা ওয়াইফাই চেক করুন।";
        }
        return rawError;
    }
}
