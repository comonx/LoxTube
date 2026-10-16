package com.newtube.mobile.ui.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.liskovsoft.mediaserviceinterfaces.data.MediaItemMetadata;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.youtubeapi.service.YouTubeMediaItemService;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fast, two-tiered (LruCache in-memory + SharedPreferences on disk) cache and background fetcher
 * for channel avatar URLs on the mobile feed.
 */
public final class ChannelAvatarCache {
    private static final String PREF_NAME = "loxtube_channel_avatars";
    private static final int MEM_CACHE_SIZE = 500;
    private static final LruCache<String, String> sMemoryCache = new LruCache<>(MEM_CACHE_SIZE);
    private static final Set<String> sInFlight = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final ExecutorService sExecutor = Executors.newFixedThreadPool(2);
    private static final Handler sMainHandler = new Handler(Looper.getMainLooper());

    private ChannelAvatarCache() {
    }

    public interface Callback {
        void onAvatarLoaded(@NonNull String avatarUrl);
    }

    @Nullable
    public static String get(@NonNull Context context, @Nullable String channelId, @Nullable String author) {
        if (!TextUtils.isEmpty(channelId)) {
            String url = sMemoryCache.get(channelId);
            if (!TextUtils.isEmpty(url)) {
                return url;
            }
        }
        if (!TextUtils.isEmpty(author)) {
            String url = sMemoryCache.get("author_" + author);
            if (!TextUtils.isEmpty(url)) {
                return url;
            }
        }

        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (!TextUtils.isEmpty(channelId)) {
            String diskUrl = prefs.getString(channelId, null);
            if (!TextUtils.isEmpty(diskUrl)) {
                sMemoryCache.put(channelId, diskUrl);
                return diskUrl;
            }
        }
        if (!TextUtils.isEmpty(author)) {
            String diskUrl = prefs.getString("author_" + author, null);
            if (!TextUtils.isEmpty(diskUrl)) {
                sMemoryCache.put("author_" + author, diskUrl);
                return diskUrl;
            }
        }

        return null;
    }

    public static void put(@NonNull Context context, @Nullable String channelId, @Nullable String author, @Nullable String avatarUrl) {
        if (TextUtils.isEmpty(avatarUrl)) {
            return;
        }

        SharedPreferences.Editor editor = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
        if (!TextUtils.isEmpty(channelId)) {
            sMemoryCache.put(channelId, avatarUrl);
            editor.putString(channelId, avatarUrl);
        }
        if (!TextUtils.isEmpty(author)) {
            sMemoryCache.put("author_" + author, avatarUrl);
            editor.putString("author_" + author, avatarUrl);
        }
        editor.apply();
    }

    public static void fetchAvatar(@NonNull Context context, @NonNull Video video, @Nullable Callback callback) {
        String cached = get(context, video.channelId, video.getAuthor());
        if (!TextUtils.isEmpty(cached)) {
            video.setChannelIconUrl(cached);
            if (callback != null) {
                callback.onAvatarLoaded(cached);
            }
            return;
        }

        final String videoId = video.videoId;
        if (TextUtils.isEmpty(videoId)) {
            return;
        }

        if (sInFlight.contains(videoId)) {
            return;
        }
        sInFlight.add(videoId);

        final Context appContext = context.getApplicationContext();
        sExecutor.execute(() -> {
            try {
                MediaItemMetadata metadata = YouTubeMediaItemService.instance().getMetadata(videoId);
                if (metadata != null) {
                    String authorImg = metadata.getAuthorImageUrl();
                    if (!TextUtils.isEmpty(authorImg)) {
                        put(appContext, video.channelId != null ? video.channelId : metadata.getChannelId(),
                                video.getAuthor(), authorImg);
                        video.setChannelIconUrl(authorImg);

                        if (callback != null) {
                            sMainHandler.post(() -> callback.onAvatarLoaded(authorImg));
                        }
                    }
                }
            } catch (Throwable ignored) {
            } finally {
                sInFlight.remove(videoId);
            }
        });
    }
}
