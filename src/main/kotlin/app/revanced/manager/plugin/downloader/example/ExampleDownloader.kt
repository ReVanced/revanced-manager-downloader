@file:Suppress("Unused")

package app.revanced.manager.plugin.downloader.example

import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import android.content.res.AssetFileDescriptor
import app.revanced.manager.plugin.downloader.*

// This file contains an example downloader implementation using the system file picker.
// Remember to update the Android manifest if you move the definition file.

val exampleDownloader = Downloader {
    get { packageName, version ->
        // Use the requestStartActivity API to open the system file picker and get the resulting content URI.
        val uri = requestStartActivity(
            Intent(Intent.ACTION_GET_CONTENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/vnd.android.package-archive")
        )?.data ?: return@get null

        println("Package name: $packageName, version: $version")

        // We assume the user has selected the correct version, but this might not be the case.
        // Real plugins should verify the version and package name if possible.
        uri to version
    }

    // Get an Android context. This is only used for reading the file that the user selected.
    // This hack should not be necessary in a real plugin.
    @SuppressLint("PrivateApi")
    val application = with(Class.forName("android.app.ActivityThread")) {
        val activityThread = getMethod("currentActivityThread")(null)
        getMethod("getApplication")(activityThread) as Application
    }
    download { uri ->
        // Open the file and return an InputStream to it along with the size.
        val fd = application.contentResolver.openAssetFileDescriptor(uri, "r")!!
        AssetFileDescriptor.AutoCloseInputStream(fd) to fd.length.takeIf { it > 0L }
    }
}