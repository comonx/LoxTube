package com.newtube.mobile.ui.setup;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import java.util.Set;

public final class MobileFirstRunSetupDialog {
    public static final String PREFS_NAME = MobileFirstRunSetupActivity.PREFS_NAME;
    public static final String KEY_SETUP_COMPLETED = MobileFirstRunSetupActivity.KEY_SETUP_COMPLETED;
    public static final String KEY_OVERLAY_PROMPTED = MobileFirstRunSetupActivity.KEY_OVERLAY_PROMPTED;
    public static final String KEY_PREFERRED_CATEGORIES = MobileFirstRunSetupActivity.KEY_PREFERRED_CATEGORIES;

    private MobileFirstRunSetupDialog() {
    }

    public static boolean isSetupNeeded(Context context) {
        return MobileFirstRunSetupActivity.isSetupNeeded(context);
    }

    public static void setSetupCompleted(Context context) {
        MobileFirstRunSetupActivity.setSetupCompleted(context);
    }

    public static Set<String> getPreferredCategories(Context context) {
        return MobileFirstRunSetupActivity.getPreferredCategories(context);
    }

    public static void showIfFirstRun(Activity activity) {
        showIfFirstRun(activity, null);
    }

    public static void showIfFirstRun(Activity activity, Runnable onComplete) {
        if (!isSetupNeeded(activity) || activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        activity.startActivity(new Intent(activity, MobileFirstRunSetupActivity.class));
    }
}
