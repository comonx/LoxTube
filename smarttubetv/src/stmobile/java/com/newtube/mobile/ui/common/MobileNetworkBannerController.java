package com.newtube.mobile.ui.common;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Controls the YouTube-style floating offline/online notification banner.
 * When disconnected: displays "কোনো ইন্টারনেট সংযোগ নেই" with a "ডাউনলোডে যান" action.
 * When reconnected: displays "ইন্টারনেট সংযোগ ফিরে এসেছে" in green and auto-dismisses after 3s.
 */
public final class MobileNetworkBannerController implements NetworkStatusMonitor.Listener {
    private static final long RECONNECT_DISMISS_DELAY_MS = 3000L;

    public interface Callback {
        void onOpenDownloadsRequested();
        void onNetworkReconnected();
    }

    private final Activity mActivity;
    private final View mBannerView;
    private final ImageView mIconView;
    private final TextView mTextView;
    private final TextView mActionView;
    private final Callback mCallback;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mWasOffline = false;

    private final Runnable mDismissRunnable = this::hideBanner;

    public MobileNetworkBannerController(Activity activity, View bannerView, Callback callback) {
        mActivity = activity;
        mBannerView = bannerView;
        mCallback = callback;

        mIconView = bannerView.findViewById(R.id.mobile_network_icon);
        mTextView = bannerView.findViewById(R.id.mobile_network_text);
        mActionView = bannerView.findViewById(R.id.mobile_network_action);

        if (mActionView != null) {
            mActionView.setOnClickListener(v -> {
                if (mCallback != null) {
                    mCallback.onOpenDownloadsRequested();
                }
            });
        }
    }

    public void start() {
        NetworkStatusMonitor.instance(mActivity).addListener(this);
    }

    public void stop() {
        mHandler.removeCallbacks(mDismissRunnable);
        NetworkStatusMonitor.instance(mActivity).removeListener(this);
    }

    @Override
    public void onNetworkStatusChanged(boolean isOnline) {
        if (mActivity.isFinishing() || mActivity.isDestroyed()) {
            return;
        }

        mHandler.removeCallbacks(mDismissRunnable);

        if (!isOnline) {
            mWasOffline = true;
            showOfflineState();
        } else if (mWasOffline) {
            mWasOffline = false;
            showOnlineState();
            if (mCallback != null) {
                mCallback.onNetworkReconnected();
            }
        } else {
            // Already online on app launch, keep hidden
            hideBanner();
        }
    }

    private void showOfflineState() {
        if (mBannerView == null) return;

        mBannerView.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.bg_mobile_network_banner));
        if (mIconView != null) {
            mIconView.setImageResource(R.drawable.ic_network_offline);
            mIconView.setColorFilter(0xFFFFFFFF);
        }
        if (mTextView != null) {
            mTextView.setText("কোনো ইন্টারনেট সংযোগ নেই");
            mTextView.setTextColor(0xFFFFFFFF);
        }
        if (mActionView != null) {
            mActionView.setVisibility(View.VISIBLE);
            mActionView.setText("ডাউনলোডে যান");
        }

        mBannerView.animate().cancel();
        if (mBannerView.getVisibility() != View.VISIBLE) {
            mBannerView.setAlpha(0f);
            mBannerView.setTranslationY(30f);
            mBannerView.setVisibility(View.VISIBLE);
            mBannerView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(220)
                    .start();
        } else {
            mBannerView.setAlpha(1f);
            mBannerView.setTranslationY(0f);
        }
    }

    private void showOnlineState() {
        if (mBannerView == null) return;

        mBannerView.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.bg_mobile_network_banner_online));
        if (mIconView != null) {
            mIconView.setImageResource(R.drawable.ic_network_online);
            mIconView.clearColorFilter();
        }
        if (mTextView != null) {
            mTextView.setText("ইন্টারনেট সংযোগ ফিরে এসেছে");
            mTextView.setTextColor(0xFFE8F5E9);
        }
        if (mActionView != null) {
            mActionView.setVisibility(View.GONE);
        }

        mBannerView.animate().cancel();
        if (mBannerView.getVisibility() != View.VISIBLE) {
            mBannerView.setAlpha(0f);
            mBannerView.setTranslationY(30f);
            mBannerView.setVisibility(View.VISIBLE);
            mBannerView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(220)
                    .start();
        } else {
            mBannerView.setAlpha(1f);
            mBannerView.setTranslationY(0f);
        }

        // Auto dismiss after 3 seconds, like YouTube
        mHandler.postDelayed(mDismissRunnable, RECONNECT_DISMISS_DELAY_MS);
    }

    private void hideBanner() {
        if (mBannerView == null || mBannerView.getVisibility() != View.VISIBLE) {
            return;
        }
        mBannerView.animate().cancel();
        mBannerView.animate()
                .alpha(0f)
                .translationY(30f)
                .setDuration(250)
                .withEndAction(() -> mBannerView.setVisibility(View.GONE))
                .start();
    }
}
