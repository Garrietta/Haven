@file:Suppress("unused", "UNUSED_PARAMETER")
package androidx.media3.exoplayer.hls
class HlsMediaSource { class Factory(f: androidx.media3.datasource.DataSource.Factory) { fun setAllowChunklessPreparation(b: Boolean): Factory = this; fun createMediaSource(i: androidx.media3.common.MediaItem): androidx.media3.exoplayer.source.MediaSource = throw Error() } }
