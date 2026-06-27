@file:Suppress("Unused")

package app.revanced.manager.downloaders.apkmirror

import android.net.Uri
import app.revanced.manager.downloader.DownloadUrl
import app.revanced.manager.downloader.Downloader
import app.revanced.manager.downloader.download
import app.revanced.manager.downloader.webview.runWebView
import app.revanced.manager.downloaders.R
import app.revanced.manager.downloaders.shared.Merger
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.URI
import java.nio.file.Files
import java.util.UUID
import java.util.zip.ZipFile
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.outputStream

@OptIn(ExperimentalPathApi::class, DelicateCoroutinesApi::class)
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
                .appendQueryParameter("bundles%5B%5D" /* bundles[] */, "apk_files")
                .toString()
        } to version
    }

    download { downloadUrl, outputStream ->
        val workingDir = Files.createTempDirectory("apkmirror_dl")
        try {
            if (URI(downloadUrl.url).path.substringAfterLast('/').endsWith(".apk")) {
                val (inputStream, size) = downloadUrl.toDownloadResult()
                inputStream.use { stream ->
                    if (size != null) reportSize(size)
                    val buffer = ByteArray(64 * 1024)
                    var bytes = stream.read(buffer)
                    while (bytes >= 0) {
                        outputStream.write(buffer, 0, bytes)
                        bytes = stream.read(buffer)
                    }
                }
            } else {
                val downloadedFile = workingDir.resolve(UUID.randomUUID().toString()).also { file ->
                    file.outputStream().use { output ->
                        val (inputStream, size) = downloadUrl.toDownloadResult()
                        inputStream.use { stream ->
                            if (size != null) reportSize(size)
                            val buffer = ByteArray(64 * 1024)
                            var bytes = stream.read(buffer)
                            while (bytes >= 0) {
                                output.write(buffer, 0, bytes)
                                bytes = stream.read(buffer)
                            }
                        }
                    }
                }
                val xapkWorkingDir = workingDir.resolve("xapk").also { it.toFile().mkdirs() }

                ZipFile(downloadedFile.toFile()).use { zip ->
                    val rawApkEntries = zip.entries().asSequence()
                        .filter { !it.isDirectory && it.name.endsWith(".apk") }
                        .toList()

                    for (entry in rawApkEntries) {
                        val outputFile = xapkWorkingDir.resolve(entry.name)
                        outputFile.parent.toFile().mkdirs()
                        zip.getInputStream(entry).use { input -> Files.copy(input, outputFile) }
                    }
                }

                Merger.merge(xapkWorkingDir).writeApk(outputStream)
            }
        } finally {
            GlobalScope.launch(Dispatchers.IO) {
                try { workingDir.deleteRecursively() } catch (e: Exception) {}
            }
        }
    }
}