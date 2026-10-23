package com.mishachan.honiko.data;

import org.json.JSONObject;

/** One video returned by the local server. */
public final class Video {

    public String id = "";
    public String title = "";
    public String series;
    public String folder = "";
    public String filename = "";
    public String mimeType = "";
    public String videoCodec;
    public String audioCodec;
    public String streamUrl = "";
    public String thumbUrl = "";
    public Integer episode;
    public Integer width;
    public Integer height;
    public long size;
    public long addedAt;
    public double duration;
    public boolean probed;

    public static Video from(JSONObject json) {
        Video video = new Video();
        video.id = string(json, "id", "");
        video.title = string(json, "title", "");
        video.series = string(json, "series", null);
        video.folder = string(json, "folder", "");
        video.filename = string(json, "filename", "");
        video.mimeType = string(json, "mimeType", "");
        video.videoCodec = string(json, "videoCodec", null);
        video.audioCodec = string(json, "audioCodec", null);
        video.streamUrl = string(json, "streamUrl", "");
        video.thumbUrl = string(json, "thumbUrl", "");
        video.episode = integer(json, "episode");
        video.width = integer(json, "width");
        video.height = integer(json, "height");
        video.size = json.optLong("size", 0L);
        video.addedAt = json.optLong("addedAt", 0L);
        video.duration = json.optDouble("duration", 0d);
        video.probed = json.optBoolean("probed", false);
        return video;
    }

    private static String string(JSONObject json, String key, String fallback) {
        if (!json.has(key) || json.isNull(key)) {
            return fallback;
        }
        return json.optString(key, fallback);
    }

    private static Integer integer(JSONObject json, String key) {
        if (!json.has(key) || json.isNull(key)) {
            return null;
        }
        return json.optInt(key, 0);
    }
}
