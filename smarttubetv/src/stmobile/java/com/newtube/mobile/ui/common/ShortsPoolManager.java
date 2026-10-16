package com.newtube.mobile.ui.common;

import android.content.Context;
import androidx.annotation.Nullable;

import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Manages a curated, verified pool of true YouTube Shorts for fast, 0-ms instant playback,
 * strict vertical short filtering (no long videos allowed), and automatic next-short transitions.
 */
public final class ShortsPoolManager {
    private static ShortsPoolManager sInstance;

    public static synchronized ShortsPoolManager instance() {
        if (sInstance == null) {
            sInstance = new ShortsPoolManager();
        }
        return sInstance;
    }

    private final List<Video> mPool = new ArrayList<>();
    private final Random mRandom = new Random();

    private ShortsPoolManager() {
    }

    public synchronized void addShorts(@Nullable List<Video> videos) {
        if (videos == null || videos.isEmpty()) {
            return;
        }
        for (Video v : videos) {
            if (isValidShort(v)) {
                v.isShorts = true;
                if (!containsVideo(v.videoId)) {
                    mPool.add(v);
                }
            }
        }
    }

    public synchronized void addShort(@Nullable Video v) {
        if (isValidShort(v)) {
            v.isShorts = true;
            if (!containsVideo(v.videoId)) {
                mPool.add(v);
            }
        }
    }

    public synchronized boolean hasShorts() {
        return !mPool.isEmpty();
    }

    @Nullable
    public synchronized Video getRandomShort() {
        if (mPool.isEmpty()) {
            restoreFromCache();
        }
        if (mPool.isEmpty()) {
            return null;
        }
        int index = mRandom.nextInt(mPool.size());
        Video chosen = mPool.get(index);
        chosen.isShorts = true;
        VideoGroup group = VideoGroup.from(new ArrayList<>(mPool));
        chosen.setGroup(group);
        return chosen;
    }

    @Nullable
    public synchronized Video getNextShort(@Nullable Video current) {
        if (mPool.isEmpty()) {
            restoreFromCache();
        }
        if (mPool.isEmpty()) {
            return current;
        }
        if (current == null || current.videoId == null) {
            return getRandomShort();
        }
        int idx = -1;
        for (int i = 0; i < mPool.size(); i++) {
            Video v = mPool.get(i);
            if (v != null && current.videoId.equals(v.videoId)) {
                idx = i;
                break;
            }
        }
        if (idx >= 0 && mPool.size() > 1) {
            int nextIdx = (idx + 1) % mPool.size();
            Video next = mPool.get(nextIdx);
            next.isShorts = true;
            VideoGroup group = VideoGroup.from(new ArrayList<>(mPool));
            next.setGroup(group);
            return next;
        }
        return getRandomShort();
    }

    @Nullable
    public synchronized Video getPreviousShort(@Nullable Video current) {
        if (mPool.isEmpty()) {
            restoreFromCache();
        }
        if (mPool.isEmpty()) {
            return current;
        }
        if (current == null || current.videoId == null) {
            return getRandomShort();
        }
        int idx = -1;
        for (int i = 0; i < mPool.size(); i++) {
            Video v = mPool.get(i);
            if (v != null && current.videoId.equals(v.videoId)) {
                idx = i;
                break;
            }
        }
        if (idx >= 0 && mPool.size() > 1) {
            int prevIdx = (idx - 1 + mPool.size()) % mPool.size();
            Video prev = mPool.get(prevIdx);
            prev.isShorts = true;
            VideoGroup group = VideoGroup.from(new ArrayList<>(mPool));
            prev.setGroup(group);
            return prev;
        }
        return getRandomShort();
    }

    public synchronized void prefetch(Context context) {
        restoreFromCache();
    }

    private synchronized void restoreFromCache() {
        List<Video> cached = FeedCache.getOrRestore(MediaGroup.TYPE_SHORTS);
        if (cached != null && !cached.isEmpty()) {
            addShorts(cached);
        }
        List<Video> homeCached = FeedCache.getOrRestore(MediaGroup.TYPE_HOME);
        if (homeCached != null && !homeCached.isEmpty()) {
            addShorts(homeCached);
        }
    }

    private synchronized boolean containsVideo(String videoId) {
        if (videoId == null) return false;
        for (Video v : mPool) {
            if (v != null && videoId.equals(v.videoId)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isValidShort(@Nullable Video v) {
        if (v == null || !v.hasVideo()) {
            return false;
        }
        if (ShortsFilter.isDefinitelyNotShort(v)) {
            return false;
        }
        return v.isShorts || ShortsFilter.isShort(v);
    }
}
