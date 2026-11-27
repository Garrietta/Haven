@file:Suppress("unused", "UNUSED_PARAMETER")
package androidx.media3.exoplayer.source
interface MediaSource
class DefaultMediaSourceFactory(f: androidx.media3.datasource.DataSource.Factory) { fun createMediaSource(i: androidx.media3.common.MediaItem): MediaSource = throw Error() }
