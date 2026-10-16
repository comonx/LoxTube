package com.newtube.mobile.ui.browse;

import android.app.Activity;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import com.liskovsoft.smartyoutubetv2.tv.R;
import com.newtube.mobile.ui.common.CountryDetector;

/**
 * NEWTUBE(you-subscreen): Browse's top bar. On a bottom-nav tab it is the "LoxTube" wordmark
 * with a regional country code badge (e.g. "BD"); a section opened from a You row (Sports, Playlists, ...)
 * is a sub-screen of You, so it gets a back arrow and the section's name - like YouTube's You sub-pages.
 */
final class BrowseTopBar {
    private static final float WORDMARK_SP = 22f;
    private static final float SUBSCREEN_TITLE_SP = 20f;

    private final TextView mTitle;
    private final View mBack;
    private final TextView mCountryBadge;
    private final int mWordmarkInset;
    private final int mBesideBackInset;

    BrowseTopBar(Activity activity, Runnable onBack) {
        mTitle = activity.findViewById(R.id.mobile_title_bar);
        mBack = activity.findViewById(R.id.mobile_title_back);
        mCountryBadge = activity.findViewById(R.id.mobile_title_country_badge);
        mBack.setOnClickListener(v -> onBack.run());
        mWordmarkInset = mTitle.getPaddingStart();
        mBesideBackInset = activity.getResources()
                .getDimensionPixelSize(R.dimen.mobile_dialog_title_inset_with_back);
    }

    /**
     * A tab shows the wordmark and regional country badge; a sub-screen shows the back arrow
     * and {@code title} (the wordmark text if the section came without one).
     */
    void show(boolean subScreen, CharSequence title) {
        mBack.setVisibility(subScreen ? View.VISIBLE : View.GONE);
        if (subScreen && title != null && title.length() > 0) {
            mTitle.setText(title);
            mTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null);
            if (mCountryBadge != null) {
                mCountryBadge.setVisibility(View.GONE);
            }
        } else {
            mTitle.setText(mTitle.getContext().getString(R.string.mobile_brand_title));
            android.graphics.drawable.Drawable logo = androidx.core.content.ContextCompat.getDrawable(mTitle.getContext(), R.drawable.ic_loxtube_logo);
            if (logo != null) {
                int size = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 30, mTitle.getResources().getDisplayMetrics());
                logo.setBounds(0, 0, size, size);
                mTitle.setCompoundDrawablesRelative(logo, null, null, null);
            } else {
                mTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_loxtube_logo, 0, 0, 0);
            }
            mTitle.setCompoundDrawablePadding((int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 1, mTitle.getResources().getDisplayMetrics()));
            if (mCountryBadge != null) {
                mCountryBadge.setText(CountryDetector.getCountryCode(mTitle.getContext()));
                mCountryBadge.setVisibility(View.VISIBLE);
            }
        }
        mTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, subScreen ? SUBSCREEN_TITLE_SP : WORDMARK_SP);
        mTitle.setPaddingRelative(subScreen ? mBesideBackInset : mWordmarkInset,
                mTitle.getPaddingTop(), mTitle.getPaddingEnd(), mTitle.getPaddingBottom());
    }
}
