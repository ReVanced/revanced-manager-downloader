@file:Suppress("Unused")

package app.revanced.manager.downloaders.charm

import app.revanced.manager.downloader.DownloadUrl
import app.revanced.manager.downloader.Downloader
import app.revanced.manager.downloader.download
import app.revanced.manager.downloader.webview.runWebView

import app.revanced.manager.downloaders.R
import app.revanced.manager.downloaders.shared.Merger

import java.nio.file.Files
import java.util.UUID
import java.util.zip.ZipFile
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

@OptIn(ExperimentalPathApi::class)
val CharmDownloader = Downloader(R.string.charm) {

    get { packageName, version ->
        runWebView("Charm") {
            download { url, _, userAgent ->
                finish(
                    DownloadUrl(
                        url,
                        mapOf("User-Agent" to userAgent)
                    )
                )
            }
            "https://charm-cat.github.io/#$packageName"
        } to version
    }

    download { downloadUrl, outputStream ->
        val workingDir = Files.createTempDirectory("charm_dl")
        try {
            val downloadedFile = workingDir.resolve(UUID.randomUUID().toString())
            val (inputStream, size) = downloadUrl.toDownloadResult()

            downloadedFile.outputStream().use { output ->
                inputStream.use { stream ->
                    if (size != null) reportSize(size)
                    stream.copyTo(output, bufferSize = 64 * 1024)
                }
            }

            var isBundle = false
            try {
                ZipFile(downloadedFile.toFile()).use { zip ->
                    isBundle = zip.getEntry("AndroidManifest.xml") == null
                }
            } catch (e: Exception) {
            }

            if (isBundle) {
                val xapkWorkingDir = workingDir.resolve("xapk").also { it.toFile().mkdirs() }

                ZipFile(downloadedFile.toFile()).use { zip ->
                    zip.entries().asSequence().forEach { entry ->
                        if (!entry.isDirectory && entry.name.endsWith(".apk")) {
                            val outputFile = xapkWorkingDir.resolve(entry.name)
                            
                            outputFile.parent.toFile().mkdirs()

                            zip.getInputStream(entry).use { input ->
                                Files.copy(input, outputFile)
                            }
                        }
                    }
                }
                
                Merger.merge(xapkWorkingDir).writeApk(outputStream)

            } else {
                downloadedFile.inputStream().use { stream ->
                    stream.copyTo(outputStream, bufferSize = 64 * 1024)
                }
            }
        } finally {
            workingDir.deleteRecursively()
        }
    }
}