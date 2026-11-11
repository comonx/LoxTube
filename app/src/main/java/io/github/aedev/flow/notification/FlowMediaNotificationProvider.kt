package io.github.aedev.flow.notification

import android.content.Context
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import com.google.common.collect.ImmutableList
import io.github.aedev.flow.R
import io.github.aedev.flow.player.floating.FloatingMiniPlayerManager

@UnstableApi
class FlowMediaNotificationProvider(
    context: Context,
) : DefaultMediaNotificationProvider(
    context,
    { NotificationHelper.NOTIFICATION_PLAYBACK },
    NotificationHelper.CHANNEL_PLAYBACK,
    R.string.notification_channel_video_playback,
) {
    companion object {
        const val ACTION_TOGGLE_PIP = "io.github.aedev.flow.action.PIP"
        const val ACTION_SHOW_RELATED = "io.github.aedev.flow.action.SHOW_RELATED"
        const val ACTION_STOP = "io.github.aedev.flow.action.STOP"
        const val COLOR_PRIMARY_RED = 0xFFFF0000.toInt()
    }

    init {
        setSmallIcon(R.drawable.ic_notification_loxtube)
    }

    override fun getMediaButtons(
        mediaSession: MediaSession,
        playerCommands: Player.Commands,
        customLayout: ImmutableList<CommandButton>,
        showPauseButton: Boolean,
    ): ImmutableList<CommandButton> {
        val list = ImmutableList.builder<CommandButton>()

        // 1. Headphone / PiP toggle button (leftmost in notification / lock screen)
        val isShowing = FloatingMiniPlayerManager.isShowing()
        val leftIcon = if (isShowing) R.drawable.ic_notif_headphone else R.drawable.ic_notif_pip
        val leftName = if (isShowing) "Hide Mini Player" else "Show Mini Player"
        list.add(
            CommandButton.Builder()
                .setSessionCommand(SessionCommand(ACTION_TOGGLE_PIP, Bundle.EMPTY))
                .setIconResId(leftIcon)
                .setCustomIconResId(leftIcon)
                .setDisplayName(leftName)
                .setEnabled(true)
                .build(),
        )

        // 2. Previous button
        list.add(
            CommandButton.Builder(CommandButton.ICON_PREVIOUS)
                .setPlayerCommand(Player.COMMAND_SEEK_TO_PREVIOUS)
                .setDisplayName("Previous")
                .setEnabled(true)
                .build(),
        )

        // 3. Play / Pause button
        if (showPauseButton) {
            list.add(
                CommandButton.Builder(CommandButton.ICON_PAUSE)
                    .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
                    .setDisplayName("Pause")
                    .setEnabled(true)
                    .build(),
            )
        } else {
            list.add(
                CommandButton.Builder(CommandButton.ICON_PLAY)
                    .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
                    .setDisplayName("Play")
                    .setEnabled(true)
                    .build(),
            )
        }

        // 4. Next button
        list.add(
            CommandButton.Builder(CommandButton.ICON_NEXT)
                .setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT)
                .setDisplayName("Next")
                .setEnabled(true)
                .build(),
        )

        // 5. Related Videos button (rightmost in notification / lock screen)
        val relatedIcon = R.drawable.ic_notif_related
        list.add(
            CommandButton.Builder()
                .setSessionCommand(SessionCommand(ACTION_SHOW_RELATED, Bundle.EMPTY))
                .setIconResId(relatedIcon)
                .setCustomIconResId(relatedIcon)
                .setDisplayName("Related Videos")
                .setEnabled(true)
                .build(),
        )

        return list.build()
    }

    override fun addNotificationActions(
        mediaSession: MediaSession,
        mediaButtons: ImmutableList<CommandButton>,
        builder: NotificationCompat.Builder,
        actionFactory: MediaNotification.ActionFactory,
    ): IntArray {
        builder.setColor(COLOR_PRIMARY_RED)
        builder.setColorized(true)
        builder.setOngoing(true)
        builder.setSmallIcon(R.drawable.ic_notification_loxtube)

        super.addNotificationActions(mediaSession, mediaButtons, builder, actionFactory)

        // Compact view shows: Previous (1), Play/Pause (2), Next (3)
        return intArrayOf(1, 2, 3)
    }
}
