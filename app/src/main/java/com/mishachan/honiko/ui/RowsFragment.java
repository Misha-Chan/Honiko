package com.mishachan.honiko.ui;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.app.RowsSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;

import com.mishachan.honiko.PlayerActivity;
import com.mishachan.honiko.R;
import com.mishachan.honiko.data.ApiClient;
import com.mishachan.honiko.data.CardItem;
import com.mishachan.honiko.data.FolderInfo;
import com.mishachan.honiko.data.Library;
import com.mishachan.honiko.data.Prefs;
import com.mishachan.honiko.data.ProgressEntry;
import com.mishachan.honiko.data.Video;

/** Leanback rows fragment: continue-watching plus one row per folder. */
public class RowsFragment extends RowsSupportFragment {

    private VideoCardPresenter cardPresenter;
    private ArrayObjectAdapter rowsAdapter;
    private boolean empty = true;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Context context = requireContext();
        cardPresenter = new VideoCardPresenter(context);

        ListRowPresenter rowPresenter = new ListRowPresenter();
        rowPresenter.setSelectEffectEnabled(false);
        rowPresenter.setShadowEnabled(false);

        rowsAdapter = new ArrayObjectAdapter(rowPresenter);
        setAdapter(rowsAdapter);

        setOnItemViewClickedListener((itemViewHolder, item, rowViewHolder, row) -> {
            if (item instanceof CardItem && getActivity() != null) {
                PlayerActivity.start(getActivity(), (CardItem) item);
            }
        });
    }

    public boolean isEmpty() {
        return empty;
    }

    public void show(@NonNull Library library) {
        if (rowsAdapter == null) {
            return;
        }

        Context context = requireContext();
        String base = Prefs.serverUrl(context);
        String token = Prefs.token(context);
        rowsAdapter.clear();

        ArrayObjectAdapter resumeAdapter = new ArrayObjectAdapter(cardPresenter);
        for (ProgressEntry entry : library.progress) {
            if (entry.video != null) {
                resumeAdapter.add(card(entry.video, entry.position, base, token));
            }
        }
        if (resumeAdapter.size() > 0) {
            rowsAdapter.add(new ListRow(
                    new HeaderItem(getString(R.string.continue_watching)), resumeAdapter));
        }

        for (FolderInfo folder : library.folders) {
            ArrayObjectAdapter folderAdapter = new ArrayObjectAdapter(cardPresenter);
            for (Video video : library.videos) {
                if (!folder.folder.equals(video.folder)) {
                    continue;
                }
                ProgressEntry entry = library.progressById.get(video.id);
                double position = entry != null && !entry.completed ? entry.position : 0d;
                folderAdapter.add(card(video, position, base, token));
            }
            if (folderAdapter.size() > 0) {
                rowsAdapter.add(new ListRow(new HeaderItem(headerFor(folder)), folderAdapter));
            }
        }

        empty = rowsAdapter.size() == 0;
    }

    private String headerFor(FolderInfo folder) {
        if (folder.folder == null || folder.folder.isEmpty()) {
            return getString(R.string.singles);
        }
        String label = Ui.shortLabel(folder.name);
        if (label == null) {
            label = Ui.shortLabel(folder.folder);
        }
        return label == null ? getString(R.string.video) : label;
    }

    private CardItem card(Video video, double position, String base, String token) {
        String thumb = ApiClient.absolute(base, token, video.thumbUrl + "?w=480");
        String stream = ApiClient.absolute(base, token, video.streamUrl);
        return new CardItem(video, position, thumb, stream);
    }
}
