package com.mishachan.honiko.ui;

import java.util.Locale;

public final class Ui {

    private Ui() {
    }

    public static String time(double seconds) {
        if (seconds < 0 || Double.isNaN(seconds) || Double.isInfinite(seconds)) {
            seconds = 0;
        }
        long total = Math.round(seconds);
        long hours = total / 3600;
        long minutes = (total % 3600) / 60;
        long secs = total % 60;
        if (hours > 0) {
            return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, secs);
        }
        return String.format(Locale.US, "%d:%02d", minutes, secs);
    }

    /** Trims the long download-folder names to something readable in a row header. */
    public static String shortLabel(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        String[] separators = {"｜", " | ", " - "};
        for (String separator : separators) {
            int index = value.indexOf(separator);
            if (index >= 3) {
                value = value.substring(0, index).trim();
                break;
            }
        }
        if (value.length() > 46) {
            value = value.substring(0, 45).trim() + "…";
        }
        return value;
    }
}
