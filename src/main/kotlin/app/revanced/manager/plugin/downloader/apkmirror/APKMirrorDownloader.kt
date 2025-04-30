@file:Suppress("Unused")

package app.revanced.manager.plugin.downloader.apkmirror

import android.net.Uri
import app.revanced.manager.plugin.downloader.webview.WebViewDownloader

val apkMirrorDownloader = WebViewDownloader { packageName, version ->
    Uri.Builder()
        .scheme("https")
        .authority("www.apkmirror.com")
        .appendQueryParameter("post_type", "app_release")
        .appendQueryParameter("searchtype", "apk")
        .appendQueryParameter("s", version?.let { "$packageName $it" } ?: packageName)
        .appendQueryParameter("bundles%5B%5D" /* bundles[] */, "apk_files")
        .toString()
}   
