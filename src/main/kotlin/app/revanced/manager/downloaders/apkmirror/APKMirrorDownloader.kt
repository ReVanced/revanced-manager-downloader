@file:Suppress("Unused")

package app.revanced.manager.downloaders.apkmirror

import android.net.Uri
import app.revanced.manager.downloader.DownloadUrl
import app.revanced.manager.downloader.Downloader
import app.revanced.manager.downloader.download
import app.revanced.manager.downloader.webview.runWebView
import app.revanced.manager.downloaders.R
import app.revanced.manager.downloaders.shared.Merger
import java.net.URI
import java.nio.file.Files
import java.util.UUID
import java.util.zip.ZipFile
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.outputStream

@OptIn(ExperimentalPathApi::class)
val ApkMirrorDownloader = Downloader(R.string.apkmirror) {
    get { packageName, version ->
        runWebView("APKMirror") {
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
                .appendQueryParameter("bundles%5B%5D", "apk_files")
                .toString()
        } to version
    }

    download { downloadUrl, outputStream ->
        val workingPath = Files.createTempDirectory("apkmirror_dl")

        try {
            val isApk = URI(downloadUrl.url).path.substringAfterLast('/').endsWith(".apk")
            if (isApk) {
                val (inputStream, _) = downloadUrl.toDownloadResult()
                inputStream.use { stream ->
                    stream.copyTo(outputStream, 128 * 1024)
                }
            } else {
                val downloadedZipPath = workingPath.resolve(UUID.randomUUID().toString())
                
                downloadedZipPath.outputStream().use { output ->
                    val (inputStream, _) = downloadUrl.toDownloadResult()
                    inputStream.use { stream ->
                        stream.copyTo(output, 128 * 1024)
                    }
                }

                val xapkWorkingPath = workingPath.resolve("xapk").also { it.toFile().mkdirs() }

                ZipFile(downloadedZipPath.toFile()).use { zip ->
                    zip.entries().asSequence()
                        .filter { !it.isDirectory && it.name.endsWith(".apk") }
                        .forEach { entry ->
                            xapkWorkingPath.resolve(entry.name).also { it.parent.toFile().mkdirs() }.let { extractedApkPath ->
                                zip.getInputStream(entry).use { input -> 
                                    Files.copy(input, extractedApkPath) 
                                }
                            }
                        }
                }

                Merger.mergeAndWrite(xapkWorkingPath, outputStream)
            }
        } finally {
            runCatching { workingPath.deleteRecursively() }
        }
    }
}