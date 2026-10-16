package com.newtube.mobile.ui.browse;

import com.liskovsoft.mediaserviceinterfaces.data.SearchOptions;

import java.util.Objects;

public class CategoryChip {
    public static final int TYPE_ALL = 0;
    public static final int TYPE_SEARCH = 1;
    public static final int TYPE_LIVE = 2;
    public static final int TYPE_WATCHED = 3;
    public static final int TYPE_NEWS = 4;
    public static final int TYPE_GAMING = 5;
    public static final int TYPE_MUSIC = 6;

    private final String mId;
    private final String mTitle;
    private final String mSearchQuery;
    private final int mType;
    private final int mSearchOptions;

    public CategoryChip(String id, String title, int type, String searchQuery, int searchOptions) {
        mId = id;
        mTitle = title;
        mType = type;
        mSearchQuery = searchQuery;
        mSearchOptions = searchOptions;
    }

    public static CategoryChip createAll() {
        return new CategoryChip("all", "All", TYPE_ALL, null, 0);
    }

    public static CategoryChip createNews() {
        return new CategoryChip("news", "News", TYPE_NEWS, null, 0);
    }

    public static CategoryChip createGaming() {
        return new CategoryChip("gaming", "Gaming", TYPE_GAMING, null, 0);
    }

    public static CategoryChip createMusic() {
        return new CategoryChip("mixes", "Music", TYPE_MUSIC, null, 0);
    }

    public static CategoryChip createWatched() {
        return new CategoryChip("watched", "Watched", TYPE_WATCHED, null, 0);
    }

    public static CategoryChip createLive() {
        return new CategoryChip("live", "Live", TYPE_LIVE, null, 0);
    }

    public static CategoryChip createSearch(String id, String title, String searchQuery) {
        // Enforce SORT_BY_UPLOAD_DATE alone (maps cleanly to CAI%3D in SearchFilterHelper)
        return new CategoryChip(id, title, TYPE_SEARCH, searchQuery,
                SearchOptions.SORT_BY_UPLOAD_DATE);
    }

    public static CategoryChip createAction(String id, String title) {
        return new CategoryChip(id, title, -1, null, 0);
    }

    public String getId() {
        return mId;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getSearchQuery() {
        return mSearchQuery;
    }

    public int getType() {
        return mType;
    }

    public int getSearchOptions() {
        return mSearchOptions;
    }

    public boolean isAll() {
        return mType == TYPE_ALL;
    }

    public boolean isAction() {
        return mType == -1;
    }

    public boolean isNews() {
        return mType == TYPE_NEWS;
    }

    public boolean isGaming() {
        return mType == TYPE_GAMING;
    }

    public boolean isMusic() {
        return mType == TYPE_MUSIC;
    }

    public boolean isWatched() {
        return mType == TYPE_WATCHED;
    }

    public boolean isLive() {
        return mType == TYPE_LIVE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CategoryChip that = (CategoryChip) o;
        return Objects.equals(mId, that.mId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mId);
    }
}
