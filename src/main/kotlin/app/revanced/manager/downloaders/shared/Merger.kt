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

private object ArscLogger : APKLogger {
    const val TAG = "ARSCLib"

    override fun logMessage(msg: String) {
        if (msg.startsWith("Merging") || msg.startsWith("Writing") || msg.startsWith("Found")) {
            Log.i(TAG, msg)
        }
    }

    override fun logError(msg: String, tr: Throwable?) {
        Log.e(TAG, msg, tr)
    }

    override fun logVerbose(msg: String) {
    }
}

class MergedApkWrapper(
    private val apkDir: Path, 
    private val bundle: ApkBundle, 
    private val merged: ApkModule
) {
    suspend fun writeApk(outputStream: OutputStream) {
        withContext(Dispatchers.IO) {
            val tempApk = apkDir.resolve("temp_merged.apk").toFile()
            
            try {
                merged.writeApk(tempApk)
            } finally {
                try { merged.close() } catch (e: Exception) {}
                try { bundle.modules.forEach { it.close() } } catch (e: Exception) {}
                
                System.gc() 
            }

            tempApk.inputStream().use { input ->
                input.copyTo(outputStream, bufferSize = 128 * 1024)
            }
        }
    }
}

class Merger {
    companion object Factory {
        suspend fun merge(apkDir: Path): MergedApkWrapper {
            val localBundle = ApkBundle()
            val merged = withContext(Dispatchers.Default) {
                localBundle.setAPKLogger(ArscLogger)
                localBundle.loadApkDirectory(apkDir.toFile())
                localBundle.mergeModules()
            }

            merged.androidManifest.apply {
                arrayOf(
                    AndroidManifest.ID_isSplitRequired,
                    AndroidManifest.ID_extractNativeLibs
                ).forEach {
                    applicationElement.removeAttributesWithId(it)
                    manifestElement.removeAttributesWithId(it)
                }

                arrayOf(
                    AndroidManifest.NAME_requiredSplitTypes,
                    AndroidManifest.NAME_splitTypes
                ).forEach {
                    manifestElement.removeAttributeIf { attribute -> attribute.name == it }
                }

                val pattern = "^com\\.android\\.(stamp|vending)\\.".toRegex()
                applicationElement.removeElementsIf { element ->
                    if (element.name != AndroidManifest.TAG_meta_data) return@removeElementsIf false
                    val nameAttr =
                        element.getAttributes { it.nameId == AndroidManifest.ID_name }
                            .asSequence().single()

                    pattern.containsMatchIn(nameAttr.valueString)
                }

                refresh()
            }

            return MergedApkWrapper(apkDir, localBundle, merged)
        }
    }
}