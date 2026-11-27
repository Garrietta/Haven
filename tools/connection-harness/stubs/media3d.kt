@file:Suppress("unused", "UNUSED_PARAMETER")
package androidx.media3.exoplayer
interface LoadControl
class DefaultLoadControl : LoadControl {
    class Builder { fun setBufferDurationsMs(a: Int, b: Int, c: Int, d: Int): Builder = this; fun build(): DefaultLoadControl = DefaultLoadControl() }
}
interface ExoPlayer {
    fun setMediaSource(s: androidx.media3.exoplayer.source.MediaSource)
    fun prepare()
    var playWhenReady: Boolean
    class Builder(c: android.content.Context) { fun setLoadControl(l: LoadControl): Builder = this; fun build(): ExoPlayer = throw Error() }
}
