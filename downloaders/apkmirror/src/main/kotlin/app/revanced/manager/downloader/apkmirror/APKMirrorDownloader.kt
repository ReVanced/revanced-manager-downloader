@file:Suppress("Unused")

package app.revanced.manager.downloader.apkmirror

import android.net.Uri
import app.revanced.manager.downloader.DownloadUrl
import app.revanced.manager.downloader.Downloader
import app.revanced.manager.downloader.download
import app.revanced.manager.downloader.webview.runWebView
import app.revanced.manager.downloader.shared.Merger
import java.net.URI
import java.nio.file.Files
import java.util.UUID
import java.util.zip.ZipFile
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.outputStream

@OptIn(ExperimentalPathApi::class)
val ApkMirrorDownloader = Downloader(R.string.app_name) {
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
                .appendQueryParameter("bundles%5B%5D" /* bundles[] */, "apk_files")
                .toString()
        } to version
    }

    download { downloadUrl, outputStream ->
        val workingDir = Files.createTempDirectory("apkmirror_dl")
        try {
            if (URI(downloadUrl.url).path.substringAfterLast('/').endsWith(".apk")) {
                val (inputStream, size) = downloadUrl.toDownloadResult()
                inputStream.use {
                    if (size != null) reportSize(size)
                    it.copyTo(outputStream)
                }
            } else {
                val downloadedFile = workingDir.resolve(UUID.randomUUID().toString()).also {
                    it.outputStream().use { output ->
                        downloadUrl.toDownloadResult().first.copyTo(output)
                    }
                }
                val xapkWorkingDir = workingDir.resolve("xapk").also { it.toFile().mkdirs() }

                ZipFile(downloadedFile.toString()).use { zip ->
                    zip.entries().asSequence().forEach { entry ->
                        xapkWorkingDir.resolve(entry.name).also { it.parent.toFile().mkdirs() }.also { outputFile ->
                            zip.getInputStream(entry).use { input ->
                                Files.copy(input, outputFile)
                            }
                        }
                    }
                }

                Merger.merge(xapkWorkingDir).writeApk(outputStream)
            }
        } finally {
            workingDir.deleteRecursively()
        }
    }
}