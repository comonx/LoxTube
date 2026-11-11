package io.github.aedev.flow.utils

import android.content.Context
import android.telephony.TelephonyManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import io.github.aedev.flow.innertube.YouTube
import io.github.aedev.flow.innertube.models.YouTubeLocale
import java.util.Locale

object UserCountryHelper {
    @Volatile
    private var cachedCountryCode: String? = null

    fun getCountryCode(context: Context): String {
        cachedCountryCode?.let { return it }

        var code = ""
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            if (tm != null) {
                val simCountry = tm.simCountryIso
                if (!simCountry.isNullOrBlank() && simCountry.trim().length == 2) {
                    code = simCountry.trim().uppercase(Locale.US)
                } else {
                    val networkCountry = tm.networkCountryIso
                    if (!networkCountry.isNullOrBlank() && networkCountry.trim().length == 2) {
                        code = networkCountry.trim().uppercase(Locale.US)
                    }
                }
            }
        } catch (_: Exception) {
        }

        if (code.isBlank() || code.length != 2) {
            val localeCountry = Locale.getDefault().country
            if (!localeCountry.isNullOrBlank() && localeCountry.trim().length == 2) {
                code = localeCountry.trim().uppercase(Locale.US)
            }
        }

        if (code.isBlank() || code.length != 2) {
            code = "BD"
        }

        cachedCountryCode = code

        // Synchronize InnerTube locale so recommendations and feeds match the user's country
        try {
            val current = YouTube.locale
            if (current.gl != code) {
                YouTube.locale = YouTubeLocale(
                    gl = code,
                    hl = Locale.getDefault().toLanguageTag(),
                )
            }
        } catch (_: Exception) {
        }

        return code
    }
}

@Composable
fun rememberUserCountryCode(): String {
    val context = LocalContext.current
    return remember(context) {
        UserCountryHelper.getCountryCode(context)
    }
}
