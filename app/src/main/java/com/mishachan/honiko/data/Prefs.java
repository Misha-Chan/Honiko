package com.mishachan.honiko.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

/** Small wrapper around SharedPreferences for the server address and optional API token. */
public final class Prefs {

    public static final String DEFAULT_SERVER = "http://192.168.1.8:8090";

    private static final String FILE = "honiko";
    private static final String KEY_SERVER = "server_url";
    private static final String KEY_TOKEN = "api_token";

    private Prefs() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static String serverUrl(Context context) {
        return normalize(prefs(context).getString(KEY_SERVER, DEFAULT_SERVER));
    }

    public static String token(Context context) {
        String value = prefs(context).getString(KEY_TOKEN, "");
        return value == null ? "" : value.trim();
    }

    public static void save(Context context, String server, String token) {
        prefs(context).edit()
                .putString(KEY_SERVER, normalize(server))
                .putString(KEY_TOKEN, token == null ? "" : token.trim())
                .apply();
    }

    /** Accepts "192.168.1.8:8090" or a full URL and returns a clean base URL without a trailing slash. */
    public static String normalize(String input) {
        String value = input == null ? "" : input.trim();
        if (value.isEmpty()) {
            value = DEFAULT_SERVER;
        }
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            value = "http://" + value;
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    public static String hostLabel(Context context) {
        String url = serverUrl(context);
        try {
            String authority = Uri.parse(url).getAuthority();
            return authority == null ? url : authority;
        } catch (Exception ignored) {
            return url;
        }
    }
}
