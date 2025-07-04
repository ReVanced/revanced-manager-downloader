@file:Suppress("Unused")

package app.revanced.manager.plugin.downloader.apkmirror

import android.net.Uri
import android.os.Parcelable
import android.util.Log
import app.revanced.manager.plugin.downloader.DownloadUrl
import app.revanced.manager.plugin.downloader.Downloader
import app.revanced.manager.plugin.downloader.download
import app.revanced.manager.plugin.downloader.webview.runWebView
import app.revanced.manager.plugin.utils.Merger
import com.reandroid.apk.APKLogger
import kotlinx.parcelize.Parcelize
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.util.UUID
import java.util.zip.ZipFile
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.outputStream

@Parcelize
class ApkMirrorApp(
    val downloadUrl: DownloadUrl
) : Parcelable

private object ArscLogger : APKLogger {
    const val TAG = "ARSCLib"

    override fun logMessage(msg: String) {
        Log.i(TAG, msg)
    }

    override fun logError(msg: String, tr: Throwable?) {
        Log.e(TAG, msg, tr)
    }

    override fun logVerbose(msg: String) {
        Log.v(TAG, msg)
    }
}

@OptIn(ExperimentalPathApi::class)
val ApkMirrorDownloader = Downloader<ApkMirrorApp> {
    get { packageName, version ->
        ApkMirrorApp(
            downloadUrl =  runWebView("APKMirror") {
                download { url, _, userAgent ->
                    finish(
                        DownloadUrl(
                            url,
                            mapOf("User-Agent" to userAgent)
                        )
                    )
                }

                Uri.Builder()
                    .scheme("https")
                    .authority("www.apkmirror.com")
                    .appendQueryParameter("post_type", "app_release")
                    .appendQueryParameter("searchtype", "apk")
                    .appendQueryParameter("s", version?.let { "$packageName $it" } ?: packageName)
                    .appendQueryParameter("bundles%5B%5D" /* bundles[] */, "apk_files")
                    .toString()
            }
        ) to version
    }

    download { app, outputStream ->
        val workingDir = Files.createTempDirectory("apkmirror_dl")
        try {
            if (URI(app.downloadUrl.url).path.substringAfterLast('/').endsWith(".apk")) {
                val (inputStream, size) = app.downloadUrl.toDownloadResult()
                inputStream.use {
                    if (size != null) reportSize(size)
                    it.copyTo(outputStream)
                }
            } else {
                val downloadedFile = workingDir.resolve(UUID.randomUUID().toString()).also {
                    it.outputStream().use { output ->
                        app.downloadUrl.toDownloadResult().first.copyTo(output)
                    }
                }
                val xapkWorkingDir = workingDir.resolve("xapk").also { it.toFile().mkdirs() }

                ZipFile(downloadedFile.toString()).use { zip ->
                    zip.entries().asSequence().forEach { entry ->
                        xapkWorkingDir.resolve(entry.name).also { it.parent.toFile().mkdirs() }.also { outputFile ->
                            zip.getInputStream(entry).use { input ->
                                File(outputFile.toString()).outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                }

                Merger.merge(xapkWorkingDir, ArscLogger).writeApk(outputStream)
            }
        } finally {
            workingDir.deleteRecursively()
        }
    }
}