package com.mishachan.honiko.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Parsed result of the four endpoints the home screen needs. */
public final class Library {

    public final List<Video> videos = new ArrayList<>();
    public final List<FolderInfo> folders = new ArrayList<>();
    public final List<ProgressEntry> progress = new ArrayList<>();
    public final Map<String, ProgressEntry> progressById = new HashMap<>();

    public int videoCount;
    public String version = "";
    public boolean authRequired;

    public static Library parse(JSONObject health,
                                JSONObject videosJson,
                                JSONObject foldersJson,
                                JSONObject progressJson) {
        Library library = new Library();

        if (health != null) {
            library.version = health.optString("version", "");
            library.authRequired = health.optBoolean("authRequired", false);
        }

        JSONArray videos = videosJson == null ? null : videosJson.optJSONArray("items");
        if (videos != null) {
            for (int i = 0; i < videos.length(); i++) {
                JSONObject item = videos.optJSONObject(i);
                if (item != null) {
                    library.videos.add(Video.from(item));
                }
            }
        }

        JSONArray folders = foldersJson == null ? null : foldersJson.optJSONArray("items");
        if (folders != null) {
            for (int i = 0; i < folders.length(); i++) {
                JSONObject item = folders.optJSONObject(i);
                if (item != null) {
                    library.folders.add(FolderInfo.from(item));
                }
            }
        }

        JSONArray progress = progressJson == null ? null : progressJson.optJSONArray("items");
        if (progress != null) {
            for (int i = 0; i < progress.length(); i++) {
                JSONObject item = progress.optJSONObject(i);
                if (item == null) {
                    continue;
                }
                ProgressEntry entry = ProgressEntry.from(item);
                if (entry.video != null) {
                    library.progress.add(entry);
                    library.progressById.put(entry.video.id, entry);
                }
            }
        }

        library.fillMissingFolders();
        library.videoCount = health != null && health.has("videos")
                ? health.optInt("videos", library.videos.size())
                : library.videos.size();
        if (library.videoCount <= 0) {
            library.videoCount = library.videos.size();
        }
        return library;
    }

    /** Defensive: make sure every folder that appears in the video list has a row. */
    private void fillMissingFolders() {
        Set<String> known = new HashSet<>();
        for (FolderInfo info : folders) {
            known.add(info.folder);
        }
        for (Video video : videos) {
            String key = video.folder == null ? "" : video.folder;
            if (known.add(key)) {
                FolderInfo info = new FolderInfo();
                info.folder = key;
                info.name = video.series == null ? "" : video.series;
                folders.add(info);
            }
        }
    }

    public boolean isEmpty() {
        return videos.isEmpty();
    }
}
