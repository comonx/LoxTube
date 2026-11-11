package io.github.aedev.flow.data.feed

import android.util.Log
import io.github.aedev.flow.data.model.Video
import io.github.aedev.flow.innertube.InnerTube
import io.github.aedev.flow.innertube.models.YouTubeClient
import io.github.aedev.flow.innertube.pages.channel.channelItemContinuation
import io.github.aedev.flow.innertube.pages.channel.gridItemLists
import io.github.aedev.flow.innertube.pages.renderer.FeedItem
import io.github.aedev.flow.innertube.pages.renderer.FeedItemOwner
import io.github.aedev.flow.innertube.pages.renderer.toFeedItem
import io.github.aedev.flow.innertube.pages.renderer.toFeedShelves
import io.ktor.client.call.body
import kotlinx.serialization.json.JsonElement
import javax.inject.Inject
import javax.inject.Singleton

data class LoxTubeFeedResult(
    val videos: List<Video>,
    val continuationToken: String?,
)

/**
 * Secondary official YouTube Browse Feed Engine (LoxTube Engine).
 *
 * Fetches YouTube's official recommendation and trending browse feeds (FEwhat_to_watch / default /
 * FEtrending) directly via InnerTube, exactly as LoxTube's BrowseService2 does.
 * This runs in parallel with Flow's Neuro personalized recommendation engine, giving users a
 * rich, hybrid home feed with maximum diversity and immediate content availability.
 */
@Singleton
class LoxTubeFeedSource
    @Inject
    constructor() {
        private val innerTube = InnerTube()

        companion object {
            private const val TAG = "LoxTubeFeedSource"
            private const val BROWSE_WHAT_TO_WATCH = "FEwhat_to_watch"
            private const val BROWSE_TRENDING = "FEtrending"
            private const val TRENDING_PARAMS = "6gQJRkVleHBsb3Jl"
        }

        suspend fun fetchOfficialHomeFeed(continuation: String? = null): LoxTubeFeedResult =
            try {
                val response =
                    if (continuation != null) {
                        innerTube.browse(
                            client = YouTubeClient.WEB,
                            continuation = continuation,
                        ).body<JsonElement>()
                    } else {
                        innerTube.browse(
                            client = YouTubeClient.WEB,
                            browseId = BROWSE_WHAT_TO_WATCH,
                        ).body<JsonElement>()
                    }

                val owner = FeedItemOwner()
                val videos = mutableListOf<Video>()

                // 1. Grid items (richGridRenderer, richItemRenderer, videoRenderer, lockupViewModel)
                val gridItems =
                    response.gridItemLists()
                        .flatMap { list -> list.mapNotNull { it.toFeedItem(owner) } }

                gridItems.forEach { item ->
                    if (item is FeedItem.VideoItem && item.video.id.isNotBlank()) {
                        videos.add(item.video)
                    }
                }

                // 2. Shelf sections (horizontal carousels or sections if present)
                val shelves = response.toFeedShelves(owner)
                shelves.forEach { shelf ->
                    shelf.items.forEach { item ->
                        if (item is FeedItem.VideoItem && item.video.id.isNotBlank()) {
                            videos.add(item.video)
                        }
                    }
                }

                val distinctVideos = videos.distinctBy { it.id }
                val nextToken = response.channelItemContinuation()

                Log.d(TAG, "LoxTube official browse fetched ${distinctVideos.size} videos (token=$nextToken)")
                if (distinctVideos.isEmpty() && continuation == null) {
                    fetchTrendingFallback()
                } else {
                    LoxTubeFeedResult(videos = distinctVideos, continuationToken = nextToken)
                }
            } catch (e: Exception) {
                Log.w(TAG, "LoxTube official browse request failed, trying trending fallback", e)
                fetchTrendingFallback()
            }

        private suspend fun fetchTrendingFallback(): LoxTubeFeedResult =
            runCatching {
                val response =
                    innerTube.browse(
                        client = YouTubeClient.WEB,
                        browseId = BROWSE_TRENDING,
                        params = TRENDING_PARAMS,
                    ).body<JsonElement>()

                val owner = FeedItemOwner()
                val gridItems =
                    response.gridItemLists()
                        .flatMap { list -> list.mapNotNull { it.toFeedItem(owner) } }

                val videos =
                    gridItems.mapNotNull { item ->
                        if (item is FeedItem.VideoItem && item.video.id.isNotBlank()) item.video else null
                    }.distinctBy { it.id }

                val nextToken = response.channelItemContinuation()
                Log.d(TAG, "LoxTube trending fallback fetched ${videos.size} videos")
                LoxTubeFeedResult(videos = videos, continuationToken = nextToken)
            }.getOrElse {
                Log.w(TAG, "LoxTube trending fallback also failed", it)
                LoxTubeFeedResult(videos = emptyList(), continuationToken = null)
            }
    }
