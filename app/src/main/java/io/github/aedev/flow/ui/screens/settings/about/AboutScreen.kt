package io.github.aedev.flow.ui.screens.settings.about

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import io.github.aedev.flow.R
import io.github.aedev.flow.ui.components.settings.SettingsPage
import io.github.aedev.flow.ui.components.settings.nav
import io.github.aedev.flow.ui.screens.settings.index.AboutIndex

private enum class AboutDialog { CHANGELOG, DEVICE, NOTICES }

private const val TELEGRAM_CHANNEL_URL = "https://t.me/loxtube"
private const val TELEGRAM_PERSONAL_URL = "https://t.me/YASIN99995"
private const val FACEBOOK_URL = "https://www.facebook.com/share/1CN8ZwiesC/"
private const val EMAIL_URL = "mailto:arafat.website@Gmail.com"
private const val WEBSITE_URL = "https://loxtube.com"
private const val GITHUB_URL = "https://github.com/loxtube"
private const val NEWPIPE_URL = "https://github.com/TeamNewPipe/NewPipeExtractor"
private const val LICENSE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"

/** The app's version, where to find and reach the project, developer profile, and licences. */
@Composable
internal fun AboutScreen(
    onBack: (() -> Unit)?,
    highlight: String?,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var dialog by rememberSaveable { mutableStateOf<AboutDialog?>(null) }
    var selectedGalleryPhoto by remember { mutableStateOf<String?>(null) }
    val open = { url: String -> runCatching { uriHandler.openUri(url) } }

    val unknown = stringResource(R.string.unknown)
    val version =
        remember {
            runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }
                .getOrNull()
                ?.let { it.versionName to it.longVersionCode.toString() }
        }

    val galleryPhotos =
        remember {
            try {
                val list =
                    context.assets.list("developer_gallery")?.filter { file ->
                        file.endsWith(".png", ignoreCase = true) ||
                            file.endsWith(".jpg", ignoreCase = true) ||
                            file.endsWith(".jpeg", ignoreCase = true) ||
                            file.endsWith(".webp", ignoreCase = true)
                    }?.map { "file:///android_asset/developer_gallery/$it" }
                if (!list.isNullOrEmpty()) list else listOf("drawable://developer_profile")
            } catch (e: Exception) {
                listOf("drawable://developer_profile")
            }
        }

    SettingsPage(
        title = stringResource(R.string.settings_item_about_flow),
        onBack = onBack,
        highlight = highlight,
    ) {
        // Developer Cover Banner
        item("about.developer.banner") {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Image(
                    painter = painterResource(R.drawable.developer_profile),
                    contentDescription = stringResource(R.string.about_creator_name),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        // Developer Profile & Quick Action Buttons
        item("about.developer.profile") {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.about_creator_name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.about_creator_role_loxtube),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = stringResource(R.string.about_pro_badge),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    QuickActionCircle(
                        icon = IconTelegram,
                        label = stringResource(R.string.about_telegram_personal),
                        onClick = { open(TELEGRAM_PERSONAL_URL) },
                    )
                    QuickActionCircle(
                        icon = IconFacebook,
                        label = stringResource(R.string.about_facebook),
                        onClick = { open(FACEBOOK_URL) },
                    )
                    QuickActionCircle(
                        icon = Icons.Outlined.Public,
                        label = stringResource(R.string.about_website),
                        onClick = { open(WEBSITE_URL) },
                    )
                    QuickActionCircle(
                        icon = Icons.Outlined.Email,
                        label = stringResource(R.string.about_email),
                        onClick = { open(EMAIL_URL) },
                    )
                }
            }
        }

        // My Gallery & Albums Section
        item("about.gallery") {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.about_gallery_albums),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                ) {
                    items(galleryPhotos) { photoUrl ->
                        Box(
                            modifier =
                                Modifier
                                    .size(width = 96.dp, height = 120.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .clickable { selectedGalleryPhoto = photoUrl },
                        ) {
                            if (photoUrl == "drawable://developer_profile") {
                                Image(
                                    painter = painterResource(R.drawable.developer_profile),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }
                    }
                }
            }
        }

        // App Info Section
        item("about.app.info") {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.section_app),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_notification_logo),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text =
                                    stringResource(
                                        R.string.about_app_version_name,
                                        stringResource(R.string.app_name),
                                        version?.first ?: unknown,
                                        version?.second ?: "0",
                                    ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(R.string.about_app_companion_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // Contact & Social Section (2 columns per row)
        item("about.contact.social") {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.section_contact),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                // Row 1: Telegram & Apps Channel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AboutContactCard(
                        title = stringResource(R.string.about_telegram_personal),
                        subtitle = stringResource(R.string.about_telegram_handle_full),
                        icon = {
                            Icon(
                                IconTelegram,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        modifier = Modifier.weight(1f),
                        onClick = { open(TELEGRAM_PERSONAL_URL) },
                    )
                    AboutContactCard(
                        title = stringResource(R.string.about_telegram_channel),
                        subtitle = stringResource(R.string.about_apps_channel_sub),
                        icon = {
                            Icon(
                                Icons.Outlined.Smartphone,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        modifier = Modifier.weight(1f),
                        onClick = { open(TELEGRAM_CHANNEL_URL) },
                    )
                }

                // Row 2: Facebook & Email
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AboutContactCard(
                        title = stringResource(R.string.about_facebook),
                        subtitle = stringResource(R.string.about_creator_name),
                        icon = {
                            Icon(
                                IconFacebook,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        modifier = Modifier.weight(1f),
                        onClick = { open(FACEBOOK_URL) },
                    )
                    AboutContactCard(
                        title = stringResource(R.string.about_email),
                        subtitle = stringResource(R.string.about_email_address),
                        icon = {
                            Icon(
                                Icons.Outlined.Email,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        modifier = Modifier.weight(1f),
                        onClick = { open(EMAIL_URL) },
                    )
                }

                // Row 3: Website & Open Source Code
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AboutContactCard(
                        title = stringResource(R.string.about_website),
                        subtitle = stringResource(R.string.about_website_handle),
                        icon = {
                            Icon(
                                Icons.Outlined.Public,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        modifier = Modifier.weight(1f),
                        onClick = { open(WEBSITE_URL) },
                    )
                    AboutContactCard(
                        title = stringResource(R.string.about_open_source_code),
                        subtitle = stringResource(R.string.about_github_handle),
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_github),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        modifier = Modifier.weight(1f),
                        onClick = { open(GITHUB_URL) },
                    )
                }
            }
        }

        // Additional Settings Groups (Changelog, Legal, Device Info)
        group(key = "about.app.extra", header = R.string.section_app) {
            nav(AboutIndex.changelog, icon = Icons.Outlined.History, showChevron = false, onClick = { dialog = AboutDialog.CHANGELOG })
        }
        group(key = "about.legal", header = R.string.section_legal) {
            nav(AboutIndex.license, icon = Icons.Outlined.Description, showChevron = false, onClick = { open(LICENSE_URL) })
            nav(AboutIndex.newPipe, icon = Icons.Outlined.Extension, showChevron = false, onClick = { open(NEWPIPE_URL) })
            nav(AboutIndex.notices, icon = Icons.Outlined.Gavel, showChevron = false, onClick = { dialog = AboutDialog.NOTICES })
        }
        group(key = "about.device", header = R.string.section_device) {
            nav(
                AboutIndex.deviceInfo,
                value = "${Build.MANUFACTURER} ${Build.MODEL}",
                icon = Icons.Outlined.Smartphone,
                showChevron = false,
                onClick = { dialog = AboutDialog.DEVICE },
            )
        }
    }

    when (dialog) {
        AboutDialog.CHANGELOG -> ChangelogSheet(onDismiss = { dialog = null })
        AboutDialog.DEVICE -> DeviceInfoDialog(onDismiss = { dialog = null })
        AboutDialog.NOTICES -> NoticesDialog(onDismiss = { dialog = null })
        null -> Unit
    }

    // Full photo preview dialog when clicking a gallery photo
    if (selectedGalleryPhoto != null) {
        Dialog(onDismissRequest = { selectedGalleryPhoto = null }) {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.padding(16.dp),
            ) {
                if (selectedGalleryPhoto == "drawable://developer_profile") {
                    Image(
                        painter = painterResource(R.drawable.developer_profile),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(360.dp),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    AsyncImage(
                        model = selectedGalleryPhoto,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(360.dp),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionCircle(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier =
            Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable(onClick = onClick)
                .padding(4.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun AboutContactCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
