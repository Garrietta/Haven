@file:Suppress("unused", "UNUSED_PARAMETER")
package androidx.media3.datasource.okhttp
class OkHttpDataSource {
    class Factory(callFactory: okhttp3.Call.Factory) : androidx.media3.datasource.DataSource.Factory {
        fun setDefaultRequestProperties(p: Map<String, String>): Factory = this
    }
}
