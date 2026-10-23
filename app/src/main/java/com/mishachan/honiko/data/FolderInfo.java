package com.mishachan.honiko.data;

import org.json.JSONObject;

/** A folder / series from /api/folders. */
public final class FolderInfo {

    public String folder = "";
    public String name = "";
    public int count;
    public long size;
    public double duration;

    public static FolderInfo from(JSONObject json) {
        FolderInfo info = new FolderInfo();
        info.folder = json.optString("folder", "");
        info.name = json.optString("name", "");
        info.count = json.optInt("count", 0);
        info.size = json.optLong("size", 0L);
        info.duration = json.optDouble("duration", 0d);
        return info;
    }
}
