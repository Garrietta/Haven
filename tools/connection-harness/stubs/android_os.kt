@file:Suppress("unused", "UNUSED_PARAMETER")
package android.os
class Looper { companion object { @JvmStatic fun getMainLooper(): Looper = Looper() } }
class Handler(looper: Looper) { fun post(r: Runnable): Boolean = true }
