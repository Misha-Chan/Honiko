package com.mishachan.honiko.data;

/** A video plus the URL/position needed to display and play it. */
public final class CardItem {

    public final Video video;
    public final double position;
    public final String thumbUrl;
    public final String streamUrl;

    public CardItem(Video video, double position, String thumbUrl, String streamUrl) {
        this.video = video;
        this.position = position;
        this.thumbUrl = thumbUrl;
        this.streamUrl = streamUrl;
    }
}
