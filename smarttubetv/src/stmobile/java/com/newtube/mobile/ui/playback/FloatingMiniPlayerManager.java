package com.newtube.mobile.ui.playback;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
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
import androidx.core.content.ContextCompat;
import androidx.media3.common.Player;
import androidx.media3.common.VideoSize;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.service.VideoStateService;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.Locale;

/**
 * Manages the system-wide movable/draggable floating overlay mini-player
 * ({@code R.layout.mobile_floating_mini_player}) shown outside the app or toggled
 * from the Notification Bar's PiP / Headphone action button.
 */
public final class FloatingMiniPlayerManager {
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
    private ImageButton mCloseBtn;
    private ImageButton mRewindBtn;
    private ImageButton mPlayPauseBtn;
    private ImageButton mForwardBtn;
    private ImageButton mVolumeBtn;
    private ImageButton mDockTabBtn;
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
        Utils.postDelayed(mDimDockTabRunnable, 10000L);
    }

    private void cancelDimDockTab() {
        Utils.removeCallbacks(mDimDockTabRunnable);
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
                    Utils.postDelayed(FloatingMiniPlayerManager.this::updateInAppMode, 200);
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
        return mForegroundActivities > 0 || Utils.isAppInForegroundFixed();
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
            if (!inApp) {
                // When app goes to background while docked, keep it tucked at screen edge
                return;
            } else {
                // When re-entering the app from outside while docked, restore mini player inside app
                undockToScreen();
                return;
            }
        }

        float density = mAppContext.getResources().getDisplayMetrics().density;
        int screenW = mAppContext.getResources().getDisplayMetrics().widthPixels;
        int screenH = mAppContext.getResources().getDisplayMetrics().heightPixels;

        com.google.android.material.card.MaterialCardView card =
                mRootView.findViewById(R.id.floating_mini_card);

        if (inApp) {
            // In-app mode: 240dp x 135dp (16:9), hovering above bottom navigation bar with rounded corners
            int widthPx = Math.round(240 * density);
            int heightPx = Math.round(135 * density);
            int marginEndPx = Math.round(12 * density);
            int marginBottomPx = Math.round(58 * density);
            int maxY = Math.max(0, screenH - heightPx - marginBottomPx);

            mLayoutParams.width = widthPx;
            mLayoutParams.height = heightPx;
            if (changed || mLayoutParams.x < 0 || mLayoutParams.y < 0) {
                mLayoutParams.x = Math.max(0, screenW - widthPx - marginEndPx);
                mLayoutParams.y = maxY;
            } else if (mLayoutParams.y > maxY) {
                mLayoutParams.y = maxY;
            }

            if (card != null) {
                card.setRadius(Math.round(8 * density));
                card.setCardBackgroundColor(Color.BLACK);
            }
        } else {
            // Outside the app: Standard 265dp x 150dp floating mini-player
            int widthPx = Math.round(265 * density);
            int heightPx = Math.round(150 * density);
            mLayoutParams.width = widthPx;
            mLayoutParams.height = heightPx;

            if (card != null) {
                card.setRadius(0f);
                card.setCardBackgroundColor(Color.BLACK);
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
        MobilePlaybackActivity activity = MobilePlaybackActivity.getCurrentInstance();
        if (activity != null && activity.getSharedPlayer() != null) {
            return activity.getSharedPlayer();
        }
        return MiniPlayerBridge.getPlayer();
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
            return true;
        }

        ensureViewCreated();

        updateInAppMode();

        applyLockState(false);

        attachTextureAndPlayer(player);

        try {
            if (mRootView.getParent() == null) {
                mWindowManager.addView(mRootView, mLayoutParams);
            }
            mIsShowing = true;
            // Keep screen on while playing
            updateScreenOnFlag();
        } catch (Exception e) {
            return false;
        }

        mIsDocked = false;
        if (mDockTabBtn != null) {
            mDockTabBtn.setVisibility(View.GONE);
        }
        if (mFrame != null) {
            mFrame.setVisibility(View.VISIBLE);
        }
        showControls();
        updatePlayPause(player);
        updateAspectRatio(player.getVideoSize());

        Utils.removeCallbacks(mTickRunnable);
        Utils.postDelayed(mTickRunnable, 0);

        syncNotificationIcon();
        return true;
    }

    /**
     * Hide the floating overlay mini-player while keeping background audio alive (unless stopping).
     */
    public void hide(boolean notifyAudioActive) {
        Utils.removeCallbacks(mTickRunnable);
        Utils.removeCallbacks(mHideControlsRunnable);
        Utils.removeCallbacks(mSingleTapRunnable);
        Utils.removeCallbacks(mHideSeekIndicatorRunnable);
        hideSeekIndicators();
        cancelDimDockTab();
        mIsDocked = false;
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
                MiniPlayerBridge.setResumePosition(player.getCurrentPosition());
            }
        }

        // Restore the main player's session surface before releasing our floating surface
        MobilePlaybackActivity activity = MobilePlaybackActivity.getCurrentInstance();
        if (activity != null) {
            activity.restoreSessionSurface();
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
        mControlsLocked = false;

        syncNotificationIcon();
    }

    private void syncNotificationIcon() {
        MobilePlaybackService service = MobilePlaybackService.getInstance();
        if (service != null) {
            service.refreshNotificationActions();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void ensureViewCreated() {
        if (mRootView != null) {
            return;
        }

        Context themedContext = new ContextThemeWrapper(mAppContext, R.style.Theme_NewTube);
        mRootView = LayoutInflater.from(themedContext).inflate(R.layout.mobile_floating_mini_player, null, false);

        float density = mAppContext.getResources().getDisplayMetrics().density;
        int widthPx = Math.round(265 * density);
        int heightPx = Math.round(150 * density);
        int screenW = mAppContext.getResources().getDisplayMetrics().widthPixels;
        int screenH = mAppContext.getResources().getDisplayMetrics().heightPixels;

        int overlayType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        mLayoutParams = new WindowManager.LayoutParams(
                widthPx,
                heightPx,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        mLayoutParams.gravity = Gravity.TOP | Gravity.START;
        mLayoutParams.x = Math.max(0, screenW - widthPx - Math.round(16 * density));
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
        mCloseBtn = mRootView.findViewById(R.id.floating_mini_close);
        mRewindBtn = mRootView.findViewById(R.id.floating_mini_rewind);
        mPlayPauseBtn = mRootView.findViewById(R.id.floating_mini_play_pause);
        mForwardBtn = mRootView.findViewById(R.id.floating_mini_forward);
        mVolumeBtn = mRootView.findViewById(R.id.floating_mini_volume);
        mFullscreenBtn = mRootView.findViewById(R.id.floating_mini_fullscreen);
        mSeekBar = mRootView.findViewById(R.id.floating_mini_progress);
        mTimeText = mRootView.findViewById(R.id.floating_mini_time);
        mDockTabBtn = mRootView.findViewById(R.id.floating_mini_dock_tab);
        mSeekLeftIndicator = mRootView.findViewById(R.id.floating_mini_seek_left);
        mSeekLeftText = mRootView.findViewById(R.id.floating_mini_seek_left_text);
        mSeekRightIndicator = mRootView.findViewById(R.id.floating_mini_seek_right);
        mSeekRightText = mRootView.findViewById(R.id.floating_mini_seek_right_text);

        if (mDockTabBtn != null) {
            mDockTabBtn.setOnClickListener(v -> undockToScreen());
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
        if (mCloseBtn != null) {
            mCloseBtn.setOnClickListener(v -> closeAndStop());
        }
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
                MobilePlaybackActivity activity = MobilePlaybackActivity.getCurrentInstance();
                if (activity != null) {
                    activity.playPrevious();
                } else {
                    com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter presenter =
                            com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter.instance(mAppContext);
                    if (presenter != null) {
                        presenter.onPreviousClicked();
                    }
                }
                rescheduleAutoHide();
            });
        }

        if (mForwardBtn != null) {
            mForwardBtn.setOnClickListener(v -> {
                MobilePlaybackActivity activity = MobilePlaybackActivity.getCurrentInstance();
                if (activity != null) {
                    activity.playNext();
                } else {
                    com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter presenter =
                            com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter.instance(mAppContext);
                    if (presenter != null) {
                        presenter.onNextClicked();
                    }
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
                    Utils.removeCallbacks(mHideControlsRunnable);
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    mIsUserSeeking = false;
                    ExoPlayer player = getActivePlayer();
                    if (player != null && player.getDuration() > 0) {
                        long newPos = (player.getDuration() * seekBar.getProgress()) / 1000L;
                        player.seekTo(newPos);
                        MiniPlayerBridge.setResumePosition(newPos);
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
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        mInitialX = mLayoutParams.x;
                        mInitialY = mLayoutParams.y;
                        mDownRawX = event.getRawX();
                        mDownRawY = event.getRawY();
                        mDragging = false;
                        if (mIsDocked) {
                            cancelDimDockTab();
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE: {
                        float dx = event.getRawX() - mDownRawX;
                        float dy = event.getRawY() - mDownRawY;
                        if (!mDragging && (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop)) {
                            mDragging = true;
                        }
                        if (mDragging && mIsShowing && mRootView != null && mRootView.getParent() != null) {
                            float density = mAppContext.getResources().getDisplayMetrics().density;
                            int screenW = mAppContext.getResources().getDisplayMetrics().widthPixels;
                            int screenH = mAppContext.getResources().getDisplayMetrics().heightPixels;
                            int bottomNavInset = mInAppMode ? Math.round(58 * density) : 0;
                            int maxY = Math.max(0, screenH - mLayoutParams.height - bottomNavInset);

                            if (mIsDocked) {
                                // While docked, user can drag the tab vertically AND horizontally anywhere
                                int dockTabWidth = Math.round(36 * density);
                                int maxX = screenW - dockTabWidth;
                                mLayoutParams.x = Math.max(0, Math.min(maxX, mInitialX + Math.round(dx)));
                                mLayoutParams.y = Math.max(0, Math.min(maxY, mInitialY + Math.round(dy)));
                                try {
                                    mWindowManager.updateViewLayout(mRootView, mLayoutParams);
                                } catch (Exception ignored) {
                                }
                                return true;
                            }

                            int dockTabWidth = Math.round(34 * density);
                            int minX = -(mLayoutParams.width - dockTabWidth);
                            int maxX = screenW - dockTabWidth;
                            mLayoutParams.x = Math.max(minX, Math.min(maxX, mInitialX + Math.round(dx)));
                            mLayoutParams.y = Math.max(0, Math.min(maxY, mInitialY + Math.round(dy)));
                            try {
                                mWindowManager.updateViewLayout(mRootView, mLayoutParams);
                            } catch (Exception ignored) {
                            }
                        }
                        return true;
                    }
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (mDragging) {
                            float density = mAppContext.getResources().getDisplayMetrics().density;
                            int screenH = mAppContext.getResources().getDisplayMetrics().heightPixels;
                            int screenW = mAppContext.getResources().getDisplayMetrics().widthPixels;
                            int bottomDismissThreshold = screenH - Math.round(110 * density);

                            // Drag to bottom center: dismiss floating window, pause video, preserve notification card
                            if (mLayoutParams.y >= bottomDismissThreshold || event.getRawY() >= screenH - Math.round(80 * density)) {
                                dismissToBottom();
                                mDragging = false;
                                return true;
                            }

                            if (mIsDocked) {
                                mDragging = false;
                                int dockTabWidth = Math.round(36 * density);
                                boolean snapToLeft = (mLayoutParams.x + dockTabWidth / 2) < (screenW / 2);
                                dockToSide(snapToLeft);
                                return true;
                            }

                            int bottomNavInset = mInAppMode ? Math.round(58 * density) : 0;
                            int maxY = Math.max(0, screenH - mLayoutParams.height - bottomNavInset);
                            mLayoutParams.y = Math.max(0, Math.min(maxY, mLayoutParams.y));

                            // Snap to edge-dock if dragged towards either edge
                            if (mLayoutParams.x > screenW - Math.round(mLayoutParams.width * 0.70f)) {
                                dockToSide(false);
                            } else if (mLayoutParams.x < -Math.round(mLayoutParams.width * 0.30f)) {
                                dockToSide(true);
                            } else {
                                mIsDocked = false;
                                if (mDockTabBtn != null) {
                                    mDockTabBtn.setVisibility(View.GONE);
                                }
                                int maxX = Math.max(0, screenW - mLayoutParams.width);
                                int targetX = (mLayoutParams.x + mLayoutParams.width / 2) < (screenW / 2) ? 0 : maxX;
                                mLayoutParams.x = targetX;
                                try {
                                    mWindowManager.updateViewLayout(mRootView, mLayoutParams);
                                } catch (Exception ignored) {
                                }
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
                    default:
                        return false;
                }
            }
        };

        View touchArea = mRootView.findViewById(R.id.floating_mini_touch_area);
        if (touchArea != null) {
            touchArea.setOnTouchListener(dragAndTapListener);
        }
        mRootView.setOnTouchListener(dragAndTapListener);
        if (mFrame != null) {
            mFrame.setOnTouchListener(dragAndTapListener);
        }
        if (mScrim != null) {
            mScrim.setOnTouchListener(dragAndTapListener);
        }
        if (mDockTabBtn != null) {
            mDockTabBtn.setOnTouchListener(dragAndTapListener);
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
        if (mSeekBar != null) mSeekBar.setVisibility(View.GONE);
        if (mFrame != null) mFrame.setVisibility(View.GONE);

        float density = mAppContext.getResources().getDisplayMetrics().density;
        int dockTabWidth = Math.round(36 * density);
        int dockTabHeight = Math.round(52 * density);
        int screenW = mAppContext.getResources().getDisplayMetrics().widthPixels;

        if (mSavedNormalWidth <= 0 || mLayoutParams.width > dockTabWidth) {
            mSavedNormalWidth = mLayoutParams.width;
            mSavedNormalHeight = mLayoutParams.height;
        }

        if (mDockTabBtn != null) {
            ViewGroup.LayoutParams rawLp = mDockTabBtn.getLayoutParams();
            if (rawLp instanceof FrameLayout.LayoutParams) {
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) rawLp;
                lp.gravity = Gravity.CENTER;
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                mDockTabBtn.setLayoutParams(lp);
            }
            mDockTabBtn.setImageResource(left ? R.drawable.ic_float_dock_arrow_right : R.drawable.ic_float_dock_arrow_left);
            mDockTabBtn.setVisibility(View.VISIBLE);
            mDockTabBtn.setAlpha(1.0f);
            mDockTabBtn.bringToFront();
        }

        mLayoutParams.width = dockTabWidth;
        mLayoutParams.height = dockTabHeight;
        mLayoutParams.x = left ? 0 : (screenW - dockTabWidth);

        if (mRootView instanceof com.google.android.material.card.MaterialCardView) {
            com.google.android.material.card.MaterialCardView card =
                    (com.google.android.material.card.MaterialCardView) mRootView;
            card.setRadius(density * 16);
            card.setCardBackgroundColor(Color.TRANSPARENT);
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
        cancelDimDockTab();

        if (mDockTabBtn != null) {
            mDockTabBtn.setVisibility(View.GONE);
        }
        if (mFrame != null) {
            mFrame.setVisibility(View.VISIBLE);
        }

        float density = mAppContext.getResources().getDisplayMetrics().density;
        int screenW = mAppContext.getResources().getDisplayMetrics().widthPixels;
        int defaultWidth = mInAppMode ? Math.round(240 * density) : Math.round(265 * density);
        int defaultHeight = mInAppMode ? Math.round(135 * density) : Math.round(150 * density);
        int width = mSavedNormalWidth > 0 ? mSavedNormalWidth : defaultWidth;
        int height = mSavedNormalHeight > 0 ? mSavedNormalHeight : defaultHeight;

        mLayoutParams.width = width;
        mLayoutParams.height = height;
        int maxX = Math.max(0, screenW - width - Math.round(8 * density));
        mLayoutParams.x = mDockedOnLeft ? Math.round(8 * density) : maxX;

        if (mRootView instanceof com.google.android.material.card.MaterialCardView) {
            com.google.android.material.card.MaterialCardView card =
                    (com.google.android.material.card.MaterialCardView) mRootView;
            card.setRadius(mInAppMode ? density * 8 : 0);
            card.setCardBackgroundColor(Color.BLACK);
        }

        try {
            mWindowManager.updateViewLayout(mRootView, mLayoutParams);
        } catch (Exception ignored) {
        }
        if (mTopBar != null) {
            mTopBar.setVisibility(View.VISIBLE);
        }
        if (mSeekBar != null) {
            mSeekBar.setVisibility(View.VISIBLE);
        }
        showControls();
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
                && currentSide != 0
                && currentSide == mLastTapSide
                && (now - mLastTapTime < 350);

        mLastTapTime = now;
        mLastTapSide = currentSide;

        if (isDoubleTap) {
            Utils.removeCallbacks(mSingleTapRunnable);
            performDoubleTapSeek(currentSide == 1);
        } else {
            Utils.removeCallbacks(mSingleTapRunnable);
            if (currentSide != 0 && !mControlsLocked) {
                Utils.postDelayed(mSingleTapRunnable, 260);
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
        MiniPlayerBridge.setResumePosition(target);

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
            textView.setText((forward ? "+" : "-") + mAccumulatedSeekSeconds + "s");
            container.animate().cancel();
            container.setVisibility(View.VISIBLE);
            container.setAlpha(1f);
            container.setScaleX(1.15f);
            container.setScaleY(1.15f);
            container.animate().scaleX(1.0f).scaleY(1.0f).setDuration(160).start();

            Utils.removeCallbacks(mHideSeekIndicatorRunnable);
            Utils.postDelayed(mHideSeekIndicatorRunnable, 750);
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
        mControlsLocked = locked;
        mControlsVisible = true;
        com.liskovsoft.smartyoutubetv2.common.prefs.AppPrefs prefs =
                com.liskovsoft.smartyoutubetv2.common.prefs.AppPrefs.instance(mAppContext);
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
        if (mCloseBtn != null) {
            mCloseBtn.setVisibility(locked ? View.GONE : View.VISIBLE);
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
        updateWatermarkVisibility();
        if (!locked) {
            // Unlocked: normal auto-hide behavior
            rescheduleAutoHide();
        } else {
            // Locked: auto-hide controls after a short timeout (3 seconds)
            Utils.removeCallbacks(mHideControlsRunnable);
            Utils.postDelayed(mHideControlsRunnable, UNLOCK_BTN_TIMEOUT_MS);
        }
    }

    private void toggleMute() {
        ExoPlayer player = getActivePlayer();
        if (player != null) {
            mMuted = !mMuted;
            player.setVolume(mMuted ? 0f : 1f);
            updateVolumeIcon();
        }
    }

    private void updateVolumeIcon() {
        if (mVolumeBtn != null) {
            mVolumeBtn.setImageResource(mMuted ? R.drawable.ic_float_mute : R.drawable.ic_float_volume);
        }
    }

    private void updateWatermarkVisibility() {
        if (mBrandWatermark != null) {
            boolean enabled = com.liskovsoft.smartyoutubetv2.common.prefs.AppPrefs.instance(mAppContext)
                    .getBoolean("floating_watermark_enabled", true);
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
        View[] views = new View[] {mTopActions, mCenterControls, mBottomBar, mBottomScrim};
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
        Utils.removeCallbacks(mHideControlsRunnable);

        if (mSeekBar != null) {
            try {
                mSeekBar.setThumb(ContextCompat.getDrawable(mAppContext, R.drawable.bg_float_thumb_invisible));
                mSeekBar.setThumbOffset(0);
            } catch (Exception ignored) {
            }
        }
        // Hide overlay controls so only video surface and borderless bottom progress bar remain
        View[] views = new View[] {mTopActions, mCenterControls, mBottomBar, mBottomScrim};
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
        // Always ensure the bottom red progress bar remains visible at the bottom border!
        if (mSeekBar != null) {
            mSeekBar.setVisibility(View.VISIBLE);
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
        Utils.removeCallbacks(mHideControlsRunnable);
        if (!mControlsLocked) {
            Utils.postDelayed(mHideControlsRunnable, CONTROLS_TIMEOUT_MS);
        }
    }

    public void openFullPlayer(boolean forceLandscapeFullscreen) {
        ExoPlayer player = getActivePlayer();
        if (player != null && player.getCurrentPosition() > 0) {
            MiniPlayerBridge.setResumePosition(player.getCurrentPosition());
        }
        hide(false);

        MobilePlaybackActivity activity = MobilePlaybackActivity.getCurrentInstance();
        Activity topActivity = mCurrentResumedActivity != null ? mCurrentResumedActivity.get() : null;

        if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
            activity.expandFromFloating(forceLandscapeFullscreen);

            Context caller = (topActivity != null && !topActivity.isFinishing() && !topActivity.isDestroyed())
                    ? topActivity : activity;
            Intent intent = new Intent(caller, MobilePlaybackActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            if (forceLandscapeFullscreen) {
                intent.putExtra(MobilePlaybackActivity.EXTRA_FORCE_FULLSCREEN, true);
            } else {
                intent.putExtra(MobilePlaybackActivity.EXTRA_FORCE_PORTRAIT, true);
            }
            try {
                caller.startActivity(intent);
            } catch (Exception e) {
                try {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mAppContext.startActivity(intent);
                } catch (Exception ignored) {}
            }
        } else {
            Intent intent = new Intent(mAppContext, MobilePlaybackActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            if (forceLandscapeFullscreen) {
                intent.putExtra(MobilePlaybackActivity.EXTRA_FORCE_FULLSCREEN, true);
            } else {
                intent.putExtra(MobilePlaybackActivity.EXTRA_FORCE_PORTRAIT, true);
            }
            try {
                if (topActivity != null && !topActivity.isFinishing()) {
                    topActivity.startActivity(intent);
                } else {
                    mAppContext.startActivity(intent);
                }
            } catch (Exception ignored) {
                com.liskovsoft.smartyoutubetv2.common.app.views.ViewManager.instance(mAppContext)
                        .startView(com.liskovsoft.smartyoutubetv2.common.app.views.PlaybackView.class);
            }
        }
    }

    private void closeAndStop() {
        hide(false);
        MobilePlaybackActivity activity = MobilePlaybackActivity.getCurrentInstance();
        if (activity != null) {
            activity.closeFromFloatingMiniPlayer();
        } else {
            MiniPlayerBridge.close();
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
    }

    private void updateAspectRatio(VideoSize videoSize) {
        if (mFrame == null) {
            return;
        }
        if (videoSize != null && videoSize.width > 0 && videoSize.height > 0) {
            float ratio = (videoSize.width * videoSize.pixelWidthHeightRatio) / videoSize.height;
            mFrame.setAspectRatio(ratio);
        } else {
            mFrame.setAspectRatio(16f / 9f);
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
            if (current > 1000) {
                MiniPlayerBridge.setResumePosition(current);
                MobilePlaybackActivity activity = MobilePlaybackActivity.getCurrentInstance();
                Video video = activity != null ? activity.getVideo() : MiniPlayerBridge.getVideo();
                if (video != null && duration - current > 2000) {
                    VideoStateService ss = VideoStateService.instance(mAppContext);
                    if (ss != null) {
                        ss.save(new VideoStateService.State(
                                video, current, duration, player.getPlaybackParameters().speed));
                    }
                }
            }
            int progress = (int) (1000L * current / duration);
            if (mSeekBar != null) {
                mSeekBar.setVisibility(View.VISIBLE);
                if (!mIsUserSeeking) {
                    mSeekBar.setProgress(progress);
                }
            }
            updateTimeDisplay(current, duration);
        } else if (mTimeText != null) {
            mTimeText.setText("");
        }

        updatePlayPause(player);
        Utils.postDelayed(mTickRunnable, TICK_MS);
    }

    private void updatePlayPause(ExoPlayer player) {
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
