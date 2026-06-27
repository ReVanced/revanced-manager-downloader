package app.revanced.manager.downloaders.shared

import android.util.Log
import com.reandroid.apk.APKLogger
import com.reandroid.apk.ApkBundle
import com.reandroid.apk.ApkModule
import com.reandroid.app.AndroidManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.nio.file.Path
import kotlin.io.path.inputStream

private object ArscLogger : APKLogger {
    const val TAG = "ARSCLib"

    override fun logMessage(msg: String) {
        Log.i(TAG, msg)
    }

    override fun logError(msg: String, tr: Throwable?) {
        Log.e(TAG, msg, tr)
    }

    override fun logVerbose(msg: String) {}
}

object Merger {
    suspend fun mergeAndWrite(apkDir: Path, outputStream: OutputStream) {
        val tempApkPath = withContext(Dispatchers.Default) {
            val bundle = ApkBundle()
            var merged: ApkModule? = null
            
            try {
                bundle.setAPKLogger(ArscLogger)
                bundle.loadApkDirectory(apkDir.toFile())
                merged = bundle.mergeModules()

                merged.androidManifest.apply {
                    listOf(
                        AndroidManifest.ID_isSplitRequired,
                        AndroidManifest.ID_extractNativeLibs
                    ).forEach { id ->
                        applicationElement.removeAttributesWithId(id)
                        manifestElement.removeAttributesWithId(id)
                    }

                    listOf(
                        AndroidManifest.NAME_requiredSplitTypes,
                        AndroidManifest.NAME_splitTypes
                    ).forEach { name ->
                        manifestElement.removeAttributeIf { it.name == name }
                    }

                    val vendingPattern = Regex("^com\\.android\\.(stamp|vending)\\.")
                    applicationElement.removeElementsIf { element ->
                        if (element.name != AndroidManifest.TAG_meta_data) return@removeElementsIf false
                        
                        val nameAttr = element.getAttributes { it.nameId == AndroidManifest.ID_name }
                            .asSequence()
                            .singleOrNull()
                            
                        nameAttr?.valueString?.let { vendingPattern.containsMatchIn(it) } == true
                    }

                    refresh()
                }

                val tempApkFile = apkDir.resolve("temp_merged.apk").toFile()
                merged.writeApk(tempApkFile)
                
                tempApkFile.toPath()
            } finally {
                runCatching { merged?.close() }
                    .onFailure { Log.e("Merger", "Failed to close merged module", it) }
                runCatching { bundle.modules.forEach { it.close() } }
                    .onFailure { Log.e("Merger", "Failed to close bundle modules", it) }
            }
        }

        withContext(Dispatchers.IO) {
            tempApkPath.inputStream().use { input ->
                input.copyTo(outputStream, bufferSize = 128 * 1024)
            }
        }
    }
}