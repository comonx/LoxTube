package com.newtube.mobile.ui.playback;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Animated video watermark for LoxTube.
 * 
 * Features:
 * - Displays the "LoxTube" brand watermark.
 * - The letter 'o' bounces up and falls back down to the baseline.
 * - On landing, 'o' triggers an energetic splash/splatter particle effect.
 * - An outward color shockwave disperses across the remaining letters ('L', 'x', 'T', 'u', 'b', 'e'),
 *   temporarily transforming their colors before smoothly fading back to crystalline white.
 */
public class LoxTubeWatermarkView extends View {

    private static final String BRAND_TEXT = "LoxTube";
    private static final int O_INDEX = 1; // 'o' in "LoxTube"

    // Animation cycle configuration
    private static final long CYCLE_DURATION_MS = 4200; // Total duration of one cycle
    private static final long BOUNCE_START_MS = 300;
    private static final long BOUNCE_PEAK_MS = 800;
    private static final long LANDING_MS = 1200;
    private static final long SQUASH_END_MS = 1450;
    private static final long COLOR_WAVE_END_MS = 2500;

    // Default letter color (sleek translucent white)
    private static final int COLOR_NORMAL = 0xD0FFFFFF;
    private static final int COLOR_NORMAL_O = 0xF0FFFFFF;

    // Splash vibrant colors for each letter [L, o, x, T, u, b, e]
    private static final int[] LETTER_SPLASH_COLORS = new int[] {
            0xFFFF4757, // L: Neon Coral / Red
            0xFFFFBE76, // o: Radiant Amber / Gold
            0xFFFF6B81, // x: Vivid Magenta / Pink
            0xFF70A1FF, // T: Electric Blue
            0xFF5352ED, // u: Royal Indigo
            0xFF2ED573, // b: Emerald Mint
            0xFF1DD1A1  // e: Neon Cyan
    };

    private static final int[] PARTICLE_COLORS = new int[] {
            0xFFFF4757, 0xFFFFA502, 0xFFFF6B81, 0xFF70A1FF,
            0xFF2ED573, 0xFF00D2D3, 0xFFE056FD, 0xFFFEEAA7
    };

    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shockwavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Rect bounds = new Rect();
    private final RectF bgRect = new RectF();

    private final float[] letterWidths = new float[BRAND_TEXT.length()];
    private final float[] letterXOffsets = new float[BRAND_TEXT.length()];
    private float totalTextWidth = 0;
    private float textHeight = 0;
    private float baseLineY = 0;

    private float paddingHorizontal;
    private float paddingVertical;
    private float cornerRadius;

    private float maxJumpHeight;
    private final Random random = new Random();

    // Particle splash pool
    private static class Particle {
        float x, y;
        float vx, vy;
        float radius;
        int color;
        float alpha; // 0..1
    }

    private final List<Particle> particles = new ArrayList<>();
    private static final int NUM_PARTICLES = 22;

    // Shockwave ripple
    private float shockwaveRadius = 0;
    private float shockwaveMaxRadius = 0;
    private float shockwaveAlpha = 0;

    private ValueAnimator cycleAnimator;
    private long currentAnimTime = 0;
    private boolean splashTriggered = false;

    public LoxTubeWatermarkView(Context context) {
        this(context, null);
    }

    public LoxTubeWatermarkView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LoxTubeWatermarkView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float textSizeSp = 14f;
        float textSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSizeSp, dm);

        textPaint.setTextSize(textSizePx);
        textPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        textPaint.setShadowLayer(
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2.5f, dm),
                0,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1.2f, dm),
                0x80000000
        );

        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setColor(Color.TRANSPARENT); // Completely transparent glass background

        particlePaint.setStyle(Paint.Style.FILL);

        shockwavePaint.setStyle(Paint.Style.STROKE);
        shockwavePaint.setStrokeWidth(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1.5f, dm));

        paddingHorizontal = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, dm);
        paddingVertical = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, dm);
        cornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14f, dm);
        maxJumpHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, dm);

        // Precompute glyph bounds and metrics
        totalTextWidth = 0;
        for (int i = 0; i < BRAND_TEXT.length(); i++) {
            String ch = String.valueOf(BRAND_TEXT.charAt(i));
            float w = textPaint.measureText(ch);
            letterWidths[i] = w;
            letterXOffsets[i] = totalTextWidth;
            totalTextWidth += w;
        }

        textPaint.getTextBounds(BRAND_TEXT, 0, BRAND_TEXT.length(), bounds);
        textHeight = bounds.height();

        // Create particles
        for (int i = 0; i < NUM_PARTICLES; i++) {
            particles.add(new Particle());
        }

        // Setup animator
        cycleAnimator = ValueAnimator.ofFloat(0f, (float) CYCLE_DURATION_MS);
        cycleAnimator.setDuration(CYCLE_DURATION_MS);
        cycleAnimator.setInterpolator(new LinearInterpolator());
        cycleAnimator.setRepeatCount(ValueAnimator.INFINITE);
        cycleAnimator.addUpdateListener(anim -> {
            float val = (float) anim.getAnimatedValue();
            updateAnimationState((long) val);
            invalidate();
        });
    }

    private void updateAnimationState(long timeMs) {
        currentAnimTime = timeMs;

        // Reset or trigger splash at landing moment
        if (timeMs < LANDING_MS) {
            splashTriggered = false;
        } else if (timeMs >= LANDING_MS && !splashTriggered) {
            triggerSplash();
            splashTriggered = true;
        }

        // Update active particles if past landing
        if (timeMs >= LANDING_MS && timeMs < COLOR_WAVE_END_MS) {
            float dt = 0.045f;
            for (Particle p : particles) {
                p.x += p.vx * dt;
                p.y += p.vy * dt;
                p.vy += 18f * dt; // gravity
                p.alpha = Math.max(0f, p.alpha - 0.035f);
            }

            // Shockwave progression
            float progress = (float) (timeMs - LANDING_MS) / (COLOR_WAVE_END_MS - LANDING_MS);
            shockwaveRadius = shockwaveMaxRadius * progress;
            shockwaveAlpha = (1f - progress);
        } else {
            shockwaveAlpha = 0f;
        }
    }

    private void triggerSplash() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float originX = paddingHorizontal + letterXOffsets[O_INDEX] + (letterWidths[O_INDEX] / 2f);
        float originY = baseLineY;

        shockwaveMaxRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32f, dm);

        for (Particle p : particles) {
            p.x = originX;
            p.y = originY;

            // Explode upwards and outwards in an arc
            double angle = Math.toRadians(-20 - random.nextInt(140)); // -20 to -160 degrees
            float speed = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    25f + random.nextFloat() * 45f,
                    dm
            );

            p.vx = (float) (Math.cos(angle) * speed);
            p.vy = (float) (Math.sin(angle) * speed);
            p.radius = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    1.4f + random.nextFloat() * 2.2f,
                    dm
            );
            p.color = PARTICLE_COLORS[random.nextInt(PARTICLE_COLORS.length)];
            p.alpha = 1.0f;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = (int) Math.ceil(totalTextWidth + (paddingHorizontal * 2));
        // Allocate extra headroom above text for the jumping 'o' and splash particles
        float extraTopHeadroom = maxJumpHeight * 1.35f;
        int h = (int) Math.ceil(textHeight + (paddingVertical * 2) + extraTopHeadroom);

        setMeasuredDimension(
                resolveSize(w, widthMeasureSpec),
                resolveSize(h, heightMeasureSpec)
        );
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float extraTopHeadroom = maxJumpHeight * 1.35f;
        bgRect.set(0, extraTopHeadroom, w, h);
        baseLineY = extraTopHeadroom + paddingVertical + textHeight;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Pure transparent glass watermark: no background pill drawn

        // Calculate bounce state for 'o'
        float oOffsetY = 0f;
        float oScaleX = 1f;
        float oScaleY = 1f;

        if (currentAnimTime >= BOUNCE_START_MS && currentAnimTime < BOUNCE_PEAK_MS) {
            // Ascending (decelerate)
            float t = (float) (currentAnimTime - BOUNCE_START_MS) / (BOUNCE_PEAK_MS - BOUNCE_START_MS);
            float decel = (float) Math.sin(t * (Math.PI / 2.0));
            oOffsetY = -maxJumpHeight * decel;
            oScaleX = 0.95f;
            oScaleY = 1.08f;
        } else if (currentAnimTime >= BOUNCE_PEAK_MS && currentAnimTime < LANDING_MS) {
            // Descending (accelerate)
            float t = (float) (currentAnimTime - BOUNCE_PEAK_MS) / (LANDING_MS - BOUNCE_PEAK_MS);
            float accel = t * t;
            oOffsetY = -maxJumpHeight * (1f - accel);
            oScaleX = 0.96f;
            oScaleY = 1.05f;
        } else if (currentAnimTime >= LANDING_MS && currentAnimTime < SQUASH_END_MS) {
            // Landing impact squash & rebound spring
            float t = (float) (currentAnimTime - LANDING_MS) / (SQUASH_END_MS - LANDING_MS);
            // Elastic spring formula: sin wave damped
            float squash = (float) (Math.sin(t * Math.PI) * Math.exp(-t * 2.5));
            oScaleX = 1f + (squash * 0.32f); // Expand sideways
            oScaleY = 1f - (squash * 0.28f); // Squash vertically
            oOffsetY = 0f;
        }

        // Color wave calculation from origin 'o'
        float waveProgress = 0f;
        if (currentAnimTime >= LANDING_MS && currentAnimTime < COLOR_WAVE_END_MS) {
            waveProgress = (float) (currentAnimTime - LANDING_MS) / (COLOR_WAVE_END_MS - LANDING_MS);
        }

        // 2. Draw shockwave ripple on landing
        if (shockwaveAlpha > 0.01f) {
            float oCenterX = paddingHorizontal + letterXOffsets[O_INDEX] + (letterWidths[O_INDEX] / 2f);
            shockwavePaint.setColor(0x00D2D3);
            shockwavePaint.setAlpha((int) (shockwaveAlpha * 180));
            canvas.drawCircle(oCenterX, baseLineY, shockwaveRadius, shockwavePaint);
        }

        // 3. Draw letters
        for (int i = 0; i < BRAND_TEXT.length(); i++) {
            char ch = BRAND_TEXT.charAt(i);
            String str = String.valueOf(ch);
            float lx = paddingHorizontal + letterXOffsets[i];
            float ly = baseLineY;

            // Determine dynamic letter color
            int letterColor = COLOR_NORMAL;

            if (waveProgress > 0f) {
                // Wave reaches letter based on distance from index 1 ('o')
                int dist = Math.abs(i - O_INDEX);
                float delay = dist * 0.08f;
                float localProgress = (waveProgress - delay) / 0.55f;

                if (localProgress >= 0f && localProgress <= 1f) {
                    // Smooth bell-curve blend between normal color and splash color
                    float blend = (float) Math.sin(localProgress * Math.PI);
                    letterColor = interpolateColor(COLOR_NORMAL, LETTER_SPLASH_COLORS[i], blend);
                }
            }

            textPaint.setColor(letterColor);

            if (i == O_INDEX) {
                // Render animated 'o' with jump offset & squash/stretch
                float glyphCenterX = lx + (letterWidths[i] / 2f);
                float glyphCenterY = ly - (textHeight / 2f);

                canvas.save();
                canvas.translate(glyphCenterX, glyphCenterY + oOffsetY);
                canvas.scale(oScaleX, oScaleY);
                canvas.translate(-glyphCenterX, -glyphCenterY);

                if (oOffsetY < -1f) {
                    textPaint.setColor(COLOR_NORMAL_O);
                }
                canvas.drawText(str, lx, ly, textPaint);
                canvas.restore();
            } else {
                canvas.drawText(str, lx, ly, textPaint);
            }
        }

        // 4. Draw splash particles
        if (currentAnimTime >= LANDING_MS && currentAnimTime < COLOR_WAVE_END_MS) {
            for (Particle p : particles) {
                if (p.alpha > 0.01f) {
                    particlePaint.setColor(p.color);
                    particlePaint.setAlpha((int) (p.alpha * 255));
                    canvas.drawCircle(p.x, p.y, p.radius, particlePaint);
                }
            }
        }
    }

    private static int interpolateColor(int colorStart, int colorEnd, float fraction) {
        fraction = Math.max(0f, Math.min(1f, fraction));
        int a = (int) (Color.alpha(colorStart) + fraction * (Color.alpha(colorEnd) - Color.alpha(colorStart)));
        int r = (int) (Color.red(colorStart) + fraction * (Color.red(colorEnd) - Color.red(colorStart)));
        int g = (int) (Color.green(colorStart) + fraction * (Color.green(colorEnd) - Color.green(colorStart)));
        int b = (int) (Color.blue(colorStart) + fraction * (Color.blue(colorEnd) - Color.blue(colorStart)));
        return Color.argb(a, r, g, b);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (getVisibility() == VISIBLE && cycleAnimator != null && !cycleAnimator.isRunning()) {
            cycleAnimator.start();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (cycleAnimator != null) {
            cycleAnimator.cancel();
        }
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (cycleAnimator == null) return;
        if (visibility == VISIBLE) {
            if (!cycleAnimator.isRunning() && isAttachedToWindow()) {
                cycleAnimator.start();
            }
        } else {
            cycleAnimator.cancel();
        }
    }
}
