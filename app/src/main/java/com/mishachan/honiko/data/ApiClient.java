package com.mishachan.honiko.data;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** Tiny OkHttp based client for the local video server. */
public final class ApiClient {

    public interface Callback<T> {
        void onSuccess(T value);

        void onFailure(Exception error);
    }

    public static final class ApiException extends IOException {
        public final int code;

        public ApiException(int code, String message) {
            super(message);
            this.code = code;
        }
    }

    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build();

    private static final ExecutorService IO = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private ApiClient() {
    }

    public static String absolute(Context context, String path) {
        return absolute(Prefs.serverUrl(context), Prefs.token(context), path);
    }

    public static String absolute(String base, String token, String path) {
        String url = path == null ? "" : path;
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = base + (url.startsWith("/") ? "" : "/") + url;
        }
        if (token != null && !token.isEmpty()) {
            url += (url.contains("?") ? "&" : "?") + "token=" + Uri.encode(token);
        }
        return url;
    }

    public static void loadLibrary(Context context, Callback<Library> callback) {
        Context app = context.getApplicationContext();
        final String base = Prefs.serverUrl(app);
        final String token = Prefs.token(app);

        IO.execute(() -> {
            try {
                JSONObject health = getJson(absolute(base, token, "/api/health"));
                JSONObject videos = getJson(absolute(base, token, "/api/videos?limit=5000&sort=series"));
                JSONObject folders = getJson(absolute(base, token, "/api/folders"));
                JSONObject progress = getJson(absolute(base, token, "/api/progress?limit=50"));
                Library library = Library.parse(health, videos, folders, progress);
                MAIN.post(() -> callback.onSuccess(library));
            } catch (Exception error) {
                MAIN.post(() -> callback.onFailure(error));
            }
        });
    }

    public static void testServer(String baseUrl, String token, Callback<JSONObject> callback) {
        final String base = Prefs.normalize(baseUrl);
        final String cleanToken = token == null ? "" : token.trim();

        IO.execute(() -> {
            try {
                JSONObject health = getJson(absolute(base, cleanToken, "/api/health"));
                MAIN.post(() -> callback.onSuccess(health));
            } catch (Exception error) {
                MAIN.post(() -> callback.onFailure(error));
            }
        });
    }

    /** Fire-and-forget progress save. The server ignores positions under 5 seconds. */
    public static void saveProgress(Context context, String videoId, double position, double duration) {
        if (videoId == null || videoId.isEmpty()) {
            return;
        }
        Context app = context.getApplicationContext();
        final String url = absolute(Prefs.serverUrl(app), Prefs.token(app),
                "/api/progress/" + videoId);

        IO.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("position", Math.max(0d, position));
                payload.put("duration", Math.max(0d, duration));
                RequestBody body = RequestBody.create(payload.toString(), JSON_TYPE);
                Request request = new Request.Builder().url(url).post(body).build();
                try (Response response = HTTP.newCall(request).execute()) {
                    // Result deliberately ignored: next save tick will retry.
                }
            } catch (Exception ignored) {
            }
        });
    }

    private static JSONObject getJson(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "Honiko/1.0 (Android TV)")
                .build();

        try (Response response = HTTP.newCall(request).execute()) {
            int code = response.code();
            if (code < 200 || code >= 300) {
                throw new ApiException(code, "HTTP " + code);
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("empty response");
            }
            return new JSONObject(body.string());
        } catch (org.json.JSONException error) {
            throw new IOException("invalid JSON response", error);
        }
    }
}
