@file:Suppress("Unused")

package app.revanced.manager.downloader.apkmirror

import android.net.Uri
import app.revanced.manager.plugin.downloader.webview.WebViewDownloader

val apkMirrorDownloader = WebViewDownloader { packageName, version ->
    with(Uri.Builder()) {
        scheme("https")
        authority("www.apkmirror.com")
        mapOf(
            "post_type" to "app_release",
            "searchtype" to "apk",
            "s" to (version?.let { "$packageName $it" } ?: packageName),
            "bundles%5B%5D" to "apk_files" // bundles[]
        ).forEach { (key, value) ->
            appendQueryParameter(key, value)
        }

        build().toString()
    }
}
