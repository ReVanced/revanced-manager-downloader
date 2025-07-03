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
import java.util.zip.ZipFile
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.outputStream

@Parcelize
class ApkMirrorApp(
    val downloadUrl: DownloadUrl,
    val packageName: String
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
            },
            packageName = packageName
        ) to version
    }

    download { app, outputStream ->
        val workingDir = Files.createTempDirectory("apkmirror_dl")
        val downloadedFile = workingDir.resolve("${app.packageName}.apk").also {
            it.outputStream().use { output ->
                app.downloadUrl.toDownloadResult().first.copyTo(output)
            }
        }

        try {
            if (URI(app.downloadUrl.url).path.substringAfterLast('/').endsWith(".apk")) {
                Files.copy(downloadedFile, outputStream)
            } else {
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