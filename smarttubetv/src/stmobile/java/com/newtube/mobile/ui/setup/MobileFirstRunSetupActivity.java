package com.newtube.mobile.ui.setup;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.SwitchCompat;

import com.liskovsoft.smartyoutubetv2.tv.R;
import com.newtube.mobile.ui.browse.MobileBrowseActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MobileFirstRunSetupActivity extends AppCompatActivity {
    public static final String PREFS_NAME = "loxtube_setup_prefs";
    public static final String KEY_SETUP_COMPLETED = "setup_completed";
    public static final String KEY_OVERLAY_PROMPTED = "overlay_prompted_v2";
    public static final String KEY_PREFERRED_CATEGORIES = "user_preferred_categories";

    private static final int REQUEST_CODE_NOTIFICATIONS = 8001;
    private static final int REQUEST_CODE_STORAGE = 8002;

    private int mCurrentStep = 1;
    private final int TOTAL_STEPS = 5;

    // Header & Progress UI
    private TextView mTextStepBadge;
    private TextView mTextHeaderTitle;
    private TextView mTextHeaderSubtitle;
    private TextView mIndicatorDot1;
    private TextView mIndicatorDot2;
    private TextView mIndicatorDot3;
    private TextView mIndicatorDot4;
    private TextView mIndicatorDot5;
    private View mIndicatorLine12;
    private View mIndicatorLine23;
    private View mIndicatorLine34;
    private View mIndicatorLine45;

    // Step Containers
    private View mStep1Container;
    private View mStep2Container;
    private View mStep3Container;
    private View mStep4Container;
    private View mStep5Container;

    // Switches & Status
    private SwitchCompat mSwitchOverlay;
    private TextView mTextStatusOverlay;
    private SwitchCompat mSwitchNotification;
    private TextView mTextStatusNotification;
    private SwitchCompat mSwitchBattery;
    private TextView mTextStatusBattery;
    private SwitchCompat mSwitchStorage;
    private TextView mTextStatusStorage;

    // Navigation Controls
    private AppCompatImageButton mBtnPrev;
    private TextView mBtnNext;
    private TextView mBtnSkip;

    // Category Preferences
    private final Set<String> mSelectedCategories = new HashSet<>();
    private TextView mTextCatSummary;
    private TextView mBtnToggleAllChips;

    private final int[] mChipIds = {
            R.id.chip_setup_news,
            R.id.chip_setup_movies,
            R.id.chip_setup_music,
            R.id.chip_setup_drama,
            R.id.chip_setup_funny,
            R.id.chip_setup_gaming,
            R.id.chip_setup_tech,
            R.id.chip_setup_sports,
            R.id.chip_setup_islamic,
            R.id.chip_setup_education,
            R.id.chip_setup_cooking,
            R.id.chip_setup_animation
    };

    private final String[] mCategoryKeys = {
            "news", "movies", "music", "drama", "funny", "gaming",
            "tech", "sports", "islamic", "education", "cooking", "animation"
    };

    private boolean mIsUpdatingSwitches = false;

    public static boolean isSetupNeeded(Context context) {
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return !prefs.getBoolean(KEY_SETUP_COMPLETED, false);
    }

    public static void setSetupCompleted(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean(KEY_SETUP_COMPLETED, true)
                .putBoolean(KEY_OVERLAY_PROMPTED, true)
                .apply();
    }

    public static Set<String> getPreferredCategories(Context context) {
        if (context == null) return new HashSet<>();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getStringSet(KEY_PREFERRED_CATEGORIES, new HashSet<>());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_mobile_first_run_setup);
            bindViews();
            setupDefaultCategories();
            setupClickListeners();
            updateStepUi(false);
        } catch (Throwable t) {
            t.printStackTrace();
            setSetupCompleted(this);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAllPermissionStatuses();
    }

    private void bindViews() {
        mTextStepBadge = findViewById(R.id.text_setup_step_badge);
        mTextHeaderTitle = findViewById(R.id.text_setup_header_title);
        mTextHeaderSubtitle = findViewById(R.id.text_setup_header_subtitle);

        mIndicatorDot1 = findViewById(R.id.indicator_dot_1);
        mIndicatorDot2 = findViewById(R.id.indicator_dot_2);
        mIndicatorDot3 = findViewById(R.id.indicator_dot_3);
        mIndicatorDot4 = findViewById(R.id.indicator_dot_4);
        mIndicatorDot5 = findViewById(R.id.indicator_dot_5);

        mIndicatorLine12 = findViewById(R.id.indicator_line_1_2);
        mIndicatorLine23 = findViewById(R.id.indicator_line_2_3);
        mIndicatorLine34 = findViewById(R.id.indicator_line_3_4);
        mIndicatorLine45 = findViewById(R.id.indicator_line_4_5);

        mStep1Container = findViewById(R.id.setup_step_1_container);
        mStep2Container = findViewById(R.id.setup_step_2_container);
        mStep3Container = findViewById(R.id.setup_step_3_container);
        mStep4Container = findViewById(R.id.setup_step_4_container);
        mStep5Container = findViewById(R.id.setup_step_5_container);

        mSwitchOverlay = findViewById(R.id.switch_setup_overlay);
        mTextStatusOverlay = findViewById(R.id.text_status_overlay);
        mSwitchNotification = findViewById(R.id.switch_setup_notification);
        mTextStatusNotification = findViewById(R.id.text_status_notification);
        mSwitchBattery = findViewById(R.id.switch_setup_battery);
        mTextStatusBattery = findViewById(R.id.text_status_battery);
        mSwitchStorage = findViewById(R.id.switch_setup_storage);
        mTextStatusStorage = findViewById(R.id.text_status_storage);

        mBtnPrev = findViewById(R.id.btn_setup_prev);
        mBtnNext = findViewById(R.id.btn_setup_next);
        mBtnSkip = findViewById(R.id.btn_setup_skip);

        mTextCatSummary = findViewById(R.id.text_setup_cat_summary);
        mBtnToggleAllChips = findViewById(R.id.btn_setup_toggle_all_chips);
    }

    private void setupDefaultCategories() {
        mSelectedCategories.add("news");
        mSelectedCategories.add("movies");
        mSelectedCategories.add("music");
        mSelectedCategories.add("drama");
        mSelectedCategories.add("funny");

        for (int i = 0; i < mChipIds.length; i++) {
            final TextView chip = findViewById(mChipIds[i]);
            final String catKey = mCategoryKeys[i];
            if (chip != null) {
                updateChipVisual(chip, mSelectedCategories.contains(catKey));
                chip.setOnClickListener(v -> {
                    if (mSelectedCategories.contains(catKey)) {
                        mSelectedCategories.remove(catKey);
                        updateChipVisual(chip, false);
                    } else {
                        mSelectedCategories.add(catKey);
                        updateChipVisual(chip, true);
                    }
                    updateCategorySummary();
                });
            }
        }

        if (mBtnToggleAllChips != null) {
            mBtnToggleAllChips.setOnClickListener(v -> {
                boolean allSelected = (mSelectedCategories.size() == mCategoryKeys.length);
                if (allSelected) {
                    mSelectedCategories.clear();
                    mBtnToggleAllChips.setText("সব সিলেক্ট করুন");
                } else {
                    for (String key : mCategoryKeys) {
                        mSelectedCategories.add(key);
                    }
                    mBtnToggleAllChips.setText("সব বাদ দিন");
                }
                for (int i = 0; i < mChipIds.length; i++) {
                    TextView chip = findViewById(mChipIds[i]);
                    if (chip != null) {
                        updateChipVisual(chip, mSelectedCategories.contains(mCategoryKeys[i]));
                    }
                }
                updateCategorySummary();
            });
        }
        updateCategorySummary();
    }

    private void updateChipVisual(TextView chip, boolean selected) {
        if (chip == null) return;
        chip.setBackgroundResource(selected
                ? R.drawable.bg_setup_chip_colorful_selected
                : R.drawable.bg_setup_chip_colorful_unselected);
        chip.setTextColor(selected ? Color.WHITE : Color.parseColor("#334155"));
    }

    private void updateCategorySummary() {
        if (mTextCatSummary != null) {
            mTextCatSummary.setText("✓ " + mSelectedCategories.size() + "টি পছন্দের ক্যাটাগরি নির্বাচিত হয়েছে");
        }
    }

    private void setupClickListeners() {
        // Step 1: Overlay Switch & Row
        View cardOverlay = findViewById(R.id.card_setup_switch_overlay);
        if (cardOverlay != null) {
            cardOverlay.setOnClickListener(v -> {
                if (!hasOverlayPermission()) {
                    requestOverlayPermission();
                } else {
                    Toast.makeText(this, "অনুমতি ইতিমধ্যে দেওয়া হয়েছে ✓", Toast.LENGTH_SHORT).show();
                }
            });
        }
        if (mSwitchOverlay != null) {
            mSwitchOverlay.setOnClickListener(v -> {
                if (!mIsUpdatingSwitches) {
                    if (!hasOverlayPermission()) {
                        requestOverlayPermission();
                    } else {
                        mSwitchOverlay.setChecked(true);
                    }
                }
            });
        }

        // Step 2: Notification Switch & Row
        View cardNotif = findViewById(R.id.card_setup_switch_notification);
        if (cardNotif != null) {
            cardNotif.setOnClickListener(v -> {
                if (!hasNotificationPermission()) {
                    requestNotificationPermission();
                } else {
                    Toast.makeText(this, "অনুমতি ইতিমধ্যে দেওয়া হয়েছে ✓", Toast.LENGTH_SHORT).show();
                }
            });
        }
        if (mSwitchNotification != null) {
            mSwitchNotification.setOnClickListener(v -> {
                if (!mIsUpdatingSwitches) {
                    if (!hasNotificationPermission()) {
                        requestNotificationPermission();
                    } else {
                        mSwitchNotification.setChecked(true);
                    }
                }
            });
        }

        // Step 3: Battery Optimization Switch & Row
        View cardBattery = findViewById(R.id.card_setup_switch_battery);
        if (cardBattery != null) {
            cardBattery.setOnClickListener(v -> {
                if (!hasBatteryOptimizationExemption()) {
                    requestBatteryOptimization();
                } else {
                    Toast.makeText(this, "অনুমতি ইতিমধ্যে দেওয়া হয়েছে ✓", Toast.LENGTH_SHORT).show();
                }
            });
        }
        if (mSwitchBattery != null) {
            mSwitchBattery.setOnClickListener(v -> {
                if (!mIsUpdatingSwitches) {
                    if (!hasBatteryOptimizationExemption()) {
                        requestBatteryOptimization();
                    } else {
                        mSwitchBattery.setChecked(true);
                    }
                }
            });
        }

        // Step 4: Storage Switch & Row
        View cardStorage = findViewById(R.id.card_setup_switch_storage);
        if (cardStorage != null) {
            cardStorage.setOnClickListener(v -> {
                if (!hasStoragePermission()) {
                    requestStoragePermissions();
                } else {
                    Toast.makeText(this, "অনুমতি ইতিমধ্যে দেওয়া হয়েছে ✓", Toast.LENGTH_SHORT).show();
                }
            });
        }
        if (mSwitchStorage != null) {
            mSwitchStorage.setOnClickListener(v -> {
                if (!mIsUpdatingSwitches) {
                    if (!hasStoragePermission()) {
                        requestStoragePermissions();
                    } else {
                        mSwitchStorage.setChecked(true);
                    }
                }
            });
        }

        // Navigation: Prev, Next, Skip
        if (mBtnPrev != null) {
            mBtnPrev.setOnClickListener(v -> {
                if (mCurrentStep > 1) {
                    mCurrentStep--;
                    updateStepUi(true);
                }
            });
        }

        if (mBtnNext != null) {
            mBtnNext.setOnClickListener(v -> {
                if (mCurrentStep < TOTAL_STEPS) {
                    mCurrentStep++;
                    updateStepUi(true);
                } else {
                    finishSetupAndLaunchHome();
                }
            });
        }

        if (mBtnSkip != null) {
            mBtnSkip.setOnClickListener(v -> {
                if (mCurrentStep < TOTAL_STEPS) {
                    mCurrentStep++;
                    updateStepUi(true);
                } else {
                    finishSetupAndLaunchHome();
                }
            });
        }
    }

    private void updateStepUi(boolean animate) {
        if (mTextStepBadge != null) {
            mTextStepBadge.setText("ধাপ " + getBanglaNumber(mCurrentStep) + " / ৫");
        }

        // Containers visibility
        View[] containers = {mStep1Container, mStep2Container, mStep3Container, mStep4Container, mStep5Container};
        for (int i = 0; i < containers.length; i++) {
            View c = containers[i];
            if (c != null) {
                if (i == (mCurrentStep - 1)) {
                    c.setVisibility(View.VISIBLE);
                    if (animate) {
                        c.setAlpha(0f);
                        c.animate().alpha(1f).setDuration(220).start();
                    } else {
                        c.setAlpha(1f);
                    }
                } else {
                    c.setVisibility(View.GONE);
                }
            }
        }

        // Header Titles according to step
        if (mTextHeaderTitle != null && mTextHeaderSubtitle != null) {
            switch (mCurrentStep) {
                case 1:
                    mTextHeaderTitle.setText("১. ভাসমান মিনি প্লেয়ার (PiP)");
                    mTextHeaderSubtitle.setText("অন্যান্য অ্যাপে ভিডিও ছোট স্ক্রিনে দেখার জন্য ডিসপ্লে ওভার অ্যাপস দিন");
                    break;
                case 2:
                    mTextHeaderTitle.setText("২. নোটিফিকেশন ও মিডিয়া বার");
                    mTextHeaderSubtitle.setText("নোটিফিকেশন বার থেকে প্লে/পজ এবং স্টপ বাটন নিয়ন্ত্রণের জন্য প্রয়োজন");
                    break;
                case 3:
                    mTextHeaderTitle.setText("৩. নিরবচ্ছিন্ন ব্যাকগ্রাউন্ড প্লেব্যাক");
                    mTextHeaderSubtitle.setText("স্ক্রিন বন্ধ থাকলেও ভিডিও বা গান নিজে নিজে বন্ধ হওয়া প্রতিরোধ করবে");
                    break;
                case 4:
                    mTextHeaderTitle.setText("৪. মিডিয়া ও ডাউনলোড অনুমতি");
                    mTextHeaderSubtitle.setText("ভিডিও ডাউনলোড এবং সুপার ফাস্ট ভিডিও ক্যাশিংয়ের জন্য প্রয়োজন");
                    break;
                case 5:
                    mTextHeaderTitle.setText("৫. আপনার পছন্দের ক্যাটাগরি");
                    mTextHeaderSubtitle.setText("হোম ফিডে আপনার পছন্দের ভিডিওগুলো সাজিয়ে নিতে নির্বাচন করুন");
                    break;
            }
        }

        // Step Dots and Lines
        updateStepIndicators();

        // Prev & Next Buttons
        if (mBtnPrev != null) {
            mBtnPrev.setVisibility(mCurrentStep > 1 ? View.VISIBLE : View.GONE);
        }

        if (mBtnNext != null) {
            if (mCurrentStep == TOTAL_STEPS) {
                mBtnNext.setText("LoxTube শুরু করুন  ★");
            } else {
                mBtnNext.setText("পরবর্তী ধাপ  ➔");
            }
        }

        if (mBtnSkip != null) {
            mBtnSkip.setText(mCurrentStep == TOTAL_STEPS ? "ডিফল্ট ক্যাটাগরিতে শুরু করুন" : "এই ধাপটি পরে করতে চান? এগিয়ে যান");
        }

        refreshAllPermissionStatuses();
    }

    private void updateStepIndicators() {
        TextView[] dots = {mIndicatorDot1, mIndicatorDot2, mIndicatorDot3, mIndicatorDot4, mIndicatorDot5};
        View[] lines = {mIndicatorLine12, mIndicatorLine23, mIndicatorLine34, mIndicatorLine45};

        for (int i = 0; i < dots.length; i++) {
            TextView dot = dots[i];
            if (dot == null) continue;
            int stepNum = i + 1;
            if (stepNum < mCurrentStep) {
                // Completed
                dot.setBackgroundResource(R.drawable.bg_setup_icon_circle_purple);
                dot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#10B981"))); // Emerald green
                dot.setTextColor(Color.WHITE);
                dot.setText("✓");
            } else if (stepNum == mCurrentStep) {
                // Active
                dot.setBackgroundResource(R.drawable.bg_setup_icon_circle_purple);
                dot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
                dot.setTextColor(Color.parseColor("#4F46E5")); // Indigo
                dot.setText(getBanglaNumber(stepNum));
            } else {
                // Upcoming
                dot.setBackgroundResource(R.drawable.bg_setup_icon_circle_purple);
                dot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#40FFFFFF")));
                dot.setTextColor(Color.WHITE);
                dot.setText(getBanglaNumber(stepNum));
            }
        }

        for (int i = 0; i < lines.length; i++) {
            View line = lines[i];
            if (line == null) continue;
            int stepBefore = i + 1;
            if (stepBefore < mCurrentStep) {
                line.setBackgroundColor(Color.parseColor("#10B981")); // Completed green line
            } else {
                line.setBackgroundColor(Color.parseColor("#40FFFFFF"));
            }
        }
    }

    private String getBanglaNumber(int num) {
        switch (num) {
            case 1: return "১";
            case 2: return "২";
            case 3: return "৩";
            case 4: return "৪";
            case 5: return "৫";
            default: return String.valueOf(num);
        }
    }

    private void refreshAllPermissionStatuses() {
        mIsUpdatingSwitches = true;

        // Overlay
        boolean overlayGranted = hasOverlayPermission();
        if (mSwitchOverlay != null) {
            mSwitchOverlay.setChecked(overlayGranted);
        }
        if (mTextStatusOverlay != null) {
            mTextStatusOverlay.setText(overlayGranted ? "অনুমতি দেওয়া হয়েছে  ✓" : "সক্রিয় করতে সুইচ চাপুন");
            mTextStatusOverlay.setTextColor(overlayGranted ? Color.parseColor("#16A34A") : Color.parseColor("#64748B"));
        }

        // Notification
        boolean notifGranted = hasNotificationPermission();
        if (mSwitchNotification != null) {
            mSwitchNotification.setChecked(notifGranted);
        }
        if (mTextStatusNotification != null) {
            mTextStatusNotification.setText(notifGranted ? "অনুমতি দেওয়া হয়েছে  ✓" : "সক্রিয় করতে সুইচ চাপুন");
            mTextStatusNotification.setTextColor(notifGranted ? Color.parseColor("#16A34A") : Color.parseColor("#64748B"));
        }

        // Battery
        boolean batteryGranted = hasBatteryOptimizationExemption();
        if (mSwitchBattery != null) {
            mSwitchBattery.setChecked(batteryGranted);
        }
        if (mTextStatusBattery != null) {
            mTextStatusBattery.setText(batteryGranted ? "অনুমতি দেওয়া হয়েছে  ✓" : "সক্রিয় করতে সুইচ চাপুন");
            mTextStatusBattery.setTextColor(batteryGranted ? Color.parseColor("#16A34A") : Color.parseColor("#64748B"));
        }

        // Storage
        boolean storageGranted = hasStoragePermission();
        if (mSwitchStorage != null) {
            mSwitchStorage.setChecked(storageGranted);
        }
        if (mTextStatusStorage != null) {
            mTextStatusStorage.setText(storageGranted ? "অনুমতি দেওয়া হয়েছে  ✓" : "সক্রিয় করতে সুইচ চাপুন");
            mTextStatusStorage.setTextColor(storageGranted ? Color.parseColor("#16A34A") : Color.parseColor("#64748B"));
        }

        mIsUpdatingSwitches = false;
    }

    private void finishSetupAndLaunchHome() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putStringSet(KEY_PREFERRED_CATEGORIES, mSelectedCategories).apply();
            setSetupCompleted(this);
        } catch (Throwable ignored) {
        }
        finish();
    }

    // Permission Helpers
    public boolean hasOverlayPermission() {
        try {
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
        } catch (Throwable t) {
            return false;
        }
    }

    public void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception ignored) {
            }
        }
    }

    public boolean hasNotificationPermission() {
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                return checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
            }
        } catch (Throwable t) {
            return true;
        }
        return true;
    }

    public void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_NOTIFICATIONS);
            } catch (Throwable ignored) {
            }
        }
    }

    public boolean hasBatteryOptimizationExemption() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            return pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
        } catch (Throwable t) {
            return false;
        }
    }

    public void requestBatteryOptimization() {
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            try {
                Intent fallback = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                startActivity(fallback);
            } catch (Exception ignored) {
            }
        }
    }

    public boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            return checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
        }
        return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public void requestStoragePermissions() {
        List<String> list = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 33) {
            list.add(Manifest.permission.READ_MEDIA_VIDEO);
            list.add(Manifest.permission.READ_MEDIA_AUDIO);
        } else {
            list.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            if (Build.VERSION.SDK_INT <= 32) {
                list.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
        requestPermissions(list.toArray(new String[0]), REQUEST_CODE_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        refreshAllPermissionStatuses();
    }
}
