package app.revanced.manager.downloaders.play.store

import android.os.Parcelable
import android.util.Log
import app.revanced.manager.downloader.*
import app.revanced.manager.downloaders.shared.Merger
import app.revanced.manager.downloaders.R
import app.revanced.manager.downloaders.play.store.data.Credentials
import app.revanced.manager.downloaders.play.store.data.Http
import app.revanced.manager.downloaders.play.store.service.CredentialProviderService
import app.revanced.manager.downloaders.play.store.ui.AuthActivity
import com.aurora.gplayapi.data.models.File as GPlayFile
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.gplayapi.helpers.PurchaseHelper
import io.ktor.client.request.url
import kotlinx.parcelize.Parcelize
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.Properties
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.outputStream

private val allowedFileTypes = arrayOf(GPlayFile.FileType.BASE, GPlayFile.FileType.SPLIT)
const val LOG_TAG = "PlayStorePlugin"

@Parcelize
class GPlayApp(
    val files: List<GPlayFile>
) : Parcelable

@Suppress("Unused")
@OptIn(ExperimentalPathApi::class)
val playStoreDownloader = Downloader(R.string.play_store) {
    get { packageName, version ->
        val (credentials, deviceProps) = useService<CredentialProviderService, Pair<Credentials, Properties>> { binder ->
            val credentialProvider = ICredentialProvider.Stub.asInterface(binder)
            val props = credentialProvider.properties.value
            credentialProvider.retrieveCredentials()?.let { return@useService it to props }

            try {
                requestStartActivity<AuthActivity>()
            } catch (e: UserInteractionException.Activity.NotCompleted) {
                if (e.resultCode == AuthActivity.RESULT_FAILED) throw Exception(
                    "Login failed: ${
                        e.intent?.getStringExtra(
                            AuthActivity.FAILURE_MESSAGE_KEY
                        )
                    }"
                )
                throw e
            }

            credentialProvider.retrieveCredentials()?.let { it to props }
                ?: throw Exception("Could not get credentials")
        }
        val authData = credentials.toAuthData(deviceProps)

        val app = try {
            AppDetailsHelper(authData).using(Http).getAppByPackageName(packageName)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Got exception while trying to get app", e)
            null
        }?.takeUnless { version != null && it.versionName != version } ?: return@get null
        if (!app.isFree) return@get null

        GPlayApp(
            app.fileList.filterNot { it.url.isBlank() }.ifEmpty {
                PurchaseHelper(authData).using(Http)
                    .purchase(
                        app.packageName,
                        app.versionCode,
                        app.offerType
                    )
            }
        ) to app.versionName
    }

    download { app, outputStream ->
        val apkDir = Files.createTempDirectory("play_dl")
        try {
            if (app.files.isEmpty()) error("No valid files to download")
            app.files.forEach { file ->
                if (file.type !in allowedFileTypes) error("${file.name} could not be downloaded because it has an unsupported type: ${file.type.name}")
                apkDir.resolve(file.name).outputStream(StandardOpenOption.CREATE_NEW)
                    .use { stream ->
                        Http.download(stream) {
                            url(file.url)
                        }
                    }
            }

            val apkFiles = apkDir.listDirectoryEntries()
            if (apkFiles.size == 1)
                Files.copy(apkFiles.first(), outputStream)
            else
                Merger.merge(apkDir)
                    .writeApk(outputStream)

        } finally {
            apkDir.deleteRecursively()
        }
    }
}