package com.newtube.mobile.sponsored;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.liskovsoft.mediaserviceinterfaces.data.MediaItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages remotely configured sponsored / featured videos and promotional notices
 * fetched from GitHub (adslist.json).
 *
 * Injects sponsored videos seamlessly into the Home feed at configurable intervals
 * so they appear and behave like native videos without intrusive interruption.
 */
public final class SponsoredContentManager {
    private static final String TAG = "SponsoredContent";
    private static final String PREFS_NAME = "loxtube_sponsored_config";
    private static final String KEY_CACHED_JSON = "cached_adslist_json";
    private static final String KEY_LAST_FETCH = "last_fetch_timestamp";

    /** Default GitHub hosted adslist.json URL (raw). Can be overridden. */
    public static final String DEFAULT_CONFIG_URL =
            "https://raw.githubusercontent.com/comonx/loxtube-config/main/adslist.json";

    private static final long FETCH_COOLDOWN_MS = 15 * 60 * 1000L; // 15 minutes

    private static volatile SponsoredContentManager sInstance;
    private final Context mContext;
    private final SharedPreferences mPrefs;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    private boolean mEnabled = true;
    private int mRepeatInterval = 7;
    private int mInitialOffset = 3;
    private final List<Video> mSponsoredVideos = new ArrayList<>();
    private NoticeInfo mCurrentNotice;

    public static class NoticeInfo {
        public final String id;
        public final String title;
        public final String message;
        public final String actionUrl;

        public NoticeInfo(String id, String title, String message, String actionUrl) {
            this.id = id;
            this.title = title;
            this.message = message;
            this.actionUrl = actionUrl;
        }
    }

    public static SponsoredContentManager instance(@NonNull Context context) {
        if (sInstance == null) {
            synchronized (SponsoredContentManager.class) {
                if (sInstance == null) {
                    sInstance = new SponsoredContentManager(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    private SponsoredContentManager(Context context) {
        mContext = context;
        mPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadFromCache();
        fetchIfStale();
    }

    private void loadFromCache() {
        String cachedJson = mPrefs.getString(KEY_CACHED_JSON, null);
        if (cachedJson != null && !cachedJson.isEmpty()) {
            try {
                parseConfig(new JSONObject(cachedJson));
            } catch (Exception e) {
                Log.w(TAG, "Failed to parse cached adslist.json", e);
            }
        }
    }

    public void fetchIfStale() {
        long last = mPrefs.getLong(KEY_LAST_FETCH, 0);
        long now = System.currentTimeMillis();
        if (now - last > FETCH_COOLDOWN_MS || mSponsoredVideos.isEmpty()) {
            fetchRemote(null);
        }
    }

    public void fetchRemote(@Nullable Runnable onComplete) {
        mExecutor.execute(() -> {
            try {
                URL url = new URL(DEFAULT_CONFIG_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);
                conn.setRequestProperty("User-Agent", "LoxTube/1.15.0");
                conn.setInstanceFollowRedirects(true);

                int code = conn.getResponseCode();
                if (code == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append('\n');
                    }
                    reader.close();

                    String jsonStr = sb.toString();
                    JSONObject json = new JSONObject(jsonStr);

                    mPrefs.edit()
                            .putString(KEY_CACHED_JSON, jsonStr)
                            .putLong(KEY_LAST_FETCH, System.currentTimeMillis())
                            .apply();

                    mMainHandler.post(() -> {
                        parseConfig(json);
                        if (onComplete != null) onComplete.run();
                    });
                } else {
                    if (onComplete != null) mMainHandler.post(onComplete);
                }
                conn.disconnect();
            } catch (Exception e) {
                Log.d(TAG, "Remote config fetch error (normal if offline / repo not yet created): " + e.getMessage());
                if (onComplete != null) mMainHandler.post(onComplete);
            }
        });
    }

    private synchronized void parseConfig(JSONObject root) {
        try {
            mEnabled = root.optBoolean("enabled", true);
            mRepeatInterval = Math.max(3, root.optInt("repeat_interval", 7));
            mInitialOffset = Math.max(1, root.optInt("initial_offset", 3));

            mSponsoredVideos.clear();
            JSONArray videosArray = root.optJSONArray("sponsored_videos");
            if (videosArray != null) {
                for (int i = 0; i < videosArray.length(); i++) {
                    JSONObject obj = videosArray.optJSONObject(i);
                    if (obj == null) continue;
                    Video v = parseVideo(obj);
                    if (v != null) {
                        mSponsoredVideos.add(v);
                    }
                }
            }

            JSONObject noticeObj = root.optJSONObject("notice");
            if (noticeObj != null) {
                String id = noticeObj.optString("id", "1");
                String title = noticeObj.optString("title", "");
                String message = noticeObj.optString("message", "");
                String actionUrl = noticeObj.optString("action_url", "");
                mCurrentNotice = new NoticeInfo(id, title, message, actionUrl);
            } else {
                mCurrentNotice = null;
            }
        } catch (Exception e) {
            Log.w(TAG, "Error parsing adslist.json structure", e);
        }
    }

    private Video parseVideo(JSONObject obj) {
        String videoId = obj.optString("videoId", null);
        if (videoId == null || videoId.isEmpty()) {
            return null;
        }

        Video v = new Video();
        v.videoId = videoId;
        v.title = obj.optString("title", "Sponsored Video");
        v.author = obj.optString("author", "Featured");
        v.channelId = obj.optString("channelId", null);
        v.cardImageUrl = obj.optString("cardImageUrl", "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg");
        v.bgImageUrl = v.cardImageUrl;
        v.channelIconUrl = obj.optString("channelIconUrl", null);
        v.badge = obj.optString("badge", "Featured");
        v.secondTitle = obj.optString("secondTitle", v.author + " · Promoted");
        v.itemType = MediaItem.TYPE_VIDEO;
        v.id = videoId.hashCode();
        return v;
    }

    /**
     * Injects sponsored videos seamlessly into the feed list.
     *
     * @param context Application context
     * @param originalList Current list of videos
     * @param isHomeFeed True if currently displaying the Home feed
     * @return New list with sponsored content safely interspersed, or originalList if disabled
     */
    public static List<Video> inject(@NonNull Context context, @Nullable List<Video> originalList, boolean isHomeFeed) {
        if (originalList == null || originalList.isEmpty()) {
            return originalList == null ? Collections.emptyList() : originalList;
        }

        SponsoredContentManager mgr = instance(context);
        if (!isHomeFeed || !mgr.mEnabled || mgr.mSponsoredVideos.isEmpty()) {
            return originalList;
        }

        List<Video> sponsored = mgr.mSponsoredVideos;
        int sponsoredCount = sponsored.size();
        List<Video> result = new ArrayList<>(originalList.size() + sponsoredCount);

        int sponsorIdx = 0;
        int nextInjectPos = mgr.mInitialOffset;

        for (int i = 0; i < originalList.size(); i++) {
            if (i == nextInjectPos && sponsorIdx < sponsoredCount) {
                Video toInject = sponsored.get(sponsorIdx % sponsoredCount);
                // Prevent duplicate if already in feed
                if (!containsVideo(originalList, toInject.videoId)) {
                    result.add(toInject);
                    sponsorIdx++;
                    nextInjectPos += mgr.mRepeatInterval;
                }
            }
            result.add(originalList.get(i));
        }

        return result;
    }

    private static boolean containsVideo(List<Video> list, String videoId) {
        if (videoId == null) return false;
        for (Video v : list) {
            if (videoId.equals(v.videoId)) return true;
        }
        return false;
    }

    public NoticeInfo getCurrentNotice() {
        return mCurrentNotice;
    }

    public boolean isNoticeDismissed(String noticeId) {
        return mPrefs.getBoolean("dismissed_notice_" + noticeId, false);
    }

    public void dismissNotice(String noticeId) {
        mPrefs.edit().putBoolean("dismissed_notice_" + noticeId, true).apply();
    }
}
