package com.mishachan.honiko.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.leanback.widget.Presenter;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.mishachan.honiko.R;
import com.mishachan.honiko.data.CardItem;
import com.mishachan.honiko.data.Video;

import java.util.Locale;

/** Focusable 16:9 card used by every row. */
public class VideoCardPresenter extends Presenter {

    private final Context context;
    private final int normalTitleColor;

    public VideoCardPresenter(Context context) {
        this.context = context;
        this.normalTitleColor = context.getColor(R.color.text);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_video_card, parent, false);
        final Holder holder = new Holder(view);

        view.setFocusable(true);
        view.setFocusableInTouchMode(true);
        view.setOnFocusChangeListener((v, hasFocus) -> {
            holder.title.setTextColor(hasFocus ? Color.WHITE : normalTitleColor);
            v.animate()
                    .scaleX(hasFocus ? 1.03f : 1f)
                    .scaleY(hasFocus ? 1.03f : 1f)
                    .setDuration(130)
                    .start();
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        Holder holder = (Holder) viewHolder;
        CardItem card = (CardItem) item;
        Video video = card.video;

        holder.title.setText(video.title);
        holder.subtitle.setText(subtitle(video));
        holder.duration.setText(Ui.time(video.duration));
        holder.title.setTextColor(viewHolder.view.hasFocus() ? Color.WHITE : normalTitleColor);
        viewHolder.view.setScaleX(viewHolder.view.hasFocus() ? 1.03f : 1f);
        viewHolder.view.setScaleY(viewHolder.view.hasFocus() ? 1.03f : 1f);

        Glide.with(holder.thumb)
                .load(card.thumbUrl)
                .placeholder(R.drawable.thumb_placeholder)
                .error(R.drawable.thumb_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .override(480, 270)
                .into(holder.thumb);

        if (card.position > 15 && video.duration > 0) {
            holder.progress.setVisibility(View.VISIBLE);
            int value = (int) Math.min(1000, Math.round(1000 * card.position / video.duration));
            holder.progress.setProgress(Math.max(0, value));
        } else {
            holder.progress.setVisibility(View.GONE);
        }
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        Holder holder = (Holder) viewHolder;
        Glide.with(holder.thumb).clear(holder.thumb);
        holder.thumb.setImageDrawable(null);
    }

    private String subtitle(Video video) {
        if (video.episode != null && video.episode > 0) {
            return context.getString(R.string.episode, video.episode);
        }
        String series = Ui.shortLabel(video.series);
        if (series != null) {
            return series;
        }
        if (video.videoCodec != null && !video.videoCodec.isEmpty()) {
            String codec = video.videoCodec.toUpperCase(Locale.US);
            if (video.height != null && video.height > 0) {
                return codec + " • " + video.height + "p";
            }
            return codec;
        }
        return context.getString(R.string.video);
    }

    private static final class Holder extends Presenter.ViewHolder {
        final ImageView thumb;
        final TextView title;
        final TextView subtitle;
        final TextView duration;
        final ProgressBar progress;

        Holder(View view) {
            super(view);
            thumb = view.findViewById(R.id.thumb);
            title = view.findViewById(R.id.title);
            subtitle = view.findViewById(R.id.subtitle);
            duration = view.findViewById(R.id.duration);
            progress = view.findViewById(R.id.watch_progress);
        }
    }
}
