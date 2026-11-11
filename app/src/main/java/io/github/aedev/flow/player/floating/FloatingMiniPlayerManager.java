package io.github.aedev.flow.player.floating;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.VideoSize;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;

import android.os.Handler;
import android.os.Looper;
import io.github.aedev.flow.MainActivity;
import io.github.aedev.flow.R;
import io.github.aedev.flow.player.EnhancedPlayerManager;
import io.github.aedev.flow.player.PictureInPictureHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.github.aedev.flow.data.model.Video;
import io.github.aedev.flow.player.GlobalPlayerState;
import java.util.List;

import java.util.Locale;

/**
 * Manages the system-wide movable/draggable floating overlay mini-player
 * ({@code R.layout.mobile_floating_mini_player}) shown outside the app or toggled
 * from the Notification Bar's PiP / Headphone action button.
 */
public final class FloatingMiniPlayerManager {
    private static final Handler sHandler = new Handler(Looper.getMainLooper());
    private static void postDelayed(Runnable r, long delayMillis) { sHandler.postDelayed(r, delayMillis); }
    private static void removeCallbacks(Runnable r) { sHandler.removeCallbacks(r); }
    private static final long TICK_MS = 500L;
    private static final long CONTROLS_TIMEOUT_MS = 3500L;
    private static final long UNLOCK_BTN_TIMEOUT_MS = 3000L;

    @SuppressLint("StaticFieldLeak")
    private static FloatingMiniPlayerManager sInstance;

    public static synchronized FloatingMiniPlayerManager instance(Context context) {
        if (sInstance == null) {
            sInstance = new FloatingMiniPlayerManager(context.getApplicationContext());
        }
        return sInstance;
    }

    public static boolean isShowing() {
        return sInstance != null && sInstance.mIsShowing;
    }

    public static boolean canDrawOverlays(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context);
    }

    private final Context mAppContext;
    private final WindowManager mWindowManager;

    private View mRootView;
    private WindowManager.LayoutParams mLayoutParams;
    private AspectRatioFrameLayout mFrame;
    private ImageView mFreeze;
    private View mScrim;
    private View mTopBar;
    private View mTopActions;
    private View mCenterControls;
    private View mBottomBar;
    private View mBottomScrim;
    private View mBrandWatermark;
    private ImageButton mLockBtn;
    private ImageButton mHeadphoneBtn;
    private ImageButton mPipExpandBtn;
    private ImageButton mFullscreenBtn;
    public static final int SIZE_SMALL = 0;   // 200dp x 112dp
    public static final int SIZE_MEDIUM = 1;  // 240dp x 135dp (Default)
    public static final int SIZE_LARGE = 2;   // 280dp x 158dp
    private int mCurrentSize = SIZE_MEDIUM;
    private boolean mAutoHideControls = true;

    private ImageButton mSettingsBtn;
    private View mSettingsPanel;
    private View mSettingsCloseBtn;

    // Redesigned Tabbed Settings Panel
    private View mTabDisplayBtn;
    private TextView mTabDisplayText;
    private View mTabDisplayIndicator;
    private View mTabAudioBtn;
    private TextView mTabAudioText;
    private View mTabAudioIndicator;
    private View mTabControlsBtn;
    private TextView mTabControlsText;
    private View mTabControlsIndicator;

    private View mPanelTabDisplay;
    private View mPanelTabAudio;
    private View mPanelTabControls;

    private TextView mSizeSmallBtn;
    private TextView mSizeMediumBtn;
    private TextView mSizeLargeBtn;
    private TextView mSpeedPillBtn;
    private TextView mExpandPillBtn;
    private TextView mZoomFitBtn;
    private TextView mZoomCropBtn;
    private boolean mIsZoomMode = false;

    private SeekBar mVolumeSeekBar;
    private TextView mVolumeText;
    private SwitchCompat mSwitchMute;
    private TextView mDockAudioPillBtn;

    private SwitchCompat mSwitchAutoHide;
    private SwitchCompat mSwitchLock;
    private View mActionClosePlayerBtn;

    private int mSelectedSettingsTab = 0; // 0: DISPLAY, 1: AUDIO, 2: CONTROLS
    private int mVolumePercent = 100;
    private static final float[] PLAYBACK_SPEEDS = { 0.75f, 1.0f, 1.25f, 1.5f, 2.0f };
    private ImageButton mRewindBtn;
    private ImageButton mPlayPauseBtn;
    private ImageButton mForwardBtn;
    private ImageButton mVolumeBtn;
    private View mDockTabBtn;
    private FrameLayout mDockContainer;
    private SeekBar mSeekBar;
    private TextView mTimeText;
    private View mSeekLeftIndicator;
    private TextView mSeekLeftText;
    private View mSeekRightIndicator;
    private TextView mSeekRightText;
    private int mAccumulatedSeekSeconds = 0;
    private long mLastSeekIndicatorTime = 0;
    private boolean mLastSeekForward = false;
    private long mLastTapTime = 0;
    private int mLastTapSide = 0;
    private final Runnable mSingleTapRunnable = this::onFloatingWindowTapped;
    private final Runnable mHideSeekIndicatorRunnable = this::hideSeekIndicators;

    private TextureView mTextureView;

    // Floating Card Mode UI (matching user screenshot)
    private View mCardContent;
    private View mVideoContent;
    private ImageView mCardThumb;
    private TextView mCardTitle;
    private TextView mCardChannel;
    private FloatingEqualizerView mCardEqualizer;
    private SeekBar mCardProgressBar;
    private TextView mCardTimeLeft;
    private TextView mCardTimeRight;
    private ImageButton mCardPrevBtn;
    private ImageButton mCardPlayPauseBtn;
    private ImageButton mCardNextBtn;
    private ImageButton mCardPipBtn;
    private ImageButton mCardPlaylistBtn;
    private ImageButton mVideoPlaylistBtn;
    private android.view.ScaleGestureDetector mScaleDetector;
    private boolean mIsScaling = false;

    // Expandable Related Videos Container
    private View mRelatedContainer;
    private RecyclerView mRelatedRecycler;
    private ImageButton mRelatedCloseBtn;
    private FloatingRelatedVideosAdapter mRelatedAdapter;
    private boolean mIsLoadingMoreRelated = false;
    private int mRelatedSeedIndex = 0;
    private boolean mIsCardMode = false;
    private boolean mIsRelatedListOpen = false;
    private int mNormalHeightPx = 0;
    private String mLastLoadedVideoId = null;
    @Nullable
    private Surface mFloatingSurface;

    private boolean mIsShowing;
    private boolean mIsDocked = false;
    private boolean mDockedOnLeft = false;
    private int mSavedNormalWidth = 0;
    private int mSavedNormalHeight = 0;
    private boolean mControlsVisible = true;
    private boolean mControlsLocked = false;
    private boolean mIsUserSeeking = false;
    private boolean mMuted = false;

    private int mForegroundActivities = 0;
    private boolean mInAppMode = false;
    private Application.ActivityLifecycleCallbacks mLifecycleCallbacks;

    private final Runnable mTickRunnable = this::onTick;
    private final Runnable mHideControlsRunnable = this::hideControls;

    private final Runnable mDimDockTabRunnable = () -> {
        if (mIsDocked && mDockTabBtn != null && mDockTabBtn.getVisibility() == View.VISIBLE) {
            mDockTabBtn.animate().cancel();
            mDockTabBtn.animate().alpha(0.25f).setDuration(400).start();
        }
    };

    private void scheduleDimDockTab() {
        cancelDimDockTab();
        postDelayed(mDimDockTabRunnable, 10000L);
    }

    private void cancelDimDockTab() {
        removeCallbacks(mDimDockTabRunnable);
        if (mDockTabBtn != null) {
            mDockTabBtn.animate().cancel();
            mDockTabBtn.setAlpha(1.0f);
        }
    }

    private void dismissToBottom() {
        ExoPlayer player = getActivePlayer();
        if (player != null) {
            try {
                player.setPlayWhenReady(false);
            } catch (Exception ignored) {
            }
        }
        hide(true);
    }

    /**
     * Update the keep‑screen‑on flag based on playback state and visibility.
     * When the mini‑player is visible on screen AND the video is playing
     * (playWhenReady == true and not ended/idle), prevent the screen from turning off.
     * If the video is paused, or if the player is hidden/backgrounded, allow the
     * device to sleep/lock normally according to system settings.
     */
    private void updateScreenOnFlag() {
        if (mRootView == null) return;
        ExoPlayer player = getActivePlayer();
        boolean isPlaying = player != null
                && player.getPlayWhenReady()
                && player.getPlaybackState() != Player.STATE_ENDED
                && player.getPlaybackState() != Player.STATE_IDLE;
        boolean keepOn = mIsShowing && isPlaying;

        mRootView.setKeepScreenOn(keepOn);

        if (mLayoutParams != null && mRootView.getParent() != null) {
            int oldFlags = mLayoutParams.flags;
            if (keepOn) {
                mLayoutParams.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
            } else {
                mLayoutParams.flags &= ~WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
            }
            if (mLayoutParams.flags != oldFlags) {
                try {
                    mWindowManager.updateViewLayout(mRootView, mLayoutParams);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private final Player.Listener mPlayerListener = new Player.Listener() {
        @Override
        public void onVideoSizeChanged(VideoSize videoSize) {
            updateAspectRatio(videoSize);
        }

        @Override
        public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
            ExoPlayer player = getActivePlayer();
            if (player != null) {
                updatePlayPause(player);
                // Update screen-on flag based on new play state
                updateScreenOnFlag();
            }
        }

        @Override
        public void onPlaybackStateChanged(int playbackState) {
            ExoPlayer player = getActivePlayer();
            if (player != null) {
                updatePlayPause(player);
                // Update screen-on flag in case state affects visibility
                updateScreenOnFlag();
            }
        }
    };

    private FloatingMiniPlayerManager(Context appContext) {
        mAppContext = appContext;
        mWindowManager = (WindowManager) appContext.getSystemService(Context.WINDOW_SERVICE);
        try {
            SharedPreferences prefs = mAppContext.getSharedPreferences("loxtube_floating_prefs", Context.MODE_PRIVATE);
            mCurrentSize = prefs.getInt("player_size", SIZE_MEDIUM);
            mAutoHideControls = prefs.getBoolean("controls_autohide", true);
        } catch (Exception ignored) {
        }
        initLifecycle();
    }

    private java.lang.ref.WeakReference<Activity> mCurrentResumedActivity = new java.lang.ref.WeakReference<>(null);

    private void initLifecycle() {
        if (mAppContext instanceof Application) {
            Application app = (Application) mAppContext;
            mLifecycleCallbacks = new Application.ActivityLifecycleCallbacks() {
                @Override
                public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

                @Override
                public void onActivityStarted(Activity activity) {
                    mForegroundActivities++;
                    updateInAppMode();
                }

                @Override
                public void onActivityResumed(Activity activity) {
                    mCurrentResumedActivity = new java.lang.ref.WeakReference<>(activity);
                    updateInAppMode();
                }

                @Override
                public void onActivityPaused(Activity activity) {
                    postDelayed(FloatingMiniPlayerManager.this::updateInAppMode, 200);
                }

                @Override
                public void onActivityStopped(Activity activity) {
                    mForegroundActivities = Math.max(0, mForegroundActivities - 1);
                    updateInAppMode();
                }

                @Override
                public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

                @Override
                public void onActivityDestroyed(Activity activity) {}
            };
            app.registerActivityLifecycleCallbacks(mLifecycleCallbacks);
        }
    }

    public boolean isAppInForeground() {
        return mForegroundActivities > 0;
    }

    public void updateInAppMode() {
        setInAppMode(isAppInForeground());
    }

    public void setInAppMode(boolean inApp) {
        if (mRootView == null || mLayoutParams == null) {
            return;
        }
        boolean changed = (mInAppMode != inApp);
        mInAppMode = inApp;

        if (mIsDocked) {
            // Stay docked at screen edge until the user taps or pulls the handle
            return;
        }

        float density = mAppContext.getResources().getDisplayMetrics().density;
        int screenW = mAppContext.getResources().getDisplayMetrics().widthPixels;
        int screenH = mAppContext.getResources().getDisplayMetrics().heightPixels;

        com.google.android.material.card.MaterialCardView card =
                mRootView.findViewById(R.id.floating_mini_card);

        if (inApp) {
            // In-app mode: using user selected size (Small: 200x112, Medium: 240x135, Large: 280x158)
            int widthDp = (mCurrentSize == SIZE_SMALL) ? 200 : (mCurrentSize == SIZE_LARGE ? 280 : 240);
            int heightDp = (mCurrentSize == SIZE_SMALL) ? 112 : (mCurrentSize == SIZE_LARGE ? 158 : 135);
            int widthPx = Math.round(widthDp * density);
            int heightPx = Math.round(heightDp * density);
            int marginEndPx = Math.round(12 * density);
            int marginBottomPx = Math.round(58 * density);
            int maxY = Math.max(0, screenH - heightPx - marginBottomPx);
            int maxX = Math.max(0, screenW - widthPx - marginEndPx);

            mLayoutParams.width = widthPx;
            mLayoutParams.height = heightPx;
            if (changed || mLayoutParams.x < 0 || mLayoutParams.y < 0 || mLayoutParams.x > maxX) {
                mLayoutParams.x = maxX;
                mLayoutParams.y = maxY;
            } else {
                mLayoutParams.x = Math.max(0, Math.min(maxX, mLayoutParams.x));
                mLayoutParams.y = Math.max(0, Math.min(maxY, mLayoutParams.y));
            }

            if (card != null) {
                card.setRadius(Math.round(8 * density));
                card.setCardBackgroundColor(0xFF16161A);
            }
            if (mVideoContent != null) {
                android.view.ViewGroup.LayoutParams vLp = mVideoContent.getLayoutParams();
                if (vLp != null) {
                    vLp.height = heightPx;
                    mVideoContent.setLayoutParams(vLp);
                }
            }
        } else {
            // Outside the app: using user selected size (Small: 215x122, Medium: 265x150, Large: 300x170)
            int widthDp = (mCurrentSize == SIZE_SMALL) ? 215 : (mCurrentSize == SIZE_LARGE ? 300 : 265);
            int heightDp = (mCurrentSize == SIZE_SMALL) ? 122 : (mCurrentSize == SIZE_LARGE ? 170 : 150);
            int widthPx = Math.round(widthDp * density);
            int heightPx = Math.round(heightDp * density);
            int maxX = Math.max(0, screenW - widthPx);
            int maxY = Math.max(0, screenH - heightPx);

            mLayoutParams.width = widthPx;
            mLayoutParams.height = heightPx;
            if (changed || mLayoutParams.x < 0 || mLayoutParams.y < 0 || mLayoutParams.x > maxX) {
                mLayoutParams.x = Math.max(0, (screenW - widthPx) / 2);
                mLayoutParams.y = Math.max(0, screenH - heightPx - Math.round(120 * density));
            } else {
                mLayoutParams.x = Math.max(0, Math.min(maxX, mLayoutParams.x));
                mLayoutParams.y = Math.max(0, Math.min(maxY, mLayoutParams.y));
            }

            if (card != null) {
                card.setRadius(Math.round(12 * density));
                card.setCardBackgroundColor(0xFF16161A);
            }
            if (mVideoContent != null) {
                android.view.ViewGroup.LayoutParams vLp = mVideoContent.getLayoutParams();
                if (vLp != null) {
                    vLp.height = heightPx;
                    mVideoContent.setLayoutParams(vLp);
                }
            }
        }

        updateWatermarkVisibility();
        if (mControlsVisible) {
            applyLockState(mControlsLocked);
        } else {
            hideControls();
        }

        if (mIsShowing && mRootView.getParent() != null) {
            try {
                mWindowManager.updateViewLayout(mRootView, mLayoutParams);
            } catch (Exception ignored) {
            }
        }
    }

    @Nullable
    private ExoPlayer getActivePlayer() {
        return EnhancedPlayerManager.getInstance().getPlayer();
    }

    /**
     * Toggles the floating overlay mini-player when the Notification Bar's PiP / Headphone
     * button is clicked.
     */
    public void toggleFromNotification() {
        if (mIsShowing) {
            hide(true);
        } else {
            show(true);
        }
    }

    /**
     * Show the floating overlay mini-player over any screen.
     */
    public boolean show(boolean requestPermissionIfMissing) {
        ExoPlayer player = getActivePlayer();
        if (player == null) {
            return false;
        }

        if (!canDrawOverlays(mAppContext)) {
            if (requestPermissionIfMissing && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    Toast.makeText(mAppContext,
                            "Please allow 'Display over other apps' for LoxTube Floating Mini-Player",
                            Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + mAppContext.getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mAppContext.startActivity(intent);
                } catch (Exception ignored) {
                }
            }
            return false;
        }

        if (mIsShowing && mRootView != null) {
            updateInAppMode();
            updatePlayPause(player);
            syncNotificationIcon();
            ensureVideoSurfaceAttached();
            return true;
        }

        ensureViewCreated();

        updateInAppMode();

        applyLockState(false);

        if (mLayoutParams != null && !mIsDocked) {
            float density = mAppContext.getResources().getDisplayMetrics().density;
        int screenW = mAppContext.getResources().getDisplayMetrics().widthPixels;
        int screenH = mAppContext.getResources().getDisplayMetrics().heightPixels;
            int bottomNavInset = mInAppMode ? Math.round(58 * density) : 0;
            int marginEndPx = mInAppMode ? Math.round(12 * density) : 0;
            int maxX = Math.max(0, screenW - mLayoutParams.width - marginEndPx);
            int maxY = Math.max(0, screenH - mLayoutParams.height - bottomNavInset);

            if (mLayoutParams.x > maxX || mLayoutParams.x < 0) {
                mLayoutParams.x = maxX;
            }
            if (mLayoutParams.y > maxY || mLayoutParams.y < 0) {
                mLayoutParams.y = maxY;
            }
        }

        attachTextureAndPlayer(player);

        try {
            if (mRootView.getParent() == null) {
                mWindowManager.addView(mRootView, mLayoutParams);
            }
            mIsShowing = true;
            PictureInPictureHelper.INSTANCE.setPopupActive(true);
            // Keep screen on while playing
            updateScreenOnFlag();
        } catch (Exception e) {
            return false;
        }

        mIsDocked = false;
        mIsRelatedListOpen = false;
        mIsCardMode = false;
        if (mCardContent != null) {
            mCardContent.setVisibility(View.GONE);
        }
        if (mVideoContent != null) {
            mVideoContent.setVisibility(View.VISIBLE);
        }
        if (mRelatedContainer != null) {
            mRelatedContainer.setVisibility(View.GONE);
        }
        if (mCardEqualizer != null) {
            mCardEqualizer.setPlaying(false);
        }
        if (mDockTabBtn != null) {
            mDockTabBtn.setVisibility(View.GONE);
        }
        if (mFrame != null) {
            mFrame.setVisibility(View.VISIBLE);
        }
        hideControls();
        updatePlayPause(player);
        updateAspectRatio(player.getVideoSize());

        removeCallbacks(mTickRunnable);
        postDelayed(mTickRunnable, 0);

        syncNotificationIcon();
        return true;
    }

    /**
     * Hide the floating overlay mini-player while keeping background audio alive (unless stopping).
     */
    public void hide(boolean notifyAudioActive) {
        removeCallbacks(mTickRunnable);
        removeCallbacks(mHideControlsRunnable);
        removeCallbacks(mSingleTapRunnable);
        removeCallbacks(mHideSeekIndicatorRunnable);
        hideSeekIndicators();
        cancelDimDockTab();
        mIsDocked = false;
        mIsRelatedListOpen = false;
        if (mRelatedContainer != null) {
            mRelatedContainer.setVisibility(View.GONE);
        }
        if (mCardEqualizer != null) {
            mCardEqualizer.setPlaying(false);
        }
        mSavedNormalWidth = 0;
        mSavedNormalHeight = 0;
        if (mSettingsPanel != null) {
            mSettingsPanel.setVisibility(View.GONE);
        }
        if (mDockTabBtn != null) {
            mDockTabBtn.setVisibility(View.GONE);
        }
        if (mFrame != null) {
            mFrame.setVisibility(View.VISIBLE);
        }

        ExoPlayer player = getActivePlayer();
        if (player != null) {
            player.removeListener(mPlayerListener);
            if (player.getCurrentPosition() > 1000) {
                
            }
        }

        

        if (mFloatingSurface != null) {
            try {
                mFloatingSurface.release();
            } catch (Exception ignored) {
            }
            mFloatingSurface = null;
        }

        if (mTextureView != null && mFrame != null) {
            mFrame.removeView(mTextureView);
            mTextureView = null;
        }

        if (mIsShowing && mRootView != null && mRootView.getParent() != null) {
            try {
                mWindowManager.removeViewImmediate(mRootView);
            } catch (Exception ignored) {
            }
        }
        mIsShowing = false;
        PictureInPictureHelper.INSTANCE.setPopupActive(false);
        mControlsLocked = false;
        try {
            EnhancedPlayerManager.getInstance().reattachVideoSurface();
            EnhancedPlayerManager.getInstance().restoreVideoOutput();
        } catch (Exception ignored) {
        }

        syncNotificationIcon();
    }

    private void syncNotificationIcon() {
        try {
            EnhancedPlayerManager.getInstance().refreshNotificationLayout();
        } catch (Exception ignored) {
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void ensureViewCreated() {
        if (mRootView != null) {
            return;
        }

        Context themedContext = new ContextThemeWrapper(mAppContext, R.style.Theme_FloatingMiniPlayer);
        mRootView = LayoutInflater.from(themedContext).inflate(R.layout.mobile_floating_mini_player, null, false);

        float density = mAppContext.getResources().getDisplayMetrics().density;
        int screenW = mAppContext.getResources().getDisplayMetrics().widthPixels;
        int screenH = mAppContext.getResources().getDisplayMetrics().heightPixels;
        int widthPx = Math.round(260 * density);
        int heightPx = Math.round(146 * density);
        mNormalHeightPx = heightPx;

        mScaleDetector = new android.view.ScaleGestureDetector(mAppContext, new android.view.ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScaleBegin(android.view.ScaleGestureDetector detector) {
                mIsScaling = true;
                return true;
            }

            @Override
            public boolean onScale(android.view.ScaleGestureDetector detector) {
                float factor = detector.getScaleFactor();
                if (factor == 1.0f || mLayoutParams == null || mRootView == null) return true;

                float d = mAppContext.getResources().getDisplayMetrics().density;
                int sW = mAppContext.getResources().getDisplayMetrics().widthPixels;
                int sH = mAppContext.getResources().getDisplayMetrics().heightPixels;

                int minWidth = Math.round(140 * d);
                int maxWidth = sW - Math.round(16 * d);

                int currentW = mLayoutParams.width;
                int currentH = mLayoutParams.height;
                if (currentW <= 0 || currentH <= 0) return true;

                int newWidth = Math.round(currentW * factor);
                newWidth = Math.max(minWidth, Math.min(maxWidth, newWidth));

                float ratio = (float) currentW / (float) currentH;
                if (ratio < 0.6f || ratio > 2.5f) {
                    ratio = 16f / 9f;
                }
                int newHeight = Math.round(newWidth / ratio);

                mLayoutParams.width = newWidth;
                mLayoutParams.height = newHeight;
                mNormalHeightPx = newHeight;

                if (mVideoContent != null) {
                    android.view.ViewGroup.LayoutParams vLp = mVideoContent.getLayoutParams();
                    if (vLp != null) {
                        vLp.height = newHeight;
                        mVideoContent.setLayoutParams(vLp);
                    }
                }

                int maxX = Math.max(0, sW - newWidth);
                int maxY = Math.max(0, sH - newHeight);
                mLayoutParams.x = Math.max(0, Math.min(maxX, mLayoutParams.x));
                mLayoutParams.y = Math.max(0, Math.min(maxY, mLayoutParams.y));

                try {
                    mWindowManager.updateViewLayout(mRootView, mLayoutParams);
                } catch (Exception ignored) {
                }
                return true;
            }

            @Override
            public void onScaleEnd(android.view.ScaleGestureDetector detector) {
                mIsScaling = false;
            }
        });

        int overlayType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        mLayoutParams = new WindowManager.LayoutParams(
                widthPx,
                heightPx,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                PixelFormat.TRANSLUCENT);
        mLayoutParams.gravity = Gravity.TOP | Gravity.START;
        mLayoutParams.x = Math.max(0, (screenW - widthPx) / 2);
        mLayoutParams.y = Math.max(0, screenH - heightPx - Math.round(120 * density));

        mFrame = mRootView.findViewById(R.id.floating_mini_player_frame);
        mFreeze = mRootView.findViewById(R.id.floating_mini_freeze);
        mScrim = mRootView.findViewById(R.id.floating_mini_top_scrim);
        mTopBar = mRootView.findViewById(R.id.floating_mini_top_bar);
        mTopActions = mRootView.findViewById(R.id.floating_mini_top_actions);
        mCenterControls = mRootView.findViewById(R.id.floating_mini_center_controls);
        mBottomBar = mRootView.findViewById(R.id.floating_mini_bottom_bar);
        mBottomScrim = mRootView.findViewById(R.id.floating_mini_bottom_scrim);
        mBrandWatermark = mRootView.findViewById(R.id.floating_mini_brand);
        mLockBtn = mRootView.findViewById(R.id.floating_mini_lock);
        mHeadphoneBtn = mRootView.findViewById(R.id.floating_mini_headphone);
        mPipExpandBtn = mRootView.findViewById(R.id.floating_mini_pip_expand);
        mSettingsBtn = mRootView.findViewById(R.id.floating_mini_settings);
        mSettingsPanel = mRootView.findViewById(R.id.floating_mini_settings_panel);
        mSettingsCloseBtn = mRootView.findViewById(R.id.floating_mini_settings_close);
        mTabDisplayBtn = mRootView.findViewById(R.id.floating_mini_tab_display_btn);
        mTabDisplayText = mRootView.findViewById(R.id.floating_mini_tab_display_text);
        mTabDisplayIndicator = mRootView.findViewById(R.id.floating_mini_tab_display_indicator);
        mTabAudioBtn = mRootView.findViewById(R.id.floating_mini_tab_audio_btn);
        mTabAudioText = mRootView.findViewById(R.id.floating_mini_tab_audio_text);
        mTabAudioIndicator = mRootView.findViewById(R.id.floating_mini_tab_audio_indicator);
        mTabControlsBtn = mRootView.findViewById(R.id.floating_mini_tab_controls_btn);
        mTabControlsText = mRootView.findViewById(R.id.floating_mini_tab_controls_text);
        mTabControlsIndicator = mRootView.findViewById(R.id.floating_mini_tab_controls_indicator);

        mPanelTabDisplay = mRootView.findViewById(R.id.panel_tab_display);
        mPanelTabAudio = mRootView.findViewById(R.id.panel_tab_audio);
        mPanelTabControls = mRootView.findViewById(R.id.panel_tab_controls);

        mSizeSmallBtn = mRootView.findViewById(R.id.floating_mini_size_small);
        mSizeMediumBtn = mRootView.findViewById(R.id.floating_mini_size_medium);
        mSizeLargeBtn = mRootView.findViewById(R.id.floating_mini_size_large);
        mSpeedPillBtn = mRootView.findViewById(R.id.floating_mini_speed_pill);
        mExpandPillBtn = mRootView.findViewById(R.id.floating_mini_expand_pill);
        mZoomFitBtn = mRootView.findViewById(R.id.floating_mini_zoom_fit);
        mZoomCropBtn = mRootView.findViewById(R.id.floating_mini_zoom_crop);

        mVolumeSeekBar = mRootView.findViewById(R.id.floating_mini_volume_seek);
        mVolumeText = mRootView.findViewById(R.id.floating_mini_volume_text);
        mSwitchMute = mRootView.findViewById(R.id.floating_mini_switch_mute);
        mDockAudioPillBtn = mRootView.findViewById(R.id.floating_mini_dock_audio_pill);

        mSwitchAutoHide = mRootView.findViewById(R.id.floating_mini_switch_autohide);
        mSwitchLock = mRootView.findViewById(R.id.floating_mini_switch_lock);
        mActionClosePlayerBtn = mRootView.findViewById(R.id.floating_mini_action_close_player);
        mRewindBtn = mRootView.findViewById(R.id.floating_mini_rewind);
        mPlayPauseBtn = mRootView.findViewById(R.id.floating_mini_play_pause);
        mForwardBtn = mRootView.findViewById(R.id.floating_mini_forward);
        mVolumeBtn = mRootView.findViewById(R.id.floating_mini_volume);
        mFullscreenBtn = mRootView.findViewById(R.id.floating_mini_fullscreen);
        mSeekBar = mRootView.findViewById(R.id.floating_mini_progress);
        mTimeText = mRootView.findViewById(R.id.floating_mini_time);
        mDockTabBtn = mRootView.findViewById(R.id.floating_mini_dock_tab);
        mDockContainer = mRootView.findViewById(R.id.floating_mini_dock_container);
        mSeekLeftIndicator = mRootView.findViewById(R.id.floating_mini_seek_left);
        mSeekLeftText = mRootView.findViewById(R.id.floating_mini_seek_left_text);
        mSeekRightIndicator = mRootView.findViewById(R.id.floating_mini_seek_right);
        mSeekRightText = mRootView.findViewById(R.id.floating_mini_seek_right_text);

        mCardContent = mRootView.findViewById(R.id.floating_mini_card_content);
        mVideoContent = mRootView.findViewById(R.id.floating_mini_video_content);
        mCardThumb = mRootView.findViewById(R.id.floating_mini_card_thumb);
        mCardTitle = mRootView.findViewById(R.id.floating_mini_card_title);
        mCardChannel = mRootView.findViewById(R.id.floating_mini_card_channel);
        mCardEqualizer = mRootView.findViewById(R.id.floating_mini_card_equalizer);
        mCardProgressBar = mRootView.findViewById(R.id.floating_mini_card_progress);
        mCardTimeLeft = mRootView.findViewById(R.id.floating_mini_card_time_left);
        mCardTimeRight = mRootView.findViewById(R.id.floating_mini_card_time_right);
        mCardPrevBtn = mRootView.findViewById(R.id.floating_mini_card_prev);
        mCardPlayPauseBtn = mRootView.findViewById(R.id.floating_mini_card_play_pause);
        mCardNextBtn = mRootView.findViewById(R.id.floating_mini_card_next);
        mCardPipBtn = mRootView.findViewById(R.id.floating_mini_card_pip);
        mCardPlaylistBtn = mRootView.findViewById(R.id.floating_mini_card_playlist);

        mRelatedContainer = mRootView.findViewById(R.id.floating_mini_related_container);
        mRelatedRecycler = mRootView.findViewById(R.id.floating_mini_related_recycler);
        mRelatedCloseBtn = mRootView.findViewById(R.id.floating_mini_related_close);

        if (mRelatedRecycler != null) {
            LinearLayoutManager llm = new LinearLayoutManager(mAppContext);
            mRelatedRecycler.setLayoutManager(llm);
            mRelatedAdapter = new FloatingRelatedVideosAdapter(video -> {
                EnhancedPlayerManager.getInstance().playVideoFromFloating(video);
                setRelatedListOpen(false);
                updateCardMetadata(video);
                postDelayed(this::ensureVideoSurfaceAttached, 300);
            });
            mRelatedRecycler.setAdapter(mRelatedAdapter);
            mRelatedRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    if (dy <= 0) return;
                    int totalItemCount = llm.getItemCount();
                    int lastVisible = llm.findLastVisibleItemPosition();
                    if (!mIsLoadingMoreRelated && totalItemCount > 0 && totalItemCount <= (lastVisible + 4)) {
                        loadMoreRelatedVideos();
                    }
                }
            });
        }

        if (mCardPrevBtn != null) {
            mCardPrevBtn.setOnClickListener(v -> {
                try {
                    EnhancedPlayerManager.getInstance().playPrevious(true);
                } catch (Exception ignored) {
                }
            });
        }
        if (mCardPlayPauseBtn != null) {
            mCardPlayPauseBtn.setOnClickListener(v -> {
                ExoPlayer player = getActivePlayer();
                if (player != null) {
                    if (player.getPlayWhenReady()) {
                        player.setPlayWhenReady(false);
                    } else {
                        if (player.getPlaybackState() == Player.STATE_ENDED) {
                            player.seekTo(0);
                        }
                        player.setPlayWhenReady(true);
                    }
                    updatePlayPause(player);
                }
            });
        }
        if (mCardNextBtn != null) {
            mCardNextBtn.setOnClickListener(v -> {
                try {
                    EnhancedPlayerManager.getInstance().skipToNextFromSession();
                } catch (Exception e) {
                    EnhancedPlayerManager.getInstance().playNext(true);
                }
            });
        }
        if (mCardPipBtn != null) {
            mCardPipBtn.setOnClickListener(v -> {
                mIsCardMode = false;
                setRelatedListOpen(false);
            });
        }
        if (mCardPlaylistBtn != null) {
            mCardPlaylistBtn.setOnClickListener(v -> toggleRelatedList());
        }
        mVideoPlaylistBtn = mRootView.findViewById(R.id.floating_mini_video_playlist);
        if (mVideoPlaylistBtn != null) {
            mVideoPlaylistBtn.setOnClickListener(v -> toggleRelatedList());
        }
        if (mRelatedCloseBtn != null) {
            mRelatedCloseBtn.setOnClickListener(v -> setRelatedListOpen(false));
        }

        if (mCardProgressBar != null) {
            mCardProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        ExoPlayer player = getActivePlayer();
                        if (player != null && player.getDuration() > 0) {
                            long targetMs = (player.getDuration() * progress) / 1000L;
                            if (mCardTimeLeft != null) {
                                mCardTimeLeft.setText(formatTime(targetMs));
                            }
                        }
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    mIsUserSeeking = true;
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    ExoPlayer player = getActivePlayer();
                    if (player != null && player.getDuration() > 0) {
                        long targetMs = (player.getDuration() * seekBar.getProgress()) / 1000L;
                        player.seekTo(targetMs);
                    }
                    mIsUserSeeking = false;
                }
            });
        }

        if (mDockTabBtn != null) {
            mDockTabBtn.setOnClickListener(v -> undockToScreen());
        }
        if (mDockContainer != null) {
            mDockContainer.setOnClickListener(v -> undockToScreen());
        }

        if (mLockBtn != null) {
            mLockBtn.setOnClickListener(v -> toggleLock());
        }
        if (mHeadphoneBtn != null) {
            mHeadphoneBtn.setOnClickListener(v -> dockToSide(false));
        }
        if (mPipExpandBtn != null) {
            mPipExpandBtn.setOnClickListener(v -> openFullPlayer(false));
        }
        if (mFullscreenBtn != null) {
            mFullscreenBtn.setOnClickListener(v -> openFullPlayer(true));
        }
        if (mSettingsBtn != null) {
            mSettingsBtn.setOnClickListener(v -> toggleSettingsPanel());
        }
        if (mSettingsCloseBtn != null) {
            mSettingsCloseBtn.setOnClickListener(v -> hideSettingsPanel());
        }
        if (mTabDisplayBtn != null) {
            mTabDisplayBtn.setOnClickListener(v -> selectSettingsTab(0));
        }
        if (mTabAudioBtn != null) {
            mTabAudioBtn.setOnClickListener(v -> selectSettingsTab(1));
        }
        if (mTabControlsBtn != null) {
            mTabControlsBtn.setOnClickListener(v -> selectSettingsTab(2));
        }
        if (mSizeSmallBtn != null) {
            mSizeSmallBtn.setOnClickListener(v -> applySize(SIZE_SMALL));
        }
        if (mSizeMediumBtn != null) {
            mSizeMediumBtn.setOnClickListener(v -> applySize(SIZE_MEDIUM));
        }
        if (mSizeLargeBtn != null) {
            mSizeLargeBtn.setOnClickListener(v -> applySize(SIZE_LARGE));
        }
        if (mSpeedPillBtn != null) {
            mSpeedPillBtn.setOnClickListener(v -> cyclePlaybackSpeed());
        }
        if (mExpandPillBtn != null) {
            mExpandPillBtn.setOnClickListener(v -> openFullPlayer(true));
        }
        if (mZoomFitBtn != null) {
            mZoomFitBtn.setOnClickListener(v -> setZoomMode(false));
        }
        if (mZoomCropBtn != null) {
            mZoomCropBtn.setOnClickListener(v -> setZoomMode(true));
        }
        if (mVolumeSeekBar != null) {
            mVolumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        applyVolumePercent(progress);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
        }
        if (mSwitchMute != null) {
            mSwitchMute.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed()) {
                    setMuteState(isChecked);
                }
            });
        }
        if (mDockAudioPillBtn != null) {
            mDockAudioPillBtn.setOnClickListener(v -> dockToSide(false));
        }
        if (mSwitchAutoHide != null) {
            mSwitchAutoHide.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed()) {
                    setAutoHideControls(isChecked);
                }
            });
        }
        if (mSwitchLock != null) {
            mSwitchLock.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed()) {
                    toggleLock();
                }
            });
        }
        if (mActionClosePlayerBtn != null) {
            mActionClosePlayerBtn.setOnClickListener(v -> closeAndStop());
        }
        if (mSettingsPanel != null) {
            mSettingsPanel.setOnTouchListener((v, ev) -> true);
        }
        updateSettingsUi();
        if (mVolumeBtn != null) {
            mVolumeBtn.setOnClickListener(v -> toggleMute());
        }
        updateWatermarkVisibility();
        updateVolumeIcon();

        if (mPlayPauseBtn != null) {
            mPlayPauseBtn.setOnClickListener(v -> {
                ExoPlayer player = getActivePlayer();
                if (player != null) {
                    boolean next = !player.getPlayWhenReady();
                    player.setPlayWhenReady(next);
                    updatePlayPause(player);
                    updateScreenOnFlag();
                    rescheduleAutoHide();
                }
            });
        }

        if (mRewindBtn != null) {
            mRewindBtn.setOnClickListener(v -> {
                try {
                    EnhancedPlayerManager.getInstance().playPrevious(true);
                } catch (Exception ignored) {
                }
                rescheduleAutoHide();
            });
        }

        if (mForwardBtn != null) {
            mForwardBtn.setOnClickListener(v -> {
                try {
                    EnhancedPlayerManager.getInstance().skipToNextFromSession();
                } catch (Exception e) {
                    EnhancedPlayerManager.getInstance().playNext(true);
                }
                rescheduleAutoHide();
            });
        }

        if (mSeekBar != null) {
            mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && !mControlsLocked) {
                        ExoPlayer player = getActivePlayer();
                        if (player != null && player.getDuration() > 0) {
                            long newPos = (player.getDuration() * progress) / 1000L;
                            updateTimeDisplay(newPos, player.getDuration());
                        }
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    mIsUserSeeking = true;
                    removeCallbacks(mHideControlsRunnable);
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    mIsUserSeeking = false;
                    ExoPlayer player = getActivePlayer();
                    if (player != null && player.getDuration() > 0) {
                        long newPos = (player.getDuration() * seekBar.getProgress()) / 1000L;
                        player.seekTo(newPos);
                        
                    }
                    rescheduleAutoHide();
                }
            });
        }

        // Draggable window + tap to toggle controls (or show unlock icon when locked)
        final int touchSlop = ViewConfiguration.get(mAppContext).getScaledTouchSlop();
        View.OnTouchListener dragAndTapListener = new View.OnTouchListener() {
            private int mInitialX;
            private int mInitialY;
            private float mDownRawX;
            private float mDownRawY;
            private boolean mDragging;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getActionMasked();
                if (event.getPointerCount() > 1) {
                    if (mScaleDetector != null) {
                        mScaleDetector.onTouchEvent(event);
                    }
                    mIsScaling = true;
                    mDragging = false;
                    return true;
                }
                if (mIsScaling) {
                    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_POINTER_UP) {
                        if (event.getPointerCount() <= 2) {
                            mIsScaling = false;
                        }
                    }
                    return true;
                }
                if (action == MotionEvent.ACTION_DOWN) {
                    android.util.Log.d("BubbleTouch", "ACTION_DOWN x=" + event.getRawX() + " y=" + event.getRawY() + " docked=" + mIsDocked);
                    mInitialX = mLayoutParams.x;
                    mInitialY = mLayoutParams.y;
                    mDownRawX = event.getRawX();
                    mDownRawY = event.getRawY();
                    mDragging = false;
                    if (mIsDocked) {
                        cancelDimDockTab();
                    }
                    if (v != null) {
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    return true;
                } else if (action == MotionEvent.ACTION_MOVE) {
                    float dx = event.getRawX() - mDownRawX;
                    float dy = event.getRawY() - mDownRawY;
                    if (!mDragging && (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop)) {
                        mDragging = true;
                        if (v != null) {
                            v.getParent().requestDisallowInterceptTouchEvent(true);
                        }
                    }
                    if (mDragging && mIsShowing && mRootView != null && mRootView.getParent() != null) {
                        float density = mAppContext.getResources().getDisplayMetrics().density;
        int screenW = mAppContext.getResources().getDisplayMetrics().widthPixels;
        int screenH = mAppContext.getResources().getDisplayMetrics().heightPixels;
                        int bottomNavInset = mInAppMode ? Math.round(58 * density) : 0;
                        int maxY = Math.max(0, screenH - mLayoutParams.height - bottomNavInset);

                        if (mIsDocked) {
                            // While docked, user can drag the handle vertically along the edge or inward
                            int touchWindowWidth = Math.round(36 * density);
                            int maxX = screenW - touchWindowWidth;
                            mLayoutParams.x = Math.max(0, Math.min(maxX, mInitialX + Math.round(dx)));
                            mLayoutParams.y = Math.max(0, Math.min(maxY, mInitialY + Math.round(dy)));
                            try {
                                mWindowManager.updateViewLayout(mRootView, mLayoutParams);
                            } catch (Exception ignored) {
                            }
                            return true;
                        }

                        // Keep floating player strictly within visible screen while dragging - never cut off or poke off-screen
                        int minX = 0;
                        int maxX = Math.max(0, screenW - mLayoutParams.width);
                        mLayoutParams.x = Math.max(minX, Math.min(maxX, mInitialX + Math.round(dx)));
                        mLayoutParams.y = Math.max(0, Math.min(maxY, mInitialY + Math.round(dy)));
                        try {
                            mWindowManager.updateViewLayout(mRootView, mLayoutParams);
                        } catch (Exception ignored) {
                        }
                    }
                    return true;
                } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    android.util.Log.d("BubbleTouch", (action == MotionEvent.ACTION_UP ? "ACTION_UP" : "ACTION_CANCEL")
                            + " dragging=" + mDragging + " docked=" + mIsDocked);
                    if (mDragging) {
                        float density = mAppContext.getResources().getDisplayMetrics().density;
                        int screenW = mAppContext.getResources().getDisplayMetrics().widthPixels;
                        int screenH = mAppContext.getResources().getDisplayMetrics().heightPixels;
                        int bottomDismissThreshold = screenH - Math.round(110 * density);

                        // Drag to bottom center: dismiss floating window, pause video, preserve notification card
                        if (mLayoutParams.y >= bottomDismissThreshold || event.getRawY() >= screenH - Math.round(80 * density)) {
                            dismissToBottom();
                            mDragging = false;
                            return true;
                        }

                        if (mIsDocked) {
                            mDragging = false;
                            int touchWindowWidth = Math.round(36 * density);
                            int inwardDrag = mDockedOnLeft ? mLayoutParams.x : (screenW - touchWindowWidth - mLayoutParams.x);
                            if (inwardDrag > Math.round(14 * density)) {
                                undockToScreen();
                            } else {
                                boolean snapToLeft = (mLayoutParams.x + touchWindowWidth / 2) < (screenW / 2);
                                dockToSide(snapToLeft);
                            }
                            return true;
                        }

                        int bottomNavInset = mInAppMode ? Math.round(58 * density) : 0;
                        int maxY = Math.max(0, screenH - mLayoutParams.height - bottomNavInset);
                        mLayoutParams.y = Math.max(0, Math.min(maxY, mLayoutParams.y));

                        int maxX = Math.max(0, screenW - mLayoutParams.width);
                        mLayoutParams.x = Math.max(0, Math.min(maxX, mLayoutParams.x));
                        mIsDocked = false;
                        if (mDockTabBtn != null) {
                            mDockTabBtn.setVisibility(View.GONE);
                        }
                        if (mDockContainer != null) {
                            mDockContainer.setVisibility(View.GONE);
                        }
                        try {
                            mWindowManager.updateViewLayout(mRootView, mLayoutParams);
                        } catch (Exception ignored) {
                        }
                    } else {
                        if (mIsDocked) {
                            undockToScreen();
                        } else {
                            handleTap(v, event);
                        }
                    }
                    mDragging = false;
                    return true;
                }
                return false;
            }
        };

        View touchArea = mRootView.findViewById(R.id.floating_mini_touch_area);
        if (touchArea != null) {
            touchArea.setOnTouchListener(dragAndTapListener);
        }
        mRootView.setOnTouchListener(dragAndTapListener);
        View cardHeader = mRootView.findViewById(R.id.floating_mini_card_header);
        if (cardHeader != null) {
            cardHeader.setOnTouchListener(dragAndTapListener);
        }
        if (mFrame != null) {
            mFrame.setOnTouchListener(dragAndTapListener);
        }
        if (mScrim != null) {
            mScrim.setOnTouchListener(dragAndTapListener);
        }
        if (mDockTabBtn != null) {
            mDockTabBtn.setOnTouchListener(dragAndTapListener);
        }
        if (mDockContainer != null) {
            mDockContainer.setOnTouchListener(dragAndTapListener);
        }
    }

    public void dockToSide(boolean left) {
        if (!mIsShowing || mRootView == null || mRootView.getParent() == null) {
            return;
        }
        mIsDocked = true;
        mDockedOnLeft = left;

        hideControls();
        if (mTopBar != null) mTopBar.setVisibility(View.GONE);
        if (mTopActions != null) mTopActions.setVisibility(View.GONE);
        if (mCenterControls != null) mCenterControls.setVisibility(View.GONE);
        if (mBottomBar != null) mBottomBar.setVisibility(View.GONE);
        if (mBottomScrim != null) mBottomScrim.setVisibility(View.GONE);
        if (mScrim != null) mScrim.setVisibility(View.GONE);
        if (mSeekBar != null) mSeekBar.setVisibility(View.GONE);
        if (mFrame != null) mFrame.setVisibility(View.GONE);

        float density = mAppContext.getResources().getDisplayMetrics().density;
        int screenW = mAppContext.getResources().getDisplayMetrics().widthPixels;
        int screenH = mAppContext.getResources().getDisplayMetrics().heightPixels;
        int handleWidth = Math.round(6 * density);
        int handleHeight = Math.round(48 * density);
        int touchWindowWidth = Math.round(36 * density);
        int touchWindowHeight = Math.round(64 * density);

        if (mSavedNormalWidth <= 0 || mLayoutParams.width > touchWindowWidth) {
            mSavedNormalWidth = mLayoutParams.width;
            mSavedNormalHeight = mLayoutParams.height;
        }

        if (mDockContainer != null) {
            FrameLayout.LayoutParams cLp = new FrameLayout.LayoutParams(touchWindowWidth, touchWindowHeight);
            cLp.gravity = Gravity.CENTER_VERTICAL | (left ? Gravity.START : Gravity.END);
            mDockContainer.setLayoutParams(cLp);
            mDockContainer.setVisibility(View.VISIBLE);
            mDockContainer.bringToFront();
        }

        if (mDockTabBtn != null) {
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(handleWidth, handleHeight);
            lp.gravity = Gravity.CENTER_VERTICAL | (left ? Gravity.START : Gravity.END);
            mDockTabBtn.setLayoutParams(lp);
            mDockTabBtn.setBackgroundResource(left ? R.drawable.bg_float_dock_handle_left : R.drawable.bg_float_dock_handle_right);
            if (mDockTabBtn instanceof ImageView) {
                ((ImageView) mDockTabBtn).setImageDrawable(null);
            }
            mDockTabBtn.setVisibility(View.VISIBLE);
            mDockTabBtn.setAlpha(1.0f);
        }

        mLayoutParams.width = touchWindowWidth;
        mLayoutParams.height = touchWindowHeight;
        mLayoutParams.x = left ? 0 : (screenW - touchWindowWidth);

        com.google.android.material.card.MaterialCardView card =
                mRootView.findViewById(R.id.floating_mini_card);
        if (card != null) {
            card.setRadius(0);
            card.setCardElevation(0);
            card.setElevation(0);
            card.setCardBackgroundColor(Color.TRANSPARENT);
            card.setStrokeWidth(0);
        }

        try {
            mWindowManager.updateViewLayout(mRootView, mLayoutParams);
        } catch (Exception ignored) {
        }

        scheduleDimDockTab();
    }

    private void undockToScreen() {
        if (!mIsShowing || mRootView == null || mRootView.getParent() == null) {
            return;
        }
        mIsDocked = false;
        mIsRelatedListOpen = false;
        if (mRelatedContainer != null) {
            mRelatedContainer.setVisibility(View.GONE);
        }
        if (mCardEqualizer != null) {
            mCardEqualizer.setPlaying(false);
        }
        cancelDimDockTab();

        if (mDockTabBtn != null) {
            mDockTabBtn.setVisibility(View.GONE);
        }
        if (mDockContainer != null) {
            mDockContainer.setVisibility(View.GONE);
        }
        if (mFrame != null) {
            mFrame.setVisibility(View.VISIBLE);
            ExoPlayer player = getActivePlayer();
            if (player != null) {
                attachTextureAndPlayer(player);
            }
        }

        float density = mAppContext.getResources().getDisplayMetrics().density;
        int screenW = mAppContext.getResources().getDisplayMetrics().widthPixels;
        int screenH = mAppContext.getResources().getDisplayMetrics().heightPixels;
        int widthDp = (mCurrentSize == SIZE_SMALL) ? 200 : (mCurrentSize == SIZE_LARGE ? 280 : 240);
        int heightDp = (mCurrentSize == SIZE_SMALL) ? 112 : (mCurrentSize == SIZE_LARGE ? 158 : 135);
        if (!mInAppMode) {
            widthDp = (mCurrentSize == SIZE_SMALL) ? 215 : (mCurrentSize == SIZE_LARGE ? 300 : 265);
            heightDp = (mCurrentSize == SIZE_SMALL) ? 122 : (mCurrentSize == SIZE_LARGE ? 170 : 150);
        }
        int defaultWidth = Math.round(widthDp * density);
        int defaultHeight = Math.round(heightDp * density);
        int width = mSavedNormalWidth > 0 ? mSavedNormalWidth : defaultWidth;
        int height = mSavedNormalHeight > 0 ? mSavedNormalHeight : defaultHeight;

        mLayoutParams.width = width;
        mLayoutParams.height = height;
        int maxX = Math.max(0, screenW - width - Math.round(8 * density));
        mLayoutParams.x = mDockedOnLeft ? Math.round(8 * density) : maxX;

        com.google.android.material.card.MaterialCardView card =
                mRootView.findViewById(R.id.floating_mini_card);
        if (card != null) {
            card.setRadius(mInAppMode ? density * 8 : density * 12);
            card.setCardElevation(density * 12);
            card.setElevation(density * 12);
            card.setCardBackgroundColor(0xFF16161A);
            card.setStrokeWidth(Math.round(1 * density));
            card.setStrokeColor(0xFF262630);
        }

        try {
            mWindowManager.updateViewLayout(mRootView, mLayoutParams);
        } catch (Exception ignored) {
        }
        if (mScrim != null) {
            mScrim.setVisibility(View.VISIBLE);
        }
        if (mTopBar != null) {
            mTopBar.setVisibility(View.VISIBLE);
        }
        if (mSeekBar != null) {
            mSeekBar.setVisibility(View.VISIBLE);
        }
        hideControls();
    }

    private void handleTap(View v, MotionEvent event) {
        float width = 0;
        float localX = 0;
        if (v != null && v.getWidth() > 0) {
            width = v.getWidth();
            localX = event.getX();
        } else if (mLayoutParams != null && mLayoutParams.width > 0) {
            width = mLayoutParams.width;
            localX = event.getRawX() - mLayoutParams.x;
        }

        if (width <= 0) {
            onFloatingWindowTapped();
            return;
        }

        boolean isLeftSide = (localX < width * 0.38f);
        boolean isRightSide = (localX > width * 0.62f);
        int currentSide = isLeftSide ? -1 : (isRightSide ? 1 : 0);

        long now = android.os.SystemClock.uptimeMillis();
        boolean isDoubleTap = !mControlsLocked
                && currentSide == mLastTapSide
                && (now - mLastTapTime < 350);

        mLastTapTime = now;
        mLastTapSide = currentSide;

        if (isDoubleTap) {
            removeCallbacks(mSingleTapRunnable);
            if (currentSide == 0) {
                toggleZoomMode();
            } else {
                performDoubleTapSeek(currentSide == 1);
            }
        } else {
            removeCallbacks(mSingleTapRunnable);
            if (!mControlsLocked) {
                postDelayed(mSingleTapRunnable, 260);
            } else {
                onFloatingWindowTapped();
            }
        }
    }

    private void performDoubleTapSeek(boolean forward) {
        ExoPlayer player = getActivePlayer();
        if (player == null) return;
        long duration = player.getDuration();
        long current = player.getCurrentPosition();
        if (duration <= 0) return;

        long delta = 10000L;
        long target = forward ? Math.min(duration, current + delta) : Math.max(0, current - delta);
        player.seekTo(target);
        

        if (mSeekBar != null) {
            int progress = (int) (1000L * target / duration);
            mSeekBar.setProgress(progress);
        }
        if (mTimeText != null) {
            updateTimeDisplay(target, duration);
        }

        hideControls();
        showDoubleTapIndicator(forward);
    }

    private void showDoubleTapIndicator(boolean forward) {
        long now = android.os.SystemClock.uptimeMillis();
        if (now - mLastSeekIndicatorTime < 900 && mLastSeekForward == forward) {
            mAccumulatedSeekSeconds += 10;
        } else {
            mAccumulatedSeekSeconds = 10;
        }
        mLastSeekIndicatorTime = now;
        mLastSeekForward = forward;

        View container = forward ? mSeekRightIndicator : mSeekLeftIndicator;
        TextView textView = forward ? mSeekRightText : mSeekLeftText;
        View other = forward ? mSeekLeftIndicator : mSeekRightIndicator;

        if (other != null) {
            other.setVisibility(View.GONE);
            other.animate().cancel();
        }

        if (container != null && textView != null) {
            textView.setText(mAccumulatedSeekSeconds + " seconds");
            container.animate().cancel();
            container.setVisibility(View.VISIBLE);
            container.setAlpha(1f);
            container.setScaleX(1.15f);
            container.setScaleY(1.15f);
            container.animate().scaleX(1.0f).scaleY(1.0f).setDuration(160).start();

            removeCallbacks(mHideSeekIndicatorRunnable);
            postDelayed(mHideSeekIndicatorRunnable, 750);
        }
    }

    private void hideSeekIndicators() {
        mAccumulatedSeekSeconds = 0;
        if (mSeekLeftIndicator != null) {
            mSeekLeftIndicator.animate().cancel();
            if (!mIsShowing || mRootView == null || mRootView.getParent() == null) {
                mSeekLeftIndicator.setVisibility(View.GONE);
            } else if (mSeekLeftIndicator.getVisibility() == View.VISIBLE) {
                mSeekLeftIndicator.animate().alpha(0f).setDuration(200).withEndAction(() -> {
                    if (mSeekLeftIndicator != null) mSeekLeftIndicator.setVisibility(View.GONE);
                }).start();
            }
        }
        if (mSeekRightIndicator != null) {
            mSeekRightIndicator.animate().cancel();
            if (!mIsShowing || mRootView == null || mRootView.getParent() == null) {
                mSeekRightIndicator.setVisibility(View.GONE);
            } else if (mSeekRightIndicator.getVisibility() == View.VISIBLE) {
                mSeekRightIndicator.animate().alpha(0f).setDuration(200).withEndAction(() -> {
                    if (mSeekRightIndicator != null) mSeekRightIndicator.setVisibility(View.GONE);
                }).start();
            }
        }
    }

    private void onFloatingWindowTapped() {
        if (mControlsLocked) {
            // When locked: single tap shows controls (with lock button) and auto-hides after timeout
            if (mControlsVisible) {
                hideControls();
            } else {
                showControls();
            }
        } else {
            toggleControls();
        }
    }

    private void toggleLock() {
        applyLockState(!mControlsLocked);
    }

    private void applyLockState(boolean locked) {
        if (mIsDocked) {
            return;
        }
        mControlsLocked = locked;
        mControlsVisible = true;
        SharedPreferences prefs = mAppContext.getSharedPreferences("floating_player_prefs", Context.MODE_PRIVATE);
        boolean lockEnabled = prefs.getBoolean("floating_lock_enabled", true);
        boolean timeEnabled = prefs.getBoolean("floating_time_enabled", true);
        boolean volumeEnabled = prefs.getBoolean("floating_volume_enabled", true);
        boolean fullscreenEnabled = prefs.getBoolean("floating_fullscreen_enabled", true);
        boolean seekEnabled = prefs.getBoolean("floating_seek_enabled", true);

        if (mTopActions != null) {
            mTopActions.setVisibility(View.VISIBLE);
            mTopActions.setAlpha(1f);
        }
        if (mBottomScrim != null) {
            mBottomScrim.setVisibility(View.VISIBLE);
            mBottomScrim.setAlpha(1f);
        }
        if (mLockBtn != null) {
            mLockBtn.setVisibility(lockEnabled ? View.VISIBLE : View.GONE);
            mLockBtn.setImageResource(locked ? R.drawable.ic_float_locked : R.drawable.ic_float_lock);
        }
        if (mHeadphoneBtn != null) {
            mHeadphoneBtn.setVisibility(locked ? View.GONE : View.VISIBLE);
        }
        if (mPipExpandBtn != null) {
            mPipExpandBtn.setVisibility(locked ? View.GONE : View.VISIBLE);
        }
        if (mSettingsBtn != null) {
            mSettingsBtn.setVisibility(locked ? View.GONE : View.VISIBLE);
        }
        if (locked && mSettingsPanel != null) {
            mSettingsPanel.setVisibility(View.GONE);
        }
        if (mCenterControls != null) {
            mCenterControls.setVisibility(locked ? View.GONE : View.VISIBLE);
            if (!locked) {
                mCenterControls.setAlpha(1f);
            }
        }
        if (mRewindBtn != null) {
            mRewindBtn.setVisibility((!locked && seekEnabled) ? View.VISIBLE : View.GONE);
        }
        if (mPlayPauseBtn != null) {
            mPlayPauseBtn.setVisibility(locked ? View.GONE : View.VISIBLE);
        }
        if (mForwardBtn != null) {
            mForwardBtn.setVisibility((!locked && seekEnabled) ? View.VISIBLE : View.GONE);
        }
        if (mTimeText != null) {
            mTimeText.setVisibility(timeEnabled ? View.VISIBLE : View.GONE);
        }
        if (mVolumeBtn != null) {
            mVolumeBtn.setVisibility(volumeEnabled ? View.VISIBLE : View.GONE);
        }
        if (mFullscreenBtn != null) {
            mFullscreenBtn.setVisibility(fullscreenEnabled ? View.VISIBLE : View.GONE);
        }
        if (mBottomBar != null) {
            mBottomBar.setVisibility((timeEnabled || volumeEnabled || fullscreenEnabled) ? View.VISIBLE : View.GONE);
            mBottomBar.setAlpha(1f);
        }
        if (mSeekBar != null) {
            try {
                mSeekBar.setThumb(ContextCompat.getDrawable(mAppContext, R.drawable.bg_float_progress_thumb));
                mSeekBar.setThumbOffset(0);
            } catch (Exception ignored) {
            }
            mSeekBar.setVisibility(View.VISIBLE);
        }
        if (mSwitchLock != null) {
            mSwitchLock.setChecked(locked);
        }
        updateWatermarkVisibility();
        if (!locked) {
            // Unlocked: normal auto-hide behavior
            rescheduleAutoHide();
        } else {
            // Locked: auto-hide controls after a short timeout (3 seconds)
            removeCallbacks(mHideControlsRunnable);
            postDelayed(mHideControlsRunnable, UNLOCK_BTN_TIMEOUT_MS);
        }
    }

    private void toggleMute() {
        setMuteState(!mMuted);
    }

    private void updateVolumeIcon() {
        if (mVolumeBtn != null) {
            mVolumeBtn.setImageResource(mMuted ? R.drawable.ic_float_mute : R.drawable.ic_float_volume);
        }
    }

    private void updateWatermarkVisibility() {
        if (mBrandWatermark != null) {
            SharedPreferences prefs = mAppContext.getSharedPreferences("floating_player_prefs", Context.MODE_PRIVATE);
            boolean enabled = prefs.getBoolean("floating_watermark_enabled", true);
            mBrandWatermark.setVisibility(enabled ? View.VISIBLE : View.GONE);
        }
    }

    private void showControls() {
        if (mIsDocked) {
            return;
        }
        mControlsVisible = true;

        if (mSeekBar != null) {
            try {
                mSeekBar.setThumb(ContextCompat.getDrawable(mAppContext, R.drawable.bg_float_progress_thumb));
                mSeekBar.setThumbOffset(0);
            } catch (Exception ignored) {
            }
        }
        if (mControlsLocked) {
            applyLockState(true);
            return;
        }
        View[] views = new View[] {mTopBar, mScrim, mTopActions, mCenterControls, mBottomBar, mBottomScrim};
        for (View view : views) {
            if (view != null) {
                view.animate().cancel();
                view.setVisibility(View.VISIBLE);
                view.animate().alpha(1f).setDuration(160).start();
            }
        }
        applyLockState(false);
        rescheduleAutoHide();
    }

    private void hideControls() {
        mControlsVisible = false;
        removeCallbacks(mHideControlsRunnable);
        if (mSettingsPanel != null) {
            mSettingsPanel.setVisibility(View.GONE);
        }

        if (mSeekBar != null) {
            try {
                mSeekBar.setThumb(ContextCompat.getDrawable(mAppContext, R.drawable.bg_float_thumb_invisible));
                mSeekBar.setThumbOffset(0);
            } catch (Exception ignored) {
            }
        }
        // Hide overlay controls so only video surface and borderless bottom progress bar remain
        View[] views = new View[] {mTopBar, mScrim, mTopActions, mCenterControls, mBottomBar, mBottomScrim};
        for (View view : views) {
            if (view != null) {
                view.animate().cancel();
                view.animate().alpha(0f).setDuration(200).withEndAction(() -> {
                    if (!mControlsVisible && view != null) {
                        view.setVisibility(View.GONE);
                    }
                }).start();
            }
        }
        // Ensure progress bar is only visible when NOT docked!
        if (mSeekBar != null) {
            mSeekBar.setVisibility(mIsDocked ? View.GONE : View.VISIBLE);
        }
    }

    private void toggleControls() {
        if (mControlsLocked) {
            return;
        }
        if (mControlsVisible) {
            hideControls();
        } else {
            showControls();
        }
    }

    private void rescheduleAutoHide() {
        removeCallbacks(mHideControlsRunnable);
        if (!mAutoHideControls) {
            return;
        }
        if (!mControlsLocked) {
            postDelayed(mHideControlsRunnable, CONTROLS_TIMEOUT_MS);
        }
    }

    private void toggleSettingsPanel() {
        if (mSettingsPanel == null) return;
        if (mSettingsPanel.getVisibility() == View.VISIBLE) {
            hideSettingsPanel();
        } else {
            showSettingsPanel();
        }
    }

    private void showSettingsPanel() {
        if (mSettingsPanel == null) return;
        removeCallbacks(mHideControlsRunnable);
        updateSettingsUi();
        mSettingsPanel.setVisibility(View.VISIBLE);
        mSettingsPanel.bringToFront();
    }

    private void hideSettingsPanel() {
        if (mSettingsPanel == null) return;
        mSettingsPanel.setVisibility(View.GONE);
        if (mAutoHideControls) {
            rescheduleAutoHide();
        }
    }

    private void applySize(int sizeOption) {
        mCurrentSize = sizeOption;
        try {
            mAppContext.getSharedPreferences("loxtube_floating_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putInt("player_size", mCurrentSize)
                    .apply();
        } catch (Exception ignored) {
        }

        if (mRootView == null || mLayoutParams == null) return;
        if (mIsDocked) return;

        float density = mAppContext.getResources().getDisplayMetrics().density;
        int screenW = mAppContext.getResources().getDisplayMetrics().widthPixels;
        int screenH = mAppContext.getResources().getDisplayMetrics().heightPixels;

        int widthDp = (sizeOption == SIZE_SMALL) ? 200 : (sizeOption == SIZE_LARGE ? 280 : 240);
        int heightDp = (sizeOption == SIZE_SMALL) ? 112 : (sizeOption == SIZE_LARGE ? 158 : 135);
        if (!mInAppMode) {
            widthDp = (sizeOption == SIZE_SMALL) ? 215 : (sizeOption == SIZE_LARGE ? 300 : 265);
            heightDp = (sizeOption == SIZE_SMALL) ? 122 : (sizeOption == SIZE_LARGE ? 170 : 150);
        }

        int widthPx = Math.round(widthDp * density);
        int heightPx = Math.round(heightDp * density);
        int marginEndPx = mInAppMode ? Math.round(12 * density) : 0;
        int bottomNavInset = mInAppMode ? Math.round(58 * density) : 0;
        int maxY = Math.max(0, screenH - heightPx - bottomNavInset);
        int maxX = Math.max(0, screenW - widthPx - marginEndPx);

        mLayoutParams.width = widthPx;
        mLayoutParams.height = heightPx;
        mLayoutParams.x = Math.max(0, Math.min(maxX, mLayoutParams.x));
        mLayoutParams.y = Math.max(0, Math.min(maxY, mLayoutParams.y));

        if (mIsShowing && mRootView.getParent() != null) {
            try {
                mWindowManager.updateViewLayout(mRootView, mLayoutParams);
            } catch (Exception ignored) {
            }
        }
        updateSettingsUi();
    }

    private void setAutoHideControls(boolean autoHide) {
        mAutoHideControls = autoHide;
        try {
            mAppContext.getSharedPreferences("loxtube_floating_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("controls_autohide", mAutoHideControls)
                    .apply();
        } catch (Exception ignored) {
        }
        updateSettingsUi();
        if (mAutoHideControls) {
            rescheduleAutoHide();
        } else {
            removeCallbacks(mHideControlsRunnable);
            showControls();
        }
    }

    private void selectSettingsTab(int tabIndex) {
        mSelectedSettingsTab = tabIndex;
        if (mTabDisplayText != null) {
            mTabDisplayText.setTextColor(tabIndex == 0 ? 0xFFFFFFFF : 0x70FFFFFF);
        }
        if (mTabDisplayIndicator != null) {
            mTabDisplayIndicator.setVisibility(tabIndex == 0 ? View.VISIBLE : View.INVISIBLE);
        }
        if (mTabAudioText != null) {
            mTabAudioText.setTextColor(tabIndex == 1 ? 0xFFFFFFFF : 0x70FFFFFF);
        }
        if (mTabAudioIndicator != null) {
            mTabAudioIndicator.setVisibility(tabIndex == 1 ? View.VISIBLE : View.INVISIBLE);
        }
        if (mTabControlsText != null) {
            mTabControlsText.setTextColor(tabIndex == 2 ? 0xFFFFFFFF : 0x70FFFFFF);
        }
        if (mTabControlsIndicator != null) {
            mTabControlsIndicator.setVisibility(tabIndex == 2 ? View.VISIBLE : View.INVISIBLE);
        }

        if (mPanelTabDisplay != null) {
            mPanelTabDisplay.setVisibility(tabIndex == 0 ? View.VISIBLE : View.GONE);
        }
        if (mPanelTabAudio != null) {
            mPanelTabAudio.setVisibility(tabIndex == 1 ? View.VISIBLE : View.GONE);
        }
        if (mPanelTabControls != null) {
            mPanelTabControls.setVisibility(tabIndex == 2 ? View.VISIBLE : View.GONE);
        }
    }

    private void cyclePlaybackSpeed() {
        ExoPlayer player = getActivePlayer();
        float currentSpeed = 1.0f;
        if (player != null) {
            currentSpeed = player.getPlaybackParameters().speed;
        }
        int nextIndex = 1;
        for (int i = 0; i < PLAYBACK_SPEEDS.length; i++) {
            if (Math.abs(PLAYBACK_SPEEDS[i] - currentSpeed) < 0.05f) {
                nextIndex = (i + 1) % PLAYBACK_SPEEDS.length;
                break;
            }
        }
        float newSpeed = PLAYBACK_SPEEDS[nextIndex];
        if (player != null) {
            player.setPlaybackParameters(new PlaybackParameters(newSpeed));
        }
        updateSpeedUi(newSpeed);
    }

    private void updateSpeedUi(float speed) {
        if (mSpeedPillBtn != null) {
            String text;
            if (Math.abs(speed - 0.75f) < 0.05f) {
                text = "0.75x ▾";
            } else if (Math.abs(speed - 1.0f) < 0.05f) {
                text = "1.0x ▾";
            } else if (Math.abs(speed - 1.25f) < 0.05f) {
                text = "1.25x ▾";
            } else if (Math.abs(speed - 1.5f) < 0.05f) {
                text = "1.5x ▾";
            } else if (Math.abs(speed - 2.0f) < 0.05f) {
                text = "2.0x ▾";
            } else {
                text = String.format(Locale.US, "%.2fx ▾", speed);
            }
            mSpeedPillBtn.setText(text);
        }
    }

    private void applyVolumePercent(int percent) {
        mVolumePercent = Math.max(0, Math.min(100, percent));
        mMuted = (mVolumePercent == 0);
        ExoPlayer player = getActivePlayer();
        if (player != null) {
            player.setVolume(mMuted ? 0f : (mVolumePercent / 100f));
        }
        updateVolumeIcon();
        if (mVolumeText != null) {
            mVolumeText.setText(mVolumePercent + "%");
        }
        if (mSwitchMute != null) {
            mSwitchMute.setChecked(mMuted);
        }
    }

    private void setMuteState(boolean mute) {
        mMuted = mute;
        ExoPlayer player = getActivePlayer();
        if (player != null) {
            player.setVolume(mMuted ? 0f : (mVolumePercent / 100f));
        }
        updateVolumeIcon();
        if (mVolumeSeekBar != null) {
            mVolumeSeekBar.setProgress(mMuted ? 0 : mVolumePercent);
        }
        if (mVolumeText != null) {
            mVolumeText.setText((mMuted ? 0 : mVolumePercent) + "%");
        }
        if (mSwitchMute != null) {
            mSwitchMute.setChecked(mMuted);
        }
    }

    private void updateSettingsUi() {
        if (mSizeSmallBtn != null) {
            mSizeSmallBtn.setBackgroundResource(mCurrentSize == SIZE_SMALL
                    ? R.drawable.bg_float_setting_pill_active
                    : R.drawable.bg_float_setting_pill_inactive);
        }
        if (mSizeMediumBtn != null) {
            mSizeMediumBtn.setBackgroundResource(mCurrentSize == SIZE_MEDIUM
                    ? R.drawable.bg_float_setting_pill_active
                    : R.drawable.bg_float_setting_pill_inactive);
        }
        if (mSizeLargeBtn != null) {
            mSizeLargeBtn.setBackgroundResource(mCurrentSize == SIZE_LARGE
                    ? R.drawable.bg_float_setting_pill_active
                    : R.drawable.bg_float_setting_pill_inactive);
        }

        ExoPlayer player = getActivePlayer();
        float speed = 1.0f;
        if (player != null) {
            speed = player.getPlaybackParameters().speed;
        }
        updateSpeedUi(speed);

        if (mVolumeSeekBar != null) {
            mVolumeSeekBar.setProgress(mMuted ? 0 : mVolumePercent);
        }
        if (mVolumeText != null) {
            mVolumeText.setText((mMuted ? 0 : mVolumePercent) + "%");
        }
        if (mSwitchMute != null) {
            mSwitchMute.setChecked(mMuted);
        }

        if (mSwitchAutoHide != null) {
            mSwitchAutoHide.setChecked(mAutoHideControls);
        }
        if (mSwitchLock != null) {
            mSwitchLock.setChecked(mControlsLocked);
        }

        selectSettingsTab(mSelectedSettingsTab);
    }

    public void openFullPlayer(boolean forceLandscapeFullscreen) {
        try {
            Intent intent = new Intent(mAppContext, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            mAppContext.startActivity(intent);
            hide(false);
            EnhancedPlayerManager.getInstance().reattachVideoSurface();
            EnhancedPlayerManager.getInstance().restoreVideoOutput();
        } catch (Exception ignored) {
        }
    }

    private void closeAndStop() {
        hide(false);
        try {
            EnhancedPlayerManager.getInstance().stop();
        } catch (Exception ignored) {
        }
    }

    private void attachTextureAndPlayer(ExoPlayer player) {
        player.removeListener(mPlayerListener);
        player.addListener(mPlayerListener);

        if (mFrame == null) {
            return;
        }
        if (mTextureView != null && mTextureView.getParent() != null) {
            mFrame.removeView(mTextureView);
        }

        mTextureView = new TextureView(mAppContext);
        mTextureView.setOpaque(false);
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                if (mFloatingSurface != null) {
                    try {
                        mFloatingSurface.release();
                    } catch (Exception ignored) {
                    }
                }
                mFloatingSurface = new Surface(surface);
                ExoPlayer active = getActivePlayer();
                if (active != null) {
                    active.setVideoSurface(mFloatingSurface);
                    try {
                        EnhancedPlayerManager.getInstance().restoreVideoOutput();
                    } catch (Exception ignored) {
                    }
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                if (mFloatingSurface != null) {
                    try {
                        mFloatingSurface.release();
                    } catch (Exception ignored) {
                    }
                    mFloatingSurface = null;
                }
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                if (mFreeze != null && mFreeze.getVisibility() == View.VISIBLE) {
                    mFreeze.setVisibility(View.GONE);
                }
            }
        });

        mFrame.addView(mTextureView, 0, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        applyZoomMode();

        if (mTextureView.isAvailable()) {
            SurfaceTexture surface = mTextureView.getSurfaceTexture();
            if (surface != null) {
                if (mFloatingSurface != null) {
                    try {
                        mFloatingSurface.release();
                    } catch (Exception ignored) {
                    }
                }
                mFloatingSurface = new Surface(surface);
                player.setVideoSurface(mFloatingSurface);
                try {
                    EnhancedPlayerManager.getInstance().restoreVideoOutput();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public void ensureVideoSurfaceAttached() {
        sHandler.post(() -> {
            ExoPlayer active = getActivePlayer();
            if (active == null) {
                return;
            }
            try {
                EnhancedPlayerManager.getInstance().restoreVideoOutput();
            } catch (Exception ignored) {
            }
            if (mTextureView != null && mTextureView.isAvailable()) {
                SurfaceTexture st = mTextureView.getSurfaceTexture();
                if (st != null) {
                    if (mFloatingSurface == null || !mFloatingSurface.isValid()) {
                        mFloatingSurface = new Surface(st);
                    }
                    active.setVideoSurface(mFloatingSurface);
                }
            } else if (mTextureView == null || mTextureView.getParent() == null) {
                attachTextureAndPlayer(active);
            }
        });
    }

    private void updateAspectRatio(VideoSize videoSize) {
        if (mFrame == null) {
            return;
        }
        float ratio = 16f / 9f;
        if (videoSize != null && videoSize.width > 0 && videoSize.height > 0) {
            ratio = (videoSize.width * videoSize.pixelWidthHeightRatio) / videoSize.height;
            mFrame.setAspectRatio(ratio);
        } else {
            mFrame.setAspectRatio(16f / 9f);
        }
        applyZoomMode();

        if (!mIsDocked && !mIsCardMode && !mIsRelatedListOpen && mLayoutParams != null && mLayoutParams.width > 0 && ratio > 0.5f && ratio < 3.0f) {
            int desiredHeight = Math.round(mLayoutParams.width / ratio);
            if (desiredHeight > 50 && Math.abs(mLayoutParams.height - desiredHeight) > 3) {
                mLayoutParams.height = desiredHeight;
                mNormalHeightPx = desiredHeight;
                if (mVideoContent != null) {
                    android.view.ViewGroup.LayoutParams vLp = mVideoContent.getLayoutParams();
                    if (vLp != null) {
                        vLp.height = desiredHeight;
                        mVideoContent.setLayoutParams(vLp);
                    }
                }
                try {
                    mWindowManager.updateViewLayout(mRootView, mLayoutParams);
                } catch (Exception ignored) {
                }
            }
        }
    }

    public void setZoomMode(boolean zoom) {
        mIsZoomMode = zoom;
        applyZoomMode();
    }

    private void toggleZoomMode() {
        setZoomMode(!mIsZoomMode);
        try {
            Toast.makeText(mAppContext, mIsZoomMode ? "Borderless (Zoom)" : "Original (Fit)", Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {
        }
    }

    private void applyZoomMode() {
        if (mTextureView != null) {
            float scale = mIsZoomMode ? 1.33f : 1.0f;
            mTextureView.setScaleX(scale);
            mTextureView.setScaleY(scale);
        }
        if (mFrame != null) {
            mFrame.setResizeMode(mIsZoomMode
                    ? AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    : AspectRatioFrameLayout.RESIZE_MODE_FIT);
        }
        if (mFreeze != null) {
            mFreeze.setScaleType(mIsZoomMode ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.FIT_CENTER);
        }
        updateZoomPills();
    }

    private void updateZoomPills() {
        if (mZoomFitBtn != null) {
            mZoomFitBtn.setBackgroundResource(!mIsZoomMode
                    ? R.drawable.bg_float_setting_pill_active
                    : R.drawable.bg_float_setting_pill_inactive);
        }
        if (mZoomCropBtn != null) {
            mZoomCropBtn.setBackgroundResource(mIsZoomMode
                    ? R.drawable.bg_float_setting_pill_active
                    : R.drawable.bg_float_setting_pill_inactive);
        }
    }

    private void onTick() {
        if (!mIsShowing) {
            return;
        }
        ExoPlayer player = getActivePlayer();
        if (player == null) {
            hide(false);
            return;
        }

        long duration = player.getDuration();
        long current = player.getCurrentPosition();
        if (duration > 0) {
            int progress = (int) (1000L * current / duration);
            if (mSeekBar != null) {
                mSeekBar.setVisibility(mIsDocked ? View.GONE : View.VISIBLE);
                if (!mIsUserSeeking) {
                    mSeekBar.setProgress(progress);
                }
            }
            updateTimeDisplay(current, duration);
        } else if (mTimeText != null) {
            mTimeText.setText("");
        }

        updatePlayPause(player);
        updateCardUiTick(player, current, duration);
        postDelayed(mTickRunnable, TICK_MS);
    }

    private void updateCardUiTick(ExoPlayer player, long current, long duration) {
        if (player == null) return;
        Video curVideo = GlobalPlayerState.INSTANCE.getCurrentVideo().getValue();
        if (curVideo != null && !curVideo.getId().equals(mLastLoadedVideoId)) {
            mLastLoadedVideoId = curVideo.getId();
            updateCardMetadata(curVideo);
        }
        if (duration > 0) {
            int progress = (int) (1000L * current / duration);
            if (mCardProgressBar != null && !mIsUserSeeking) {
                mCardProgressBar.setProgress(progress);
            }
            if (mCardTimeLeft != null && !mIsUserSeeking) {
                mCardTimeLeft.setText(formatTime(current));
            }
            if (mCardTimeRight != null) {
                mCardTimeRight.setText(formatTime(duration));
            }
        }
    }

    private void updatePlayPause(ExoPlayer player) {
        if (player == null) return;
        boolean playing = player.getPlayWhenReady()
                && player.getPlaybackState() != Player.STATE_ENDED
                && player.getPlaybackState() != Player.STATE_IDLE;
        if (mPlayPauseBtn != null) {
            mPlayPauseBtn.setImageResource(playing ? R.drawable.ic_float_pause : R.drawable.ic_float_play);
        }
        if (mCardPlayPauseBtn != null) {
            mCardPlayPauseBtn.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
        }
        if (mCardEqualizer != null) {
            mCardEqualizer.setPlaying(playing);
        }
        return;
    }
    private void toggleViewMode() {
        mIsCardMode = !mIsCardMode;
        if (mCardContent != null) {
            mCardContent.setVisibility(mIsCardMode ? View.VISIBLE : View.GONE);
        }
        if (mVideoContent != null) {
            mVideoContent.setVisibility(mIsCardMode ? View.GONE : View.VISIBLE);
        }
        float density = mAppContext.getResources().getDisplayMetrics().density;
        int screenW = mAppContext.getResources().getDisplayMetrics().widthPixels;
        int screenH = mAppContext.getResources().getDisplayMetrics().heightPixels;
        int baseWidth = mIsCardMode ? Math.min(Math.round(screenW - 24 * density), Math.round(350 * density)) : Math.round(260 * density);
        int baseHeight = mIsCardMode ? Math.round(152 * density) : Math.round(146 * density);
        mLayoutParams.width = baseWidth;
        mNormalHeightPx = baseHeight;
        mLayoutParams.height = mIsRelatedListOpen ? (baseHeight + Math.round(285 * density)) : baseHeight;
        if (mVideoContent != null) {
            android.view.ViewGroup.LayoutParams vLp = mVideoContent.getLayoutParams();
            if (vLp != null) {
                vLp.height = baseHeight;
                mVideoContent.setLayoutParams(vLp);
            }
        }
        try {
            if (mRootView != null && mRootView.getParent() != null) {
                mWindowManager.updateViewLayout(mRootView, mLayoutParams);
            }
        } catch (Exception ignored) {
        }
    }

    public void showWithRelated(boolean requestPermissionIfMissing) {
        show(requestPermissionIfMissing);
        ensureVideoSurfaceAttached();
        postDelayed(() -> setRelatedListOpen(true), 250);
    }

    private void toggleRelatedList() {
        setRelatedListOpen(!mIsRelatedListOpen);
    }

    private void setRelatedListOpen(boolean open) {
        mIsRelatedListOpen = open;
        mIsCardMode = false;
        if (mCardContent != null) {
            mCardContent.setVisibility(View.GONE);
        }
        if (mVideoContent != null) {
            mVideoContent.setVisibility(View.VISIBLE);
        }
        if (mRelatedContainer != null) {
            mRelatedContainer.setVisibility(open ? View.VISIBLE : View.GONE);
        }
        if (mLayoutParams != null && mRootView != null && mRootView.getParent() != null) {
            float density = mAppContext.getResources().getDisplayMetrics().density;
            int screenW = mAppContext.getResources().getDisplayMetrics().widthPixels;
            int screenH = mAppContext.getResources().getDisplayMetrics().heightPixels;

            if (open) {
                // Expanded mode matching Image 1: full width video player on top + related videos list below
                int fullWidth = Math.max(Math.round(280 * density), screenW - Math.round(16 * density));
                int videoHeight = Math.round(fullWidth * 9f / 16f);
                int relatedHeight = Math.min(Math.round(380 * density), screenH - videoHeight - Math.round(80 * density));
                int totalHeight = videoHeight + relatedHeight;

                mLayoutParams.width = fullWidth;
                mLayoutParams.height = totalHeight;
                mNormalHeightPx = totalHeight;

                mLayoutParams.x = Math.max(0, (screenW - fullWidth) / 2);
                mLayoutParams.y = Math.max(0, Math.round(32 * density));

                if (mVideoContent != null) {
                    android.view.ViewGroup.LayoutParams vLp = mVideoContent.getLayoutParams();
                    if (vLp != null) {
                        vLp.height = videoHeight;
                        mVideoContent.setLayoutParams(vLp);
                    }
                }
                if (mRelatedContainer != null) {
                    android.view.ViewGroup.LayoutParams rLp = mRelatedContainer.getLayoutParams();
                    if (rLp != null) {
                        rLp.height = relatedHeight;
                        mRelatedContainer.setLayoutParams(rLp);
                    }
                }
            } else {
                // Collapsed mode: return to normal small floating video player
                int normalWidth = Math.round(240 * density);
                int normalHeight = Math.round(135 * density);

                mLayoutParams.width = normalWidth;
                mLayoutParams.height = normalHeight;
                mNormalHeightPx = normalHeight;

                if (mVideoContent != null) {
                    android.view.ViewGroup.LayoutParams vLp = mVideoContent.getLayoutParams();
                    if (vLp != null) {
                        vLp.height = normalHeight;
                        mVideoContent.setLayoutParams(vLp);
                    }
                }

                int maxX = Math.max(0, screenW - normalWidth);
                int maxY = Math.max(0, screenH - normalHeight);
                mLayoutParams.x = Math.max(0, Math.min(maxX, mLayoutParams.x));
                mLayoutParams.y = Math.max(0, Math.min(maxY, mLayoutParams.y));
            }

            try {
                mWindowManager.updateViewLayout(mRootView, mLayoutParams);
            } catch (Exception ignored) {
            }
        }

        if (open) {
            loadRelatedVideos();
        }
        ensureVideoSurfaceAttached();
    }

    private void loadRelatedVideos() {
        mRelatedSeedIndex = 0;
        mIsLoadingMoreRelated = false;
        if (mRelatedAdapter == null) return;
        List<Video> candidates = EnhancedPlayerManager.getInstance().getCurrentRelatedCandidates();
        if (candidates != null && !candidates.isEmpty()) {
            mRelatedAdapter.submitList(candidates);
            return;
        }

        Video cur = GlobalPlayerState.INSTANCE.getCurrentVideo().getValue();
        String videoId = cur != null ? cur.getId() : EnhancedPlayerManager.getInstance().getPlayerState().getValue().getCurrentVideoId();
        if (videoId != null && !videoId.isEmpty()) {
            EnhancedPlayerManager.getInstance().loadRelatedForFloating(videoId, videos -> {
                if (mRelatedAdapter != null && videos != null) {
                    mRelatedAdapter.submitList(videos);
                }
            });
        }
    }

    private void loadMoreRelatedVideos() {
        if (mRelatedAdapter == null || mIsLoadingMoreRelated) return;
        List<Video> currentList = mRelatedAdapter.getVideos();
        if (currentList == null || currentList.isEmpty()) return;

        if (mRelatedSeedIndex >= currentList.size()) {
            mRelatedSeedIndex = 0;
        }
        Video seedVideo = currentList.get(mRelatedSeedIndex);
        mRelatedSeedIndex++;

        mIsLoadingMoreRelated = true;
        EnhancedPlayerManager.getInstance().loadRelatedForFloating(seedVideo.getId(), moreVideos -> {
            mIsLoadingMoreRelated = false;
            if (mRelatedAdapter != null && moreVideos != null && !moreVideos.isEmpty()) {
                mRelatedAdapter.appendVideos(moreVideos);
            }
        });
    }

    private void updateCardMetadata(@Nullable Video video) {
        if (video == null) return;
        if (mCardTitle != null) {
            mCardTitle.setText(video.getTitle());
        }
        if (mCardChannel != null) {
            mCardChannel.setText(video.getChannelName());
        }
        if (mCardThumb != null) {
            FloatingImageLoader.loadThumbnail(mCardThumb, video.getThumbnailUrl());
        }
    }

    private void dummyMethod(ExoPlayer player) {
        if (true) return;
        if (mPlayPauseBtn == null) {
            return;
        }
        boolean playing = player.getPlayWhenReady()
                && player.getPlaybackState() != Player.STATE_ENDED
                && player.getPlaybackState() != Player.STATE_IDLE;
        mPlayPauseBtn.setImageResource(playing ? R.drawable.ic_float_pause : R.drawable.ic_float_play);
    }

    private void updateTimeDisplay(long currentMs, long durationMs) {
        if (mTimeText != null) {
            mTimeText.setText(formatTime(currentMs) + " / " + formatTime(durationMs));
        }
    }

    private static String formatTime(long millis) {
        if (millis < 0) {
            millis = 0;
        }
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        seconds %= 60;
        minutes %= 60;
        if (hours > 0) {
            return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.US, "%d:%02d", minutes, seconds);
        }
    }
}
