package com.newtube.mobile.ui.playback;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.newtube.mobile.ui.common.FrameGate;
import com.newtube.mobile.ui.common.Motion;

/**
 * Renders an active {@link MiniPlayerBridge} session inside a screen's floating mini card.
 *
 * <p>The playback Activity still owns both ExoPlayer and its session-long SurfaceTexture. This
 * controller only adopts that texture into a temporary TextureView while its host Activity is in
 * front, then returns it before the host pauses. Keeping this small host adapter outside the
 * channel Activity makes the surface ownership rules identical to Home's proven mini-player.</p>
 */
public final class MobileMiniPlayerController {
    private static final long TICK_MS = 500;
    private static final long ENTRY_DURATION_MS = 280;
    /** NEWTUBE(motion): the longest the expanding player waits for this card to fold. */
    private static final long FOLD_WAIT_MS = 50;

    private final Activity mActivity;
    private final View mBar;
    /** NEWTUBE(haptics): stopped before this controller moves the card itself (MiniCardSwipe#stop). */
    private MiniCardSwipe mSwipe;
    private final FrameLayout mFrame;
    private final ImageView mFreeze;
    private final ImageButton mPlayPause;
    private final ImageButton mClose;
    private final ProgressBar mBottomProgress;
    private final ImageButton mExpand;
    private final TextView mTitleText;
    private final Runnable mTick = this::onTick;

    private boolean mControlsVisible = true;

    private TextureView mTexture;
    private boolean mEntryAnimating;
    private boolean mTextureHasFrame;
    /** NEWTUBE(motion): pre-drawn for a minimize; the buttons fade in once the host is in front. */
    private boolean mButtonsFadePending;
    private final MiniPlayerBridge.CardFold mCardFold = this::foldForPlayer;

    public MobileMiniPlayerController(Activity activity) {
        mActivity = activity;
        mBar = activity.findViewById(R.id.mobile_mini_player);
        if (mBar != null) {
            mBar.setVisibility(View.GONE);
        }
        mFrame = activity.findViewById(R.id.mobile_mini_player_frame);
        mFreeze = activity.findViewById(R.id.mobile_mini_freeze);
        mPlayPause = activity.findViewById(R.id.mobile_mini_play_pause);
        mClose = activity.findViewById(R.id.mobile_mini_close);
        mBottomProgress = activity.findViewById(R.id.mobile_mini_bottom_progress);
        mExpand = activity.findViewById(R.id.mobile_mini_expand);
        mTitleText = activity.findViewById(R.id.mobile_mini_title);

        View.OnClickListener expandAction = v -> {
            if (mClosing || !MiniPlayerBridge.isActive()) {
                return; // a closed card leaving
            }
            ExoPlayer player = MiniPlayerBridge.getPlayer();
            if (player != null && player.getCurrentPosition() > 0) {
                MiniPlayerBridge.setResumePosition(player.getCurrentPosition());
            }
            publishBounds();
            MiniPlayerBridge.expand(mActivity);
        };
        if (mExpand != null) {
            mExpand.setOnClickListener(expandAction);
        }
        if (mTitleText != null) {
            mTitleText.setOnClickListener(expandAction);
        }
        if (mBar != null) {
            mBar.setOnClickListener(expandAction);
        }
        if (mFrame != null) {
            mFrame.setOnClickListener(expandAction);
        }

        if (mPlayPause != null) {
            mPlayPause.setOnClickListener(v -> {
                ExoPlayer player = MiniPlayerBridge.getPlayer();
                if (player != null) {
                    boolean nextPlayWhenReady = !player.getPlayWhenReady();
                    player.setPlayWhenReady(nextPlayWhenReady);
                    updatePlayPause(player);
                }
            });
        }

        if (mClose != null) {
            mClose.setOnClickListener(v -> close());
        }

        updateButtonTints();

        // NEWTUBE(motion): swipe the docked bar away sideways to close it.
        mSwipe = MiniCardSwipe.attach(mBar, new MiniCardSwipe.Callback() {
            @Override
            public boolean canSwipe() {
                return !mClosing && !mEntryAnimating && MiniPlayerBridge.isActive();
            }

            @Override
            public void onSwipedAway(float toTranslationX, long durationMs) {
                close(toTranslationX, durationMs);
            }

            @Override
            public void onSwipedUp() {
                if (mClosing || !MiniPlayerBridge.isActive()) {
                    return;
                }
                ExoPlayer player = MiniPlayerBridge.getPlayer();
                if (player != null && player.getCurrentPosition() > 0) {
                    MiniPlayerBridge.setResumePosition(player.getCurrentPosition());
                }
                publishBounds();
                MiniPlayerBridge.expand(mActivity);
            }
        }, mFrame, mBar, mTitleText);
    }

    public void showControls() {
        if (mClosing) {
            return;
        }
        mControlsVisible = true;
        if (mBottomProgress != null) {
            mBottomProgress.setVisibility(View.VISIBLE);
        }
        com.liskovsoft.smartyoutubetv2.common.prefs.AppPrefs prefs =
                com.liskovsoft.smartyoutubetv2.common.prefs.AppPrefs.instance(mActivity);
        boolean closeEnabled = prefs.getBoolean("dock_close_button_enabled", true);
        boolean pipEnabled = prefs.getBoolean("dock_pip_button_enabled", true);
        if (mClose != null) {
            mClose.setVisibility(closeEnabled ? View.VISIBLE : View.GONE);
        }
        if (mExpand != null) {
            mExpand.setVisibility(pipEnabled ? View.VISIBLE : View.GONE);
        }
        setControlsAlpha(1f);
    }

    public void hideControls() {
        if (mClosing) {
            return;
        }
        if (mBottomProgress != null) {
            mBottomProgress.setVisibility(View.VISIBLE);
        }
    }

    public void toggleControls() {
    }

    public void rescheduleAutoHide() {
    }

    /**
     * Close the in-app docked mini-player bar and stop the session.
     */
    private boolean mClosing;

    private void close() {
        close(null, 0);
    }

    /** {@code flyToX}: swiped away - the card keeps going off that side instead of shrinking. */
    private void close(@androidx.annotation.Nullable Float flyToX, long flyMs) {
        if (mClosing) {
            return;
        }
        mClosing = true;
        MiniPlayerBridge.clearPendingCardFold(mCardFold);
        detachTexture();
        mSwipe.stop();
        mBar.animate().cancel();
        android.view.ViewPropertyAnimator exit;
        if (flyToX != null) {
            exit = mBar.animate().translationX(flyToX).alpha(0f).setDuration(flyMs)
                    .setInterpolator(new android.view.animation.LinearInterpolator());
        } else {
            resetTransform();
            exit = mBar.animate().alpha(0f).scaleX(0.95f).scaleY(0.95f)
                    .setDuration(Motion.EXIT_MS).setInterpolator(Motion.EMPHASIZED_ACCELERATE);
        }
        exit.withLayer().withEndAction(() -> {
            mClosing = false;
            fold();
            MiniPlayerBridge.close();
        }).start();
    }

    private void finishClose() {
        if (!mClosing) {
            return;
        }
        mClosing = false;
        fold();
        MiniPlayerBridge.close();
    }

    /**
     * NEWTUBE(motion): the host is pausing. Give the texture back (the player may be taking it) but
     * keep a docked card up, frozen on its last frame, until the player covers it - see
     * MiniPlayerBridge.setPendingCardFold. Hiding it here was a blink before every expand.
     */
    public void onHostPause() {
        finishClose();
        detachTexture();
        if (mBar.getVisibility() == View.VISIBLE && MiniPlayerBridge.isActive()) {
            MiniPlayerBridge.setPendingCardFold(mCardFold);
        } else {
            hide();
        }
    }

    /** The host is covered: a card still waiting to be folded can go now. */
    public void onHostStop() {
        hide();
    }

    /**
     * Show the live mini session. Replaced by system-wide FloatingMiniPlayerManager,
     * so docked horizontal bar is kept hidden.
     */
    public void sync(boolean animateFromPlayer) {
        hide();
    }

    private void updateTitle() {
        if (mTitleText != null) {
            com.liskovsoft.smartyoutubetv2.common.app.models.data.Video video = MiniPlayerBridge.getVideo();
            if (video != null && video.getTitle() != null) {
                mTitleText.setText(video.getTitle());
            }
        }
    }

    /**
     * The translucent playback Activity is still on top when this runs. Make the endpoint card
     * visible now so this host can draw it underneath before Android reorders it to the front.
     * Mirrors Home's proven pre-render gate (see MobileBrowseActivity#prepareMiniPlayerForHandoff).
     */
    public boolean prepareForHandoff(Runnable onDrawn) {
        hide();
        return false;
    }

    /**
     * Freeze the last mini frame, release this screen's TextureView, and hide the card. The player
     * Activity can then reclaim the same session texture during Back/expand without a black beat.
     */
    public void hide() {
        finishClose();
        MiniPlayerBridge.clearPendingCardFold(mCardFold);
        detachTexture();
        fold();
    }

    /** Freeze the last frame and give the session texture back; the card itself stays as it is. */
    private void detachTexture() {
        Utils.removeCallbacks(mTick);
        mTextureHasFrame = false;
        publishBounds();

        if (mTexture != null && mTexture.getParent() != null) {
            if (mTexture.isAvailable() && MiniPlayerBridge.isActive()) {
                Bitmap still = mTexture.getBitmap();
                if (still != null) {
                    mFreeze.setImageBitmap(still);
                    mFreeze.setVisibility(View.VISIBLE);
                    MiniPlayerBridge.setHandoffStill(still);
                }
            }
            // No throwaway-texture swap here: TextureView#setSurfaceTexture releases the
            // texture the view currently holds, which would destroy the session texture
            // (see MobilePlaybackActivity#detachVideoTexture).
            mFrame.removeView(mTexture);
            mTexture = null;
        }
    }

    private void foldForPlayer(@androidx.annotation.Nullable Runnable onFolded) {
        fold();
        if (onFolded != null) {
            FrameGate.afterNextFrame(mBar.getRootView(), FOLD_WAIT_MS, onFolded);
        }
    }

    private void fold() {
        mSwipe.stop();
        mBar.animate().cancel();
        mEntryAnimating = false;
        mButtonsFadePending = false;
        resetTransform();
        mBar.setAlpha(1f);
        mBar.setVisibility(View.GONE);
    }

    private void fadeInControls() {
        View[] views = new View[] {mTitleText, mPlayPause, mExpand, mClose};
        for (View view : views) {
            if (view != null) {
                view.animate().alpha(1f).setDuration(Motion.FADE_IN_MS).setInterpolator(Motion.STANDARD).start();
            }
        }
    }

    private void attachTexture() {
        if (mTexture == null) {
            final TextureView texture = new TextureView(mActivity);
            texture.setOpaque(false); // NEWTUBE(texture-opaque): see MobilePlaybackActivity
            texture.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture created, int width, int height) {
                    SurfaceTexture session = MiniPlayerBridge.getSessionTexture();
                    if (session != null && created != session) {
                        texture.setSurfaceTexture(session);
                        created.release();
                    }
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
                    // The playback Activity owns the session texture. Release only a temporary
                    // TextureView-created surface that was never replaced by that session.
                    return texture != MiniPlayerBridge.getSessionTexture();
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture texture) {
                    mTextureHasFrame = true;
                    hideFreezeWhenReady();
                }
            });
            mTexture = texture;
        }

        if (mTexture.getParent() == null) {
            mFrame.addView(mTexture, 0, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }

    /** Start as the full-width 16:9 watch video, then land on the channel's floating card. */
    private void animateFromWatchPage() {
        int width = mBar.getWidth();
        int height = mBar.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        mSwipe.stop();

        int[] cardLocation = new int[2];
        int[] contentLocation = new int[2];
        mBar.getLocationOnScreen(cardLocation);
        View content = mActivity.findViewById(android.R.id.content);
        content.getLocationOnScreen(contentLocation);

        float sourceWidth = mActivity.getResources().getDisplayMetrics().widthPixels;
        float sourceHeight = sourceWidth * 9f / 16f;

        mBar.setPivotX(0f);
        mBar.setPivotY(0f);
        mBar.setScaleX(sourceWidth / width);
        mBar.setScaleY(sourceHeight / height);
        mBar.setTranslationX(contentLocation[0] - cardLocation[0]);
        mBar.setTranslationY(contentLocation[1] - cardLocation[1]);

        setControlsAlpha(0f);
        mBar.animate()
                .scaleX(1f)
                .scaleY(1f)
                .translationX(0f)
                .translationY(0f)
                .setDuration(ENTRY_DURATION_MS)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    resetTransform();
                    mEntryAnimating = false;
                    hideFreezeWhenReady();
                    View[] views = new View[] {mTitleText, mPlayPause, mExpand, mClose};
                    for (View view : views) {
                        if (view != null) {
                            view.animate().alpha(1f).setDuration(90).start();
                        }
                    }
                    publishBounds();
                })
                .start();
    }

    private void hideFreezeWhenReady() {
        if (!mEntryAnimating && mTextureHasFrame && mFreeze.getVisibility() == View.VISIBLE) {
            mFreeze.setVisibility(View.GONE);
        }
    }

    private void resetTransform() {
        mBar.setPivotX(mBar.getWidth() / 2f);
        mBar.setPivotY(mBar.getHeight() / 2f);
        mBar.setScaleX(1f);
        mBar.setScaleY(1f);
        mBar.setTranslationX(0f);
        mBar.setTranslationY(0f);
        setControlsAlpha(1f);
    }

    private void setControlsAlpha(float alpha) {
        View[] views = new View[] {mTitleText, mPlayPause, mExpand, mClose};
        for (View view : views) {
            if (view != null) {
                view.animate().cancel();
                view.setAlpha(alpha);
            }
        }
    }

    private void publishBounds() {
        if (mBar.getVisibility() != View.VISIBLE || mBar.getWidth() <= 0 || mBar.getHeight() <= 0) {
            return;
        }
        int[] location = new int[2];
        mBar.getLocationOnScreen(location);
        MiniPlayerBridge.setMiniBounds(new Rect(location[0], location[1],
                location[0] + mBar.getWidth(), location[1] + mBar.getHeight()));
    }

    private void onTick() {
        if (mBar.getVisibility() != View.VISIBLE) {
            return;
        }
        ExoPlayer player = MiniPlayerBridge.getPlayer();
        if (player == null) {
            hide();
            return;
        }
        updateTitle();
        long duration = player.getDuration();
        long current = player.getCurrentPosition();
        if (duration > 0) {
            if (current > 1000) {
                MiniPlayerBridge.setResumePosition(current);
                com.liskovsoft.smartyoutubetv2.common.app.models.data.Video video = MiniPlayerBridge.getVideo();
                if (video != null && duration - current > 2000) {
                    com.liskovsoft.smartyoutubetv2.common.app.models.playback.service.VideoStateService stateService =
                            com.liskovsoft.smartyoutubetv2.common.app.models.playback.service.VideoStateService.instance(mActivity);
                    if (stateService != null) {
                        stateService.save(new com.liskovsoft.smartyoutubetv2.common.app.models.playback.service.VideoStateService.State(
                                video, current, duration, player.getPlaybackParameters().speed));
                    }
                }
            }
            int progress = (int) (current * 1000 / duration);
            if (mBottomProgress != null) {
                mBottomProgress.setProgress(progress);
            }
        }
        updatePlayPause(player);
        MiniPlayerBridge.fitToVideo(mFrame); // an autoplayed next video may have another shape
        Utils.postDelayed(mTick, TICK_MS);
    }



    private void updateButtonTints() {
        int tintColor = androidx.core.content.ContextCompat.getColor(mActivity, R.color.mobile_color_on_surface);
        if (mPlayPause != null) {
            mPlayPause.setColorFilter(tintColor);
        }
        if (mExpand != null) {
            mExpand.setColorFilter(tintColor);
        }
        if (mClose != null) {
            mClose.setColorFilter(tintColor);
        }
    }

    private void updatePlayPause(ExoPlayer player) {
        boolean playing = player.getPlayWhenReady()
                && player.getPlaybackState() != Player.STATE_ENDED
                && player.getPlaybackState() != Player.STATE_IDLE;
        if (mPlayPause != null) {
            mPlayPause.setImageResource(playing ? R.drawable.ic_mini_dock_pause : R.drawable.ic_mini_dock_play);
            mPlayPause.setColorFilter(androidx.core.content.ContextCompat.getColor(mActivity, R.color.mobile_color_on_surface));
        }
    }
}
