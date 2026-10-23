package com.mishachan.honiko.data;

import org.json.JSONObject;

/** A saved playback position from /api/progress. */
public final class ProgressEntry {

    public String videoId = "";
    public double position;
    public double duration;
    public boolean completed;
    public long updatedAt;
    public Video video;

    public static ProgressEntry from(JSONObject json) {
        ProgressEntry entry = new ProgressEntry();
        entry.videoId = json.optString("videoId", "");
        entry.position = json.optDouble("position", 0d);
        entry.duration = json.optDouble("duration", 0d);
        entry.completed = json.optBoolean("completed", false);
        entry.updatedAt = json.optLong("updatedAt", 0L);
        JSONObject nested = json.optJSONObject("video");
        if (nested != null) {
            entry.video = Video.from(nested);
        }
        return entry;
    }
}
