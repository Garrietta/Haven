@file:Suppress("unused", "UNUSED_PARAMETER")
package androidx.media3.common
class MediaItem {
    companion object { @JvmStatic fun fromUri(uri: String): MediaItem = MediaItem() }
    class Builder { fun setUri(u: String): Builder = this; fun setMimeType(m: String?): Builder = this; fun build(): MediaItem = MediaItem() }
}
