package com.newtube.mobile.ui.browse;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Manages category chips for Home feed and adapts their order based on
 * user viewing habits and clicks.
 */
public class CategoryHabitManager {
    private static final String PREFS_NAME = "loxtube_category_habits";
    private static final String KEY_SCORE_PREFIX = "score_";

    private final SharedPreferences mPrefs;
    private static CategoryHabitManager sInstance;

    private CategoryHabitManager(Context context) {
        mPrefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized CategoryHabitManager instance(Context context) {
        if (sInstance == null) {
            sInstance = new CategoryHabitManager(context);
        }
        return sInstance;
    }

    public int getScore(String categoryId) {
        return mPrefs.getInt(KEY_SCORE_PREFIX + categoryId, 0);
    }

    public void recordChipClick(String categoryId) {
        if (categoryId == null || categoryId.equals("all")) return;
        int current = getScore(categoryId);
        mPrefs.edit().putInt(KEY_SCORE_PREFIX + categoryId, current + 5).apply();
    }

    public void recordVideoWatch(String videoTitle, String author) {
        if (videoTitle == null) return;
        String lower = videoTitle.toLowerCase(Locale.ROOT);
        String lowerAuthor = author != null ? author.toLowerCase(Locale.ROOT) : "";

        if (lower.contains("news") || lower.contains("সংবাদ") || lower.contains("খবর") || lower.contains("bulletin") || lowerAuthor.contains("news") || lowerAuthor.contains("সময়") || lowerAuthor.contains("যমুনা")) {
            increment("news", 3);
        }
        if (lower.contains("natok") || lower.contains("drama") || lower.contains("নাটক") || lower.contains("serial")) {
            increment("natok", 2);
            if (lower.contains("star jalsha") || lower.contains("zee bangla") || lowerAuthor.contains("zee") || lowerAuthor.contains("star")) {
                increment("indian_soap_operas", 2);
            }
        }
        if (lower.contains("game") || lower.contains("gaming") || lower.contains("gameplay") || lower.contains("stream")) {
            increment("gaming", 2);
            if (lower.contains("mobile") || lower.contains("pubg") || lower.contains("free fire")) {
                increment("mobile_games", 2);
            }
        }
        if (lower.contains("dub") || lower.contains("dubbed") || lower.contains("dubbing")) {
            increment("dubbing", 2);
        }
        if (lower.contains("live") || lower.contains("stream") || lower.contains("সরাসরি")) {
            increment("live", 2);
        }
        if (lower.contains("horror") || lower.contains("comedy") || lower.contains("funny") || lower.contains("ভুত")) {
            increment("horror_comedies", 2);
        }
        if (lower.contains("mix") || lower.contains("song") || lower.contains("remix") || lower.contains("mashup") || lower.contains("গান")) {
            increment("mixes", 2);
        }
        if (lower.contains("phone") || lower.contains("smartphone") || lower.contains("mobile") || lower.contains("unboxing")) {
            increment("feature_phones", 2);
            increment("consumer_electronics", 2);
        }
        if (lower.contains("animation") || lower.contains("animated") || lower.contains("cartoon") || lower.contains("anime") || lower.contains("কার্টুন")) {
            increment("animated_films", 2);
        }
        if (lower.contains("tech") || lower.contains("gadget") || lower.contains("electronics") || lower.contains("laptop")) {
            increment("consumer_electronics", 2);
        }
        if (lower.contains("new") || lower.contains("latest") || lower.contains("recent") || lower.contains("আজকের") || lower.contains("নতুন")) {
            increment("recently_uploaded", 2);
        }
    }

    private void increment(String categoryId, int weight) {
        int current = getScore(categoryId);
        mPrefs.edit().putInt(KEY_SCORE_PREFIX + categoryId, current + weight).apply();
    }

    private static final String KEY_CUSTOM_TABS = "custom_category_tabs_v2";

    public synchronized List<CategoryChip> getCustomTabs() {
        String json = mPrefs.getString(KEY_CUSTOM_TABS, null);
        List<CategoryChip> list = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) {
            return list;
        }
        try {
            org.json.JSONArray array = new org.json.JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                org.json.JSONObject obj = array.getJSONObject(i);
                String id = obj.optString("id");
                String title = obj.optString("title");
                String query = obj.optString("query");
                if (id != null && !id.isEmpty() && title != null && !title.isEmpty()) {
                    list.add(CategoryChip.createSearch(id, title, query != null && !query.isEmpty() ? query : title));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public synchronized void saveCustomTabs(List<CategoryChip> list) {
        org.json.JSONArray array = new org.json.JSONArray();
        if (list != null) {
            for (CategoryChip chip : list) {
                if (chip == null || chip.isAll() || chip.isAction()) continue;
                try {
                    org.json.JSONObject obj = new org.json.JSONObject();
                    obj.put("id", chip.getId());
                    obj.put("title", chip.getTitle());
                    obj.put("query", chip.getSearchQuery());
                    array.put(obj);
                } catch (Exception ignored) {
                }
            }
        }
        mPrefs.edit().putString(KEY_CUSTOM_TABS, array.toString()).apply();
    }

    public synchronized void addCustomTab(String title, String query) {
        if (title == null || title.trim().isEmpty()) return;
        String id = "custom_" + System.currentTimeMillis();
        String q = (query != null && !query.trim().isEmpty()) ? query.trim() : title.trim();
        List<CategoryChip> list = getCustomTabs();
        list.add(CategoryChip.createSearch(id, title.trim(), q));
        saveCustomTabs(list);
    }

    public synchronized void updateCustomTab(String id, String newTitle, String newQuery) {
        if (id == null || newTitle == null || newTitle.trim().isEmpty()) return;
        String q = (newQuery != null && !newQuery.trim().isEmpty()) ? newQuery.trim() : newTitle.trim();
        List<CategoryChip> list = getCustomTabs();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(id)) {
                list.set(i, CategoryChip.createSearch(id, newTitle.trim(), q));
                break;
            }
        }
        saveCustomTabs(list);
    }

    public synchronized void deleteCustomTab(String id) {
        if (id == null) return;
        List<CategoryChip> list = getCustomTabs();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(id)) {
                list.remove(i);
                break;
            }
        }
        saveCustomTabs(list);
    }

    /**
     * Returns the ordered list of CategoryChips:
     * "All" is always first at index 0.
     * Then user-created custom tabs.
     * "+ ট্যাব যোগ করুন" action chip at the end.
     */
    public List<CategoryChip> getOrderedChips() {
        List<CategoryChip> result = new ArrayList<>();
        result.add(CategoryChip.createAll());
        result.addAll(getCustomTabs());
        result.add(CategoryChip.createAction("manage_categories", "+ ট্যাব যোগ করুন"));
        return result;
    }

    public static List<CategoryChip> getDefaultCategories() {
        List<CategoryChip> list = new ArrayList<>();
        list.add(CategoryChip.createAll());
        return list;
    }
}
