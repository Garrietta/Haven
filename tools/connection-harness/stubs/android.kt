@file:Suppress("unused", "UNUSED_PARAMETER")
package android.graphics
class Bitmap {
    enum class CompressFormat { JPEG, PNG }
    val width: Int = 0
    val height: Int = 0
    fun compress(format: CompressFormat, quality: Int, stream: java.io.OutputStream): Boolean = true
}
object BitmapFactory {
    class Options {
        @JvmField var inJustDecodeBounds: Boolean = false
        @JvmField var inSampleSize: Int = 1
        @JvmField var outWidth: Int = 0
        @JvmField var outHeight: Int = 0
    }
    @JvmStatic fun decodeByteArray(data: ByteArray, offset: Int, length: Int): Bitmap? = null
    @JvmStatic fun decodeByteArray(data: ByteArray, offset: Int, length: Int, opts: Options?): Bitmap? = null
    @JvmStatic fun decodeStream(s: java.io.InputStream?): Bitmap? = null
    @JvmStatic fun decodeFile(path: String?): Bitmap? = null
}
