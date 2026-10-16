package com.newtube.mobile.ui.common;

import android.content.Context;
import android.telephony.TelephonyManager;

import java.util.Locale;

/**
 * Detects the user's country code to display next to the logo in the top bar (like YouTube's country badge, e.g. "BD").
 */
public final class CountryDetector {
    private static String sCountryCode;

    private CountryDetector() {
    }

    public static String getCountryCode(Context context) {
        if (sCountryCode != null && !sCountryCode.isEmpty()) {
            return sCountryCode;
        }

        if (context != null) {
            try {
                TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                if (tm != null) {
                    String simCountry = tm.getSimCountryIso();
                    if (simCountry != null && simCountry.trim().length() == 2) {
                        sCountryCode = simCountry.trim().toUpperCase(Locale.US);
                        return sCountryCode;
                    }
                    String networkCountry = tm.getNetworkCountryIso();
                    if (networkCountry != null && networkCountry.trim().length() == 2) {
                        sCountryCode = networkCountry.trim().toUpperCase(Locale.US);
                        return sCountryCode;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        try {
            String localeCountry = Locale.getDefault().getCountry();
            if (localeCountry != null && localeCountry.trim().length() == 2) {
                sCountryCode = localeCountry.trim().toUpperCase(Locale.US);
                return sCountryCode;
            }
        } catch (Exception ignored) {
        }

        // Default to "BD" as requested
        sCountryCode = "BD";
        return sCountryCode;
    }
}
