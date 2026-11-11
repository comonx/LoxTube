package io.github.aedev.flow.player.floating

import android.widget.ImageView
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object FloatingImageLoader {
    private val scope = CoroutineScope(Dispatchers.Main)

    @JvmStatic
    fun loadThumbnail(imageView: ImageView, url: String?) {
        if (url.isNullOrBlank()) {
            imageView.setImageDrawable(null)
            return
        }
        val context = imageView.context.applicationContext
        imageView.tag = url
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    val request = ImageRequest.Builder(context)
                        .data(url)
                        .size(320)
                        .build()
                    val result = SingletonImageLoader.get(context).execute(request)
                    if (result is SuccessResult) {
                        result.image.toBitmap()
                    } else null
                } catch (e: Exception) {
                    null
                }
            }
            if (imageView.tag == url && bitmap != null) {
                imageView.setImageBitmap(bitmap)
            }
        }
    }
}
